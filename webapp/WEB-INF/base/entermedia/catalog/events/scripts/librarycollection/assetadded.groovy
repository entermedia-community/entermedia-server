package librarycollection

import org.entermediadb.asset.MediaArchive
import org.entermediadb.email.WebEmail
import org.entermediadb.projects.LibraryCollection
import org.openedit.Data
import org.openedit.users.User

MediaArchive mediaArchive = (MediaArchive)context.getPageValue("mediaarchive");

public void init()
{

	Object send = librarycol.getValue("sentnotifyassetadded");
	if( send != null && send )
	{
			//Make sure the root folder is within the library root folder
			String userid = followerdata.get("followeruser");
			
			User followeruser = mediaArchive.getUserManager().getUser(userid);
			//Send welcomome email
	
			if( followeruser != null && followeruser.getEmail() != null)
			{

				Data profile = mediaArchive.getData("userprofile", userid);
				
				String appid = profile.get("lastviewedapp")l
				if(appid == null){
					appid = context.findValue("applicationid");
				}
				
								
				String template = "/" + appid + "/theme/emails/collection-add-new-follower.html";
			
				LibraryCollection collection = mediaArchive.getData("librarycollection",followerdata.get("librarycollection") );
				
				 WebEmail templatemail = mediaArchive.createSystemEmail(followeruser, template);
				templatemail.setSubject("[EM] " + collection.getName() + " Follower Added"); //TODO: Translate
				Map objects = new HashMap();
				objects.put("followerdata",followerdata);
				objects.put("librarycol",collection);
				objects.put("apphome","/" + appid);
				templatemail.send(objects);
				 
				followerdata.setValue("sent", new Date() );
				mediaArchive.getSearcher("librarycollectionshares").saveData(followerdata, null);
				log.info("Notify follower ${followeruser.getEmail()} ${template}");
			}
	}
}

init();

