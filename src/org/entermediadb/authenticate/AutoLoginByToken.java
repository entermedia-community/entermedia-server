package org.entermediadb.authenticate;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.google.GoogleManager;
import org.openedit.WebPageRequest;
import org.openedit.users.User;
import org.openedit.users.UserManager;

public class AutoLoginByToken extends BaseAutoLogin implements AutoLoginProvider
{
	private static final Log log = LogFactory.getLog(AutoLoginByToken.class);
	public GoogleManager getGoogleManager(WebPageRequest inReq)
	{
		String catalogid = inReq.findPathValue("catalogid");
		if(catalogid == null)
		{
			catalogid = "system";
		}
		return (GoogleManager)getModuleManager().getBean(catalogid,"googleManager");
	}
	@Override
	public AutoLoginResult autoLogin(WebPageRequest inReq)
	{
		HttpServletRequest request = inReq.getRequest();
		if( request == null)
		{
			return null;
		}
		String accesskey = request.getHeader("X-token");
		String type = request.getHeader("X-tokentype");
		if( accesskey != null && type != null)
		{
			
			User user = null;
			if(type.equals("google") )
			{
				GoogleManager manager = getGoogleManager(inReq);
				Map<String,String> details = manager.getTokenDetails(accesskey);
				if( details != null)
				{
					String email = details.get("email");
					if( email != null)
					{
						user = manager.createUserIfNeeded(email);
					}	
				}
			}
			if(type.equals("adminkey") )
			{
				String email = request.getHeader("X-email");
				UserManager usermanager = getUserManager(inReq);
				user = usermanager.getUserByEmail(email);
				if(user == null)
				{
					user = (User)usermanager.getUserSearcher().createNewData();
					user.setEmail(email);
					user.setEnabled(true);
					usermanager.saveUser(user);
				}
			}
			else if( type.equals("entermedia") )
			{
				user = autoLoginFromMd5Value(inReq, accesskey);
			}
			
			if( user != null)
			{
				saveCookieForUser(inReq,user); //For next time
				AutoLoginResult result = new AutoLoginResult();
				result.setUser(user);
				return result;
			}	

		}

		return null;
	}

}
