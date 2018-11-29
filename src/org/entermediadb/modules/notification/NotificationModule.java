package org.entermediadb.modules.notification;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.email.TemplateWebEmail;
import org.entermediadb.email.WebEmail;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.WebPageRequest;
import org.openedit.locks.Lock;
import org.openedit.users.User;

public class NotificationModule extends BaseMediaModule
{
	public NotificationModule()
	{
		// TODO Auto-generated constructor stub
	}

	//Every 30 minutes go look for chat records. Add to notification queue with a boolean status
	public void sendChatNotifications(WebPageRequest inReq)
	{
		//1. Lock this operation
		//2. Track last check date in settings?
		//3. 
		//For everyuser on this assets collections
		//Save target user and asset

		MediaArchive archive = getMediaArchive(inReq);
		Lock lock = archive.getLockManager().lockIfPossible("notificationemails", "module");
		if (lock == null)
		{
			return;
		}
		try
		{
			MultiValued event = (MultiValued) archive.query("eventmanager").id("notificationemails").searchOne();
			if (event == null)
			{
				event = (MultiValued) archive.getSearcher("eventmanager").createNewData();
				event.setId("notificationemails");
				event.setName("Notification Tracker");
				archive.saveData("eventmanager", event);
			}
			Date startingfrom = (Date) event.getDate("lastrandate");

			Date today = new Date();
			// Check the search
			Collection hits = null;
			if (startingfrom != null)
			{
				hits = archive.query("chatterbox").exact("channeltype", "asset").between("date", startingfrom, today).search();
			}
			else
			{
				hits = archive.query("chatterbox").exact("channeltype", "asset").before("date", today).search();
			}

			Map users = loadUserMessages(archive, hits);

			sendEmails(archive, users, "chat");
			event.setValue("lastrandate", today);
			archive.saveData("eventmanager", event);
		}
		finally
		{
			archive.releaseLock(lock);
		}
	}

	public void sendMetadataNotifications(WebPageRequest inReq)
	{
		//1. Lock this operation
		//2. Track last check date in settings?
		//3. 
		//For everyuser on this assets collections
		//Save target user and asset

		MediaArchive archive = getMediaArchive(inReq);
		Lock lock = archive.getLockManager().lockIfPossible("notificationemails", "module");
		if (lock == null)
		{
			return;
		}
		try
		{
			MultiValued event = (MultiValued) archive.query("eventmanager").id("notificationemails").searchOne();
			if (event == null)
			{
				event = (MultiValued) archive.getSearcher("eventmanager").createNewData();
				event.setId("notificationemails");
				event.setName("Notification Tracker");
				archive.saveData("eventmanager", event);
			}
			Date startingfrom = (Date) event.getDate("lastrandate");

			Date today = new Date();
			// Check the search
			Collection hits = null;
			if (startingfrom != null)
			{
				hits = archive.query("asseteditLog").exact("operation", "edit").between("date", startingfrom, today).search();
			}
			else
			{
				hits = archive.query("asseteditLog").exact("operation", "edit").before("date", today).search();
			}

			Map users = loadUserEditMetadata(archive, hits);

			sendEmails(archive, users, "metadata");
			event.setValue("lastrandate", today);
			archive.saveData("eventmanager", event);
		}
		finally
		{
			archive.releaseLock(lock);
		}
	}

	private void sendEmails(MediaArchive inArchive, Map inUsers, String inType)
	{
		///theme/emails/notifications/chat.html
		for (Iterator iterator = inUsers.keySet().iterator(); iterator.hasNext();)
		{
			String userid = (String) iterator.next();
			Map<String, Object> objects = new HashMap<String, Object>();
			User user = inArchive.getUser(userid);
			WebEmail templatemail = null;

			if (inType != null )
			{
				if ( inType.compareTo("chat") == 0)
				{
					String templatePage = "/" + inArchive.getCatalogSettingValue("events_notify_app") + "/theme/emails/notifications/chat.html";
					templatemail = inArchive.createSystemEmail(user, templatePage);

					UserChats chats = (UserChats) inUsers.get(userid);
					
					templatemail.setSubject("[EM] " + chats.getAssetMessages().size() + " assets commented");
					objects.put("notifications", chats);
				}
				else if ( inType.compareTo("metadata") == 0)
				{
					String templatePage = "/" + inArchive.getCatalogSettingValue("events_notify_app") + "/theme/emails/notifications/metadata.html";
					templatemail = inArchive.createSystemEmail(user, templatePage);

					UserMetadata edits = (UserMetadata) inUsers.get(userid);
					
					templatemail.setSubject("[EM] " + edits.getAssetMetadata().size() + " assets edited");
					objects.put("metadatas", edits);
				}
				objects.put("sendto", user);
				objects.put("archive", inArchive);
				templatemail.send(objects);
			}
			// TODO: update the status page on the user
		}

	}

	protected Map loadUserMessages(MediaArchive archive, Collection hits)
	{
		Map users = new HashMap();

		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
		{
			Data message = (Data) iterator.next();
			String assetid = message.get("channel");
			Asset asset = archive.getAsset(assetid);

			// Make sure the owner is notified
			if (asset == null)
			{
				continue;
			}
			
			String owner = asset.get("owner");
			if (owner != null)
			{
				UserChats userchats = loadChats(owner, users);
				userchats.addAssetMessage(asset, message);
			}

			Collection allmessages = archive.query("chatterbox").exact("channel", assetid).exact("channeltype", "asset").search();

			// Look for anyone else in the chat table
			for (Iterator iterator2 = allmessages.iterator(); iterator2.hasNext();)
			{
				Data othermessage = (Data) iterator2.next();
				String otherowner = othermessage.get("user");
				UserChats moreuserchats = loadChats(otherowner, users);
				moreuserchats.addAssetMessage(asset, message);
			}

			// Check for any collection followers
			Collection shares = archive.query("librarycollectionshares").orgroup("librarycollection", asset.getCollections()).search();
			for (Iterator iterator2 = shares.iterator(); iterator2.hasNext();)
			{
				Data follower = (Data) iterator2.next();
				String userid = follower.get("followeruser");
				if (userid != null)
				{
					UserChats moreuserchats = loadChats(userid, users);
					moreuserchats.addAssetMessage(asset, message);
				}
			}
		}
		return users;
	}

	protected Map loadUserEditMetadata(MediaArchive archive, Collection hits)
	{
		Map users = new HashMap();

		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
		{
			Data edit = (Data) iterator.next();
			String assetid = edit.get("assetid");
			Asset asset = archive.getAsset(assetid);

			// Make sure the owner is notified
			if (asset == null)
			{
				continue;
			}
			
			String owner = asset.get("owner");
			if (owner != null)
			{
				UserMetadata usermetadatas = loadMetadatas(owner, users);
				usermetadatas.addAssetMetadata(asset, edit);
			}

			Collection alledits = archive.query("asseteditLog").exact("assetid", assetid).exact("operation", "edit").search();

			// Look for anyone else in the chat table
			for (Iterator iterator2 = alledits.iterator(); iterator2.hasNext();)
			{
				Data otheredit = (Data) iterator2.next();
				String otherowner = otheredit.get("user");
				UserMetadata moreusermetadatas = loadMetadatas(otherowner, users);
				moreusermetadatas.addAssetMetadata(asset, edit);
			}

			// Check for any collection followers
			Collection shares = archive.query("librarycollectionshares").orgroup("librarycollection", asset.getCollections()).search();
			for (Iterator iterator2 = shares.iterator(); iterator2.hasNext();)
			{
				Data follower = (Data) iterator2.next();
				String userid = follower.get("followeruser");
				if (userid != null)
				{
					UserMetadata moreusermetadatas = loadMetadatas(userid, users);
					moreusermetadatas.addAssetMetadata(asset, edit);
				}
			}
		}
		return users;
	}

	
	protected UserChats loadChats(String owner, Map users)
	{
		UserChats userchats = (UserChats) users.get(owner);
		if (userchats == null)
		{
			userchats = new UserChats();
			userchats.setUserId(owner);
			users.put(owner,userchats);
		}
		return userchats;
	}
	
	protected UserMetadata loadMetadatas(String owner, Map users)
	{
		UserMetadata usermetadatas = (UserMetadata) users.get(owner);
		if (usermetadatas == null)
		{
			usermetadatas = new UserMetadata();
			usermetadatas.setUserId(owner);
			users.put(owner,usermetadatas);
		}
		return usermetadatas;
	}

}
