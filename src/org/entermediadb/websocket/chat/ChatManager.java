package org.entermediadb.websocket.chat;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.entermediadb.asset.MediaArchive;
import org.entermediadb.projects.LibraryCollection;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.MultiValued;
import org.openedit.WebPageRequest;
import org.openedit.users.User;
import org.openedit.util.URLUtilities;

public class ChatManager implements CatalogEnabled
{
	protected MediaArchive fieldMediaArchive;
	protected ModuleManager fieldModuleManager;
	protected String fieldCatalogId;

	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	public MediaArchive getMediaArchive()
	{
		if (fieldMediaArchive == null)
		{
			fieldMediaArchive = (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");
		}

		return fieldMediaArchive;
	}

	public void setMediaArchive(MediaArchive inMediaArchive)
	{
		fieldMediaArchive = inMediaArchive;
	}
	

//	public synchronized void updateChatTopicLastModified(String channelid)
//	{
//		updateChatTopicLastModified(channelid, "");
//	}
//	
//	public synchronized void updateChatTopicLastModified(String channelid, String inUserId) {
//		updateChatTopicLastModified(channelid, inUserId, "");
//	}
	
	public synchronized void updateChatTopicLastModified(String channelid, String inUserId, String inMessageId)
	{
		MultiValued status = (MultiValued) getMediaArchive().query("chattopiclastmodified").exact("chattopicid", channelid).searchOne();
		if (status == null)
		{
			status = (MultiValued) getMediaArchive().getSearcher("chattopiclastmodified").createNewData();
			status.setValue("chattopicid", channelid);
			//String collectionid, 
			MultiValued topic = (MultiValued) getMediaArchive().getData("collectiveproject", channelid);
			if (topic != null)
			{
				Collection collections = topic.getValues("parentcollectionid");
				status.setValue("collectionid", collections);
			}
		}
		if(inUserId != null) {
			//save user
			status.setValue("userid", inUserId);
		}
		if(inMessageId != null) {
			//save Message
			status.setValue("messageid", inMessageId);
		}
		status.setValue("datemodified", new Date());
		getMediaArchive().saveData("chattopiclastmodified", status);
	}

	//TODO: Do this while messages are coming in
	public synchronized void updateChatTopicLastChecked(String channelid, String inUserId)
	{
		MultiValued status = (MultiValued) getMediaArchive().query("chattopiclastchecked").exact("chattopicid", channelid).match("userid", inUserId).searchOne();
		if (status == null)
		{
			status = (MultiValued) getMediaArchive().getSearcher("chattopiclastchecked").createNewData();
			status.setValue("chattopicid", channelid);
			status.setValue("userid", inUserId);
			MultiValued topic = (MultiValued) getMediaArchive().getData("collectiveproject", channelid);
			if (topic != null)
			{
				Collection collections = topic.getValues("parentcollectionid");
				status.setValue("collectionid", collections);

			}

		}
		status.setValue("datechecked", new Date());
		getMediaArchive().saveData("chattopiclastchecked", status);

	}

	public Set loadChatTopicLastChecked(String inCollectionId, String inUserId)
	{
		//First get all the topics
		if(inUserId == null || inCollectionId == null)
		{
			return Collections.EMPTY_SET;
		}
			
		Collection alltopicsmodified = getMediaArchive().query("chattopiclastmodified").exact("collectionid", inCollectionId).search();

		HashMap<String, Data> modifiedtopics = new HashMap<String, Data>();
		for (Iterator iterator = alltopicsmodified.iterator(); iterator.hasNext();)
		{
			Data expiredcheck = (Data) iterator.next();
			modifiedtopics.put(expiredcheck.get("chattopicid"), expiredcheck);
		}

		Collection lastchecks = getMediaArchive().query("chattopiclastchecked").exact("userid", inUserId).exact("collectionid", inCollectionId).search();

		for (Iterator iterator = lastchecks.iterator(); iterator.hasNext();)
		{
			Data lastcheck = (Data) iterator.next();
			String lastchecktopicid = lastcheck.get("chattopicid");
			Data existingtopicmodified = modifiedtopics.get(lastchecktopicid);
			if (existingtopicmodified != null)
			{
				Date modifiedtopic = (Date) existingtopicmodified.getValue("datemodified");
				Date checked = (Date) lastcheck.getValue("datechecked");
				if (!modifiedtopic.after(checked))
				{
					String topicid = lastcheck.get("chattopicid");
					modifiedtopics.remove(topicid);
				}
			}
		}
		//What is left has not been viewed... TODO: Deal with empty topics? put welcome message.. Please enter chat
		return new HashSet(modifiedtopics.keySet());

	}

	public Collection loadCollectionsModified(Collection inCollections, String inUserId)
	{
		if(inUserId == null)
		{
			return Collections.EMPTY_LIST;
		}
		Collection alltopicsmodified = getMediaArchive().query("chattopiclastmodified").orgroup("collectionid", inCollections).search();

		HashMap<String, Data> modifiedtopics = new HashMap<String, Data>();
		for (Iterator iterator = alltopicsmodified.iterator(); iterator.hasNext();)
		{
			Data expiredcheck = (Data) iterator.next();
			modifiedtopics.put(expiredcheck.get("chattopicid"), expiredcheck);
		}

		Collection lastchecks = getMediaArchive().query("chattopiclastchecked").exact("userid", inUserId).orgroup("collectionid", inCollections).search();

		for (Iterator iterator = lastchecks.iterator(); iterator.hasNext();)
		{
			Data lastcheck = (Data) iterator.next();
			String lastchecktopicid = lastcheck.get("chattopicid");
			Data existingtopicmodified = modifiedtopics.get(lastchecktopicid);
			if (existingtopicmodified != null)
			{
				Date modifiedtopic = (Date) existingtopicmodified.getValue("datemodified");
				Date checked = (Date) lastcheck.getValue("datechecked");
				if (!modifiedtopic.after(checked))
				{
					String topicid = lastcheck.get("chattopicid");
					modifiedtopics.remove(topicid);
				}
			}
		}

		Set collectionids = new HashSet();
		for (Iterator iterator = modifiedtopics.values().iterator(); iterator.hasNext();)
		{
			Data expiredlastmod = (Data) iterator.next();
			collectionids.add(expiredlastmod.get("collectionid"));
		}

		//What is left has not been viewed... TODO: Deal with empty topics? put welcome message.. Please enter chat
		return collectionids;

	}

	public ChatServer getChatServer()
	{
		ChatServer server  = (ChatServer) getModuleManager().getBean("system", "chatServer");
		return server;
	}
	public String escapeMessage(String inMessage) {
		
		String escaped = URLUtilities.xmlEscape(inMessage);
		escaped = escaped.replaceAll("&lt;br&gt;", "<br>");
		return escaped;
		
	}
	
	public User getOtherChatUser(LibraryCollection inData, User myself)
	{
		if( inData == null)
		{
			return null;
		}
		if( !"3".equals(inData.get("collectiontype")) )
		{
			return null;
		}
		String names = inData.getName();	
		if( names != null && names.startsWith("Messages ["))
		{
			names = names.substring("Messages [".length(), names.length() - 1);
			String[] both = names.split(",");
			for (int i = 0; i < both.length; i++)
			{
				String userid = both[i].trim();
				if( !userid.equals(myself.getId()) )
				{
					User user = getMediaArchive().getUser(userid);
					return user;
				}
			}
		}
		return null;
	}
	
	public void monitorChannels(WebPageRequest inReq) throws Exception {}
}
