package librarycollectionshares;

import org.entermediadb.asset.MediaArchive
import org.entermediadb.email.WebEmail
import org.entermediadb.projects.LibraryCollection
import org.openedit.Data
import org.openedit.users.User

public void init() {


	String id = context.getRequestParameter("id");

	log.info("id was " + id);

	MediaArchive mediaArchive = (MediaArchive)context.getPageValue("mediaarchive");
	Data followerdata = mediaArchive.getData("librarycollectionshares",id);
	if( followerdata != null ) {
		Object sent = followerdata.getValue("sent");
		if( sent == null) {
			//Make sure the root folder is within the library root folder
			String userid = followerdata.get("followeruser");

			User followeruser = mediaArchive.getUserManager().getUser(userid);
			//Send welcomome email

			if( followeruser != null && followeruser.getEmail() != null)
			{
				String appid =  context.getRequestParameter("applicationid");

				String template = "/" + appid + "/theme/emails/collection-add-new-follower.html";

				LibraryCollection collection = mediaArchive.getData("librarycollection",followerdata.get("librarycollection") );
				if(collection != null) {
					WebEmail templatemail = mediaArchive.createSystemEmail(followeruser, template);
					templatemail.setSubject("[EM] " + collection.getName() + " Follower Added"); //TODO: Translate
					Map objects = new HashMap();
					objects.put("followerdata",followerdata);
					objects.put("librarycol",collection);
					objects.put("apphome","/" + appid);
					objects.put("context", context);
					templatemail.send(objects);

					followerdata.setValue("sent", new Date() );
					mediaArchive.getSearcher("librarycollectionshares").saveData(followerdata, null);
					log.info("Notify follower ${followeruser.getEmail()} ${template}");
				}
			}
		}
	}
}

init();

