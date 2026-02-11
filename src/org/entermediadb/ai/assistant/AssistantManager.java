package org.entermediadb.ai.assistant;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.ChatMessageHandler;
import org.entermediadb.ai.informatics.InformaticsManager;
import org.entermediadb.ai.informatics.InformaticsProcessor;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.find.EntityManager;
import org.entermediadb.net.HttpSharedConnection;
import org.entermediadb.scripts.ScriptLogger;
import org.entermediadb.websocket.chat.ChatServer;
import org.entermediadb.workspace.WorkspaceManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.WebPageRequest;
import org.openedit.data.BaseData;
import org.openedit.data.PropertyDetail;
import org.openedit.data.Searcher;
import org.openedit.hittracker.FilterNode;
import org.openedit.hittracker.HitTracker;
import org.openedit.profile.UserProfile;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.PathUtilities;

public class AssistantManager extends BaseAiManager
{
	private static final Log log = LogFactory.getLog(AssistantManager.class);
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
			agent.setFirstName("AI");
			agent.setLastName("Guide");
			agent.setValue("screenname", "AI Guide");
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
			Data data = (Data) iterator.next();
			//log.info("Processing channel: " + data.getId());
			Data channel = (Data)archive.getCachedData("channel", data.getId());
			if( channel.getName() == null)  //Make smarter
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
			
			if (mostrecents.isEmpty())
			{
				continue;
			}

			//Remove from DB
			//TODO: Use a local lock
			Collection tosave = new ArrayList();
			
			for (Iterator iterator2 = mostrecents.iterator(); iterator2.hasNext();)
			{
				Data lockdata = (Data) iterator2.next();
				lockdata.setValue("chatmessagestatus","processing");
				tosave.add(lockdata);
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
			}
			Data channel = getMediaArchive().getCachedData("channel", inChannelId);
			if(channel == null)
			{
				log.error("Should not have to create new channel");
				channel = getMediaArchive().getSearcher("channel").createNewData();
				channel.setId(inChannelId);
				channel.setValue("date",new Date());
				channel.setValue("refreshdate",new Date());
				String siteid = PathUtilities.extractDirectoryPath(getMediaArchive().getCatalogId());
				channel.setValue("chatapplicationid",siteid + "/find");
				getMediaArchive().saveData("channel",channel);
				
			}
			agentContext.setChannel(channel);
			agentContext.setValue("channel", inChannelId);
			agentContext.setValue("entityid", channel.get("dataid"));
			agentContext.setValue("entitymoduleid", channel.get("searchtype"));
			searcher.saveData(agentContext);

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
		getMediaArchive().saveData("chatterbox",usermessage);  //Update the user message again to finish it
		
		agentContext.addContext("message", usermessage);
		
		agentContext.addContext("assistant", this);
		
		agentContext.addContext("channelchathistory", loadChannelChatHistory(inChannel));
		
		//Add new agentmessage
		MultiValued agentmessage = newAgentMessage(usermessage, agentContext);
		
		//ChatServer server = (ChatServer) getMediaArchive().getBean("chatServer");
		//Determine what will need to be processed
		try
		{
			String functionName = null;
			String playbackentitymoduleid = inChannel.get("playbackentitymoduleid");
			if( playbackentitymoduleid != null)
			{
				functionName = "startCreator_" + playbackentitymoduleid;
				Integer playbacksection = (Integer) inChannel.getValue("playbacksection");
				if( playbacksection != null)
				{
					agentContext.addContext("playbacksection", playbacksection);
				}
				else
				{
					agentContext.addContext("playbacksection", 0);
				}
			}
			else
			{
				String toplevelaifunctionid = agentContext.getTopLevelFunctionName();
				if(toplevelaifunctionid == null)
				{
					log.error("This should never happen");
					return;
				}
			}
			//agentContext.setFunctionName(functionName);
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
		if( functionName == null)
		{
			functionName = agentContext.getNextFunctionName();
		}
		MultiValued function = (MultiValued) getMediaArchive().getCachedData("aifunction", functionName);
		
		if (function == null)
		{
			log.info("No Ai function defined: " + functionName);
			return;
		}
		
		ChatServer server = (ChatServer) getMediaArchive().getBean("chatServer");

		
		String loader = "<i class=\"fas fa-spinner fa-spin mr-2\"></i> ";
		
		String processingmessage = function.get("processingmessage");
		
		if( processingmessage == null )
		{
			processingmessage = "Analyzing";
		}

		String processingtype = (String) agentContext.getContextValue("processingtype");
		if( processingtype != null)
		{
			processingmessage += " " + processingtype;
		}
		
		processingmessage = loader + processingmessage + "...";
		
		String message = agentContext.getMessagePrefix() + processingmessage;
		agentmessage.setValue("message", message ); //setting status
		getMediaArchive().saveData("chatterbox",agentmessage);	
		server.broadcastMessage(getMediaArchive().getCatalogId(), agentmessage);
	
		MediaArchive archive = getMediaArchive();

		Data channel = archive.getCachedData("channel", agentmessage.get("channel"));
		agentContext.addContext("channel", channel);
		
		agentContext.addContext("usermessage", usermessage);
		agentContext.addContext("agentmessage", agentmessage);
		
		agentContext.addContext("aisearchparams", agentContext.getAiSearchParams() ); // ??
		
		String apphome = "/"+ channel.get("chatapplicationid");
		agentContext.addContext("apphome", apphome);
		
		String bean = function.get("messagehandler");
		
		ChatMessageHandler handler = (ChatMessageHandler) getMediaArchive().getBean( bean);
		LlmResponse response = null;
		try
		{		
			response = handler.processMessage(agentContext, agentmessage, function);
		}
		catch (Exception e) 
		{
			log.error("Error from " + bean + " running " + function.getId(),e);
			response = handleError(agentContext, e.getMessage());
		}
		
		try
		{	
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
			
			String agentNextFn = agentContext.getNextFunctionName();
			if( agentNextFn != null)
			{
				Long wait = agentContext.getWaitTime();
				if( wait != null && wait instanceof Long)
				{
					agentContext.setWaitTime(null);
					waittime = wait;
					log.info("Previous function requested to wait " + waittime + " milliseconds");
					Thread.sleep(wait);
				}
				agentContext.setFunctionName(agentNextFn);
				agentContext.setNextFunctionName(null);
				execCurrentFunctionFromChat(usermessage, agentmessage, agentContext);
				//Save the current state
			}
		}
		catch (Exception e)
		{
			log.error("Could not execute function: " + agentContext.getFunctionName(), e);
			agentmessage.setValue("functionresponse", e.toString());
			agentmessage.setValue("chatmessagestatus", "failed");
			archive.saveData("chatterbox", agentmessage);
		}
		finally
		{
			getMediaArchive().saveData("agentcontext",agentContext);
		}
	}
	
	protected Collection<Data> loadChannelChatHistory(Data inChannel)
	{
		HitTracker messages = getMediaArchive().query("chatterbox").exact("channel", inChannel).sort("dateUp").search();
		
		Collection<Data> recent = new ArrayList<Data>();
		
		for (Iterator iterator = messages.iterator(); iterator.hasNext();) {
			Data message = (Data) iterator.next();
			if("system".equals(message.get("messagetype")))
			{
				continue;
			}
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

	/*
	public HitTracker getFunctions()
	{
		HitTracker hits = getMediaArchive().query("aifunctions").exact("pipeline", "assistant").exact("enabled", true).sort("ordering").cachedSearch();
		return hits;
	}
	*/
	
	//TODO Not used?
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
	
	/*
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
	*/
	
	//TODO Not used?
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
	
	
	public Collection<GuideStatus> prepareDataForGuide(Data inEntityModule, Data inEntity)
	{
		//Should we look for children...
		Collection<GuideStatus> statuses = new ArrayList<GuideStatus>();
	
		PropertyDetail detail = getMediaArchive().getSearcher(inEntityModule.getId()).getDetail("entityembeddingstatus");
		if( detail != null)
		{
			String mystatus = inEntity.get("entityembeddingstatus"); 
			if(mystatus == null)
			{
				mystatus = "notembedded";
			}
			if(mystatus != null && "embedded".equals(mystatus))
			{
				//Ready to roll
				GuideStatus status = new GuideStatus();
				status.setSearchType(inEntityModule.getId());
				//status.setViewData(view); //General Data?
				status.setCountTotal(1);
				status.setCountEmbedded(1);
				statuses.add(status);
			}
		}
		
		Collection detailsviews = getMediaArchive().query("view").exact("moduleid", inEntityModule.getId()).exact("systemdefined",false).cachedSearch(); 

		for (Iterator iterator = detailsviews.iterator(); iterator.hasNext();)
		{
			Data view = (Data) iterator.next();
			
			String listid = view.get("rendertable");
			if( listid != null)
			{
				GuideStatus status = new GuideStatus();
				status.setSearchType(listid);
				status.setViewData(view);
				
				HitTracker found = null;
				try
				{
					found = getMediaArchive().query(listid).exact(inEntityModule.getId(),inEntity.getId()).facet("entityembeddingstatus").search();
				}
				catch (Exception e)
				{
					log.debug(inEntityModule + " search error " + inEntity);
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
	
	public Collection<String> findDocIdsForEntity(String parentmoduleid, String inEntityId)
	{
		MediaArchive archive = getMediaArchive();
		
		Data inEntityModule = archive.getCachedData("module", parentmoduleid);

		Data inEntity = archive.getCachedData(parentmoduleid, inEntityId);
		JSONArray docids = new JSONArray();
		
		//Always Check itself first
		PropertyDetail detail = getMediaArchive().getSearcher(parentmoduleid).getDetail("entityembeddingstatus");
		if( detail != null)
		{
			String mystatus = inEntity.get("entityembeddingstatus"); 
			if(mystatus != null && "embedded".equals(mystatus))
			{
				String docid = parentmoduleid + "_" + inEntity.getId();
				docids.add(docid);
			}
		}

		Collection detailsviews = getMediaArchive().query("view").exact("moduleid", parentmoduleid).exact("systemdefined",false).cachedSearch(); 

		Collection<String> searchypes = new ArrayList();
		
		for (Iterator iterator = detailsviews.iterator(); iterator.hasNext();)
		{
			Data view = (Data) iterator.next();
			
			String listid = view.get("rendertable");
			if( listid != null)
			{
				searchypes.add(listid);
			}
		}

		HitTracker hits = getMediaArchive().query("modulesearch").put("searchtypes",searchypes).exact("entityembeddingstatus","embedded").search();
		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
		{
			MultiValued doc = (MultiValued) iterator.next();
			String type = doc.get("entitysourcetype");
			String docid = type + "_" + doc.getId();
			docids.add(docid);
		}
		return docids;
	}
	
	public InformaticsManager getInformaticManager()
	{
		InformaticsManager manager = (InformaticsManager)getMediaArchive().getBean("informaticsManager");
		return manager;
	}

	public void sendSystemMessage(AgentContext inContext, String inUser, String message)
	{
		MediaArchive archive = getMediaArchive();
		
		//currentchannel set next function
		//Save a message
		Data chat = archive.getSearcher("chatterbox").createNewData();
		chat.setValue("messagetype", "system");
		chat.setValue("user", inUser);
		chat.setValue("date", new Date());
		chat.setValue("chatmessagestatus","received");
		chat.setValue("channel", inContext.getChannel().getId());
		chat.setValue("message", message);
		
		archive.saveData("chatterbox",chat);
		//inReq.putPageValue("chat", chat);
		
		//Fire monitor
		archive.fireSharedMediaEvent("llm/monitorchats");

	}

	public void monitorAiServers(ScriptLogger inLog)
	{
		Collection<Data> currentservers = getMediaArchive().query("aiserver").exact("monitorspeed", true).search();
		
		Map<String,Integer> speeds = new HashMap();
		ArrayList tosave = new ArrayList();
		
		for(Data server : currentservers)
		{
			String serverroot = server.get("serverroot");
			String address = serverroot + "/health";
			
			Integer speed = speeds.get(serverroot);
			if( speed == null || speed == 0)
			{
				HttpSharedConnection connection = new HttpSharedConnection();
				String key = server.get("serverapikey");
				if( key != null)
				{
					connection.addSharedHeader("Authorization", "Bearer " +  server);
				}
				long start = System.currentTimeMillis();
				try
				{
					JSONObject got = connection.getJson(address);
					if( got != null )
					{
						String ok = (String)got.get("status");
						if( "ok".equals(ok))
						{
							long end =  System.currentTimeMillis();
							Integer diff = Math.round(  end - start);
							inLog.info(address + " ok run in " + diff + " milliseconds");
							speeds.put(serverroot,diff);
						}
					}
				}
				catch( Exception ex)
				{
					inLog.info(address + " had error " + ex);
					speeds.put(serverroot,Integer.MAX_VALUE); //Push back
					//Ignore
				}
			}
		}
		if(!speeds.isEmpty())
		{
			inLog.info("Saving " + speeds);
			for(Data server : currentservers)
			{
				String serverroot = server.get("serverroot");
				Integer speed = speeds.get(serverroot);
				server.setValue("ordering", speed);
				tosave.add(server);
			}
			getMediaArchive().getSearcher("aiserver").saveAllData(tosave, null);
			getMediaArchive().getCacheManager().clear("llmconnection");
		}
	}
	
	
	public void resetAiServers(ScriptLogger inLog)
	{
		HashMap<String, String> keys = new HashMap();
		Collection<Data> currentservers = getMediaArchive().query("aiserver").all().search();
		for (Iterator iterator = currentservers.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			keys.put(data.getId(), data.get("serverapikey"));
		}
		
		Searcher aiserverSearcher = getMediaArchive().getSearcher("aiserver");
		aiserverSearcher.restoreSettings();
		
		List tosave = new ArrayList();
		currentservers = getMediaArchive().query("aiserver").all().search();
		for (Iterator iterator = currentservers.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			String key = keys.get(data.getId());
			if (key != null && !key.equals(data.get("serverapikey")))
			{
				data.setValue("serverapikey", key);
				tosave.add(data);
				
			}
		}
		aiserverSearcher.saveAllData(tosave, null);
	}

	public void addMissingFunctions(ScriptLogger inLog)
	{
		Collection<Data> modules =  getMediaArchive().getList("module");
		List tosave = new ArrayList();
		
		
		//reset ai servers
		resetAiServers(inLog);
		
		//reset aifunctions table
		Searcher aifunctionSearcher = getMediaArchive().getSearcher("aifunction");
		aifunctionSearcher.restoreSettings();
		
		List usetext = new ArrayList();
		int existing = 0;
		int created = 0;
		for (Iterator iterator = modules.iterator(); iterator.hasNext();)
		{
			
			MultiValued module = (MultiValued) iterator.next();
			String method = module.get("aicreationmethod");
			
			if( method == null)
			{				
				continue;
			}
			
			String id = "";	
			String messagehandler = "";
			
			if( method.equals("fieldsonly"))
			{
				id = "fieldsonly_welcome_" + module.getId();
				messagehandler = "entityCreationManager";
			}
			else if( method.equals("smartcreator"))
			{
				id = "smartcreator_welcome_" + module.getId();
				messagehandler = "smartCreatorManager";
			}
			
			Data exists = getMediaArchive().getData("aifunction", id);
			if( exists != null)
			{
				existing++;
				inLog.info(id+" AI function exists" + module.getName());
				continue;
			}
			
			//Add all these to ollamat
			Data welcome_aifunction = aifunctionSearcher.createNewData();
			welcome_aifunction.setId(id);
			welcome_aifunction.setValue("messagehandler", messagehandler);
			welcome_aifunction.setValue("toplevel", true);
			welcome_aifunction.setName("Create " + module.getName());
			welcome_aifunction.setValue("icon", module.get("moduleicon"));
			tosave.add(welcome_aifunction);
			
			created++;
			inLog.info(id+" AI function created for " + module.getName());
			
			/* using generic from now on
			if(method.equals("smartcreator"))
			{				
				id = "smartcreator_parse_" + module.getId();
				Data create_aifunction = getMediaArchive().getSearcher("aifunction").createNewData();
				create_aifunction.setId(id);
				create_aifunction.setValue("messagehandler", messagehandler);
				create_aifunction.setValue("toplevel", false);
				create_aifunction.setValue("processingmessage", "Parsing new " + module.getName());
				create_aifunction.setName("Parse " + module.getName());
				tosave.add(create_aifunction);
				usetext.add(create_aifunction);
				
				id = "smartcreator_createoutline_" + module.getId();
				create_aifunction = getMediaArchive().getSearcher("aifunction").createNewData();
				create_aifunction.setId(id);
				create_aifunction.setValue("messagehandler", messagehandler);
				create_aifunction.setValue("toplevel", false);
				create_aifunction.setValue("processingmessage", "Creating new " + module.getName());
				create_aifunction.setName("Createing " + module.getName());
				tosave.add(create_aifunction);
				usetext.add(create_aifunction);
				
				id = "smartcreator_play_" + module.getId();
				Data play_aifunction = getMediaArchive().getSearcher("aifunction").createNewData();
				play_aifunction.setId(id);
				play_aifunction.setValue("messagehandler", messagehandler);
				play_aifunction.setValue("toplevel", true);
				play_aifunction.setValue("processingmessage", "Playing " + module.getName());
				play_aifunction.setName("View " + module.getName());
				tosave.add(play_aifunction);
			}
			*/
			
			
		}
		getMediaArchive().saveData("aifunction", tosave);
		
		inLog.info("Functions created: " + created + " Existing: " + existing);
		
		Set tosaveservers = new HashSet();
		Collection servers = getMediaArchive().getList("aiserver");
		for (Iterator iterator = servers.iterator(); iterator.hasNext();)
		{
			MultiValued server = (MultiValued) iterator.next();
			if( server.getId().startsWith("llamat"))
			{
				for (Iterator iterator2 = usetext.iterator(); iterator2.hasNext();)
				{
					Data function = (Data) iterator2.next();
					if( !server.hasValue(function.getId()))
					{
						server.addValue("aifunctions", function.getId());
						tosaveservers.add(server);
					}
				}
			}
		}
		getMediaArchive().saveData("aiserver", tosaveservers);
		inLog.info("Updated servers " + tosaveservers.size() );
		
		//Add AI functions to mediadb
		WorkspaceManager workspaceManager =  (WorkspaceManager)getMediaArchive().getBean("workspaceManager");
		workspaceManager.createMediaDbAiFunctionEndPoints(getMediaArchive().getCatalogId());
		
		//Clear Cache
		getMediaArchive().clearAll();
		
	}
	
}
