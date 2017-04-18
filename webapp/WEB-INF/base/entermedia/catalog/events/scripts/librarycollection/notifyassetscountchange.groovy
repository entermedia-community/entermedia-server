package librarycollection;

import org.entermediadb.asset.MediaArchive
import org.entermediadb.email.WebEmail
import org.openedit.Data
import org.openedit.MultiValued
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.users.User


public void init()
{
	MediaArchive mediaArchive = (MediaArchive)context.getPageValue("mediaarchive");
	
	Date fiveminago = new Date(System.currentTimeMillis() - 60*1000*5);
	Date onehourago = new Date(System.currentTimeMillis() - 60*1000*60);
	
	Set collections = new HashSet();
	
	HitTracker found = mediaArchive.query("librarycollection").match("lastmodifieddatedirty","true").search();
	if( found.isEmpty() )
	{
		return;
	}
	for (MultiValued collection in found)
	{
		Date modifiedon = collection.getDate("lastmodifieddate");  //TODO: Use database to limit list
		if( modifiedon.before(fiveminago)) //Quiet period
		{
			//Send notification?
			Date lastsent = collection.getDate("lastmodifieddatesent");
			if( lastsent == null || lastsent.before(onehourago))
			{
				collections.put(collection.getId(), collection);
				int counted = mediaArchive.query("asset").match("categories",collection.get("rootcategory")).search().size();
				collection.setValue("assetcounted", counted);
			}
		}
	}
	
	//TODO: Add owners as followers automatically
	HitTracker followers = mediaArchive.query("librarycollectionshare").orgroup("librarycollection",collections).search();
	if( followers.isEmpty() )
	{
			return;
	}
	Date now = new Date();
	
	//Load each users collections
	Map users = new HashMap();
	for (MultiValued follower in followers)
	{
		String userid = follower.get("followeruser");
		List dirtycollections = (List)users.get(userid);
		if( dirtycollections == null)
		{
			collections = new ArrayList();
			users.put(userid, dirtycollections);
		}
		String collectionid = follower.get("librarycollection");
		dirtycollections.add(collections.get(collectionid));
	}
	
	for (String userid in users.keySet())
	{
		//Make sure the root folder is within the library root folder
		User followeruser = mediaArchive.getUserManager().getUser(userid);
		if( followeruser != null && followeruser.getEmail() != null)
		{
			String appid =  mediaArchive.getCatalogSettingValue("events_notify_app");
			
			String template = "/" + appid + "/theme/emails/collection-count-modified.html";
		
			List dirtycollections = (List)users.get(userid);
			
			WebEmail templatemail = mediaArchive.createSystemEmail(followeruser, template);
			templatemail.setSubject("[EM] " + dirtycollections.size + " Collection Notifications"); //TODO: Translate
			Map objects = new HashMap();
			objects.put("dirtycollections",dirtycollections);
			objects.put("followeruser",followeruser);
			objects.put("apphome","/" + appid);
			templatemail.send(objects);
		}
	}
	Searcher searcher = mediaArchive.getSearcher("librarycollection");
	List tosave = new ArrayList();
	for (MultiValued collection in found)
	{
		Data data = searcher.loadData(collection);
		data.setValue("lastmodifieddatedirty",false);
		data.setValue("lastmodifieddatesent",now);
		tosave.add(data);
	}
	searcher.saveAllData(tosave, null);
	
}

init();

