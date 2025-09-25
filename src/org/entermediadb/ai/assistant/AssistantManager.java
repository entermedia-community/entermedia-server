package org.entermediadb.ai.assistant;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.classify.SemanticFieldManager;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmRequest;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.find.ResultsManager;
import org.entermediadb.scripts.ScriptLogger;
import org.entermediadb.websocket.chat.ChatServer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.PropertyDetail;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.profile.UserProfile;
import org.openedit.repository.ContentItem;
import org.openedit.repository.InputStreamItem;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.JSONParser;

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
		
		HitTracker allchannels = channels.query().exact("channeltype", "agentchat").after("refreshdate",now.getTime()).sort("refreshdateDown").search();

		Searcher chats = archive.getSearcher("chatterbox");
		for (Iterator iterator = allchannels.iterator(); iterator.hasNext();)
		{
			Data channel = (Data) iterator.next();
			
			Data mostrecent = chats.query()
				   .exact("channel", channel.getId())
				   .orgroup("chatmessagestatus", "received refresh")
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
					channel.setName(message.trim());
					archive.saveData("channel",channel);
				}
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
		
		LlmRequest llmrequest = new LlmRequest();
		
		String model = archive.getCatalogSettingValue("chat_agent_model");
		
		if (model == null)
		{
			model = "gpt-4o"; // Default fallback
		}
		LlmConnection llmconnection = archive.getLlmConnection(model);

		llmrequest.addContext("model", model);

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
			channeltype = "agentchat";
		}
		
		String id = channel.get("user");
		UserProfile profile = archive.getUserProfile(id);
		llmrequest.addContext("chatprofile", profile);
		
		
		llmrequest.addContext("channel", channel);

		String oldstatus = message.get("chatmessagestatus");
		
		//Update original message processing status
		message.setValue("chatmessagestatus", "complete");
		chats.saveData(message);
		
		llmrequest.addContext("message", message);

///$mediaarchive.getMediaDbId()/ai/openai/assistant/instructions/context
		
		
		llmrequest.addContext("assistant", this);
		
		AiCurrentStatus current = loadCurrentStatus(channel); //TODO: Update this often
		llmrequest.addContext("currentstatus",current);
		
		if("refresh".equals(oldstatus))
		{			
			String firstcallparams = message.get("params");
			if(firstcallparams != null)
			{
				
				JSONObject oldparams = new JSONParser().parse(firstcallparams);

				llmrequest.setFunctionName((String) oldparams.get("function"));
				oldparams.remove("function");
				llmrequest.setParameters((JSONObject) oldparams);
	
				execChatFunction(llmconnection, message, llmrequest);
			}
			return;
		}
		
		Data functionMessage = chats.createNewData();
		functionMessage.setValue("user", "agent");
		functionMessage.setValue("channel", channel.getId());
		functionMessage.setValue("date", new Date());
		functionMessage.setValue("message", "Processing...");
		functionMessage.setValue("chatmessagestatus", "processing");
		
		chats.saveData(functionMessage);
		
		server.broadcastMessage(archive.getCatalogId(), functionMessage);

		String chattemplate = "/" + archive.getMediaDbId() + "/ai/openai/assistant/instructions/current.json";
		LlmResponse response = llmconnection.runPageAsInput(llmrequest, chattemplate);
		//current update it?
		
		if (response.isToolCall())
		{
			// Function call detected
			String functionName = response.getFunctionName();
			llmrequest.setFunctionName(functionName);
			
			String nextFunctionName = response.getNextFunctionName();
			llmrequest.setNextFunctionName(nextFunctionName);
			
			JSONObject functionArguments = response.getArguments();			
			llmrequest.setParameter("arguments", functionArguments);
			
			functionMessage.setValue("params", llmrequest.toString());
			functionMessage.setValue("message", "Executing function " + functionName);
			
			chats.saveData(functionMessage);
			
			server.broadcastMessage(archive.getCatalogId(), functionMessage);
			
			execChatFunction(llmconnection, functionMessage, llmrequest);
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

				chats.saveData(responsemessage);
				server.broadcastMessage(archive.getCatalogId(), responsemessage);
			}
		}
		
	}
	
	public void execChatFunction(LlmConnection llmconnection, Data messageToUpdate, LlmRequest llmrequest) throws Exception
	{
		MediaArchive archive = getMediaArchive();

		Data channel = archive.getCachedData("channel", messageToUpdate.get("channel"));
		llmrequest.addContext("channel", channel);
		
		ChatServer server = (ChatServer) archive.getBean("chatServer");

		try
		{
			llmrequest.addContext("data", messageToUpdate);
			
			String apphome = "/"+ channel.get("chatapplicationid");
			llmrequest.addContext("apphome", apphome);
			LlmResponse response = llmconnection.loadResponseFromTemplate(llmrequest);

			messageToUpdate.setValue("message", response.getMessage());
			messageToUpdate.setValue("chatmessagestatus", "complete");
			
			Searcher chats = archive.getSearcher("chatterbox");
			chats.saveData(messageToUpdate);
			
			JSONObject functionMessageUpdate = new JSONObject();
			functionMessageUpdate.put("messagetype", "airesponse");
			functionMessageUpdate.put("catalogid", archive.getCatalogId());
			functionMessageUpdate.put("user", "agent");
			functionMessageUpdate.put("channel", messageToUpdate.get("channel"));
			functionMessageUpdate.put("messageid", messageToUpdate.getId());
			functionMessageUpdate.put("message", response.getMessage());
			server.broadcastMessage(functionMessageUpdate);
			
			if( llmrequest.getNextFunctionName() != null)
			{
				JSONObject params = llmrequest.getParameters();
				params.put("function", llmrequest.getNextFunctionName());
				
				messageToUpdate.setValue("params", params.toJSONString());
				messageToUpdate.setValue("chatmessagestatus", "refresh");
				chats.saveData(messageToUpdate);
				
			}

			archive.fireSharedMediaEvent("llm/monitorchats");
//			else
//			{
//				messageToUpdate.setValue("chatmessagestatus", "complete");
//				chats.saveData(messageToUpdate);
//			}
			
		}
		catch (Exception e)
		{
			log.error("Could not execute function: " + llmrequest.getFunctionName(), e);
			messageToUpdate.setValue("functionresponse", e.toString());
			messageToUpdate.setValue("chatmessagestatus", "failed");
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
	
	public AiSearch processSematicSearchArgs(String inJsonArguments, UserProfile userprofile) throws Exception
	{
		if(inJsonArguments == null)
		{			
			return null;
		} 
		else {	
			log.info("Args: " + inJsonArguments);
		}

		JSONObject arguments = new JSONParser().parse( inJsonArguments );
		
		AiSearch searchArgs = new AiSearch();

		Collection<String> keywords = getResultsManager().parseKeywords(arguments.get("keywords")); 
		searchArgs.setKeywords(keywords);
		
		String searchType = (String) arguments.get("search_type");
		searchArgs.setBulkSearch("bulk".equals(searchType));
		
		Object isStrict = arguments.get("strict");
		if(isStrict instanceof Boolean)
		{
			isStrict = (boolean) isStrict;
		}
		else 
		{
			isStrict = false;
		}

		searchArgs.setStrictSearch((boolean) isStrict);
		
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
		
		//JSONObject arguments = (JSONObject) inReq.getPageValue("arguments");
		String arguments = inReq.getRequestParameter("arguments");
		
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
		
		inReq.putPageValue("semanticquery", aiSearchArgs.toSemanticQuery());

	}
	
	public void semanticSearch(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive();

		String query = inReq.getRequestParameter("semanticquery");
		
		if(query == null || "null".equals(query))
		{
			throw new OpenEditException("No query found in request");
		}
		
		log.info("Semantic Search for: " + query);
		
		String[] excludeentityids = inReq.getRequestParameters("excludeentityids");
		try
		{			
			if(excludeentityids == null)
			{
				excludeentityids = (String[]) inReq.getPageValue("excludeentityids");
			}
		}
		catch( Exception e)
		{
			log.error("Could not parse excludeentityids",e);
		}
		if(excludeentityids == null)
		{
			excludeentityids = new String[0];
		}
		Collection<String> excludeEntityIds = Arrays.asList(excludeentityids);
		
		String[] excludeassetids = inReq.getRequestParameters("excludeassetids");
		try
		{			
			if(excludeassetids == null)
			{
				excludeassetids = (String[]) inReq.getPageValue("excludeassetids");
			}
		}
		catch( Exception e)
		{
			log.error("Could not parse excludeassetids",e);
		}
		if(excludeassetids == null)
		{
			excludeassetids = new String[0];
		}
		Collection<String> excludeAssetIds = Arrays.asList(excludeassetids);
		
		inReq.putPageValue("input", query);
		
		Map<String, Collection<String>> relatedEntityIds = getSemanticTopicManager().search(query,excludeEntityIds, excludeAssetIds);
		
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

		LlmRequest llmrequest = new LlmRequest();
		llmrequest.addContext("fulltext", fullText);
		
		String model = archive.getCatalogSettingValue("mcp_model");
		if(model == null)
		{
			model = "gpt-5-nano";
		}
		llmrequest.addContext("model", model);

		LlmConnection llmconnection = (LlmConnection) archive.getBean("openaiConnection");
		
		String chattemplate = "/" + archive.getMediaDbId() + "/ai/openai/mcp/prompts/generate_report.json";
		LlmResponse response = llmconnection.runPageAsInput(llmrequest, chattemplate);
		
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
	
	public void rescanSearchCategories()
	{
		//For each search category go look for relevent records. Reset old ones?
		HitTracker tracker = getMediaArchive().query("searchcategory").exists("semantictopics").search();
		for (Iterator iterator = tracker.iterator(); iterator.hasNext();)
		{
			MultiValued searchcategory = (MultiValued) iterator.next();
			
			Map<String,Collection<String>> bytype = searchRelatedEntitiesBySearchCategory(searchcategory);
			for (Iterator iterator2 = bytype.keySet().iterator(); iterator2.hasNext();)
			{
				String moduleid = (String)iterator2.next();
				Collection<String> ids = bytype.get(moduleid);
				Collection addedentites = getMediaArchive().query(moduleid).ids(ids).not("searchcategory",searchcategory.getId()).search();
				//Collection addedentites = getMediaArchive().query(moduleid).ids(ids).search();
				Collection tosave = new ArrayList(addedentites.size());
				for (Iterator iterator3 = addedentites.iterator(); iterator3.hasNext();)
				{
					MultiValued entity = (MultiValued) iterator3.next();
					entity.addValue("searchcategory",searchcategory.getId());
					tosave.add(entity);
				}
				log.info("Added " + tosave.size() + " to category " + moduleid);
				getMediaArchive().saveData(moduleid,tosave);
			}
		}
	}
	public Map<String,Collection<String>> searchRelatedEntitiesBySearchCategory(MultiValued searchcategory)
	{
		if( searchcategory.getBoolean("semanticindexed"))
		{
			//Todo: Use the Vector DB?
		}
		String text = null;
		Collection values = searchcategory.getValues("semantictopics");
		text = collectText(values);
		if( text == null)
		{
			text = searchcategory.getName();
		}
		
		Map<String,Collection<String>> results = getSemanticTopicManager().search(text, null, null);
		return results;
	}


	protected SemanticFieldManager fieldSemanticTopicManager;
	public SemanticFieldManager getSemanticTopicManager()
	{
		if (fieldSemanticTopicManager == null)
		{
			fieldSemanticTopicManager = (SemanticFieldManager)getModuleManager().getBean(getCatalogId(),"semanticFieldManager",false);
			fieldSemanticTopicManager.setSemanticSettingId("semantictopics");
		}

		return fieldSemanticTopicManager;
	}
	
//	public void hybridSearch(WebPageRequest inReq) throws Exception {
//		
//		AiSearch aiSearchArgs = processSematicSearchArgs(arguments, userprofile);
//		
//		getResultsManager().searchByKeywords(inReq, aiSearchArgs);
//		
//		int totalhits = (int) inReq.getPageValue("totalhits");
//		if(totalhits < 5)
//		{
//			inReq.putPageValue("query", String.join(" ", aiSearchArgs.getKeywords()));
//			semanticSearch(inReq);
//		}
//		
//	}
	
	public void createImage(WebPageRequest inReq) throws Exception 
	{
		MediaArchive archive = getMediaArchive();

		String model = archive.getCatalogSettingValue("llmimagegenerationmodel");
		if (model == null)
		{
			model = "gpt-image-1";
		}
		
		LlmConnection llmconnection = archive.getLlmConnection(model);

		String arguments = inReq.getRequestParameter("arguments");
		
		if(arguments == null)
		{
			log.warn("No arguments found in request");
			return;
		}
		
		String prompt = (String) inReq.getRequestParameter("prompt");

		if (prompt == null)
		{
			return;
		}

		LlmResponse results = llmconnection.createImage(model, prompt);

		for (Iterator iterator = results.getImageBase64s().iterator(); iterator.hasNext();)
		{
			String base64 = (String) iterator.next();

			Asset asset = (Asset) archive.getAssetSearcher().createNewData();

			asset.setValue("importstatus", "created");

			String filename = prompt.replaceAll("[^a-zA-Z0-9]", "_") + ".png";
			asset.setName(filename);
			
			String sourcepath = "Channels/" + inReq.getUserName() + "/" + DateStorageUtil.getStorageUtil().getTodayForDisplay() + "/" + filename;
			asset.setSourcePath(sourcepath);

			String path = "/WEB-INF/data/" + asset.getCatalogId() + "/originals/" + asset.getSourcePath();
			ContentItem saveTo = archive.getPageManager().getPage(path).getContentItem();
			
			try
			{
				InputStreamItem revision = new InputStreamItem();
				
				revision.setAbsolutePath(saveTo.getAbsolutePath());
				revision.setPath(saveTo.getPath());
				revision.setAuthor( inReq.getUserName() );
				revision.setType( ContentItem.TYPE_ADDED );
				revision.setMessage( saveTo.getMessage());
				
				revision.setPreviewImage(saveTo.getPreviewImage());
				revision.setMakeVersion(false);
				
				log.info("Saving image -> " + path + "/" + filename);
				
				InputStream input = null;
				
				String code = base64.substring(base64.indexOf(",") +1, base64.length());
				byte[] tosave = Base64.getDecoder().decode(code);
				input = new ByteArrayInputStream(tosave);
				
				revision.setInputStream(input);
				
				archive.getPageManager().getRepository().put( revision );
				asset.setProperty("importstatus", "created");
				archive.saveAsset(asset);
			}
			catch (Exception ex)
			{
				asset.setProperty("importstatus", "error");
				log.error(ex);
				archive.saveAsset(asset);
			}
			
			inReq.putPageValue("asset", asset);
		}
		

		archive.fireSharedMediaEvent("importing/assetscreated");
	}
}
