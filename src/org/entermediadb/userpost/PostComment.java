/*
 * Created on Jul 2, 2006
 */
package org.entermediadb.userpost;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import org.openedit.WebPageRequest;
import org.openedit.data.BaseData;
import org.openedit.users.BaseUser;
import org.openedit.users.User;
import org.openedit.users.UserManager;
import org.openedit.util.LocaleManager;

public class PostComment extends BaseData
{
	protected UserManager fieldUserManager;
	public UserManager getUserManager()
	{
		return fieldUserManager;
	}
	public void setUserManager(UserManager inUserManager)
	{
		fieldUserManager = inUserManager;
	}

	protected LocaleManager fieldLocaleManager;
	public PostComment()
	{
	}
    public LocaleManager getLocaleManager()
	{
		return fieldLocaleManager;
	}
	public void setLocaleManager(LocaleManager inLocaleManager)
	{
		fieldLocaleManager = inLocaleManager;
	}
	public String getShortDate(WebPageRequest inReq)
	{
		String locale = inReq.getLocale();
		return getShortDate(locale);
	}
	public String getShortDate(String inLocale)
	{
		Locale loc = getLocaleManager().getLocale(inLocale);
		DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT,loc);
		return format.format(getDate());
	}
	public String getShortDate(Locale inLocale)
	{
		DateFormat format = DateFormat.getTimeInstance(DateFormat.SHORT,inLocale);
		return format.format(getDate());
	}
	public Date getDate() 
	{
		return getDate("date");
	}

	public void setDate(Date inDate) 
	{
		setValue("date",inDate);
	}

	public String getUserLabel()
	{
		BaseUser user = (BaseUser)getUser();
		if( user == null)
		{
			return null;
		}
		String label = user.getAnonNickName();
		return label;
	}
	
	public User getUser()
	{
		String userid = get("userid");
		User user = getUserManager().getUser(userid,true);
		return user;
	}

	public String getComment()
	{
		return get("commenttext");
	}
	
	public void setComment(String inComment)
	{
		setValue("commenttext",inComment);
	}
	
}
