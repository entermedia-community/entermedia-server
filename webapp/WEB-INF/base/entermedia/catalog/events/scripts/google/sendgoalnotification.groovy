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
		
		boolean notified = Boolean.parseBoolean(projectgoal.get("notifiedteam"));
		String projectstatus = projectgoal.get("projectstatus"); 
		if( notified )
		{
			//If we are open then return. If closed then continue
			if( "open".equals( projectstatus ) )
			{
				return; //No need to send another notification on an open ticket
			}
		}
		String collectionid = projectgoal.get("collectionid");
		Data collection = mediaArchive.getData("librarycollection", collectionid);
		
		String topicid = event.get("goaltrackercolumn");
		Data topic = mediaArchive.getData("collectiveproject", topicid);
		String subject = collection.getName();
		if( topic != null)
		{
			subject = subject + " / " + topic.getName();
		}
		subject = "[" + subject + "] " + aUser.getScreenName();
		Data status =  mediaArchive.getData("projectstatus",projectgoal.get("projectstatus") );
		if( status != null )
		{
			 subject = subject + " " + status.getName() + " Goal";
		}
		else
		{
			 subject = subject + " new Goal";
		}
		Map extra = new HashMap();
		extra.put("projectgoalid", projectgoal.getId());
		extra.put("projectgoallabel",  projectgoal.getName());
		extra.put("collectionid", collectionid);
		extra.put("collectionlabel", collection.getName());
		
		String message = projectgoal.getName();
		//Level and status info
		Data level = mediaArchive.getData("ticketlevel",projectgoal.get("ticketlevel") );
		if( level != null)
		{
			message = level.getName() + ": " + message;
		}
		if( !notified)
		{	
			projectgoal.setValue("notifiedteam",true);
			mediaArchive.saveData("projectgoal",projectgoal);
		}	
		manager.notifyTopic(collectionid, aUser, subject, message, extra);
	}

}

runit();