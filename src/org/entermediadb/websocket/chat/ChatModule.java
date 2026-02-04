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

import java.util.ArrayList;
import java.util.Arrays;
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
import org.entermediadb.ai.assistant.AssistantManager;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.WebPageRequest;
import org.openedit.data.QueryBuilder;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.ListHitTracker;
import org.openedit.profile.UserProfile;
import org.openedit.util.DateStorageUtil;

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
	
	
	/*
	 * For OI Collective Projects, uses collectiveproject table
	 * 
	 * */

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
		if(channel == null) {
			return;
		}
		
		
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

		if (channel != null)
		{
			currentchannel = archive.getCachedData("channel", channel);
		}

		inReq.putPageValue("currentchannel", channel);
		return currentchannel;
	}

	public void loadPageOfChat(WebPageRequest inReq)
	{

		String name = inReq.findValue("hitsname");
		Searcher searcher = loadSearcher(inReq);
		HitTracker results = searcher.loadPageOfSearch(inReq);
		if (results == null)
		{
			results = (HitTracker) inReq.getPageValue(name);
			
		}
		if (results == null)
		{
			return;
		}
		inReq.putPageValue(name, results);
		
		Searcher chats = results.getSearcher();
		Collection page = results.getPageOfHits();
		ArrayList loaded = new ArrayList();
		String lastdateloaded = null;
		List messageids = new ArrayList(results.size());
		for (Iterator iterator = page.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			Data message = chats.loadData(data);
			if("system".equals(message.get("messagetype"))) 
			{
				continue;
			}
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
	
//	//deprecated? not used?
//	public LlmConnection loadManager(WebPageRequest inReq)
//	{
//		MediaArchive archive = getMediaArchive(inReq);
//		AssistantManager assistantManager = (AssistantManager) archive.getBean("assistantManager");
//
////		String model = inReq.findValue("aimodel");
////		if( model == null)
////		{
////			model = archive.getCatalogSettingValue("ai_default_chat_model");
////		}
//		LlmConnection llmconnection = assistantManager.getLlmConnection();//(LlmConnection) archive.getLlmConnection(model);
//		inReq.putPageValue("gpt", llmconnection); //Deprecated
//		inReq.putPageValue("llmconnection", llmconnection);
//		inReq.putPageValue("assistantManager", assistantManager);
//		
//		return llmconnection;
//	}


	public void loadChatChannel(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Searcher channelsearcher = archive.getSearcher("channel");
		boolean createnew = Boolean.parseBoolean(inReq.getRequestParameter("createnew"));
		
		String channel = inReq.findValue("channel");
		String module = inReq.findValue("module");
		
		MultiValued currentchannel = null;
		
		if(channel != null && !createnew)
		{
			currentchannel = (MultiValued) archive.getCachedData("channel", channel);
			if( currentchannel != null)
			{
				inReq.putPageValue("currentchannel", currentchannel);
				return;
			}
		}
		
		String channeltype = inReq.findValue("channeltype");
		if (channeltype == null)
		{
			throw new IllegalArgumentException("channeltype is required");
		}
		String entityid =  inReq.findValue("entityid");
		if( entityid == null)
		{
			entityid = inReq.findValue("dataid");
		}
		String channelname = null;
//		MultiValued entity  = (MultiValued) inReq.getPageValue("entity");
//		switch(channeltype)  //TODO Remove this
//		{
//			case "agententitychat":
//				dataid = entity.getId();
//				channelname = "Guided Chat";
//				break;
//			case "entity":
//				dataid = entity.getId();
//				channelname = "Entity Chat";
//				break;
//			case "agentchat":
//				dataid = inReq.getUserName();
//				channelname = "AI Chat";
//				break;
//		}
			
		if( !createnew )
		{
			currentchannel =  (MultiValued)archive.getCachedData("channel", channel);
			if (currentchannel == null )
			{
				Calendar now = DateStorageUtil.getStorageUtil().createCalendar();
				now.add(Calendar.HOUR_OF_DAY,-1);

				//TODO: Add flag for multi user
				if( entityid != null)
				{
					//Shared chat. Like in OI 
					currentchannel =  (MultiValued)channelsearcher.query().exact("dataid",entityid).exact("searchtype", module).after("refreshdate",now.getTime()).sort("refreshdateDown").searchOne();
				}
				else
				{
					//By user
					currentchannel =  (MultiValued)channelsearcher.query().exact("user",inReq.getUserName()).missing("dataid").after("refreshdate",now.getTime()).sort("refreshdateDown").searchOne();
				}
			}
		}
		
		if (currentchannel == null) 
		{
			currentchannel = (MultiValued)channelsearcher.createNewData();
			currentchannel.setName(channelname);
			currentchannel.setValue("searchtype", module);
			currentchannel.setValue("dataid", entityid );
			currentchannel.setValue("user", inReq.getUser() );
			String applicationid = inReq.findValue("applicationid");
			currentchannel.setValue("chatapplicationid", applicationid);
			currentchannel.setValue("channeltype", channeltype );
		}
		String toplevel = inReq.getRequestParameter("toplevelaifunctionid");
		if( toplevel != null )
		{
			currentchannel.setValue("toplevelaifunctionid", toplevel);
		}

		currentchannel.setValue("refreshdate", new Date() );
		channelsearcher.saveData(currentchannel);
		
		inReq.setRequestParameter("channel", currentchannel.getId());
		
		inReq.putPageValue("currentchannel", currentchannel);
		
		inReq.putPageValue("createnew", false);
	}
	


}