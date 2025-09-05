package org.entermediadb.ai.assistant;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.ai.semantics.SemanticIndexManager;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.find.ResultsManager;
import org.entermediadb.scripts.ScriptLogger;
import org.entermediadb.websocket.chat.ChatServer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.PropertyDetail;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.profile.UserProfile;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;

public class AssistantManager extends BaseAiManager
{
	private static final Log log = LogFactory.getLog(AssistantManager.class);
	
	public ResultsManager getResultsManager() {
		ResultsManager resultsManager = (ResultsManager) getMediaArchive().getBean("resultsManager");
		return resultsManager;
	}
	
	public void monitorChannels(ScriptLogger inLog) throws Exception
	{
		MediaArchive archive = getMediaArchive();
		User agent = archive.getUser("agent");
		//TODO:  REmove after a while, we checked in one for new installs
		if (agent == null)
		{
			agent = archive.getUserManager().createUser("agent", null);
			agent.setFirstName("eMediaFinder");
			agent.setLastName("AI Helper");
			agent.setValue("screenname", "eMediaFinder AI Helper");
			archive.getUserManager().saveUser(agent);
			archive.getUserProfileManager().setRoleOnUser(archive.getCatalogId(), agent, "guest");
		}
		
		Searcher channels = archive.getSearcher("channel");
		
		//TODO: How Do I know if this is still active?
		
		Calendar now = DateStorageUtil.getStorageUtil().createCalendar();
		now.add(Calendar.HOUR_OF_DAY,-1);
		
		//TODO: Only process one "open" channel at a time. What ever the last one they clicked on
		
		HitTracker allchannels = channels.query().exact("aienabled", true).after("refreshdate",now.getTime()).sort("refreshdateDown").search();

		Searcher chats = archive.getSearcher("chatterbox");
		for (Iterator iterator = allchannels.iterator(); iterator.hasNext();)
		{
			Data channel = (Data) iterator.next();
			
			Data mostrecent = chats.query()
								   .exact("channel", channel.getId())
								   .exact("processingcomplete","false")
								   .sort("dateDown")
								   .searchOne();
			
			if (mostrecent  == null)
			{
				continue;
			}
			
			if( channel.getName() == null )
			{
				String message = mostrecent.get("message");
				if( message !=  null )
				{
					if( message.length() > 25)
					{
						message = message.substring(0,25);
					}
					channel.setName(message);
					archive.saveData("channel",channel);
				}
			}

			String userid = mostrecent.get("user");
			if ("agent".equals(userid))
			{
				return;
			}
			
			respondToChannel(inLog, channel, mostrecent);
		}
	}
	
	public LlmConnection getLlmConnection()
	{
		String model = getMediaArchive().getCatalogSettingValue("gpt-model");
		if (model == null)
		{
			model = "gpt-4o"; // Default fallback
		}
		LlmConnection manager = getMediaArchive().getLlmConnection(model);
		return manager;
	}
	
	public void respondToChannel(ScriptLogger inLog, Data channel, Data message) throws Exception
	{
		MediaArchive archive = getMediaArchive();
		
		Map params = new HashMap();
		
		String model = archive.getCatalogSettingValue("chat_agent_model");
		
		if (model == null)
		{
			model = "gpt-4o"; // Default fallback
		}
		LlmConnection llmconnection = archive.getLlmConnection(model);

		params.put("model", model);

		Date now = new Date();
		DateFormat fm = DateStorageUtil.getStorageUtil().getDateFormat("dd/MM/yyyy hh:mm");

		ChatServer server = (ChatServer) archive.getBean("chatServer");
		Searcher chats = archive.getSearcher("chatterbox");
		
		if (!llmconnection.isReady()) 
		{
			inLog.error("LLM Manager is not ready: " + model + ". Cannot process channel: " + channel);
			return;
		}

		String channeltype = channel.get("channeltype");
		if (channeltype == null)
		{
			channeltype = "chatstreamer";
		}
		
		String id = channel.get("user");
		UserProfile profile = archive.getUserProfile(id);
		params.put("chatprofile", profile);
		
		
		params.put("channel", channel);

		//Update original message processing status
		message.setValue("processingcomplete", true);
		chats.saveData(message);
		
		params.put("message", message);

///$mediaarchive.getMediaDbId()/ai/assistant/instructions/context
		String chattemplate = "/" + archive.getMediaDbId() + "/ai/assistant/instructions/current.json";
		
		params.put("assistant", this);
		
		AiCurrentStatus current = loadCurrentStatus(channel); //TODO: Update this often
		params.put("currentstatus",current);
		
		LlmResponse response = llmconnection.runPageAsInput(params, model, chattemplate);
		//current update it?
		
		if (response.isToolCall())
		{
			// Function call detected
			String functionName = response.getFunctionName();
			JSONObject arguments = response.getArguments();

			String json = arguments.toJSONString();
			// Create and save function call message
			Data functionMessage = chats.createNewData();
			functionMessage.setValue("user", "agent");
			functionMessage.setValue("channel", channel.getId());
			functionMessage.setValue("arguments", json);
			functionMessage.setValue("date", new Date());
			functionMessage.setValue("message", "Processing function " + functionName);
			functionMessage.setValue("processingcomplete", true);
			
			chats.saveData(functionMessage);
			
			server.broadcastMessage(archive.getCatalogId(), functionMessage);
			
			execChatFunction(llmconnection, functionMessage, functionName, params);
			
			
			archive.fireSharedMediaEvent("chatterbox/monitorchats");
			
			//archive.fireDataEvent(inReq.getUser(), "llm", "callfunction", functionMessage);
			archive.fireSharedMediaEvent("llm/monitorchats");

			
		}
		else
		{
			// **Regular Text Response**
			String output = response.getMessage();

			if (output != null)
			{
				Data responsemessage = chats.createNewData();
				responsemessage.setValue("user", "agent");
				responsemessage.setValue("message", output);
				responsemessage.setValue("date", new Date());
				responsemessage.setValue("channel", channel.getId());
				responsemessage.setValue("messagetype", "airesponse");
				responsemessage.setValue("processingcomplete", true);

				chats.saveData(responsemessage);
				server.broadcastMessage(archive.getCatalogId(), responsemessage);
			}
		}
		
	}
	
	public void execChatFunction(LlmConnection llmconnection, Data messageToUpdate, String functionName, Map params) throws Exception
	{

		MediaArchive archive = getMediaArchive();

		//get the channel
		Data channel = archive.getCachedData("channel", messageToUpdate.get("channel"));
		params.put("channel", channel);
		
		
		ChatServer server = (ChatServer) archive.getBean("chatServer");

		String response;
		
		try
		{
			
			params.put("data", messageToUpdate);

			String args = (String) messageToUpdate.get("arguments");
			JSONObject arguments = (JSONObject) new JSONParser().parse(args);
			params.put("arguments", arguments);
			
			response = llmconnection.loadInputFromTemplate("/" + archive.getMediaDbId() +"/ai/"+ getAiFolder() + "/responses/" + functionName + ".html", params);
			//log.info("Function " + functionName + " returned : " + response);

			messageToUpdate.setValue("functionresponse", response);
			messageToUpdate.setValue("message", response);
			
			Searcher chats = archive.getSearcher("chatterbox");
			chats.saveData(messageToUpdate);
			
			JSONObject functionMessageUpdate = new JSONObject();
			functionMessageUpdate.put("messagetype", "airesponse");
			functionMessageUpdate.put("catalogid", archive.getCatalogId());
			functionMessageUpdate.put("user", "agent");
			functionMessageUpdate.put("channel", messageToUpdate.get("channel"));
			functionMessageUpdate.put("messageid", messageToUpdate.getId());
			functionMessageUpdate.put("message", response);

			server.broadcastMessage(functionMessageUpdate);
			
		}
		catch (Exception e)
		{
			log.error(e);
			messageToUpdate.setValue("functionresponse", e.toString());
			messageToUpdate.setValue("processingcomplete", true);
			archive.saveData("chatterbox", messageToUpdate);
		}
		

	}

	protected AiCurrentStatus loadCurrentStatus(Data inChannel)
	{
		AiCurrentStatus status = (AiCurrentStatus)getMediaArchive().getCacheManager().get("aistatus", inChannel.getId() );
		if( status == null)
		{
			status = new AiCurrentStatus();
			status.setChannel(inChannel);
			status.setMediaArchive(getMediaArchive());
			status.setAssistantManager(this);
			getMediaArchive().getCacheManager().put("aistatus", inChannel.getId(),status );
		}
		return status;
	}
	
	protected AiCurrentStatus loadCurrentStatus(String inChannelId)
	{
		Data channel = getMediaArchive().getCachedData("channel", inChannelId);
		return loadCurrentStatus(channel);
	}

	public String getAiFolder()
	{
		return "assistant";
	}
	
	public HitTracker getFunctions()
	{
		HitTracker hits = getMediaArchive().query("aifunctions").exact("aifolder",getAiFolder()).sort("ordering").cachedSearch();
		return hits;
	}
	
	public AiSearch processSematicSearchArgs(JSONObject arguments, UserProfile userprofile) throws Exception
	{
		if(arguments == null)
		{			
			return null;
		} 
		else {	
			log.info("Args: " + arguments.toJSONString());
		}
		
		AiSearch searchArgs = new AiSearch();

		Collection<String> keywords = getResultsManager().parseKeywords(arguments.get("keywords")); 
		searchArgs.setKeywords(keywords);
		
		String searchType = (String) arguments.get("search_type");
		searchArgs.setBulkSearch("bulk".equals(searchType));
		
		boolean isStrict = Boolean.parseBoolean((String) arguments.get("strict"));
		searchArgs.setStrictSearch(isStrict);
		
		Object selectedModulesObj = arguments.get("targets");

		Collection<String> selectedModules = new ArrayList();
		
		if(selectedModulesObj instanceof JSONArray)
		{			
			JSONArray arr = (JSONArray) selectedModulesObj;
			selectedModules.addAll( Arrays.asList( (String[]) arr.toArray( new String[arr.size()] ) ) );
		}
		else if(selectedModulesObj instanceof String)
		{
			selectedModules.add((String) selectedModulesObj); 
		}
		
		Collection<Data> permittedModules = new ArrayList();
		
		if(selectedModules.contains("all") || selectedModules.size() == 0)
		{
			permittedModules = userprofile.getEntities();
		}
		else
		{
			permittedModules = userprofile.getEntitiesByIdOrName(selectedModules);
		}
		
		searchArgs.setSelectedModules(permittedModules);
		
		return searchArgs;
	}

	public void regularSearch(WebPageRequest inReq, boolean isMcp) throws Exception {
		
		JSONObject arguments = (JSONObject) inReq.getPageValue("arguments");
		
		if(arguments == null)
		{
			log.warn("No arguments found in request");
			return;
		}
		
		UserProfile userprofile = (UserProfile) inReq.getPageValue("chatprofile");
		
		if(userprofile == null)
		{
			userprofile = (UserProfile) inReq.getPageValue("userprofile");
		}
		
		inReq.putPageValue("userprofile", userprofile);

		AiSearch aiSearchArgs = processSematicSearchArgs(arguments, userprofile);
		
		if(isMcp)
		{
			addMcpVars(inReq, aiSearchArgs);
		}
		
		getResultsManager().searchByKeywords(inReq, aiSearchArgs);
		
	}
	
	public void semanticSearch(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive();

		String query = inReq.getRequestParameter("query");
		if (query == null)
		{
			query = (String) inReq.getPageValue("query");
		}
		if (query == null)
		{
			log.warn("No query found in request");
			return;
		}
		
		log.info("Semantic Search for: " + query);
		
		String[] excludeentityids = inReq.getRequestParameters("excludeentityids");
		if(excludeentityids == null)
		{
			excludeentityids = new String[0];
		}
		Collection<String> excludeEntityIds = Arrays.asList(excludeentityids);
		
		String[] excludeassetids = inReq.getRequestParameters("excludeassetids");
		if(excludeassetids == null)
		{
			excludeassetids = new String[0];
		}
		Collection<String> excludeAssetIds = Arrays.asList(excludeassetids);
		
		inReq.putPageValue("input", query);
		
		SemanticIndexManager semanticIndexManager = (SemanticIndexManager) archive.getBean("semanticIndexManager");
		Map<String, Collection<String>> relatedEntityIds = semanticIndexManager.searchRelatedEntities(query, excludeEntityIds, excludeAssetIds);
		
		log.info("Related Entity Ids: " + relatedEntityIds);

		Collection<Data> semanticentities = new ArrayList();
		Map<String, HitTracker> semanticentityhits = new HashMap();

		for (Iterator iterator = relatedEntityIds.keySet().iterator(); iterator.hasNext();)
		{
			String moduleid = (String) iterator.next();
			
			Data module = archive.getCachedData("module", moduleid);
			
			Collection<String> ids = relatedEntityIds.get(moduleid);
			
			HitTracker entites = getMediaArchive().query(moduleid).ids(ids).search();
			
			
			if(entites == null || entites.size() == 0)
			{
				continue;
			}
			
			semanticentities.add(module);
			
			if(moduleid.equals("asset"))
			{
				inReq.putPageValue("semanticassethits", entites);
			}
			else
			{				
				semanticentityhits.put(moduleid, entites);
			}
		}
		
		inReq.putPageValue("semanticentities", semanticentities);
		inReq.putPageValue("semanticentityhits", semanticentityhits);
		
	}
	
	public void addMcpVars(WebPageRequest inReq, AiSearch searchArgs)	
	{
		Collection<String> keywords = searchArgs.getKeywords();
		inReq.putPageValue("keywordsstring", getResultsManager().joinWithAnd(keywords));
		
		Collection<Data> modules = searchArgs.getSelectedModules();
		
	
		Collection<String> moduleNames = new ArrayList<String>();
			
		for (Iterator iterator = modules.iterator(); iterator.hasNext();)
		{
			Data module = (Data) iterator.next();
			if(!moduleNames.contains(module.getName()))
			{
				moduleNames.add(module.getName());
			}
		}
		
		inReq.putPageValue("modulenamestext", getResultsManager().joinWithAnd(moduleNames));
		
	}
	
	public String generateReport(JSONObject arguments) throws Exception
	{
		Collection<String> keywords = getResultsManager().parseKeywords(arguments.get("keywords"));
		 
		MediaArchive archive = getMediaArchive();
		
		HitTracker pdfs = archive.query("asset").freeform("description", String.join(" ", keywords)).search();
		
		Collection<String> pdfTexts = new ArrayList<String>();
		
		for (Iterator iterator = pdfs.iterator(); iterator.hasNext();) {
			Data pdf = (Data) iterator.next();
			String text = (String) pdf.getValue("fulltext");
			if(text != null && text.length() > 0)
			{
				pdfTexts.add(text); 					
			}
			log.info(text);
		}

		String fullText = String.join("\n\n", pdfTexts);
		
		if(fullText.replaceAll("\\s|\\n", "").length() == 0)
		{ 
			return null;
		}

		Map params = new HashMap();
		params.put("fulltext", fullText);
		
		String model = archive.getCatalogSettingValue("mcp_model");
		if(model == null)
		{
			model = "gpt-5-nano";
		}

		LlmConnection llmconnection = (LlmConnection) archive.getBean("openaiConnection");
		
		String chattemplate = "/" + archive.getMediaDbId() + "/ai/mcp/prompts/generate_report.json";
		LlmResponse response = llmconnection.runPageAsInput(params, model, chattemplate);
		
		String report = response.getMessage();
		
		return report;
	}
	
	public Collection<PropertyDetail> getCommonFields()
	{
		Collection<PropertyDetail> fields = new ArrayList();
		PropertyDetail pd = new PropertyDetail();
		
//		Collection<String> fieldids = Arrays.from("title","description","keywords","caption","date");
		pd.setId("title");
		fields.add(pd);
		
//		fields.add("description");
//		fields.add("keywords");
//		fields.add("caption");
//		fields.add("date");
		return fields;
	}
	
}
