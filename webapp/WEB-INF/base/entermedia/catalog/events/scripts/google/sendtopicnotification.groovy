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
		Data topicdata = mediaArchive.getData("collectiveproject", topic);
		String subject;
		if( topicdata != null)
		{
			subject = "[" + collection.getName() + " / " + topicdata.getName() + "]";
		}
		else
		{
			subject = "[" + collection.getName() +"]";
		}
		subject = subject + " chat from:" + aUser.getScreenName();
		Map extra = new HashMap();
		extra.put("collectionid", collectionid);
		extra.put("collectionlabel", collection.getName());

		extra.put("collectivetopicid", topic);
		if( topicdata != null)
		{
			extra.put("collectivetopiclabel", topicdata.getName());
		}
		extra.put("userid", aUser.getId());
		
		manager.notifyTopic(collectionid, aUser, subject, message, extra);
		
	}

}

runit();