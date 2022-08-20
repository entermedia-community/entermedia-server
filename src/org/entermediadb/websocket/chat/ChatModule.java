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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.QueryBuilder;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.profile.UserProfile;
import org.openedit.util.DateStorageUtil;

public class ChatModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(ChatConnection.class);
		

	public void loadRecentChats(WebPageRequest inReq)
	{

		MediaArchive archive = getMediaArchive(inReq);
		String channel = inReq.findValue("channel");
		Searcher chats = archive.getSearcher("chatterbox");

		//HitTracker recent = chats.query().match("channel", channel).sort("dateUp").search();
		
		//inReq.putPageValue("messages", recent);

		QueryBuilder builder = archive.query("chatterbox");
		
		builder.named("messagesthitracker").exact("channel", channel).sort("dateDown");
		
		
		UserProfile prof = inReq.getUserProfile();
		if( prof != null)
		{
			Collection blocked = prof.getValues("blockedusers");
			if( blocked != null && !blocked.isEmpty() )
			{
				builder.notgroup("user", blocked );
			}
		}
		

		HitTracker results = builder.search(inReq);
		
		results.setHitsPerPage(20);
		  
		  Collection page = results.getPageOfHits(); 
		  ArrayList loaded = new  ArrayList(); 
		  String lastdateloaded = null;
		  for (Iterator iterator = page.iterator(); iterator.hasNext();) {
			  Data data = (Data) iterator.next(); 
			  Data message = chats.loadData(data); 
			  loaded.add(message); 
			  
			  lastdateloaded = message.get("date");
			  
		  }
		  log.info("Chat loaded messages: " + loaded.size());
		  Collections.reverse(loaded); 
		  inReq.putPageValue("messages", loaded);
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
			manager.updateChatTopicLastChecked(channel, userid);
		}
		
		inReq.getSession().setAttribute("chatuser", inReq.getUser());

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

		
		  HitTracker oldresults = chats.query()
				  				.before("date", startdate)
				  				.exact("channel", channel)
				  				.sort("dateDown")
				  				.search(); 
		  oldresults.setHitsPerPage(10);
		  //log.info(oldresults.getFriendlyQuery());
		  String query = oldresults.getFriendlyQuery();
		  Collection page = oldresults.getPageOfHits(); 
		  ArrayList oldloaded = new  ArrayList(); 
		  String lastdateloaded = null;
		  for (Iterator iterator = page.iterator(); iterator.hasNext();) {
			  Data data = (Data) iterator.next(); 
			  Data message = chats.loadData(data); 
			  oldloaded.add(message); 
			  lastdateloaded = message.get("date");
		  }
		  Collections.reverse(oldloaded); 
		  inReq.putPageValue("oldmessages", oldloaded);
		  //inReq.putPageValue("lastloaded", lastdateloaded);
	}
	
	public void loadChatServer(WebPageRequest inReq) {
		
				ChatServer server  = (ChatServer) getModuleManager().getBean("system", "chatServer");
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
		String catalogid = inReq.findValue("catalogid");
		ChatManager manager = (ChatManager) getModuleManager().getBean(catalogid, "chatManager");
		inReq.putPageValue("chatManager", manager);
		return manager;

	}

	public void attachFiles(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Collection savedassets = (Collection) inReq.getPageValue("savedassets");
		String chatid = inReq.getRequestParameter("chatid");
		Data chat = archive.getData("chatterbox", chatid);
		String channel = chat.get("channel");
		inReq.setRequestParameter("channel", channel);
		//Just support one upload for now
		Collection existing = chat.getValues("attachedassets");
		if (existing != null && !existing.isEmpty())
		{
			if (savedassets == null || savedassets.isEmpty())
			{
				savedassets = new ArrayList();
			}
		}
		if (savedassets == null || savedassets.isEmpty())
		{
			chat.setValue("attachedassets", null);
		}
		else
		{
			Data firstupload = (Data) savedassets.iterator().next();
			chat.setValue("attachedassets", firstupload);
		}
		archive.saveData("chatterbox", chat);

	}

	public void editMessage(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String chatid = inReq.getRequestParameter("chatid");
		String message = inReq.getRequestParameter("chatter-msg");

		Data chat = null;
		if( chatid != null)
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
			chat.setValue("user",inReq.getUserName());
			chat.setValue("date",new Date());
			String channel = inReq.getRequestParameter("channel");
			String collectionid = inReq.getRequestParameter("collectionid");
			chat.setValue("channel",channel);
			chat.setValue("collectionid",collectionid);
			
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

}