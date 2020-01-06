package org.entermediadb.authenticate;

import java.util.Date;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.SearcherManager;
import org.openedit.users.User;
import org.openedit.users.UserManager;
import org.openedit.util.StringEncryption;

public abstract class BaseAutoLogin implements AutoLoginProvider
{
	static final Log log = LogFactory.getLog(BaseAutoLogin.class);

	protected ModuleManager fieldModuleManager;

	protected String TIMESTAMP = "tstamp";
	protected long MILLISECONDS_PER_DAY = 24 * 60 * 60 * 1000;
	protected StringEncryption fieldCookieEncryption;


	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	protected SearcherManager getSearcherManager()
	{
		return (SearcherManager)getModuleManager().getBean( "searcherManager" );
	}

	protected UserManager getUserManager(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		if( catalogid == null)
		{
			catalogid = "system";
		}
		return (UserManager) getModuleManager().getBean(catalogid, "userManager");
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

	public void saveCookieForUser(WebPageRequest inReq,User inUser)
	{
		HttpServletResponse res = inReq.getResponse();
		if (res != null)
		{
			String name = getCookieEncryption().createMd5CookieName(inReq,ENTERMEDIAKEY,true);
			try
			{
				String value = getCookieEncryption().getEnterMediaKey(inUser);
				Cookie cookie = new Cookie(name, value);
				
				Data age  = getSearcherManager().getCachedData("system", "systemsettings", "cookie_expiration_age");
				int maxage = Integer.MAX_VALUE;
				if( age != null)
				{
					//how many seconds a given cookie should be
					maxage = Integer.parseInt(age.get("value"));
				}
				cookie.setMaxAge(maxage);
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

	protected User autoLoginFromMd5Value(WebPageRequest inReq, String uandpass) {
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
						getCookieEncryption().removeCookie(inReq,ENTERMEDIAKEY);
						getCookieEncryption().removeCookie(inReq,"entermedia.keyopenedit");
						log.error(ex);
					}
				}
			}
			return null;
		}

}
