package org.entermediadb.authenticate;

import java.util.Date;

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
	private static final Log log = LogFactory.getLog(AutoLoginWithCookie.class);
	public static final String ENTERMEDIAKEY = "entermedia.key"; //username + md542 + md5password + tstamp + timestampenc
	protected static final String TIMESTAMP = "tstamp";
	protected static final long MILLISECONDS_PER_DAY = 24 * 60 * 60 * 1000;// milliseconds in one day (used to calculate password expiry)

	protected StringEncryption fieldCookieEncryption;
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


	protected User autoLoginFromMd5Value(WebPageRequest inReq, String uandpass)
	{
		//get the password expiry in days
		int pwd_expiry_in_days = 1;
		String str = inReq.getPageProperty("temporary_password_expiry");
		if (str != null && !str.isEmpty())
		{
			try
			{
				pwd_expiry_in_days = Integer.parseInt(str);
			}
			catch (NumberFormatException e)
			{

			}
			if (pwd_expiry_in_days < 1)
				pwd_expiry_in_days = 1;//default if malformed
			if (log.isDebugEnabled())
			{
				log.debug("Password is set to expire in " + pwd_expiry_in_days + " days");
			}
		}
		//String uandpass = cook.getValue();
		if (uandpass != null)
		{
			int split = uandpass.indexOf("md542");
			if (split == -1)
			{
				return null;
			}
			String username = uandpass.substring(0, split);

			User user = getUserManager(inReq).getUser(username);
			if (user != null && user.getPassword() != null)
			{
				String md5 = uandpass.substring(split + 5);

				//if timestamp included, check whether the autologin has expired
				if ((split = md5.indexOf(TIMESTAMP)) != -1)
				{
					String tsenc = md5.substring(split + TIMESTAMP.length());
					md5 = md5.substring(0, split);
					try
					{
						String ctext = getCookieEncryption().decrypt(tsenc);
						long ts = Long.parseLong(ctext);
						long current = new Date().getTime();
						if ((current - ts) > (pwd_expiry_in_days * MILLISECONDS_PER_DAY))
						{
							log.debug("Autologin has expired, redirecting to login page");
							return null;
						}
						else
						{
							if (log.isDebugEnabled())
							{
								log.debug("Autologin has not expired, processing md5 password");
							}
						}
					}
					catch (Exception oex)
					{
						log.error(oex.getMessage(), oex);
						return null;
					}
				}
				else
				{
					if (log.isDebugEnabled())
					{
						log.debug("Autologin does not have a timestamp");
					}
				}

				try
				{
					String hash = getCookieEncryption().getPasswordMd5(user.getPassword());
					if (md5.equals(hash))
					{
//						String catalogid =user.get("catalogid");
//						inReq.putSessionValue(catalogid + "user", user);
						return user;
					}
					else
					{
						log.info("Auto login did not work " + username + " md5 " + md5);
						return null;
					}
				}
				catch (Exception ex)
				{
					//throw new OpenEditException(ex);
					getCookieEncryption().removeCookie(inReq,AutoLoginWithCookie.ENTERMEDIAKEY);
					getCookieEncryption().removeCookie(inReq,"entermedia.keyopenedit");
					log.error(ex);
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
			String md5 = inReq.getRequestParameter(ENTERMEDIAKEY);
			if (md5 != null)
			{
				ok = autoLoginFromMd5Value(inReq, md5);
			}
			if( ok == null && inReq.getRequest() != null)
			{
				md5 = inReq.getRequest().getHeader("X-" + ENTERMEDIAKEY);
				log.info("Found MD5 in Header" + md5);
				if (md5 != null)
				{
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

	public StringEncryption getCookieEncryption()
	{
		//		if (fieldCookieEncryption == null)
		//		{
		//			fieldCookieEncryption = new StringEncryption();
		////			String KEY = "SomeWeirdReallyLongStringYUITYGFNERDF343dfdGDFGSDGGD";
		////			fieldCookieEncryption.setEncryptionKey(KEY);
		//		}
		return fieldCookieEncryption;
	}

	public void setCookieEncryption(StringEncryption inCookieEncryption)
	{
		fieldCookieEncryption = inCookieEncryption;
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

	public void saveCookieForUser(WebPageRequest inReq,User inUser)
	{
		HttpServletResponse res = inReq.getResponse();
		if (res != null)
		{
			String name = getCookieEncryption().createMd5CookieName(inReq,AutoLoginWithCookie.ENTERMEDIAKEY,true);
			try
			{
				String value = getCookieEncryption().getEnterMediaKey(inUser);
				Cookie cookie = new Cookie(name, value);
				cookie.setMaxAge(Integer.MAX_VALUE);
				//Needs new servelet api jar
				//				cookie.setHttpOnly(true);
				
				cookie.setPath("/"); // http://www.unix.org.ua/orelly/java-ent/servlet/ch07_04.htm   This does not really work. It tends to not send the data
				res.addCookie(cookie);
				inReq.putPageValue("entermediakey", value);
			}
			catch (Exception ex)
			{
				throw new OpenEditException(ex);
			}
			//TODO: Add a new alternative cookie that will auto login the user by passing the md5 of a secret key + their password
			//TODO: If the MD5 matches on both sides then we are ok to log them in

		}
	}
	
}
