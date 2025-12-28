package org.entermediadb.ai.assistant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.ChatMessageHandler;
import org.entermediadb.ai.Schema;
import org.entermediadb.ai.classify.SemanticClassifier;
import org.entermediadb.ai.informatics.InformaticsManager;
import org.entermediadb.ai.informatics.InformaticsProcessor;
import org.entermediadb.ai.informatics.SemanticTableManager;
import org.entermediadb.ai.knn.RankedResult;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.ai.llm.openai.OpenAiConnection;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.find.EntityManager;
import org.entermediadb.find.ResultsManager;
import org.entermediadb.markdown.MarkdownUtil;
import org.entermediadb.scripts.ScriptLogger;
import org.entermediadb.websocket.chat.ChatServer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.BaseData;
import org.openedit.data.PropertyDetail;
import org.openedit.data.QueryBuilder;
import org.openedit.data.Searcher;
import org.openedit.hittracker.FilterNode;
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
		
		HitTracker allchannels = channels.query().orgroup("channeltype", "agentchat,agententitychat").after("refreshdate",now.getTime()).sort("refreshdateDown").search();

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

			//Remove from DB
			Collection tosave = new ArrayList();
			
			for (Iterator iterator2 = mostrecents.iterator(); iterator2.hasNext();)
			{
				Data	data = (Data) iterator2.next();
				data.setValue("chatmessagestatus","processing");
				tosave.add(data);
			}
			archive.saveData("chatterbox", tosave);
			
			for (Iterator iterator2 = mostrecents.iterator(); iterator2.hasNext();) {
				MultiValued mostrecent = (MultiValued) iterator2.next();
				try
				{
					Runnable runnable = new Runnable() { //Let the user broadcast finish
						public void run()
						{
							respondToChannel(inLog, channel, mostrecent);
						}
					};
					archive.getExecutorManager().execLater(runnable, 0);
				}
				catch (Throwable ex )
				{
					log.error("Could not process message " + mostrecent,ex);
				}
			}
			
		}
	}
	
//	public LlmConnection getLlmConnection()
//	{
//		String model = getMediaArchive().getCatalogSettingValue("llmagentmodel");
//		if (model == null)
//		{
//			model = "gpt-5-nano"; // Default fallback
//		}
//		LlmConnection manager = getMediaArchive().getLlmConnection(model);
//		return manager;
//	}
//	
	
	public AgentContext loadContext(String inChannelId) 
	{
		MediaArchive archive = getMediaArchive();
		AgentContext agentContext = (AgentContext) archive.getCacheManager().get("agentcontext", inChannelId);
		if( agentContext == null)
		{
			Searcher searcher =  archive.getSearcher("agentcontext");
			agentContext = (AgentContext)searcher.query().exact("channel", inChannelId).searchOne(); //TODO Look in DB or cache from hitory?
			if( agentContext == null)
			{
				agentContext = (AgentContext) searcher.createNewData();
				agentContext.setValue("channel", inChannelId);
				searcher.saveData(agentContext);
			}
			
			archive.getCacheManager().put("agentcontext", inChannelId, agentContext);
		
		}
		return agentContext;
	}
	
	public void respondToChannel(ScriptLogger inLog, Data inChannel, MultiValued usermessage)
	{
		MediaArchive archive = getMediaArchive();
		
		AgentContext agentContext = loadContext(inChannel.getId());
		
		//LlmConnection llmconnection = archive.getLlmConnection("agentChat");
		//agentContext.addContext("model", llmconnection.getModelName() );

		ChatServer server = (ChatServer) archive.getBean("chatServer");
		
//		if (!llmconnection.isReady()) 
//		{
//			inLog.error("LLM Manager is not ready, check key for: " +  llmconnection.getModelName() + ". Cannot process channel: " + inChannel);
//			return;
//		}

		String channeltype = inChannel.get("channeltype");
		if (channeltype == null)
		{
			channeltype = "agentchat";
		}
		
		String id = inChannel.get("user");
		UserProfile profile = archive.getUserProfile(id);
		agentContext.addContext("chatprofile", profile);
		agentContext.setUserProfile(profile);
		
		agentContext.addContext("channel", inChannel);

//		String oldstatus = usermessage.get("chatmessagestatus");
		
		//Update original message processing status
		usermessage.setValue("chatmessagestatus", "completed");
		getMediaArchive().saveData("chatterbox",usermessage);
		
		agentContext.addContext("message", usermessage);
		
		agentContext.addContext("assistant", this);
		
		agentContext.addContext("channelchathistory", loadChannelChatHistory(inChannel));
		
//		if("refresh".equals(oldstatus))
//		{			
//			String nextFunction = agentContext.getNextFunctionName();
//			if(nextFunction != null)
//			{
//				agentContext.setFunctionName(nextFunction);
//				agentContext.setNextFunctionName(null);
//				
//				getMediaArchive().saveData("agentcontext", agentContext);
//				
//				MultiValued realusermessage = (MultiValued)archive.getCachedData("chatterbox",usermessage.get("replytoid"));
//				execCurrentFunctionFromChat(realusermessage,usermessage, agentContext);
//			}
//			return;
//		}
		
		//Add new agentmessage
		MultiValued agentmessage = newAgentMessage(usermessage, agentContext);
		
		//ChatServer server = (ChatServer) getMediaArchive().getBean("chatServer");
		//Determine what will need to be processed
		try
		{
			if (channeltype.equals("agententitychat"))
			{
				agentContext.setFunctionName("parseRagPrompt");
			}
			else 
			{
				agentContext.setFunctionName("parsePrompt");
			}
			execCurrentFunctionFromChat(usermessage, agentmessage, agentContext);
		}
		catch( Exception ex)
		{
			agentmessage.setValue("message",ex.toString());
			archive.saveData("chatterbox",agentmessage);
			server.broadcastMessage(archive.getCatalogId(), agentmessage);
		}
	}
	
	
	public MultiValued newAgentMessage(MultiValued usermessage, AgentContext agentContext)
	{
		MultiValued agentmessage = (MultiValued)getMediaArchive().getSearcher("chatterbox").createNewData();
		agentmessage.setValue("user", "agent");
		agentmessage.setValue("replytoid",usermessage.getId() );
		agentmessage.setValue("channel", agentContext.getChannel().getId());
		agentmessage.setValue("date", new Date());
		agentmessage.setValue("chatmessagestatus", "processing");
		return agentmessage;
	}
	
	public void execCurrentFunctionFromChat(MultiValued usermessage, MultiValued agentmessage, AgentContext agentContext) 
	{

		String functionName = agentContext.getFunctionName();
		MultiValued function = (MultiValued)getMediaArchive().getCachedData("aifunction", functionName); //Chitchat etc
		
		if (function == null)
		{
			log.info("No Ai function defined: " + functionName);
			return;
		}
		
		ChatServer server = (ChatServer) getMediaArchive().getBean("chatServer");

		
		String loader = "<i class=\"fas fa-spinner fa-spin mr-2\"></i> ";
		
		String processingmessage = null; // TODO: Change to language mao
		if( function != null)
		{
			processingmessage = function.get("processingmessage");
		}
		if( processingmessage == null )
		{
			processingmessage = "Analyzing...";
		}
		
		processingmessage = loader + processingmessage;
		
		String message = agentContext.getMessagePrefix() + processingmessage;
		agentmessage.setValue("message", message ); //setting status
		getMediaArchive().saveData("chatterbox",agentmessage);	
		server.broadcastMessage(getMediaArchive().getCatalogId(), agentmessage);
	
		
		MediaArchive archive = getMediaArchive();

		Data channel = archive.getCachedData("channel", agentmessage.get("channel"));
		agentContext.addContext("channel", channel);
		
		agentContext.addContext("usermessage", usermessage);
		agentContext.addContext("agentmessage", agentmessage);
		agentContext.addContext("aisearchparams", agentContext.getAiSearchParams() );
		
		String apphome = "/"+ channel.get("chatapplicationid");
		agentContext.addContext("apphome", apphome);
		
		try
		{
				
			String bean = function.get("messagehandler");
			
			ChatMessageHandler handler = (ChatMessageHandler)getMediaArchive().getBean( bean);
			
			LlmResponse response = handler.processMessage(agentmessage, agentContext);
			
			String updatedMessage = agentContext.getMessagePrefix();
			
			if( response.getMessage() != null )
			{
				updatedMessage += response.getMessage();
			}

			agentmessage.setValue("message", updatedMessage); //Final message
			
			

			String messageplain = agentmessage.get("messageplain");
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
				agentmessage.setValue("messageplain", messageplain);
			}
			
			agentmessage.setValue("chatmessagestatus", "completed");
			getMediaArchive().saveData("chatterbox",agentmessage);
			

			
			JSONObject functionMessageUpdate = new JSONObject();
			functionMessageUpdate.put("messagetype", "airesponse");
			functionMessageUpdate.put("catalogid", archive.getCatalogId());
			functionMessageUpdate.put("user", "agent");
			functionMessageUpdate.put("channel", agentmessage.get("channel"));
			functionMessageUpdate.put("messageid", agentmessage.getId());
			functionMessageUpdate.put("message", agentmessage.get("message"));
			server.broadcastMessage(functionMessageUpdate);
			
			Long waittime = 200l;
			
			if( agentContext.getNextFunctionName() != null)
			{
				//Search semantic now?
				//params.put("function", agentContext.getNextFunctionName());
				//agentmessage.setValue("params", params.toJSONString());

//				agentmessage.setValue("chatmessagestatus", "refresh");
//				getMediaArchive().saveData("chatterbox",agentmessage);
				
				if("searchSemantic".equals(agentContext.getNextFunctionName()))
				{
					//New Agent Message
//					agentmessage = newAgentMessage(usermessage, agentContext);
					
					//Or update existing one
					getMediaArchive().saveData("chatterbox",agentmessage);
				}

				Long wait = agentContext.getLong("wait");
				if( wait != null && wait instanceof Long)
				{
					agentContext.setValue("wait", null);
					waittime = wait;
					log.info("Previous function requested to wait " + waittime + " milliseconds");
					Thread.sleep(wait);
				}
				//TODO: Just sleep for a bit? and try again
				agentContext.setFunctionName(agentContext.getNextFunctionName());
				agentContext.setNextFunctionName(null);
				execCurrentFunctionFromChat(usermessage, agentmessage, agentContext);
//				Runnable runnable = new Runnable() {
//					public void run()
//					{
//						getMediaArchive().fireSharedMediaEvent("llm/monitorchats");
//					}
//				};
//				archive.getExecutorManager().execLater(runnable, waittime);
			}
			
		}
		catch (Exception e)
		{
			log.error("Could not execute function: " + agentContext.getFunctionName(), e);
			agentmessage.setValue("functionresponse", e.toString());
			agentmessage.setValue("chatmessagestatus", "failed");
			archive.saveData("chatterbox", agentmessage);
		}
	}
	
	@Override
	public LlmResponse processMessage(MultiValued inAgentMessage, AgentContext inAgentContext)
	{
//		if(output == null || output.isEmpty())
//		{  //What is this for?
//			agentContext.addContext("messagetosend", message.get("message") );
//			LlmResponse chatresponse = llmconnection.callMessageTemplate(agentContext,response.getFunctionName()); 
//			//TODO: Add message history
//			output = chatresponse.getMessage();
//		}
		//ChatServer server = (ChatServer) getMediaArchive().getBean("chatServer");
		
		
//		String output = inMessage.getMessage();
//		agentmessage.setValue("message", output);  //Needed"
//		agentmessage.setValue("messageplain", output);
//		

		if ("parsePrompt".equals(inAgentContext.getFunctionName()))
		{
			MultiValued usermessage = (MultiValued)getMediaArchive().getCachedData("chatterbox", inAgentMessage.get("replytoid"));
			LlmResponse response = determineFunction(usermessage, inAgentMessage, inAgentContext);   
//			agentmessage.setValue("user", "agent");
//			agentmessage.setValue("replytoid",usermessage.getId() );
//			agentmessage.setValue("channel", inAgentContext.getChannel().getId());
//			agentmessage.setValue("date", new Date());
//			agentmessage.setValue("message", "<i class=\"fas fa-spinner fa-spin mr-2\"></i> Processing request...");
//			agentmessage.setValue("chatmessagestatus", "processing");
//			chats.saveData(agentmessage);
//			ChatServer server = (ChatServer) getMediaArchive().getBean("chatServer");
//			server.broadcastMessage(getMediaArchive().getCatalogId(), agentmessage);

			//Handle right now
			if ("conversation".equals(response.getFunctionName()))
			{
				//chitchat
				inAgentMessage.setValue("chatmessagestatus", "completed");
				
				String generalresponse  = response.getMessage();
				if(generalresponse != null)
				{
					MarkdownUtil md = new MarkdownUtil();
					generalresponse = md.render(generalresponse);
					//inAgentMessage.setValue("message",generalresponse);
				}
				//LlmResponse respond = new EMediaAIResponse();
				response.setMessage(generalresponse);
				
				inAgentContext.setNextFunctionName(null);

			}
			else
			{
				response.setMessage("");
				inAgentContext.setNextFunctionName(response.getFunctionName());
			}
			return response;
		}
		if ("parseRagPrompt".equals(inAgentContext.getFunctionName()))
		{
			MultiValued usermessage = (MultiValued)getMediaArchive().getCachedData("chatterbox", inAgentMessage.get("replytoid"));
			MediaArchive archive = getMediaArchive();
			
			LlmConnection llmconnection = archive.getLlmConnection("parseRagPrompt");
			
			inAgentContext.addContext("schema", loadSchema());
			
			LlmResponse response = llmconnection.callStructuredOutputList(inAgentContext.getContext()); //TODO: Replace with local API that is faster
			if(response == null)
			{
				throw new OpenEditException("No results from AI for message: " + usermessage.get("message"));
			}
			
			return response;
		}
		else if ("searchTables".equals(inAgentContext.getFunctionName()))
		{
			//search
			LlmConnection searcher = getMediaArchive().getLlmConnection("searchTables");
			LlmResponse response =  searcher.renderLocalAction(inAgentContext);
			
			String message = response.getMessage();
			
			if(message != null)
			{
				inAgentContext.setMessagePrefix(message);
			}
			
			return response;
		}
		else if ("searchSemantic".equals(inAgentContext.getFunctionName()))
		{	
			LlmConnection llmconnection = getMediaArchive().getLlmConnection("searchSemantic");
			LlmResponse result = llmconnection.renderLocalAction(inAgentContext);
			return result;
		}
		
		throw new OpenEditException("Function not supported " + inAgentContext.getFunctionName());
		
				
//		if(generalresponse != null)
//		{
//			MarkdownUtil md = new MarkdownUtil();
//			response.setMessage(md.render(generalresponse));
//		}
		
		//respond.setFunctionName(null);
//		getMediaArchive().saveData("chatterbox",inMessage);
//		server.broadcastMessage(getMediaArchive().getCatalogId(), inMessage);
	}
	
	protected LlmResponse determineFunction(MultiValued userMessage,MultiValued agentMessage, AgentContext inAgentContext)
	{
		MediaArchive archive = getMediaArchive();
		
		//TODO: Add channel type searching workflow or reporting
		LlmConnection llmconnection = archive.getLlmConnection("parsePrompt"); //Depend on the channel mode parseSearch parseReporting parseWorkflow
		
		//Run AI
		inAgentContext.addContext("schema", loadSchema());
		//inAgentContext.addContext("message", userMessage);
		
		LlmResponse response = llmconnection.callStructuredOutputList(inAgentContext.getContext()); //TODO: Replace with local API that is faster
		if(response == null)
		{
			throw new OpenEditException("No results from AI for message: " + userMessage.get("message"));
		}
		
		//TODO: Use IF statements to sort what parsing we need to do. parseSearchParams parseWorkflowParams etc
		JSONObject content = response.getMessageStructured();
		
		String type = (String) content.get("request_type");
		
		if(type == null)
		{
			throw new OpenEditException("No type specified in results: " + content.toJSONString());
		}

		JSONObject details = (JSONObject) content.get("request_details");
		
		if(details == null)
		{
			throw new OpenEditException("No details specified in results: " + content.toJSONString());
		}

		if( type.equals("search") )
		{
			JSONObject structure = (JSONObject) details.get(type);
			if(structure == null)
			{
				throw new OpenEditException("No structure found for type: " + type);
			}
			type = partsSearchParts(inAgentContext, structure, type, response.getMessage());
		}
		else if( type.equals("conversation"))
		{
			//type = "chitchat";
//			JSONObject structure = (JSONObject) results.get(type);
			JSONObject conversation = (JSONObject) details.get("conversation");
			String generalresponse = (String) conversation.get("friendly_response");
//			if(generalresponse != null)
//			{
			//String generalresponse = (String) content.get("response");
//			}
			response.setMessage( generalresponse);
		}
		else if(type.equals("create_image"))
		{
			type = "createImage";
			
			AiCreation creation = inAgentContext.getAiCreationParams();					
			creation.setCreationType("image");
			JSONObject structure = (JSONObject) details.get("create_image");
			creation.setImageFields(structure);
		}
		else if(type.equals("create_entity"))
		{
			type = "createEntity";
			
			AiCreation creation = inAgentContext.getAiCreationParams();
			creation.setCreationType("entity");
			JSONObject structure = (JSONObject) details.get("create_entity");
			creation.setEntityFields(structure);
		}
		response.setFunctionName(type);
		return response;
	}
	

	protected String partsSearchParts(AgentContext inAgentContext, JSONObject structure, String type, String messageText) 
	{
		ArrayList tables = (ArrayList) structure.get("tables");
		
		if( tables == null)
		{
			return type;
		}
		
		log.info("AI Assistant Searching Tables: " + structure.toJSONString());
		
		AiSearch search = inAgentContext.getAiSearchParams();
		search.setOriginalSearchString(messageText);
		
		search.setStep1(null);
		search.setStep2(null);
		search.setStep3(null);
		
		for (Iterator iterator = tables.iterator(); iterator.hasNext();)
		{
			JSONObject jsontable = (JSONObject) iterator.next();
			AiSearchTable searchtable = new AiSearchTable();
			
			String targetTable = (String) jsontable.get("name");
//			if( "Not Specified".equals(targetTable) )
//			{
//				targetTable = "";
//			}
			searchtable.setTargetTable(targetTable);

			Map filters = (Map) jsontable.get("parameters");
			searchtable.setParameters(filters);
			
			String foreigntablename = (String) jsontable.get("foreign_table");
			if( foreigntablename != null)
			{
				AiSearchTable ftable = new AiSearchTable();
				ftable.setTargetTable(foreigntablename);
				
				Map parameters = (Map) jsontable.get("foreign_table_parameters");
				ftable.setParameters(parameters);
				
				searchtable.setForeignTable(ftable);
				
			}
			
			
			if (search.getStep1() == null)
			{
				search.setStep1(searchtable);
			}
			else
			{
//				if( "join".equals( step.get("operation")) || search.getStep1().getTargetTable().equals(targetTable) )
//				{
//					continue; //Duplicate
//				}
				if (search.getStep2() == null)
				{
					search.setStep2(searchtable);
				}
				else if (search.getStep3() == null)
				{
					search.setStep3(searchtable);
				}
			}
			
//			if("Not specified".equals(targetTable))
//			{
//				return "searchMultiple";
//			}

		}
		
		if(search.getStep1() == null)
		{
			type = "chitchat"; 
			return type;
		}

		if( "Not specified".equalsIgnoreCase( search.getStep1().getTargetTable() ) )
		{
			Data modulesearch = getMediaArchive().getCachedData("module", "modulesearch");
			search.getStep1().setModule(modulesearch);
			type = "searchTables";
		}	
		else
		{
			
			if(search.getStep2() == null && search.getStep1().getForeignTable() != null)
			{
				search.setStep2( search.getStep1() );
				
				search.setStep1( search.getStep2().getForeignTable() );
			}
			
			String text = "Search";
			
			if( search.getStep2() != null)
			{
				text = text + " for " + search.getStep2().getTargetTable() + " in " + search.getStep1().getTargetTable();
			}
			else if( search.getStep1() != null)
			{
				text = text + " for " + search.getStep1().getTargetTable();
			}
			
			SemanticTableManager manager = loadSemanticTableManager("actionembedding");
			List<Double> tosearch = manager.makeVector(text);
			Collection<RankedResult> suggestions = manager.searchNearestItems(tosearch);
			//Load more details into this request and possibly change the type
			if( !suggestions.isEmpty())
			{
				inAgentContext.setRankedSuggestions(suggestions);
				RankedResult top = (RankedResult)suggestions.iterator().next();
				if ( top.getDistance() < .7 )
				{
					type = top.getEmbedding().get("aifunction");  //More specific type of search
				
					AiSearch aisearch = processAISearchArgs(structure, top.getEmbedding(), inAgentContext);
					inAgentContext.setAiSearchParams(aisearch);
				}
			}
			else
			{
				type = "chitchat";
			}
		}
		return type;
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
	
	public AiSearch processAISearchArgs(JSONObject airesults, Data inEmbeddingMatch, AgentContext inContext)
	{
		//Search for tomatoes in sales departments
		//airesults
		AiSearch tables = inContext.getAiSearchParams();
		
		if (inEmbeddingMatch != null)
		{
			String parentid = inEmbeddingMatch.get("parentmodule");
			Data parentmodule = getMediaArchive().getCachedData("module", parentid);
			if (parentmodule != null)
			{
				tables.getStep1().setModule(parentmodule);
			}
			if (tables.getStep2() != null) //Not needed?
			{
				String childid = inEmbeddingMatch.get("childmodule");
				Data childmodule = getMediaArchive().getCachedData("module", childid);
				if (childmodule != null)
				{
					tables.getStep2().setModule(childmodule);
				}
			}
		}
		return tables;
	}


	public void searchTables(WebPageRequest inReq, AiSearch inAiSearchParams)
	{
		AiSearchTable step1 = inAiSearchParams.getStep1();
		AiSearchTable step2 = inAiSearchParams.getStep2();
		

		//		AiSearchPart part3 = inAiSearchParams.getPart3();
		
		HitTracker entityhits = null;
		HitTracker assethits = null;
		
		String step1Keyword = null;
		String step2Keyword = null;
		
		if(step1 != null && step2 != null)
		{
			
			step1Keyword = step1.getParameterValues();
			
			HitTracker foundhits = getMediaArchive().query(step1.getModule().getId()).freeform("description", step1Keyword).search();
			
			if( foundhits.isEmpty() )
			{
				return;
			}
			Collection<String> ids = foundhits.collectValues("id");

			inReq.putPageValue("module", step2.getModule());
			
			QueryBuilder search = getMediaArchive().query(step2.getModule().getId()).named("assitedsearch").orgroup(step1.getModule().getId(),ids);
			step2Keyword = step2.getParameterValues();
			if( step2Keyword != null)
			{
				if (step1Keyword == null || !step1Keyword.equalsIgnoreCase(step2Keyword))  //Remove the keyword if is  same
				{
					//Remove Module Name from Keywords
					/*
					String step1table = step1.getTargetTable();
					step2Keyowrd = step2Keyowrd.replace(step1table, "");
					if (step1table.endsWith("s"))
					{
						step1table = step1table.substring(0, step1table.length()-1);
						step2Keyowrd = step2Keyowrd.replace(step1table, "");
					}
					*/
					search.freeform("description", step2Keyword);
				}
			}
			entityhits = search.search();
			
			step2.setCount((long) entityhits.size());
			
			//inReq.putPageValue( finalhits.getSessionId(), finalhits);
			
		}
		else if(step1 != null)
		{
			
			step1Keyword  = step1.getParameterValues();
			
			inReq.putPageValue("module", step1.getModule());
			if(step1Keyword != null)
			{
				QueryBuilder search = null;
				
				Schema schema = loadSchema();
				Collection<Data> modules = schema.getChildrenOf(step1.getModule().getId());
				Collection<String> moduleids = new ArrayList<String>();
				moduleids.add(step1.getModule().getId());
				//step1.addModule(step1.getModule());
				for (Iterator iterator = modules.iterator(); iterator.hasNext();)
				{
					Data mod = (Data) iterator.next();
					moduleids.add(mod.getId());
					step1.addModule(mod);
				}
				
				if ( moduleids.contains("modulesearch") || moduleids.size() > 1)
				{
					
					if( moduleids.size() == 1 || moduleids.contains("modulesearch"))
					{
						moduleids = schema.getModuleIds();
					}
					
					search = getMediaArchive().query("modulesearch")
						.addFacet("entitysourcetype")
						.put("searchtypes", moduleids).includeDescription(true);
					entityhits = search.freeform("description", step1Keyword).search();
					log.info(" Here:  "+ entityhits.getActiveFilterValues() );
					//TODO: Not sure this is needed or works
					if( moduleids.contains("asset") )
					{
						//Loop over the categories in these entityhits
						Collection categories = entityhits.collectValues("rootcategory");
						search = getMediaArchive().query("asset").orgroup("category",categories);
						assethits = search.freeform("description", step1Keyword).search();
						step1.setCount( (long) (entityhits.size()  + assethits.size()));
					}
					else
					{
						step1.setCount((long) entityhits.size());
					}
					
				}
				else if( "asset".equals( step1.getModule().getId() ) )
				{
					search = getMediaArchive().query(step1.getModule().getId());
					assethits = search.freeform("description", step1Keyword).search();
					step1.setCount((long) assethits.size());
				}	
				else {
					search = getMediaArchive().query(step1.getModule().getId());
					entityhits = search.freeform("description", step1Keyword).search();
					step1.setCount((long) entityhits.size());
				}
			}
			else
			{
				//step1.addModule(step1.getModule());
				entityhits = getMediaArchive().query(step1.getModule().getId()).all().search();
				step1.setCount((long) entityhits.size()); 
			}
			
		}
		
		if(entityhits != null )
		{
			inReq.putPageValue("hits", entityhits);
		}
		else
		{
			inReq.putPageValue("hits", assethits);
		}
		
		organizeResults(inReq, entityhits, assethits);
		
		String semanticquery = "";
		
		if(step1Keyword != null)
		{
			semanticquery = step1Keyword;
		}
		if(step2Keyword != null)
		{
			semanticquery = semanticquery + " " + step2Keyword;
		}
		
		if(semanticquery.isBlank())
		{
			String originalQuery = inAiSearchParams.getOriginalSearchString();
			if( originalQuery != null)
			{
				originalQuery = originalQuery.replaceAll("search|find|look for|show me", "").trim();
				semanticquery = originalQuery;
			}
		}

		if(!semanticquery.isBlank())
		{			
			inReq.putPageValue("semanticquery", semanticquery.trim());
		}
		
	}
	
	
	public void organizeResults(WebPageRequest inReq, HitTracker entityhits, HitTracker assetunsorted) 
	{

		inReq.putPageValue("assethits", assetunsorted);
		
		if(entityhits != null && assetunsorted == null)
		{
			inReq.putPageValue("totalhits", entityhits.size());
		}
		else if(entityhits == null && assetunsorted != null)
		{
			inReq.putPageValue("totalhits", assetunsorted.size());
		}
		else
		{
			inReq.putPageValue("totalhits", entityhits.size() + assetunsorted.size());
		}

		getResultsManager().loadOrganizedResults(inReq, entityhits, assetunsorted);
		
		/**
		 * log.info("Searching as:" + inReq.getUser().getName());
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
		*/
		
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
		OpenAiConnection llmconnection = (OpenAiConnection) archive.getLlmConnection("ragSearch");
		
		llmconnection.callRagFunction(ragcontext.get("context"), ragcontext.get("query"));
		
	}
	
	public void semanticSearch(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive();
		
		String semanticquery = inReq.getRequestParameter("semanticquery"); 
		
		Collection<String> excludeEntityIds = inReq.getRequestCollection("excludeentityids");
		Collection<String> excludeAssetIds = inReq.getRequestCollection("excludeassetids");
		
		log.info("Semantic Search for: " + semanticquery);
		inReq.putPageValue("input", semanticquery);
		
		Map<String, Collection<String>> relatedEntityIds = getSemanticTopicManager().search(semanticquery, excludeEntityIds, excludeAssetIds);
		
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
//		Collection<String> keywords = searchArgs.getKeywords();
//		inReq.putPageValue("keywordsstring", getResultsManager().joinWithAnd(keywords));
//		
//		Collection<Data> modules = searchArgs.getSelectedModules();
//		
//	
//		Collection<String> moduleNames = new ArrayList<String>();
//			
//		for (Iterator iterator = modules.iterator(); iterator.hasNext();)
//		{
//			Data module = (Data) iterator.next();
//			if(!moduleNames.contains(module.getName()))
//			{
//				moduleNames.add(module.getName());
//			}
//		}
//		
//		inReq.putPageValue("modulenamestext", getResultsManager().joinWithAnd(moduleNames));
		
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

		AgentContext agentcontext = new AgentContext();
		agentcontext.addContext("fulltext", fullText);
		
		String model = archive.getCatalogSettingValue("llmmcpmodel");
		if(model == null)
		{
			model = "gpt-5-nano";
		}
		agentcontext.addContext("model", model);

		LlmConnection llmconnection = (LlmConnection) archive.getBean("openaiConnection");
		
		String chattemplate = "/" + archive.getMediaDbId() + "/ai/openai/mcp/prompts/generate_report.json";
		LlmResponse response = llmconnection.runPageAsInput(agentcontext, chattemplate);
		
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

	protected SemanticClassifier fieldSemanticTopicManager;
	public SemanticClassifier getSemanticTopicManager()
	{
		if (fieldSemanticTopicManager == null)
		{
			fieldSemanticTopicManager = (SemanticClassifier)getModuleManager().getBean(getCatalogId(), "semanticClassifier",false);
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
	
	
	
	public void loadAllActions(ScriptLogger inLog)
	{
		SemanticTableManager manager = loadSemanticTableManager("actionembedding");
		
		//Create batch of english words that describe how to search all these things
		Schema shema = loadSchema();
		
		Searcher embedsearcher = getMediaArchive().getSearcher("actionembedding");
		
		for (Iterator iterator = shema.getModules().iterator(); iterator.hasNext();)
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
			action.setAiFunction("searchTables");
			action.setSemanticText("Search for " + parentmodule.getName());
			action.setParentData(parentmodule);
			actions.add(action);
//			action = new SemanticAction();
//			action.setParentData(parentmodule);
//			action.setAiFunction("createEntity");
//			action.setSemanticText("Create a new " + parentmodule.getName());
//			actions.add(action);
			
			//Check for child views
			Collection<Data> children = shema.getChildrenOf(parentmodule.getId());
			
			for (Iterator iterator2 = children.iterator(); iterator2.hasNext();)
			{
				Data childmodule = (Data) iterator2.next();
				action = new SemanticAction();
				action.setParentData(parentmodule);
				action.setChildData(childmodule);
				action.setAiFunction("searchTables");
				action.setSemanticText("Search for " + childmodule.getName() + " in " + parentmodule.getName());
				actions.add(action);
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
				data.setValue("aifunction",semanticAction.getAiFunction());
				data.setName(semanticAction.getSemanticText());
				
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

	private Schema loadSchema()
	{
		Schema schema = (Schema)getMediaArchive().getCacheManager().get("assitant","schema");
		
		if( schema == null)
		{
			schema = new Schema();
			HitTracker allmodules = getMediaArchive().query("module").exact("showonsearch",true).search();
			Collection<Data> modules = new ArrayList();
			Collection<String> moduleids = new ArrayList();
			
			for (Iterator iterator = allmodules.iterator(); iterator.hasNext();)
			{
				Data module = (Data) iterator.next();
				Data record = getMediaArchive().query(module.getId()).all().searchOne();
				
				if(record != null)
				{
					modules.add(module);
					moduleids.add(module.getId());
					
					Collection detailsviews = getMediaArchive().query("view").exact("moduleid", module.getId()).exact("rendertype", "entitysubmodules").cachedSearch();  //Cache this

					for (Iterator iterator2 = detailsviews.iterator(); iterator2.hasNext();)
					{
						Data view = (Data) iterator2.next();
						String listid = view.get("rendertable");
						if( moduleids.contains(listid) )
						{
							Data childmodule = getMediaArchive().getCachedData("module", listid);
							schema.addChildOf(module.getId(),childmodule);
						}
					}
				}
			}
			schema.setModules(modules);
			schema.setModuleIds(moduleids);
			getMediaArchive().getCacheManager().put("assitant","schema",schema);
			
		}
		
		return schema;
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

	public Collection<GuideStatus> prepareDataForGuide(Data inEntityModule, Data inEntity)
	{
		//Should we look for children...
		Collection<GuideStatus> statuses = new ArrayList<GuideStatus>();
		
		Collection detailsviews = getMediaArchive().query("view").exact("moduleid", inEntityModule.getId()).exact("systemdefined",false).cachedSearch(); 

		for (Iterator iterator = detailsviews.iterator(); iterator.hasNext();)
		{
			Data view = (Data) iterator.next();
			
			String listid = view.get("rendertable");
			if( listid != null)
			{
				GuideStatus status = new GuideStatus();
				status.setViewData(view);
				
				HitTracker found = null;
				try
				{
					found = getMediaArchive().query(listid).exact(inEntityModule.getId(),inEntity.getId()).facet("entityembeddingstatus").search();
				}
				catch (Exception e)
				{
					log.error("Entity must have entityembeddingstatus field to use guide feature.");
					continue;
				}
				
				if(found.isEmpty())
				{
					continue;
				}
				

				

				FilterNode value = found.findFilterChildValue("entityembeddingstatus", "embedded");
				int embeddedcount = value != null ? value.getCount() : 0;
				
				status.setCountEmbedded(embeddedcount);
				
				value = found.findFilterChildValue("entityembeddingstatus", "pending");
				int pendingcount = value != null ? value.getCount() : 0;
				
				status.setCountPending(pendingcount);
				
				value = found.findFilterChildValue("entityembeddingstatus", "failed");
				int failedcount = value != null ? value.getCount() : 0;

				status.setCountFailed(failedcount);
				
				int totalcount = found.size();				
				status.setCountTotal(totalcount);
				
				value = found.findFilterChildValue("entityembeddingstatus", "notembedded");
				int notembeddedcount = value != null ? value.getCount() : 0;
				notembeddedcount += totalcount - (embeddedcount + pendingcount + failedcount);
				status.setCountNotEmbedded(notembeddedcount);
				
				statuses.add(status);
				
				ScriptLogger inLog = new ScriptLogger();
				
				//If not done
				if( status.getCountNotEmbedded() > 0 )
				{	
					InformaticsProcessor processor = getInformaticManager().loadProcessor("embeddingManager");
					Collection<MultiValued> tosave = new ArrayList<MultiValued>();
					for (Iterator<MultiValued> iterator2 = found.iterator(); iterator2.hasNext();)
					{
						MultiValued data = (MultiValued) iterator2.next();
						String embeddingstatus = data.get("entityembeddingstatus");
						if( !"embedded".equals(embeddingstatus) && !"pending".equals(embeddingstatus) && !"failed".equals(embeddingstatus) )
						{
							tosave.add(data);
						}
					}
					status.setCountPending( status.getCountPending() + tosave.size());
					MultiValued config = new BaseData();
					config.setValue("searchtype", listid);
					processor.processInformaticsOnEntities(inLog, config, tosave);
					
					status.setCountEmbedded(status.getCountEmbedded() + tosave.size());
					status.setCountNotEmbedded( status.getCountNotEmbedded() - tosave.size());
					
					getMediaArchive().saveData(listid, tosave);
				}
			}
		}
		
		
		return statuses;
		
	}
	
	public InformaticsManager getInformaticManager()
	{
		InformaticsManager manager = (InformaticsManager)getMediaArchive().getBean("informaticsManager");
		return manager;
	}	
	
}
