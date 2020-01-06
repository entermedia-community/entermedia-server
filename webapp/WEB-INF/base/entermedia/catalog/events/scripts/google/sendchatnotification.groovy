package google;

import org.entermediadb.asset.MediaArchive
import org.entermediadb.google.GoogleManager
import org.entermediadb.projects.LibraryCollection
import org.openedit.Data
import org.openedit.MultiValued
import org.openedit.event.WebEvent
import org.openedit.users.User
 

public void runit()
{
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	GoogleManager manager = (GoogleManager)mediaArchive.getBean("googleManager");
	
	WebEvent event = (WebEvent)context.getPageValue("webevent");
	
	/*
	 * chat.setValue("message", inMap.get("content"));
		String userid = (String)inMap.get("user").toString();
		chat.setValue("user", userid);
		chat.setValue("channel", inMap.get("channel"));
		chat.setValue("channeltype", inMap.get("channeltype"));
	 */
	User aUser = event.getUser();
	
	String message = event.get("message");
		
	String topicid = event.get("channel");
	MultiValued topicdata = (MultiValued)mediaArchive.getData("collectiveproject", topicid);
	//If it ends with messages
	
	Collection values = topicdata.getValues("parentcollectionid");
	if( values == null)
	{
		return;
	}
	for(String parentcollectionid in values)
	{
		LibraryCollection collection = mediaArchive.getData("librarycollection", parentcollectionid);
		if( parentcollectionid.endsWith("-messages"))
		{
			String userid = (String)collection.getValue("owner");
			if( userid.equals(aUser.getId())) //Dont sent to self
			{
				continue;
			}
			User otherUser = mediaArchive.getUser(userid);
			if( otherUser != null)
			{
				Data lastchecked = mediaArchive.query("chattopiclastchecked").exact("userid", userid).
					exact("collectionid", parentcollectionid).searchOne();  //Wont be in any other collection
				if( lastchecked != null)
				{
					Date checked = lastchecked.getDate("datechecked");
					if( checked.getTime() + 1000*60*10 > System.currentTimeMillis())
					{
						continue;
					}
					String subject = "Message from " + aUser.getScreenName();
					Map extra = new HashMap();
					
					extra.put("collectionid", collection.getId());
					extra.put("collectionlabel", collection.getName());
					extra.put("collectivetopicid", topicid);
					extra.put("collectivetopiclabel", otherUser.getScreenName());
					extra.put("userid", aUser.getId());
					manager.notifyTopic(collection.getId(), aUser, subject, message, extra);
				}
			}
		}
		else
		{
			String subject = "[" + collection.getName() + " / " + topicdata.getName() + "]";
			subject = subject + " from:" + aUser.getScreenName();
			Map extra = new HashMap();
			extra.put("collectionid", collection.getId());
			extra.put("collectionlabel", collection.getName());
			extra.put("collectivetopicid", topicdata.getId());
			extra.put("collectivetopiclabel", topicdata.getName());
			extra.put("userid", aUser.getId());
			
			manager.notifyTopic(collection.getId(), aUser, subject, message, extra);
		}	
	}

}

runit();