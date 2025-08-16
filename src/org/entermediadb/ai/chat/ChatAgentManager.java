package org.entermediadb.ai.chat;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.manager.BaseManager;
import org.entermediadb.scripts.ScriptLogger;
import org.entermediadb.websocket.chat.ChatServer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.profile.UserProfile;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;

public class ChatAgentManager extends BaseManager {
	public void monitorChannels(ScriptLogger log) throws Exception
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
		
		HitTracker allchannels = channels.query().exact("aienabled", true).after("refreshdate",now.getTime()).sort("refreshdateDown").search();
		//DateFormat fm = DateStorageUtil.getStorageUtil().getDateFormat("dd/MM/yyyy hh:mm");

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
			
			respondToChannel(log, channel, mostrecent);
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
	
	public void respondToChannel(ScriptLogger log, Data channel, Data message) throws Exception
	{
		MediaArchive archive = getMediaArchive();
		
		Map params = new HashMap();
		
		String model = archive.getCatalogSettingValue("gpt-model");
		
		if (model == null)
		{
			model = "gpt-4o"; // Default fallback
		}
		LlmConnection manager = archive.getLlmConnection(model);

		params.put("model", model);

		Date now = new Date();
		DateFormat fm = DateStorageUtil.getStorageUtil().getDateFormat("dd/MM/yyyy hh:mm");

		ChatServer server = (ChatServer) archive.getBean("chatServer");
		Searcher chats = archive.getSearcher("chatterbox");
		
		if (!manager.isReady()) 
		{
			log.error("LLM Manager is not ready: " + model + ". Cannot process channel: " + channel);
			return;
		}

		HitTracker recent = chats.query().exact("channel", channel.getId()).sort("dateUp").search();
		params.put("recent", recent);

		String channeltype = channel.get("channeltype");
		if (channeltype == null)
		{
			channeltype = "chatstreamer";
		}

		if (message != null)
		{
			String id = message.get("user");
			if (!id.equals("agent"))
			{
				UserProfile profile = archive.getUserProfile(id);
				params.put("chatprofile", profile);
			}

		}
		
		params.put("channel", channel);

		//Update original message processing status
		message.setValue("processingcomplete", true);
		chats.saveData(message);
		
		params.put("message", message);


		String chattemplate = "/" + archive.getMediaDbId() + "/gpt/inputs/" + manager.getServerName() + "/" + channeltype + ".html";
		LlmResponse response = manager.runPageAsInput(params, model, chattemplate);

		if (response.isToolCall())
		{
			// Function call detected
			String functionName = response.getFunctionName();
			JSONObject arguments = response.getArguments();
			
			if(arguments.get("types") == null)
			{
				arguments.put("types", new JSONArray());
			}

			String json = arguments.toJSONString();
			// Create and save function call message
			Data functionMessage = chats.createNewData();
			functionMessage.setValue("user", "agent");
			functionMessage.setValue("channel", channel.getId());
			//functionMessage.setValue("messagetype", "function_call");
			//functionMessage.setValue("function", functionName);
			//functionMessage.setValue("message", json);
			functionMessage.setValue("arguments", json);
			functionMessage.setValue("date", new Date());
			functionMessage.setValue("message", "Processing function " + functionName);
			functionMessage.setValue("processingcomplete", true);
			
			chats.saveData(functionMessage);
			
			LlmConnection connection = archive.getLlmConnection(model);
		
			connection.callChatFunction(functionMessage, functionName, params);
			
			archive.saveData("chatterbox", functionMessage);
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
		
		//Dont listen again for a bit?
//		channel.setValue("aienabled", "false" );
//		archive.saveData("channel",channel);
		
	}

}
