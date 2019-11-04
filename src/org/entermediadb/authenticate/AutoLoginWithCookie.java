package org.entermediadb.authenticate;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.users.User;
import org.openedit.util.PathUtilities;
import org.openedit.util.StringEncryption;

public class AutoLoginWithCookie extends BaseAutoLogin implements AutoLoginProvider
{
	static final Log log = LogFactory.getLog(AutoLoginWithCookie.class);
	protected User readPasswordFromCookie(WebPageRequest inReq) throws OpenEditException
	{
		// see if we have a coookie for this person with their encrypted password
		// in it
		HttpServletRequest req = inReq.getRequest();
		if (req != null)
		{
			Cookie[] cookies = req.getCookies();

			if (cookies != null)
			{
				String id = createMd5CookieName(inReq,true);
				String idold = createMd5CookieName(inReq,false);
				for (int i = 0; i < cookies.length; i++)
				{
					Cookie cook = cookies[i];
					if (cook.getName() != null)
					{
						if( id.equals(cook.getName() ) || idold.equals(cook.getName() ) )
						{
							User user = autoLoginFromMd5Value(inReq, cook.getValue());
							if( user != null)
							{
								return user;
							}
							else
							{
								cook.setMaxAge(0); // remove the cookie
								inReq.getResponse().addCookie(cook);
							}
						}
					}
				}
			}
		}
		return null;
	}


	@Override
	public AutoLoginResult autoLogin(WebPageRequest inReq)
	{
		//log.info("Auto Login check");

		User ok = null;
		if (inReq.getSessionValue("autologindone") == null)
		{
			ok = readPasswordFromCookie(inReq);
		}
		if (ok == null)
		{
			
			if( inReq.getRequest() != null)
			{
				String ct = inReq.getRequest().getContentType();
				//"application/json; charset=utf-8", //This causes CORS to preflight
				if(ct != null && ct.contains("application/json") )
				{
					inReq.getJsonRequest();
				}
			}
			
			String md5 = inReq.getRequestParameter(ENTERMEDIAKEY);
			if (md5 != null)
			{
				ok = autoLoginFromMd5Value(inReq, md5);
			}
			if( ok == null && inReq.getRequest() != null)
			{
				md5 = inReq.getRequest().getHeader("X-" + ENTERMEDIAKEY); //If you get null headers in JSON make sure the user is logged in first
				//Otherwise you might be getting the HTTP Method == settings that is checking for CORS crap see https://www.codeproject.com/Questions/1211743/Send-custom-header-with-jquery-not-working
				if (md5 != null)
				{
					//log.info("Looking for key in " + inReq.getPathUrl() + " found " +md5);
					
					log.info("Found MD5 in Header" + md5);
					ok = autoLoginFromMd5Value(inReq, md5);
				}
			}
		}
		
		if( ok != null)
		{
			AutoLoginResult result = new AutoLoginResult();
			result.setUser(ok);
			return result;
		}
		
		return null;
	}


	String createMd5CookieName(WebPageRequest inReq, boolean withapp)
	{
		String home = (String) inReq.getPageValue("home");
		
		String name = ENTERMEDIAKEY + home;
		if( withapp )
		{
			String root = PathUtilities.extractRootDirectory(inReq.getPath() );
			if( root != null && root.length() > 1)
			{
				name = name + root.substring(1);
			}
		}
		
		return name;
	}

	
}
