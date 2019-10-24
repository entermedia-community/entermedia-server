package org.entermediadb.authenticate;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.SearcherManager;
import org.openedit.users.User;
import org.openedit.users.UserManager;
import org.openedit.util.StringEncryption;

public abstract class BaseAutoLogin implements AutoLoginProvider
{
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
