package librarycollection;

import org.apache.commons.collections.IteratorUtils
import org.entermediadb.asset.MediaArchive
import org.entermediadb.email.WebEmail
import org.openedit.Data
import org.openedit.MultiValued
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.users.User
import org.openedit.util.DateStorageUtil


public void init()
{
	MediaArchive mediaArchive = (MediaArchive)context.getPageValue("mediaarchive");
	
	
	Data notificationsent = mediaArchive.getCatalogSetting("collection_chat_notification_lastsent");
	if( notificationsent == null)
	{
		notificationsent = mediaArchive.getSearcher("catalogsettings").createNewData();
		notificationsent.setId("collection_chat_notification_lastsent");
		notificationsent.setName("Chat Notification Last Sent");
		notificationsent.setValue("value", DateStorageUtil.getStorageUtil().addDaysToDate(new Date(), -2));
	}
	String datestrinng = notificationsent.get("value");
	Date since = DateStorageUtil.getStorageUtil().parseFromStorage(datestrinng);
	Date started = new Date();
	//First get all the topics
	HitTracker alltopicsmodified = mediaArchive.query("chattopiclastmodified").after("datemodified", since).search();
	
	Collection topicsmod = alltopicsmodified.collectValues("chattopicid");

	//get the last checked
	Collection alltopicschecked = mediaArchive.query("chattopiclastchecked").orgroup("chattopicid", topicsmod).after("datechecked", since).search();

	Set userwhochecked = new HashSet();
	for (Data topiccheck in alltopicschecked) 
	{
		String userid = topiccheck.get("userid");
		String chattopicid = topiccheck.get("chattopicid");
		userwhochecked.add(userid + "_" + chattopicid);
	}
	
	Map<String,List> usertopics = new HashMap();
	
	for (Data topicmod in alltopicsmodified)
	{
		String collectionid = topicmod.get("collectionid");
		String chattopicid = topicmod.get("chattopicid");
		
		Collection users = mediaArchive.query("librarycollectionusers").exact("collectionid", collectionid).exact("ontheteam", "true").search();
		for(Data auser in users)
		{
			String userid = auser.get("followeruser");
			if(!userwhochecked.contains(userid + "_" + chattopicid))
			{
				//Notify them of what they missed only
				List topics = usertopics.get(userid);
				if( topics == null)
				{
					topics = new ArrayList();
				}
				topics.add(topicmod);
				usertopics.put(userid,topics);
			}
		}
	}
	String	appid =  mediaArchive.getCatalogSettingValue("events_notify_collective_app");
	String template = "/" + appid + "/theme/emails/collective-update-event.html";
	//Loop over the remaining topics
	for (String useerid in usertopics.keySet())
	{
		List topicmods = usertopics.get(useerid);
		User followeruser = mediaArchive.getUser(useerid);
		if (followeruser != null && followeruser.getEmail() != null) {
			WebEmail templatemail = mediaArchive.createSystemEmail(followeruser, template);
			if( topicmods.size() > 1)
			{
				templatemail.setSubject("[EM] " + topicmods.size() + " Topic Notifications"); //TODO: Translate
			}
			else
			{
				Data oneitem = topicmods.iterator().next();
				Data collection = mediaArchive.getCachedData("librarycollection", oneitem.get("collectionid") );
				Data topic = mediaArchive.getCachedData("collectiveproject", oneitem.get("chattopicid") );
				templatemail.setSubject("[EM] " + collection.getName() + "/" + topic.getName() + " Notification"); //TODO: Translate
			}
			Map objects = new HashMap();
			objects.put("topicmods",topicmods);
			objects.put("followeruser",followeruser);
			objects.put("apphome","/" + appid);
			objects.put("mediaarchive",mediaArchive);
			objects.put("messagessince",since);
			
			templatemail.send(objects);
			log.info("Chat Notified " + followeruser.getEmail() + " " + templatemail.getSubject());
		}
		else {
			log.info("User with no email address " + followeruser.getName());
		}
	}
	
	notificationsent.setValue("value", started);
	mediaArchive.saveData("catalogsettings",notificationsent);

}


init();

