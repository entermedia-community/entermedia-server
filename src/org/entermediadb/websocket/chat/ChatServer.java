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

import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.cache.CacheManager;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;

public class ChatServer {

	private static final Log log = LogFactory.getLog(ChatServer.class);

	private static final Set<ChatConnection> connections = new CopyOnWriteArraySet<ChatConnection>();

	private static final String CACHENAME = "ChatServer";

	protected CacheManager fieldCacheManager;
	protected ModuleManager fieldModuleManager;
	protected SearcherManager fieldSearcherManager;
	protected JSONParser fieldJSONParser;

	public JSONParser getJSONParser() {
		if (fieldJSONParser == null) {
			fieldJSONParser = new JSONParser();
		}
		return fieldJSONParser;
	}

	public SearcherManager getSearcherManager() {
		if (fieldSearcherManager == null) {
			fieldSearcherManager = (SearcherManager) getModuleManager().getBean("searcherManager");
		}
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager) {
		fieldSearcherManager = inSearcherManager;
	}

	public ModuleManager getModuleManager() {
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager) {
		fieldModuleManager = inModuleManager;
	}

	public CacheManager getCacheManager() {
		if (fieldCacheManager == null) {
			fieldCacheManager = (CacheManager) getModuleManager().getBean("cacheManager");// new CacheManager();
		}

		return fieldCacheManager;
	}

	public void setCacheManager(CacheManager inCacheManager) {
		fieldCacheManager = inCacheManager;
	}

	public void removeConnection(ChatConnection inChatConnection) {
		for (Iterator iterator = connections.iterator(); iterator.hasNext();) {
			ChatConnection annotationConnection2 = (ChatConnection) iterator.next();

			if (inChatConnection == annotationConnection2) {
				connections.remove(annotationConnection2);
				break;
			}
		}
	}

	public void addConnection(ChatConnection inConnection) {
		// TODO Auto-generated method stub
		connections.add(inConnection);
	}

	public void broadcastMessage(JSONObject inMap)
	{
		for (Iterator iterator = connections.iterator(); iterator.hasNext();)
		{
			ChatConnection chatConnection = (ChatConnection) iterator.next();
			chatConnection.sendMessage(inMap);
		}
		
	}

	public void saveMessage(JSONObject inMap)
	{
		String catalogid = (String) inMap.get("catalogid");
		log.info("Saving: " + inMap.toJSONString());
		MediaArchive archive = (MediaArchive) getModuleManager().getBean(catalogid, "mediaArchive");
		Searcher chats = archive.getSearcher("chatterbox");
		Data chat = chats.createNewData();
		chat.setValue("date", new Date());
		chat.setValue("message", inMap.get("content"));
		chat.setValue("user", inMap.get("user"));
		chat.setValue("channel", inMap.get("channel"));
		
		chats.saveData(chat);
		inMap.put("messageid", chat.getId());
		
		
	}

}
//    private static class EchoMessageHandlerBinary
//            implements MessageHandler.Partial<ByteBuffer> {
//
//        private final RemoteEndpoint.Basic remoteEndpointBasic;
//
//        private EchoMessageHandlerBinary(RemoteEndpoint.Basic remoteEndpointBasic) {
//            this.remoteEndpointBasic = remoteEndpointBasic;
//        }
//
//        @Override
//        public void onMessage(ByteBuffer message, boolean last) {
//            try {
//                if (remoteEndpointBasic != null) {
//                    remoteEndpointBasic.sendBinary(message, last);
//                }
//            } catch (IOException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }
//    }
