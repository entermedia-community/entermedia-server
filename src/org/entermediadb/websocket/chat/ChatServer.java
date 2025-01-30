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

import java.awt.AWTException;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.projects.LibraryCollection;
import org.entermediadb.projects.ProjectManager;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.cache.CacheManager;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.ExecutorManager;

public class ChatServer
{

	private static final Log log = LogFactory.getLog(ChatServer.class);

	private Set<ChatConnection> connections = new CopyOnWriteArraySet<ChatConnection>();

	private static final String CACHENAME = "ChatServer";

	protected CacheManager fieldCacheManager;
	protected ModuleManager fieldModuleManager;
	protected SearcherManager fieldSearcherManager;
	protected JSONParser fieldJSONParser;

	public JSONParser getJSONParser()
	{
		if (fieldJSONParser == null)
		{
			fieldJSONParser = new JSONParser();
		}
		return fieldJSONParser;
	}

	public SearcherManager getSearcherManager()
	{
		if (fieldSearcherManager == null)
		{
			fieldSearcherManager = (SearcherManager) getModuleManager().getBean("searcherManager");
		}
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	public CacheManager getCacheManager()
	{
		if (fieldCacheManager == null)
		{
			fieldCacheManager = (CacheManager) getModuleManager().getBean("cacheManager");// new CacheManager();
		}

		return fieldCacheManager;
	}

	public void setCacheManager(CacheManager inCacheManager)
	{
		fieldCacheManager = inCacheManager;
	}

	public void removeConnection(ChatConnection inChatConnection)
	{
		for (Iterator iterator = connections.iterator(); iterator.hasNext();)
		{
			ChatConnection annotationConnection2 = (ChatConnection) iterator.next();

			if (inChatConnection == annotationConnection2)
			{
				connections.remove(annotationConnection2);
				break;
			}
		}
	}

	public void addConnection(ChatConnection inConnection)
	{
		String sessionid = inConnection.getSessionId();
		ArrayList toremove = new ArrayList();
		for (Iterator iterator = connections.iterator(); iterator.hasNext();)
		{
			ChatConnection chatConnection = (ChatConnection) iterator.next();
			String oldid = chatConnection.getSessionId();
			if(oldid.equals(sessionid)) {
				toremove.add(chatConnection);
			}
		}

		connections.removeAll(toremove);
		
		// TODO Auto-generated method stub
		connections.add(inConnection);
	}

	public void broadcastMessage(String inCatalogId, Data inData)
	{
		JSONObject inMap = new JSONObject(inData.getProperties());
		//Command
		Date date = (Date)inData.getValue("date");
		inMap.put("date",DateStorageUtil.getStorageUtil().getJsonFormat().format(date));
		inMap.put("messageid",inData.getId());
		inMap.put("command","messagereceived");
		inMap.put("content",inData.get("message"));
		broadcastMessage(inCatalogId,inMap);
	}
	public void broadcastMessage(JSONObject inMap)
	{
		String inCatalogId = (String)inMap.get("catalogid");
		broadcastMessage(inCatalogId,inMap);
	}
	public void broadcastMessage(String catalogid, JSONObject inMap)
	{
		
		MediaArchive archive = (MediaArchive) getModuleManager().getBean(catalogid, "mediaArchive");
		String entityid = (String) inMap.get("entityid");
		if (entityid == null)
		{
			entityid = (String) inMap.get("collectionid");
		}
		String moduleid = (String) inMap.get("moduleid");
		if (moduleid == null)
		{
			moduleid = "librarycollection";
		}
		
		Data entity = archive.getCachedData(moduleid, entityid);
		
		//LibraryCollection collection = (LibraryCollection) archive.getCachedData("librarycollection", collectionid);

		if( catalogid != null && entity != null )
		{
			final ChatManager manager = getChatManager(catalogid);
			
			log.info("Sending " + inMap.toJSONString()		+" to " + connections.size() + " Clients");
			
			String channelid = (String)inMap.get("channel");
			String userid = null;
			if( inMap.get("user") != null )
			{
				userid = inMap.get("user").toString();
			}
			String messageid = (String)inMap.get("messageid");
			if( channelid != null)
			{
				manager.updateChatTopicLastModified( channelid, userid, messageid );
			}
			if( entityid != null && !"null".equals(entityid))
			{
				ProjectManager projectmanager = getProjectManager(catalogid);
				
				
				if(inMap.get("topic") == null)
				{
					inMap.put("topic", entity.getName());
				}
				if( inMap.get("name") == null)
				{
					User user = archive.getUser(userid);
					if(user != null)
					{
						inMap.put("name",user.getScreenName());
					}
				}
				
				Boolean broadcastAll = (Boolean)inMap.get("broadcastall");
				
				if (broadcastAll != null && broadcastAll) {
					for (Iterator iterator = connections.iterator(); iterator.hasNext();)
					{
						ChatConnection chatConnection = (ChatConnection) iterator.next();
						chatConnection.sendMessage(inMap);
					}	
				}
				else {
					
					Set userids = projectmanager.listTeam(entity);
					userids.add(userid);
					
					for (Iterator iterator = connections.iterator(); iterator.hasNext();)
					{
						ChatConnection chatConnection = (ChatConnection) iterator.next();
						if( userids.contains(chatConnection.getUserId() ) )
						{
							chatConnection.sendMessage(inMap);
						}
					}	
				}
			} 
			else 
			{ 
				for (Iterator iterator = connections.iterator(); iterator.hasNext();)
				{
					ChatConnection chatConnection = (ChatConnection) iterator.next();
					chatConnection.sendMessage(inMap);
				}	
			}
			
			//For people who are logged in, mark that they checked already
			getExecutorManager(catalogid).execute( new Runnable() {
				@Override
				public void run() 
				{
					String channelid = (String)inMap.get("channel");
					
					for (Iterator iterator = connections.iterator(); iterator.hasNext();)
					{
						ChatConnection chatConnection = (ChatConnection) iterator.next();
						String connectedUser = chatConnection.getUserId(); 
						if( connectedUser != null)
						{
							String connectionTopic = chatConnection.getChannelId();  //Current Connection Channel
							if( channelid != null && channelid.equals(connectionTopic))
							{
								manager.updateChatTopicLastChecked(String.valueOf(channelid),  connectedUser);
							}
						}						
					}	
				}
			});
		}
		else {
			log.info("Error broadcasting message, missing collection: " + entityid + " or module: "+ moduleid +" or catalog: " + catalogid);
		}
	}

	public Data saveMessage(final JSONObject inMap)
	{
		String catalogid = (String) inMap.get("catalogid");
		log.info("Saving Message: " + inMap.toJSONString());
		MediaArchive archive = (MediaArchive) getModuleManager().getBean(catalogid, "mediaArchive");
		Searcher chats = archive.getSearcher("chatterbox");
		Object channel = inMap.get("channel");
		String userid = (String)inMap.get("user").toString();
		
		long now = System.currentTimeMillis() - 9*1000;
		Data lastOne = chats.query().exact("channel",channel.toString()).after("date",new Date(now)).sort("dateDown").searchOne();
		Data chat = null;
		String newmessage = String.valueOf( inMap.get("content") );
		/*
		 * Check for previous user, if previous user is the same combine last message
		 * content with new message.
		 */
	
		if(lastOne != null)
		{
			String lastUserId = lastOne.get("user"); 
			if( lastUserId.contentEquals(userid))
			{
				chat = lastOne;  //USE LAST ONE
				String combined = lastOne.get("message");
				combined = combined + "<br>" + newmessage;
				lastOne.setValue("message",combined);
			}
		}
		String entityid = null;
		if(inMap.get("entityid")!= null) {
			//CAST FROM LONG!
			entityid = String.valueOf(inMap.get("entityid"));
		}
		String collectionid = null;
		collectionid = String.valueOf(inMap.get("collectionid"));
		
		String moduleid = null;
		if(inMap.get("moduleid")!= null) {
			moduleid = String.valueOf(inMap.get("moduleid"));
		}
		if(moduleid == null)
		{
			moduleid = "librarycollection";
		}

		if( chat == null)
		{
			chat = chats.createNewData();
			chat.setValue("date", new Date());
			chat.setValue("user", userid);
			chat.setValue("channel", channel);
			if(entityid != null)
			{
				chat.setValue("entityid", entityid);
				chat.setValue("moduleid",moduleid);
			}
			else {
				chat.setValue("collectionid", collectionid);	
			}
		
			chat.setValue("channeltype", inMap.get("channeltype"));
			chat.setValue("message", newmessage);
		}
		String replytoid = (String)inMap.get("replytoid");
		chat.setValue("replytoid",replytoid);
		chats.saveData(chat);  //<----  SAVE chat
		User user = archive.getUser(userid);
		archive.fireDataEvent(user,"chatterbox","saved", chat);
		
		String messageid = chat.getId();
		inMap.put("messageid", messageid);
		
		if (inMap.get("assetid")!= null) {
			String assetid = String.valueOf(inMap.get("assetid"));
			if( assetid != null)
			{
				Asset asset = archive.getAsset( assetid);
				if( asset != null && !asset.isPropertyTrue("haschat"))
				{
					asset.setValue("haschat", true);
					archive.saveAsset(asset);
				}
				archive.fireMediaEvent("assetchat", user,asset );
			}
		}
		return chat;//chat.get("message");
	}

	/* Desktop notificationsss */
	

    public void displayTray(String message) throws AWTException {
        //Obtain only one instance of the SystemTray object
        SystemTray tray = SystemTray.getSystemTray();
        
        String chatmessage = message;
        //If the icon is a file
        Image image = Toolkit.getDefaultToolkit().createImage("\"https://entermediadb.org/entermediadb/mediadb/services/module/asset/downloads/preset/2019/12/f0/94a/image200x200.png\"");
        //Alternative (if the icon is on the classpath):
        //Image image = Toolkit.getDefaultToolkit().createImage(getClass().getResource("icon.png"));

        TrayIcon trayIcon = new TrayIcon(image, "Tray Demo");
        //Let the system resize the image if needed
        trayIcon.setImageAutoSize(true);
        //Set tooltip text for the tray icon
        trayIcon.setToolTip("EntermediaNotifications");
        tray.add(trayIcon);

        trayIcon.displayMessage("EntermediaDB", chatmessage, MessageType.NONE);
    }
	
	public ExecutorManager getExecutorManager(String inCatalogId)
	{
		ExecutorManager queue = (ExecutorManager) getModuleManager().getBean(inCatalogId, "executorManager");
		return queue;
	}

	public ChatManager getChatManager(String inCatalogId)
	{
		ChatManager queue = (ChatManager) getModuleManager().getBean(inCatalogId, "chatManager");
		return queue;
	}

	public ProjectManager getProjectManager(String inCatalogId)
	{
		ProjectManager queue = (ProjectManager) getModuleManager().getBean(inCatalogId, "projectManager");
		return queue;
	}

			
	public void approveAsset(JSONObject inMap)
	{
		String catalogid = (String) inMap.get("catalogid");
		MediaArchive archive = (MediaArchive) getModuleManager().getBean(catalogid, "mediaArchive");
		Asset asset = archive.getAsset((String) inMap.get("assetid"));
		User user = archive.getUser((String) inMap.get("user"));
		String entityid = (String) inMap.get("entityid");
		if(entityid == null)
		{
			entityid = (String) inMap.get("collectionid");
		}
		String moduleid = String.valueOf(inMap.get("moduleid"));
		if (moduleid == null)
		{
			moduleid = "librarycollection";
		}
		String message = (String) inMap.get("content");

		archive.getProjectManager().approveAsset(asset, user, message, entityid, false);

		Searcher chats = archive.getSearcher("chatterbox");
		Data chat = chats.createNewData();
		chat.setValue("date", new Date());
		chat.setValue("message", inMap.get("content"));
		chat.setValue("user", inMap.get("user"));
		chat.setValue("channel", inMap.get("channel"));
		chat.setValue("messagetype", "approved");
		chat.setValue("channeltype","asset"); 
		chat.setValue("collectionid", entityid);
		chat.setValue("entityid", entityid);
		chat.setValue("moduleid", moduleid);
		chats.saveData(chat);
		inMap.put("messageid", chat.getId());

	}

	public void rejectAsset(JSONObject inMap)
	{
		String catalogid = (String) inMap.get("catalogid");
		MediaArchive archive = (MediaArchive) getModuleManager().getBean(catalogid, "mediaArchive");
		Asset asset = archive.getAsset((String) inMap.get("assetid"));
		User user = archive.getUser((String) inMap.get("user"));
		String entityid = (String) inMap.get("entityid");
		if(entityid == null)
		{
			entityid = (String) inMap.get("collectionid");
		}
		String moduleid = String.valueOf(inMap.get("moduleid"));
		if (moduleid == null)
		{
			moduleid = "librarycollection";
		}
		String message = (String) inMap.get("content");

		archive.getProjectManager().rejectAsset(asset, user, message, entityid, false);

		Searcher chats = archive.getSearcher("chatterbox");
		Data chat = chats.createNewData();
		chat.setValue("date", new Date());
		chat.setValue("message", inMap.get("content"));
		chat.setValue("user", inMap.get("user"));
		chat.setValue("channel", inMap.get("channel"));
		chat.setValue("channeltype","asset"); 
		chat.setValue("messagetype", "rejected");
		chat.setValue("collectionid", entityid);
		chat.setValue("entityid", entityid);
		chat.setValue("moduleid", moduleid);
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
