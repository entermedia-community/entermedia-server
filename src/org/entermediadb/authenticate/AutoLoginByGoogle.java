package org.entermediadb.authenticate;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.google.GoogleManager;
import org.openedit.WebPageRequest;
import org.openedit.users.User;
import org.openedit.users.UserManager;

public class AutoLoginByGoogle extends BaseAutoLogin implements AutoLoginProvider
{
	private static final Log log = LogFactory.getLog(AutoLoginByGoogle.class);
	public GoogleManager getGoogleManager(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		if(catalogid == null)
		{
			catalogid = "system";
		}
		return (GoogleManager)getModuleManager().getBean(catalogid,"googleManager");
	}
	@Override
	public AutoLoginResult autoLogin(WebPageRequest inReq)
	{
		String accesskey = inReq.getRequestParameter("googleaccesskey");
		if( accesskey != null)
		{
			GoogleManager manager = getGoogleManager(inReq);
			Map<String,String> details = manager.getTokenDetails(accesskey);
			if( details != null)
			{
				String email = details.get("email");
				if( email != null)
				{
					User user = manager.createUserIfNeeded(email);
					if( user != null)
					{
						saveCookieForUser(inReq,user); //For next time
						AutoLoginResult result = new AutoLoginResult();
						result.setUser(user);
						return result;
					}	
				}	
			}
		}

		return null;
	}

}
