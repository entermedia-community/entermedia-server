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

import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;

public class ChatModule extends BaseMediaModule {

	
	public void loadRecentChats(WebPageRequest inReq){
		
		MediaArchive archive = getMediaArchive(inReq);
		String channel = inReq.findValue("channel");
		Searcher chats = archive.getSearcher("chatterbox");
		
		HitTracker recent = chats.query().match("channel", channel).sort("dateUp").search();
		inReq.putPageValue("messages", recent);
		
		
		/*	Pagination
	    Searcher searcher = getMessagesSearcher(inReq);
	 	HitTracker results = searcher.query().match("channel", channel).sort("dateDown").search();
		results.setHitsPerPage(10);
		Collection page = results.getPageOfHits();
		ArrayList loaded = new ArrayList();
		for (Iterator iterator = page.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			Data message = searcher.loadData(data);
			loaded.add(message);
		}
		Collections.reverse(loaded);
		inReq.putPageValue("messages", loaded);
		 */
		String userid = null;
		if(inReq.getUser() != null) 
		{
			userid = inReq.getUser().getId();
		}
		if(  userid != null)
		{
			ChatManager manager = getChatManager(inReq);
			manager.updateChatTopicLastChecked(channel, userid);
		}
		
	}
	public void loadLastPageOfChats(WebPageRequest inReq){
		
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
		ChatManager manager = (ChatManager)getModuleManager().getBean(catalogid,"chatManager");
		inReq.putPageValue("chatManager", manager);
		return manager;
		
	}
	
}