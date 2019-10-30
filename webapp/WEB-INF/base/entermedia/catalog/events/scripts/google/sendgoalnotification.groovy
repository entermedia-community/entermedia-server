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
	
	String projectgoalid = event.get("dataid");
	if( projectgoalid != null)
	{
		Data projectgoal = mediaArchive.getData("projectgoal", projectgoalid);
		String collectionid = projectgoal.get("collectionid");
		Data collection = mediaArchive.getData("librarycollection", collectionid);
		
		String topic = event.get("goaltrackercolumn");
		Data project = mediaArchive.getData("collectiveproject", topic);
		String label = null;
		if( project != null)
		{
			label = collection.getName() + " / " + project.getName();
		}
		else
		{
			label = collection.getName();
		}
		Map extra = new HashMap();
		extra.put("chattopic", label);
		extra.put("chattopic", projectgoalid);
		extra.put("collectionid", collectionid);

		String message = projectgoal.getName();
		
		manager.notifyTopic(collectionid, aUser, aUser.getScreenName() + " Goal edited", message, extra);
	}

}

runit();