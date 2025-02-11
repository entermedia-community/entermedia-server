/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.entermediadb.websocket.chat;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.llm.BaseLLMManager;
import org.entermediadb.llm.GptManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.QueryBuilder;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.ListHitTracker;
import org.openedit.profile.UserProfile;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.ExecutorManager;

public class ChatModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(ChatConnection.class);

	public void loadMessage(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String messageid = inReq.getRequestParameter("messageid");
		List messageids = new ArrayList(1);
		messageids.add(messageid);
		inReq.putPageValue("messageids", messageids);
		loadReactions(inReq);
		loadAttachments(inReq);

		Data chat = archive.getCachedData("chatterbox", messageid);
		inReq.putPageValue("chat", chat);
	}

	public void loadRecentChatsLibraryCollection(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String channel = inReq.findValue("channel");

		String collectionid = inReq.getRequestParameter("collectionid");
		if (collectionid == null)
		{
			collectionid = inReq.findValue("collectionid");
		}

		if (collectionid == null)
		{
			Data librarycol = (Data) inReq.getPageValue("librarycol");
			if (librarycol != null)
			{
				collectionid = librarycol.getId();
			}
		}

		Searcher topicsearcher = archive.getSearcher("collectiveproject");
		Data currenttopic = null;
		if (channel != null)
		{
			currenttopic = topicsearcher.query().exact("id", channel).searchOne();
		}
		if (currenttopic == null)
		{
			currenttopic = topicsearcher.query().match("parentcollectionid", collectionid).sort("name").searchOne();
		}

		if (currenttopic == null)
		{
			currenttopic = topicsearcher.createNewData();
			currenttopic.setValue("parentcollectionid", collectionid);
			currenttopic.setName("General");
			topicsearcher.saveData(currenttopic);
		}

		inReq.putPageValue("currenttopic", currenttopic);
		inReq.putPageValue("channel", currenttopic.getId());
		channel = currenttopic.getId();

		String sortby = inReq.findActionValue("sortorder");
		if (sortby == null)
		{
			sortby = "dateDown";
		}

		QueryBuilder builder = archive.query("chatterbox");

		if (channel != null)
		{
			builder.named("messagesthitracker").exact("channel", channel).sort(sortby);
		}

		UserProfile prof = inReq.getUserProfile();
		if (prof != null)
		{
			Collection blocked = prof.getValues("blockedusers");
			if (blocked != null && !blocked.isEmpty())
			{
				builder.notgroup("user", blocked);
			}
		}

		HitTracker results = builder.search(inReq);
		if (results != null)
		{
			results.setHitsPerPage(20);
			inReq.putPageValue(results.getHitsName(), results);
		}

		//loadPageOfChat(inReq);

	}

	public void loadRecentChats(WebPageRequest inReq)
	{

		//OI Chats using -> loadRecentChatsLibraryCollection()
		MediaArchive archive = getMediaArchive(inReq);

		Data channel = loadCurrentChannel(inReq);

		String sortby = inReq.findActionValue("sortorder");
		if (sortby == null)
		{
			sortby = "dateDown";
		}

		QueryBuilder builder = archive.query("chatterbox");

		builder.named("messagesthitracker").exact("channel", channel.getId()).sort(sortby);

		UserProfile prof = inReq.getUserProfile();
		if (prof != null)
		{
			Collection blocked = prof.getValues("blockedusers");
			if (blocked != null && !blocked.isEmpty())
			{
				builder.notgroup("user", blocked);
			}
		}

		HitTracker results = builder.search(inReq);
		if (results != null)
		{
			results.setHitsPerPage(20);
			inReq.putPageValue(results.getHitsName(), results);
		}
	}

	/*
	 * 
	 * String entityid = inReq.getRequestParameter("entityid"); if(entityid ==
	 * null) { entityid = inReq.findValue("entityid"); }
	 * 
	 * String moduleid = inReq.findValue("module"); if(moduleid == null) {
	 * moduleid = inReq.getRequestParameter("entitymoduleid"); } if(moduleid ==
	 * null) { moduleid = "librarycollection"; } if (currenttopic == null) {
	 * currenttopic =
	 * topicsearcher.query().match("entityid",entityid).match("moduleid",
	 * moduleid).sort("name").searchOne(); } if (currenttopic == null) {
	 * currenttopic = topicsearcher.createNewData();
	 * currenttopic.setValue("moduleid", moduleid);
	 * currenttopic.setValue("entityid", entityid);
	 * currenttopic.setName("General"); topicsearcher.saveData(currenttopic); }
	 */

	public Data loadCurrentChannel(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);

		String channel = inReq.findValue("channel");
		Data currentchannel = (Data) inReq.getPageValue("currentchannel");
		if (currentchannel != null)
		{
			return currentchannel;
		}

		Searcher topicsearcher = archive.getSearcher("collectiveproject");

		if (channel != null)
		{
			currentchannel = archive.getCachedData("collectiveproject", channel);
		}

		inReq.putPageValue("currentchannel", channel);
		return currentchannel;
	}

	public void loadPageOfChat(WebPageRequest inReq)
	{

		String name = inReq.findValue("hitsname");
		HitTracker results = (HitTracker) inReq.getPageValue(name);
		if (results == null)
		{
			return;
		}
		Searcher chats = results.getSearcher();
		Collection page = results.getPageOfHits();
		ArrayList loaded = new ArrayList();
		String lastdateloaded = null;
		List messageids = new ArrayList(results.size());
		for (Iterator iterator = page.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			Data message = chats.loadData(data);
			loaded.add(message);

			lastdateloaded = message.get("date");
			messageids.add(message.getId());
		}
		inReq.putPageValue("messageids", messageids);

		loadReactions(inReq);
		loadAttachments(inReq);

		// log.info("Chat loaded messages: " + loaded.size());
		Collections.reverse(loaded);
		ListHitTracker newtracker = new ListHitTracker(loaded);
		newtracker.setHitsPerPage(results.getHitsPerPage());

		inReq.putPageValue("messages", newtracker);
		inReq.putPageValue("messagesthitracker", results);
		inReq.putPageValue("lastloaded", lastdateloaded);

		String userid = null;
		if (inReq.getUser() != null)
		{
			userid = inReq.getUser().getId();
		}
		if (userid != null)
		{
			ChatManager manager = getChatManager(inReq);
			String channel = results.getSearchQuery().getInput("channel");
			manager.updateChatTopicLastChecked(channel, userid);
		}

		inReq.getSession().setAttribute("chatuser", inReq.getUser());
	}

	public void toggleReaction(WebPageRequest inReq)
	{
		String messageid = inReq.getRequestParameter("messageid");
		String character = inReq.getRequestParameter("reactioncharacter");
		MediaArchive archive = getMediaArchive(inReq);

		Data found = archive.query("chatterboxreaction").exact("messageid", messageid).exact("user", inReq.getUserName()).searchOne();
		if (found != null && found.getName().equals(character))
		{
			//if its the same then delete it
			archive.getSearcher("chatterboxreaction").delete(found, inReq.getUser());
			return;
		}
		if (found == null)
		{
			//Make sure message is valid?

			found = archive.getSearcher("chatterboxreaction").createNewData();
			found.setValue("messageid", messageid);
			found.setValue("user", inReq.getUserName());
		}
		found.setValue("date", new Date());
		found.setValue("name", character);
		archive.saveData("chatterboxreaction", found);

		List messageids = new ArrayList(1);
		messageids.add(messageid);
		inReq.putPageValue("messageids", messageids);
		loadReactions(inReq);
		//loadAttachments(inReq);
	}

	public void loadReactions(WebPageRequest inReq)
	{
		Collection messageids = (Collection) inReq.getPageValue("messageids");
		if (messageids == null || messageids.isEmpty())
		{
			return;
		}

		Collection reactionhits = getMediaArchive(inReq).query("chatterboxreaction").orgroup("messageid", messageids).sort("date").search();
		Map reactionspermessage = new HashMap();
		for (Iterator iterator = reactionhits.iterator(); iterator.hasNext();)
		{
			Data reaction = (Data) iterator.next();
			List reactions = (List) reactionspermessage.get(reaction.get("messageid"));
			if (reactions == null)
			{
				reactions = new ArrayList();
			}
			reactions.add(reaction);
			reactionspermessage.put(reaction.get("messageid"), reactions);
		}

		inReq.putPageValue("reactionspermessage", reactionspermessage);
	}

	public void loadAttachments(WebPageRequest inReq)
	{
		Collection messageids = (Collection) inReq.getPageValue("messageids");
		if (messageids == null || messageids.isEmpty())
		{
			//look into the request?
			String[] requestmessageids = inReq.getRequestParameters("messageids");
			if (requestmessageids != null)
			{
				messageids = Arrays.asList(requestmessageids);
				if (messageids == null || messageids.isEmpty())
				{
					return;
				}
			}
		}
		Collection reactionhits = getMediaArchive(inReq).query("chatterboxattachment").orgroup("messageid", messageids).sort("date").search();
		Map reactionspermessage = new HashMap();
		for (Iterator iterator = reactionhits.iterator(); iterator.hasNext();)
		{
			Data reaction = (Data) iterator.next();
			List reactions = (List) reactionspermessage.get(reaction.get("messageid"));
			if (reactions == null)
			{
				reactions = new ArrayList();
			}
			reactions.add(reaction);
			reactionspermessage.put(reaction.get("messageid"), reactions);
		}

		inReq.putPageValue("attachmentspermessage", reactionspermessage);
	}

	public void loadMoreMessages(WebPageRequest inReq)
	{

		MediaArchive archive = getMediaArchive(inReq);
		String channel = inReq.findValue("channel");
		String lastloaded = inReq.findValue("lastloaded");
		Date startdate = DateStorageUtil.getStorageUtil().parseFromStorage(lastloaded);
		Searcher chats = archive.getSearcher("chatterbox");

		//HitTracker recent = chats.query().match("channel", channel).sort("dateUp").search();

		//inReq.putPageValue("messages", recent);

		HitTracker oldresults = chats.query().before("date", startdate).exact("channel", channel).sort("dateDown").search();
		oldresults.setHitsPerPage(10);
		//log.info(oldresults.getFriendlyQuery());
		String query = oldresults.getFriendlyQuery();
		Collection page = oldresults.getPageOfHits();
		ArrayList oldloaded = new ArrayList();
		String lastdateloaded = null;
		for (Iterator iterator = page.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			Data message = chats.loadData(data);
			oldloaded.add(message);
			lastdateloaded = message.get("date");
		}
		Collections.reverse(oldloaded);
		inReq.putPageValue("oldmessages", oldloaded);
		//inReq.putPageValue("lastloaded", lastdateloaded);
	}

	public void loadChatServer(WebPageRequest inReq)
	{

		ChatServer server = (ChatServer) getModuleManager().getBean("system", "chatServer");
		inReq.putPageValue("chatserver", server);
	}

	public void loadLastPageOfChats(WebPageRequest inReq)
	{

		MediaArchive archive = getMediaArchive(inReq);

		String channel = inReq.findValue("channel");

		Searcher chats = archive.getSearcher("chatterbox");

		HitTracker recent = chats.query().match("channel", channel).sort("dateUp").search(inReq);
		recent.setPage(recent.getTotalPages());
		inReq.putPageValue("messages", recent);

	}

	public ChatManager getChatManager(WebPageRequest inReq)
	{
		//For a collection show all the channel mod times
		String catalogid = inReq.findPathValue("catalogid");
		ChatManager manager = (ChatManager) getModuleManager().getBean(catalogid, "chatManager");
		inReq.putPageValue("chatManager", manager);
		return manager;

	}

	public void attachFiles(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String topicid = inReq.getRequestParameter("channel");
		String collectionid = inReq.getRequestParameter("collectionid");
		String messageid = inReq.getRequestParameter("messageid");
		Collection savedassets = (Collection) inReq.getPageValue("savedassets");
		if (savedassets == null)
		{
			String attachedassetid = inReq.getRequestParameter("attachedassetid");
			if (attachedassetid != null)
			{
				Data attachedasset = archive.getCachedData("chatterboxattachment", attachedassetid);
				if (attachedasset != null)
				{
					archive.getSearcher("chatterboxattachment").delete(attachedasset, inReq.getUser());
				}

			}
			return;
		}

		Data chat = null;
		if (messageid != null)
		{
			chat = archive.getCachedData("chatterbox", messageid);
		}
		if (chat == null)
		{
			chat = archive.getSearcher("chatterbox").createNewData();
			if (collectionid != null)
			{
				chat.setValue("collectionid", collectionid);
			}
			String entityid = inReq.getRequestParameter("entityid");
			if (entityid != null)
			{
				chat.setValue("entityid", entityid);
				String moduleid = inReq.getRequestParameter("moduleid");
				chat.setValue("moduleid", moduleid);
			}
			chat.setValue("channel", topicid);
			chat.setValue("user", inReq.getUser());
			chat.setValue("date", new Date());
			archive.saveData("chatterbox", chat);
		}

		//Now the other table
		for (Iterator iterator = savedassets.iterator(); iterator.hasNext();)
		{
			Asset asset = (Asset) iterator.next();
			Data chatattchment = archive.getSearcher("chatterboxattachment").createNewData();
			chatattchment.setValue("date", new Date());
			chatattchment.setValue("messageid", chat.getId());
			chatattchment.setValue("user", inReq.getUserName());
			chatattchment.setValue("assetid", asset.getId());

			archive.getSearcher("chatterboxattachment").saveData(chatattchment, inReq.getUser());
		}

	}

	public void editMessage(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String chatid = inReq.getRequestParameter("chatid");
		String message = inReq.getRequestParameter("chatter-msg");

		Data chat = null;
		if (chatid != null)
		{
			chat = archive.getData("chatterbox", chatid);
			if (message != null)
			{
				chat.setValue("message", message);
				archive.saveData("chatterbox", chat);
			}
		}
		else
		{
			chat = archive.getSearcher("chatterbox").createNewData();
			if (message != null)
			{
				chat.setValue("message", message);
			}
			chat.setValue("user", inReq.getUserName());
			chat.setValue("date", new Date());
			String channel = inReq.getRequestParameter("channel");
			String collectionid = inReq.getRequestParameter("collectionid");
			if (collectionid != null)
			{
				chat.setValue("collectionid", collectionid);
			}
			String entityid = inReq.getRequestParameter("entityid");
			if (entityid != null)
			{
				chat.setValue("entityid", entityid);
				String moduleid = inReq.getRequestParameter("moduleid");
				chat.setValue("moduleid", moduleid);
			}

			chat.setValue("channel", channel);

			archive.saveData("chatterbox", chat);
		}
		String channel = chat.get("channel");
		inReq.setRequestParameter("channel", channel);
		inReq.putPageValue("chat", chat);
		inReq.putPageValue("data", chat);

		archive.fireGeneralEvent(inReq.getUser(), "chatterbox", "messageedited", inReq.getPageMap());
	}

	public void clearChannel(WebPageRequest inReq)
	{

		MediaArchive archive = getMediaArchive(inReq);
		String channel = inReq.findValue("channel");

		Searcher chats = archive.getSearcher("chatterbox");

		HitTracker recent = chats.query().match("channel", channel).sort("dateUp").search(inReq);

		for (Iterator iterator = recent.iterator(); iterator.hasNext();)
		{
			Data hit = (Data) iterator.next();
			chats.delete(hit, null);
		}

	}
	
	public BaseLLMManager loadManager(WebPageRequest inReq) {
		MediaArchive archive = getMediaArchive(inReq);
		BaseLLMManager manager = (BaseLLMManager) archive.getBean("gptManager");
		inReq.putPageValue("gpt", manager);
		return manager;
		
	}

	public void monitorChannels(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);

		GptManager manager = (GptManager) archive.getBean("gptManager");

		ExecutorManager queue = (ExecutorManager) archive.getBean("executorManager");

		String model = inReq.findValue("model");
		if (model == null)
		{
			model = archive.getCatalogSettingValue("gpt-model");
		}
		if (model == null)
		{
			// model = "gpt-3.5-turbo-16k-0613";
			model = "gpt-4o";
		}
		Searcher channels = archive.getSearcher("channel");
		HitTracker allchannels = channels.query().match("id", "*").sort("dateUp").search(inReq);
		DateFormat fm = DateStorageUtil.getStorageUtil().getDateFormat("dd/MM/yyyy hh:mm");

		Searcher chats = archive.getSearcher("chatterbox");
		for (Iterator iterator = allchannels.iterator(); iterator.hasNext();)
		{
			Data channel = (Data) iterator.next();
			HitTracker recent = chats.query().exact("channel", channel.getId()).sort("dateUp").search(inReq);
			if (recent.size() == 0)
			{
				continue;
			}
			Data mostrecent = recent.get(recent.size() - 1);
			boolean interimmessage = false;

			if ("function_call".equals(mostrecent.get("messagetype")))
			{
				interimmessage = true;// so we don't respond to ourselves
				String function = mostrecent.get("function");
				String arguments = mostrecent.get("arguments");
				if (!Boolean.parseBoolean(mostrecent.get("functioncomplete")))
				{
					archive.fireDataEvent(inReq.getUser(), "llm", "callfunction", mostrecent);
					archive.fireSharedMediaEvent("chatterbox/aiflow");
					return;
				}
				else
				{
					respondToChannel(inReq, channel, "messagereceived", new HashMap());
				}
			} else {
				
				if("agent".equals(mostrecent.get("user")) || interimmessage)  {
					return;
				}

				
				respondToChannel(inReq, channel, "messagereceived", new HashMap());

			}
		}
	}

	public void respondToChannel(WebPageRequest inReq, Data channel, String command, Map inMap) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		String model = inReq.findPathValue("model");
		if (model == null)
		{
			model = getMediaArchive(inReq).getCatalogSettingValue("gpt-model");
		}
		if (model == null)
		{
			// model = "gpt-3.5-turbo-16k-0613";
			model = "gpt-4o-mini";
		}
		inReq.putPageValue("model", model);

		Date now = new Date();

		DateFormat fm = DateStorageUtil.getStorageUtil().getDateFormat("dd/MM/yyyy hh:mm");

		ChatServer server = (ChatServer) archive.getBean("chatServer");
		Searcher chats = archive.getSearcher("chatterbox");
		GptManager manager = (GptManager) archive.getBean("gptManager");

		HitTracker recent = chats.query().exact("channel", channel.getId()).sort("dateUp").search(inReq);
		inReq.putPageValue("recent", recent);

		
		JSONObject responseMap = new JSONObject();
		responseMap.put("userid", "agent");
		responseMap.put("user", "agent");
		responseMap.put("author", "agent");
		responseMap.put("message", "");
		responseMap.put("date", DateStorageUtil.getStorageUtil().getJsonFormat().format(now));
		responseMap.put("timestamp", fm.format(now));
		responseMap.put("timestampunix", now.getTime());
		responseMap.put("channel", channel.getId());
		responseMap.put("catalogid", archive.getCatalogId());
		responseMap.put("command", command);
		responseMap.put("messagetype", "airesponse");
		
		String channeltype =channel.get("channeltype");
		if (channeltype == null) {
			channeltype = "chatstreamer";
		}
		String chattemplate = "/" +  archive.getMediaDbId()+ "/gpt/inputs/" + channeltype + ".html";	

		//String input = manager.loadInputFromTemplate(inReq, chattemplate);
		JSONObject response = manager.runPageAsInput(inReq, model, chattemplate);
		JSONArray choices = (JSONArray) response.get("choices");
		String output = "";
		if (choices != null && choices.size() > 0) {
		    JSONObject first = (JSONObject) choices.get(0);
		    if (first.containsKey("message")) {
		        JSONObject messageObj = (JSONObject) first.get("message");
		       
		        if (messageObj.containsKey("function_call")) {
		            JSONObject functionCall = (JSONObject) messageObj.get("function_call");
		            String functionName = (String) functionCall.get("name");
		          
		            
		            
		            String argumentsString = (String) functionCall.get("arguments");

		            // Parse the arguments string into a JSON object
		            JSONObject arguments =  (JSONObject) new JSONParser().parse(argumentsString);
		            
		            // Create and save a function call message
		            Data functionMessage = chats.createNewData();
		            functionMessage.setValue("messagetype", "function_call");
		            functionMessage.setValue("function", functionName);
		            functionMessage.setValue("arguments", arguments.toJSONString());
		            functionMessage.setValue("userid", "function");
		            functionMessage.setValue("user", "function");
		            functionMessage.setValue("author", "function");
		            functionMessage.setValue("channel", channel.getId());
		            functionMessage.setValue("date", new Date());
		            chats.saveData(functionMessage);

		            // Create and broadcast the function call message update
		            JSONObject functionMessageUpdate = new JSONObject();
		            functionMessageUpdate.put("messagetype", "function_call");
		            functionMessageUpdate.put("catalogid", archive.getCatalogId());
		            functionMessageUpdate.put("function", functionName);
		            functionMessageUpdate.put("arguments", arguments);
		            functionMessageUpdate.put("userid", "function");
		            functionMessageUpdate.put("user", "function");
		            functionMessageUpdate.put("author", "function");
		            functionMessageUpdate.put("channel", channel.getId());
		            functionMessageUpdate.put("messageid", functionMessage.getId());
		            server.broadcastMessage(functionMessageUpdate);

		        } else if (messageObj.containsKey("content")) {
		            // Regular text response
		            output = (String) messageObj.get("content");
		            responseMap.put("response", output);
		            responseMap.put("message", output);
		            responseMap.put("content", output);

		            // Save and broadcast the message
		            Data message = server.saveMessage(responseMap);
		            responseMap.put("messageid", message.getId());
		            server.broadcastMessage(responseMap);
		        }
		        
		        
		    }
		}
		


	}

	public void callFunction(WebPageRequest inReq) throws Exception
	{

		MediaArchive archive = getMediaArchive(inReq);

		Data data = (Data) inReq.getPageValue("data");
		GptManager manager = (GptManager) archive.getBean("gptManager");

		String function = data.get("function");
		String arguments = data.get("arguments");
		JSONObject d = (JSONObject) new JSONParser().parse(arguments);
		inReq.putPageValue("args", d);
		String response;
		try
		{
			response = manager.loadInputFromTemplate(inReq, "/" + archive.getMediaDbId()+ "/gpt/functions/" + function + ".html");
			log.info("function" + function + "returned : " + response);
			data.setValue("functionresponse", response);
			data.setValue("functioncomplete", true);
		}
		catch (Exception e)
		{
			data.setValue("functionresponse", e.toString());
			data.setValue("functioncomplete", true);

		}
		archive.saveData("chatterbox", data);
		archive.fireSharedMediaEvent("chatterbox/monitorchats");
	

	}

}