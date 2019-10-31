package google;

import org.entermediadb.asset.MediaArchive
import org.entermediadb.google.GoogleManager
import org.openedit.Data
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
	String collectionid = event.get("collectionid");
	if( collectionid != null)
	{
		Data collection = mediaArchive.getData("librarycollection", collectionid);
		String topic = event.get("channel");
		Data project = mediaArchive.getData("collectiveproject", topic);
		String subject;
		if( project != null)
		{
			subject = "[" + collection.getName() + " / " + project.getName() + "]";
		}
		else
		{
			subject = "[" + collection.getName() +"]";
		}
		subject = subject + " chat from:" + aUser.getScreenName();
		Map extra = new HashMap();
		extra.put("collectionid", collectionid);
		extra.put("collectiveprojectid", topic);
		if( project != null)
		{
			extra.put("collectiveprojectlabel", project.getName());
		}
		extra.put("userid", aUser.getId());
		
		manager.notifyTopic(collectionid, aUser, subject, message, extra);
		
	}

}

runit();