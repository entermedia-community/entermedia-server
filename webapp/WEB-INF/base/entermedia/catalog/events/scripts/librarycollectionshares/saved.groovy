package librarycollection

import org.entermediadb.asset.MediaArchive
import org.entermediadb.email.WebEmail
import org.openedit.Data
import org.openedit.users.User

public void init() 
{
	String id = context.getRequestParameter("id");

	MediaArchive mediaArchive = (MediaArchive)context.getPageValue("mediaarchive");
	Data followerdata = mediaArchive.getSearcher("librarycollectionshares").searchById(id);
	if( followerdata != null ) 
	{
		//Make sure the root folder is within the library root folder
		String userid = followerdata.get("followeruser");
		
		User followeruser = mediaArchive.getUserManager().getUser(userid);
		//Send welcomome email
/*
		if( followeruser != null)
		{
			String apphome = followeruser.get("apphome");
			
			String template = apphome + "/views/notifications/collectionsnewfollower.html";
		
		 	WebEmail template = mediaArchive.createSystemEmail(followeruser, template);
			Map objects = new Map();
			objects.put("followerdata",followerdata);
			objects.put("librarycol",collection);
			template.send(objects);
			 
			followerdata.setProperty("sent", username);
			//library.setProperty("ownerprofile",context.getUserProfile().getId()); 
			mediaArchive.getSearcher("librarycollectionshares").saveData(followerdata, null);
			log.info("saving library $path");
		}	
	}
*/	
}

init();

