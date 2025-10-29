package org.entermediadb.ai.assistant;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.classify.SemanticCassifier;
import org.entermediadb.ai.informatics.SemanticTableManager;
import org.entermediadb.ai.knn.RankedResult;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmRequest;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.ai.llm.emedia.EMediaAIResponse;
import org.entermediadb.ai.llm.openai.OpenAiConnection;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.find.EntityManager;
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
import org.openedit.data.QueryBuilder;
import org.openedit.data.Searcher;
import org.openedit.hittracker.Highlighter;
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
	
	protected EntityManager getEntityManager() 
	{
		return getMediaArchive().getEntityManager();
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
			if( channel.getName() == null)
			{
				Data lastusermessage = chats.query()
						.exact("channel", channel.getId())
						.not("user", "agent")
						.sort("dateDown")
						.searchOne();
				
				if( lastusermessage != null)
				{
					String message = lastusermessage.get("message");
					if( message !=  null )
					{
						if( message.length() > 25)
						{
							message = message.substring(0,25);
						}
						channel.setName(message.trim());
						archive.saveData("channel", channel);
					}
				}
				
			}
			
			Collection mostrecents = chats.query()
				   .exact("channel", channel.getId())
				   .orgroup("chatmessagestatus", "received refresh")
				   .sort("dateDown")
				   .search();
			
			if (mostrecents  == null)
			{
				continue;
			}

			for (Iterator iterator2 = mostrecents.iterator(); iterator2.hasNext();) {
				Data mostrecent = (Data) iterator2.next();
				respondToChannel(inLog, channel, mostrecent);
			}
			
		}
	}
	
	public LlmConnection getLlmConnection()
	{
		String model = getMediaArchive().getCatalogSettingValue("llmagentmodel");
		if (model == null)
		{
			model = "gpt-5-nano"; // Default fallback
		}
		LlmConnection manager = getMediaArchive().getLlmConnection(model);
		return manager;
	}
	
	public void respondToChannel(ScriptLogger inLog, Data channel, Data message) throws Exception
	{
		MediaArchive archive = getMediaArchive();
		
		LlmRequest llmrequest = new LlmRequest(); //TODO Look in DB or cache from hitory?
		
		String model = archive.getCatalogSettingValue("llmagentmodel");
		
		if (model == null)
		{
			model = "gpt-4o"; // Default fallback
		}
		LlmConnection llmconnection = archive.getLlmConnection(model);

		llmrequest.addContext("model", model);

		ChatServer server = (ChatServer) archive.getBean("chatServer");
		Searcher chats = archive.getSearcher("chatterbox");
		
		if (!llmconnection.isReady()) 
		{
			inLog.error("LLM Manager is not ready, check key for: " + model + ". Cannot process channel: " + channel);
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
		llmrequest.setUserProfile(profile);
		
		llmrequest.addContext("channel", channel);

		String oldstatus = message.get("chatmessagestatus");
		
		//Update original message processing status
		message.setValue("chatmessagestatus", "completed");
		chats.saveData(message);
		
		llmrequest.addContext("message", message);
		
		llmrequest.addContext("assistant", this);
		
		llmrequest.addContext("channelchathistory", loadChannelChatHistory(channel));
		
		if("refresh".equals(oldstatus))
		{			
			String callerParams = message.get("params");
			if(callerParams != null)
			{
				
				JSONObject parsedParams = new JSONParser().parse(callerParams);

				llmrequest.setFunctionName((String) parsedParams.get("function"));
				parsedParams.remove("function");
				
				llmrequest.setParameters((JSONObject) parsedParams);
	
				execLocalActionFromChat(llmconnection, message, llmrequest);
			}
			return;
		}
		
		Data resopnseMessage = chats.createNewData();
		resopnseMessage.setValue("user", "agent");
		resopnseMessage.setValue("channel", channel.getId());
		resopnseMessage.setValue("date", new Date());
		resopnseMessage.setValue("message", "<i class=\"fas fa-spinner fa-spin\"></i>");
		resopnseMessage.setValue("chatmessagestatus", "processing");
		
		chats.saveData(resopnseMessage);
		
		server.broadcastMessage(archive.getCatalogId(), resopnseMessage);

		
//		String chattemplate = "/" + archive.getMediaDbId() + "/ai/openai/assistant/instructions/current.json";
		LlmResponse response = processUserRequest(llmrequest);
		
		//current update it?
		
		if (response.isToolCall())
		{
			// Function call detected
			String functionName = response.getFunctionName();
			llmrequest.setFunctionName(functionName);
			
			JSONObject functionArguments = response.getArguments();		
			
			llmrequest.setParameter("arguments", functionArguments);
			
			
			resopnseMessage.setValue("params", llmrequest.toString());
			
			Object explainer = functionArguments.get("explainer");
			if( explainer != null && explainer instanceof String)
			{
				resopnseMessage.setValue("message", (String)explainer);
			} 
			else
			{
				Data function = getMediaArchive().getCachedData("aifunctions", functionName);
				String processingmessage = null;
				if( function != null)
				{
					processingmessage = function.get("processingmessage");
				}
				if( processingmessage == null )
				{
					processingmessage = "Analyzing...";
				}
				resopnseMessage.setValue("message", processingmessage);
			}
			
			chats.saveData(resopnseMessage);
			
			server.broadcastMessage(archive.getCatalogId(), resopnseMessage);
			
			execLocalActionFromChat(llmconnection, resopnseMessage, llmrequest);
		}
		else
		{
			// **Regular Text Response**
			String output = response.getMessage();

			if (output != null)
			{
				resopnseMessage.setValue("message", output);
				resopnseMessage.setValue("messageplain", output);
				resopnseMessage.setValue("chatmessagestatus", "completed");

				chats.saveData(resopnseMessage);
				server.broadcastMessage(archive.getCatalogId(), resopnseMessage);
			}
		}
		
	}
	
	protected LlmResponse processUserRequest(LlmRequest llmRequest)
	{
		MediaArchive archive = getMediaArchive();
		
		HitTracker modules = getMediaArchive().query("module").exact("isentity",true).sort("ordering").search();
		llmRequest.addContext("modules", modules);
		
		//String model = "qwen3:8b";
		String model = archive.getCatalogSettingValue("llmagentmodel");

		LlmConnection llmconnection = archive.getLlmConnection(model);
		JSONObject results = llmconnection.callStructuredOutputList("parse_sentence", model, llmRequest.getContext());

		EMediaAIResponse response = new EMediaAIResponse();
		response.setRawResponse(results);
		String type = (String)results.get("request_type");
		if( type == null)
		{
			type = "chitchat";
		}
		response.setToolCall(true);
		
//		if( type.equals("search") )   //TODO: Look in some kind of DB to map it back to handler
//		{
			//TODO: Move this to a handler of some kind
			//Create batch of english words that describe how to search all these things
			for (Iterator iterator = modules.iterator(); iterator.hasNext();)
			{
				Data module = (Data) iterator.next();
				String value = (String)results.get(module.getName());
				if( value != null && !value.isEmpty() )
				{
					if(llmRequest.getParameter("parentmodule") == null )
					{
						llmRequest.setParameter("parentmodule", module.getId());
						llmRequest.setParameter("parentmodule.label",module.getName());
						llmRequest.setParameter("parentmodule.value", value);
					}
					else
					{
						llmRequest.setParameter("childmodule", module.getId());
						llmRequest.setParameter("childmodule.label",module.getName());
						llmRequest.setParameter("childmodule.value", value);
						//type = "searchjoin"; //???
					}
				}
			}
			
			if( "search".equals(type) )
			{
				SemanticTableManager manager = loadSemanticTableManager("actionembedding");
				//Look into our semantic DB and determine the real search type and real parameters
				
				String parent = llmRequest.getParameter("parentmodule.label");
				//String targets = llmRequest.getParameter("targets");
				
				String text = "Search for ";
				String child = llmRequest.getParameter("childmodule.label");
				if( child != null)
				{
					text = text + child;
				}
				if( parent != null)
				{
					text = text + " in " + parent;
				}
				
				List<Double> tosearch = manager.makeVector(text);
				Collection<RankedResult> suggestions = manager.searchNearestItems(tosearch);
				//Load more details into this request and possibly change the type

				//Let the search action parse any  extra parameters available from the context 
				if( !suggestions.isEmpty())
				{
					llmRequest.setRankedSuggestions(suggestions);
					RankedResult top = (RankedResult)suggestions.iterator().next();
					if ( top.getDistance() < .7 )
					{
						type = top.getEmbedding().get("requesttype");
					
						AiSearch aisearch = processAISearchArgs(results,top.getEmbedding(), llmRequest.getUserProfile());
						llmRequest.setAiSearchParams(aisearch);
					}
				}
			}
			response.setFunctionName(type);
			//			
//		}	
		return response;
	}
	
	public void execLocalActionFromChat(LlmConnection llmconnection, Data messageToUpdate, LlmRequest llmrequest) throws Exception
	{
		MediaArchive archive = getMediaArchive();

		Data channel = archive.getCachedData("channel", messageToUpdate.get("channel"));
		llmrequest.addContext("channel", channel);
		
		ChatServer server = (ChatServer) archive.getBean("chatServer");

		try
		{
			llmrequest.addContext("message", messageToUpdate);
			llmrequest.addContext("aisearchparams", llmrequest.getAiSearchParams() );
			
			String apphome = "/"+ channel.get("chatapplicationid");
			llmrequest.addContext("apphome", apphome);
			
			
			LlmResponse response = llmconnection.renderLocalAction(llmrequest);  //Run Search

			messageToUpdate.setValue("message", response.getMessage());
			String messageplain = messageToUpdate.get("messageplain");
			
			String newmessageplain = response.getMessagePlain();
			
			if(newmessageplain != null)
			{
				if(messageplain == null)
				{
					messageplain = newmessageplain;
				}
				else
				{
					messageplain += " \n " + newmessageplain;
				}
				messageToUpdate.setValue("messageplain", messageplain);
			}
			
			messageToUpdate.setValue("chatmessagestatus", "completed");
			
			Searcher chats = archive.getSearcher("chatterbox");
			chats.saveData(messageToUpdate);
			
			JSONObject functionMessageUpdate = new JSONObject();
			functionMessageUpdate.put("messagetype", "airesponse");
			functionMessageUpdate.put("catalogid", archive.getCatalogId());
			functionMessageUpdate.put("user", "agent");
			functionMessageUpdate.put("channepeoplel", messageToUpdate.get("channel"));
			functionMessageUpdate.put("messageid", messageToUpdate.getId());
			functionMessageUpdate.put("message", response.getMessage());
			server.broadcastMessage(functionMessageUpdate);
			
			Long waittime = 200l;
			if( llmrequest.getNextFunctionName() != null)
			{
				JSONObject params = llmrequest.getParameters();
				
				params.put("function", llmrequest.getNextFunctionName());
				
				messageToUpdate.setValue("params", params.toJSONString());

				messageToUpdate.setValue("chatmessagestatus", "refresh");
				chats.saveData(messageToUpdate);

				Object wait = params.get("wait");
				if( wait != null && wait instanceof Long)
				{
					params.remove("wait");
					waittime = (Long) wait;
					log.info("Previous function requested to wait " + waittime + " milliseconds");
				}
				
			}
			Runnable runnable = new Runnable() {
				public void run()
				{
					getMediaArchive().fireSharedMediaEvent("llm/monitorchats");
				}
			};
			archive.getExecutorManager().execLater(runnable, waittime);
		}
		catch (Exception e)
		{
			log.error("Could not execute function: " + llmrequest.getFunctionName(), e);
			messageToUpdate.setValue("functionresponse", e.toString());
			messageToUpdate.setValue("chatmessagestatus", "failed");
			archive.saveData("chatterbox", messageToUpdate);
		}
	}

	protected Collection<Data> loadChannelChatHistory(Data inChannel)
	{
		HitTracker messages = getMediaArchive().query("chatterbox").exact("channel", inChannel).sort("dateUp").search();
		
		Collection<Data> recent = new ArrayList<Data>();
		
		for (Iterator iterator = messages.iterator(); iterator.hasNext();) {
			Data message = (Data) iterator.next();
			if("agent".equals(message.get("user")))
			{
				String plainmessage = message.get("messageplain");
				if( plainmessage == null || plainmessage.isEmpty())
				{
					continue;
				}
			}
			recent.add(message);
		}
		
		return recent;
	}

	public HitTracker getFunctions()
	{
		HitTracker hits = getMediaArchive().query("aifunctions").exact("pipeline", "assistant").exact("enabled", true).sort("ordering").cachedSearch();
		return hits;
	}
	
	public AiSearch processAISearchArgs(JSONObject airesults, Data inEmbeddingMatch, UserProfile userprofile) 
	{
		//Search for tomatoes in sales departments
		
		AiSearch searchArgs = new AiSearch();
		
		if( inEmbeddingMatch != null)
		{						
			String parentid = inEmbeddingMatch.get("parentmodule");
			Data parentmodule = getMediaArchive().getCachedData("module", parentid);
			if( parentmodule != null)
			{
				searchArgs.setParentModule(parentmodule);
			}
			String childid = inEmbeddingMatch.get("childmodule");
			Data childmodule = getMediaArchive().getCachedData("module", childid);
			if( childmodule != null)
			{
				searchArgs.setChildModule(childmodule);
			}
		}

		Collection<String> keywords = getResultsManager().parseKeywords(airesults.get("keywords")); 
		searchArgs.setKeywords(keywords);
		
		Object selectedModulesObj = airesults.get("targets");

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
			Collection<String> selectedModuleIds = new ArrayList<String>();
			for (Iterator iterator = selectedModules.iterator(); iterator.hasNext();)
			{
				String id = (String) iterator.next();
				if(id.contains("|"))
				{
					id = id.split("\\|")[0];
				}
				selectedModuleIds.add(id);
			}
			permittedModules = userprofile.getEntitiesByIdOrName(selectedModuleIds);
		}
		
		searchArgs.setSelectedModules(permittedModules);
		
		return searchArgs;
	}

	public void regularSearch(WebPageRequest inReq, AiSearch inAiSearchParams) throws Exception {
		
		//JSONObject arguments = (JSONObject) inReq.getPageValue("arguments");

//		
//		if(isMcp)
//		{
//			addMcpVars(inReq, aiSearchArgs);
//		}
		
		searchByKeywords(inReq, inAiSearchParams);
		
		inReq.putPageValue("semanticquery", inAiSearchParams.toSemanticQuery());

	}
	
	public void searchByKeywords(WebPageRequest inReq, AiSearch searchArgs)
	{
		
		log.info("Searching as:" + inReq.getUser().getName());
		MediaArchive archive = getMediaArchive();

		Collection<String> keywords = searchArgs.getKeywords();
		
		String plainquery = String.join(" ", keywords);
		
		QueryBuilder dq = archive.query("modulesearch").addFacet("entitysourcetype").freeform("description", plainquery).hitsPerPage(30);
		dq.getQuery().setIncludeDescription(true);
		
		Collection searchmodules = getResultsManager().loadUserSearchTypes(inReq, searchArgs.getSelectedModuleIds());
		
		Collection searchmodulescopy = new ArrayList(searchmodules);
		searchmodulescopy.remove("asset");
		dq.getQuery().setValue("searchtypes", searchmodulescopy);
		
		
		HitTracker unsorted = dq.search(inReq);
		
		log.info(unsorted);

		Map<String,String> keywordsLower = new HashMap();
		
		getResultsManager().collectMatches(keywordsLower, plainquery, unsorted);
		
		inReq.putPageValue("modulehits", unsorted);
		inReq.putPageValue("livesearchfor", plainquery);
		
		List finallist = new ArrayList();
		
		for (Iterator iterator = keywordsLower.keySet().iterator(); iterator.hasNext();)
		{
			String keyword = (String) iterator.next();
			String keywordcase = keywordsLower.get(keyword);
			finallist.add(keywordcase);
		}

		Collections.sort(finallist);
		
		
		inReq.putPageValue("livesuggestions", finallist);
		inReq.putPageValue("highlighter", new Highlighter());
		
		int assetmax = 15;
		if( unsorted.size() > 10)
		{
			assetmax = 5;
		}
		
		QueryBuilder assetdq = archive.query("asset")
				.freeform("description", plainquery)
				.hitsPerPage(assetmax);
				
		HitTracker assetunsorted = assetdq.search(inReq);
		getResultsManager().collectMatches(keywordsLower, plainquery, assetunsorted);
		inReq.putPageValue("assethits", assetunsorted);
		
		Collection pageOfHits = unsorted.getPageOfHits();
		pageOfHits = new ArrayList(pageOfHits);
		
		String[] excludeentityids = new String[unsorted.size()];
		String[] excludeassetids = new String[assetunsorted.size()];
		
		StringBuilder contextString = new StringBuilder();
		
		int idx = 0;
		for (Object entity : unsorted.getPageOfHits()) {
			Data d = (Data) entity;
			
			String parentassetid = d.get("parentasset");
			if(parentassetid != null)
			{
				String fulltext = d.get("longdescription");
				if(fulltext == null || fulltext.length() == 0)
				{
					Asset parent = archive.getAsset(parentassetid);
					fulltext = parent.get("fulltext");
				}
				if(fulltext != null && fulltext.length() > 0)
				{
					contextString.append("From " + d.getName() + "\n");
					contextString.append(fulltext);
					contextString.append("\n\n");
				}
			}
			excludeentityids[idx] = d.getId();
			idx++;
		}
		idx = 0;
		for (Object asset : assetunsorted.getPageOfHits()) {
			Data d = (Data) asset;
			
			String fulltext = d.get("longdescription");
			if(fulltext != null && fulltext.length() > 0)
			{
				contextString.append("From " + d.getName() + "\n");
				contextString.append(fulltext);
				contextString.append("\n\n");
			}
			
			excludeassetids[idx] = d.getId();
			idx++;
		}
		inReq.putPageValue("excludeentityids", excludeentityids);
		inReq.putPageValue("excludeassetids", excludeassetids);
		
		inReq.putPageValue("totalhits", unsorted.size() + assetunsorted.size());
		
		getResultsManager().loadOrganizedResults(inReq, unsorted,assetunsorted);
		
		if( contextString.length() > 0)
		{
			Data ragcontext = archive.getSearcher("ragcontext").createNewData();
			ragcontext.setValue("", "");
		}
		
	}
	
	public void executeRag(WebPageRequest inReq) 
	{
		MediaArchive archive = getMediaArchive();
		Data ragcontext = (Data) archive.query("ragcontext").exact("status", "pending").sort("dateUp").searchOne();
		if(ragcontext == null)
		{
			log.info("No RAG context found to process");
			return;
		}
		String model = archive.getCatalogSettingValue("llmragmodel");
		OpenAiConnection llmconnection = (OpenAiConnection) archive.getLlmConnection(model);
		
		llmconnection.callRagFunction(model, ragcontext.get("context"), ragcontext.get("query"));
		
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
			log.error("Could not parse excludeentityids", e);
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
		
		Map<String, Collection<String>> relatedEntityIds = getSemanticTopicManager().search(query, excludeEntityIds, excludeAssetIds);
		
		log.info("Related Entity Ids: " + relatedEntityIds);

		Collection<Data> semanticentities = new ArrayList();
		Map<String, HitTracker> semanticentityhits = new HashMap();
		HitTracker semanticassethits = null;

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
				semanticassethits = entites;
			}
			else
			{				
				semanticentityhits.put(moduleid, entites);
			}
		}
		
		inReq.putPageValue("semanticentities", semanticentities);
		inReq.putPageValue("semanticentityhits", semanticentityhits);
		if(semanticassethits == null)
		{
			inReq.putPageValue("semanticassethits", new ArrayList());
		}
		else
		{	
			inReq.putPageValue("semanticassethits", semanticassethits);
		}		
	}
	
	public SemanticTableManager loadSemanticTableManager(String inConfigId)
	{
		SemanticTableManager table = (SemanticTableManager)getMediaArchive().getCacheManager().get("semantictables",inConfigId);
		if( table == null)
		{
			table = (SemanticTableManager)getModuleManager().getBean(getCatalogId(),"semanticTableManager",false);
			table.setConfigurationId(inConfigId);
			getMediaArchive().getCacheManager().put("semantictables",inConfigId,table);
		}
		
		return table;
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
		
		String model = archive.getCatalogSettingValue("llmmcpmodel");
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
	
	public Collection<PropertyDetail> getCommonFields(String inSearchtype)
	{
		Collection<PropertyDetail> fields = new ArrayList();
		PropertyDetail pd = getMediaArchive().getSearcher(inSearchtype).getPropertyDetails().createDetail("title");
//		Collection<String> fieldids = Arrays.from("title","description","keywords","caption","date");
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
		Collection values = searchcategory.getValues("semantictopics");
		Map<String,Collection<String>> results = getSemanticTopicManager().search(values, null, null);
		return results;
	}

	protected SemanticCassifier fieldSemanticTopicManager;
	public SemanticCassifier getSemanticTopicManager()
	{
		if (fieldSemanticTopicManager == null)
		{
			fieldSemanticTopicManager = (SemanticCassifier)getModuleManager().getBean(getCatalogId(),"semanticFieldManager",false);
			fieldSemanticTopicManager.setConfigurationId("semantictopics");
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

		String args = inReq.getRequestParameter("arguments");
		
		if(args == null)
		{
			log.warn("No arguments found in request");
			return;
		}
		JSONObject arguments = new JSONParser().parse( args );
		
		String prompt = (String) arguments.get("prompt");

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

			String filename =  results.getFileName();
			asset.setName(filename);
			asset.setValue("assetaddeddate", new Date());
			
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
		
		Data message = (Data) inReq.getPageValue("message");
		if( message != null)
		{
			archive.saveData("chatterbox", message);
		}
		

		archive.fireSharedMediaEvent("importing/assetscreated");
	}
	public void createEntity(WebPageRequest inReq) throws Exception 
	{
		MediaArchive archive = getMediaArchive();

		String args = inReq.getRequestParameter("arguments");
		if(args == null)
		{
			log.warn("No arguments found in request");
			return;
		}
		JSONObject arguments = new JSONParser().parse( args );
		
		String entityname = (String) arguments.get("name");
		
		JSONObject fields = (JSONObject) arguments.get("fields");
		Collection tags = null;
		String description = null;
		if(fields != null)
		{
			tags = (JSONArray) fields.get("tags");
			description = (String) fields.get("description");
		}

		String modulestr = (String) arguments.get("module");
		
		if (entityname == null)
		{
			inReq.putPageValue("error", "Please provide a name for the new entity");
			return;
		}
		if (modulestr == null)
		{
			inReq.putPageValue("error", "Please provide the module name or id for the new entity");
			return;
		}
		
		String moduleid = modulestr.split("\\|")[0];
		Data module = archive.getCachedData("module", moduleid);
		
		if(module == null)
		{
			inReq.putPageValue("error", "Could not find module. Please provide an existing module name or id");
			return;
		}
		
		Searcher searcher = archive.getSearcher(module.getId());
		
		Data entity = searcher.createNewData();
		entity.setName(entityname);
		if(description != null)
		{
			entity.setValue("longcaption", description);
		}
		if(tags != null)
		{
			entity.setValue("keywords", tags);
		}
		searcher.saveData(entity);
		
		inReq.putPageValue("entity", entity);
		inReq.putPageValue("module", module);
	}
	
	public void updateEntity(WebPageRequest inReq) throws Exception 
	{
		MediaArchive archive = getMediaArchive();

		String args = inReq.getRequestParameter("arguments");
		if(args == null)
		{
			log.warn("No arguments found in request");
			return;
		}
		JSONObject arguments = new JSONParser().parse( args );
		
		String entityid = (String) arguments.get("entityId");
		String moduleid = (String) arguments.get("moduleId");
		
		if (entityid == null)
		{
			inReq.putPageValue("error", "Please provide the entity id for the entity to update");
			return;
		}
		if (moduleid == null)
		{
			inReq.putPageValue("error", "Please provide the module id for the entity to update");
			return;
		}
		
		Data module = archive.getCachedData("module", moduleid);
		if(module == null)
		{
			inReq.putPageValue("error", "Could not find module. Please provide the module id of the entity to update");
			return;
		}
		Searcher searcher = archive.getSearcher(module.getId());
		Data entity = searcher.query().id(entityid).cachedSearchOne();
		if(entity == null)
		{
			inReq.putPageValue("error", "Could not find entity. Please provide an existing entity id to update");
			return;
		}
		
		String newmoduleid = (String) arguments.get("newModuleId");
		if(newmoduleid != null)
		{
			Data newmodule = archive.getCachedData("module", newmoduleid);
			if(newmodule == null)
			{
				inReq.putPageValue("error", "Could not find new module. Please provide an existing module id to update");
				return;
			}
			EntityManager entityManager = getEntityManager();
			String modulechangemethod = (String) arguments.get("moduleChangeMethod");
			if("copy".equals(modulechangemethod))
			{
				Data newentity = entityManager.copyEntity(inReq, module.getId(), newmodule.getId(), entity);
				entity = newentity;
				module = newmodule;
				inReq.putPageValue("changemethod", "copy");
			}
			else if("move".equals(modulechangemethod))
			{
				Data newentity = entityManager.copyEntity(inReq, module.getId(), newmodule.getId(), entity);
				if( newentity == null)
				{
					inReq.putPageValue("error", "Could not copy entity. Please try again");
					return;
				}
				entityManager.deleteEntity(inReq, module.getId(), entity.getId());
				entity = newentity;
				module = newmodule;
				inReq.putPageValue("changemethod", "move");
			}
			else
			{
				inReq.putPageValue("error", "Please specify whether to copy or move the entity to the new module");
				return;
			}
			
		}
		
		String primaryImage = (String) arguments.get("primaryImageId");
		String newName = (String) arguments.get("newName");
		
		if(newName != null && newName.length() > 0)
		{
			entity.setName(newName);
			inReq.putPageValue("newname", newName);
		}
		if(primaryImage != null && primaryImage.length() > 0)
		{
			Asset asset = archive.getAsset(primaryImage);
			if(asset != null)
			{
				EntityManager entityManager = getMediaArchive().getEntityManager();
				String destinationcategorypath = inReq.getRequestParameter("destinationcategorypath");
				Category destinationCategory = null;
				if(destinationcategorypath!= null)
				{
					destinationCategory = archive.getCategorySearcher().createCategoryPath(destinationcategorypath);
				}
				else {
					destinationCategory = entityManager.loadDefaultFolder(module, entity, inReq.getUser());
				}
				entityManager.addAssetToEntity(inReq.getUser(), module, entity, asset, destinationCategory);
				entity.setValue("primaryimage", primaryImage);
				inReq.putPageValue("primaryimage", asset);
			}
		}
		searcher.saveData(entity);
		
		inReq.putPageValue("entity", entity);
		inReq.putPageValue("module", module);
	}
	
	public void loadAllActions(ScriptLogger inLog)
	{
		SemanticTableManager manager = loadSemanticTableManager("actionembedding");
		
		//Create batch of english words that describe how to search all these things
		HitTracker modules = getMediaArchive().query("module").exact("isentity",true).search();
		
		Collection moduleids = modules.collectValues("id");
		
		Searcher embedsearcher = getMediaArchive().getSearcher("actionembedding");
		
		for (Iterator iterator = modules.iterator(); iterator.hasNext();)
		{
			Data parentmodule = (Data) iterator.next();
			
			Collection existing = embedsearcher.query().exact("parentmodule",parentmodule.getId()).search();
			if( !existing.isEmpty())
			{
				log.info("Skipping " + parentmodule);
				continue;
			}
			Collection<SemanticAction> actions = new ArrayList();
			
			SemanticAction action = new SemanticAction();
			/*
			 * "search",
							"creation",
							"how-to",
							"task",
							"conversation",
							"support request"
							*/
			action.setRequestType("search");
			action.setSemanticText("Search for " + parentmodule.getName());
			action.setParentData(parentmodule);
			actions.add(action);
			action = new SemanticAction();
			action.setParentData(parentmodule);
			action.setRequestType("create");
			action.setSemanticText("Create a new " + parentmodule.getName());
			actions.add(action);
			
			//Check for child views
			Map<String, PropertyDetail> details = loadActiveDetails(parentmodule.getId());
			if( details != null)
			{
				for (Iterator iterator2 = details.values().iterator(); iterator2.hasNext();)
				{
					PropertyDetail detail = (PropertyDetail) iterator2.next();
					if( detail.isList() )
					{
						String listid = detail.getListId();
						if( moduleids.contains(listid) )
						{
							Data childmodule = getMediaArchive().getCachedData("module", listid);
							action = new SemanticAction();
							action.setParentData(parentmodule);
							action.setChildData(childmodule);
							action.setRequestType("searchjoin");
							action.setSemanticText("Search for " + childmodule.getName() + " in " + parentmodule.getName());
							actions.add(action);
						}
					}
				}
			}
			populateVectors(manager,actions);

			//Save to db
			Collection tosave = new ArrayList();
			
			for (Iterator iterator2 = actions.iterator(); iterator2.hasNext();)
			{
				SemanticAction semanticAction = (SemanticAction) iterator2.next();
				Data data = embedsearcher.createNewData();
				data.setValue("parentmodule",semanticAction.getParentData().getId());
				if( semanticAction.getChildData() != null)
				{
					data.setValue("childmodule",semanticAction.getChildData().getId());
				}
				data.setValue("vectorarray",semanticAction.getVectors());
				data.setValue("requesttype",semanticAction.getRequestType());
				
				tosave.add(data);
			}
			embedsearcher.saveAllData(tosave, null);
			
		}
		//Test search
		manager.reinitClusters(inLog);

//		List<Double> tosearch = manager.makeVector("Find all records in US States in 2023");
//		Collection<RankedResult> results = manager.searchNearestItems(tosearch);
//		log.info(results);
		
		
	}

	protected void populateVectors(SemanticTableManager manager, Collection<SemanticAction> inActions)
	{
		Collection<String> textonly = new ArrayList(inActions.size());
		Map<String,SemanticAction> actions = new HashMap();
		Integer count = 0;
		for (Iterator iterator = inActions.iterator(); iterator.hasNext();)
		{
			SemanticAction action = (SemanticAction) iterator.next();
			textonly.add(action.getSemanticText());
			actions.put( String.valueOf(count) , action);
			count++;
		}
		
		JSONObject response = manager.execMakeVector(textonly);
		
		JSONArray results = (JSONArray)response.get("results");
		Collection<MultiValued> newrecords = new ArrayList(results.size());
		for (int i = 0; i < results.size(); i++)
		{
			Map hit = (Map)results.get(i);
			String countdone = (String)hit.get("id");
			SemanticAction action = actions.get(countdone);
			List vector = (List)hit.get("embedding");
			vector = manager.collectDoubles(vector);
			action.setVectors(vector);
		}
		
	}
	
}
