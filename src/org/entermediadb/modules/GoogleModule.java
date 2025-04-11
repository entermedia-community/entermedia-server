package org.entermediadb.modules;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.google.GoogleManager;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.users.User;

public class GoogleModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(GoogleModule.class);

	public GoogleManager getGoogleManager(WebPageRequest inReq)
	{
		String catalogid = inReq.findPathValue("catalogid");
		return (GoogleManager)getModuleManager().getBean(catalogid,"googleManager");
	}


	public void syncAssets(WebPageRequest inReq) throws Exception
	{

		MediaArchive archive = getMediaArchive(inReq);
		Data authinfo = archive.getData("oauthprovider", "google");
		log.info("Running syncAssets");
		String token = getGoogleManager(inReq).getAccessToken();
		//getGoogleManager(inReq).syncAssets(token, "Drive", true);

	}

	public void syncContacts(WebPageRequest inReq) throws Exception
	{
		//MediaArchive archive = getMediaArchive(inReq);
		log.info("Running syncUsers");
		
		getGoogleManager(inReq).syncContacts(inReq.getUser());

	}

	public void createUserInFirebase(WebPageRequest inReq) throws Exception
	{
		User user = inReq.getUser();
		
		if( user != null)
		{
			getGoogleManager(inReq).createFireBaseUser(user);
			//update Firebase		
			String value = getMediaArchive(inReq).getUserManager().getEnterMediaKey(user);
			
			inReq.putPageValue("entermediakey", value);
			inReq.putPageValue("user", user);
			
			String firebasepassword = user.get("firebasepassword");
			if( firebasepassword == null)
			{
				throw new OpenEditException("No password found");
			}
			inReq.putPageValue("firebasepassword", firebasepassword);
			inReq.putPageValue("status","ok");
		}
		else
		{
			log.info("No user logged in");
			inReq.putPageValue("status","loginfailed");
		}
	}	
	
	public void createUserFromGoogleKey(WebPageRequest inReq) throws Exception
	{
		String accesstoken = inReq.getRequestParameter("accesstoken");
		
		GoogleManager manager = getGoogleManager(inReq);
		Map<String,String> details = manager.getTokenDetails(accesstoken);
		if( details != null)
		{
			String email = details.get("email");
			if( email != null)
			{
				User user = manager.createUserIfNeeded(email);
				String value = getMediaArchive(inReq).getUserManager().getEnterMediaKey(user);
				inReq.putPageValue("entermediakey", value);
				inReq.putPageValue("user", user);
			}	
		}

	}	

	
}
