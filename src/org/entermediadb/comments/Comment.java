/*
 * Created on Jul 2, 2006
 */
package org.entermediadb.comments;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.openedit.data.BaseData;
import org.openedit.users.User;
import org.openedit.util.LocaleManager;

public class Comment extends BaseData
{
	private final static String fieldFormat = "MM/dd/yyyy HH:mm:ss Z";
	protected Date fieldDate;
	//protected String fieldCreationDate;
	protected String fieldComment;
	protected User fieldUser;
	protected LocaleManager fieldLocaleManager;
	public Comment()
	{
		// TODO Auto-generated constructor stub
	}
    public LocaleManager getLocaleManager()
	{
		return fieldLocaleManager;
	}
	public void setLocaleManager(LocaleManager inLocaleManager)
	{
		fieldLocaleManager = inLocaleManager;
	}
	/**
	 * @deprecated Use a shared formater or an internationalized version
	 * @return
	 */
	public String getCreationDate()
	{
		return new SimpleDateFormat(fieldFormat).format(getDate());
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
	public void setCreationDate(String inCreationDate)
	{
		try {
			setDate(new SimpleDateFormat(fieldFormat).parse(inCreationDate));
		} catch (ParseException e) {
			//throw new OpenEditException(e);
		}
	}
   //
	public Date getDate() {
		if (fieldDate == null)
			fieldDate = new Date();
		return fieldDate;
	}

	public void setDate(Date inDate) {
		fieldDate = inDate;
	}

	public User getUser()
	{
		return fieldUser;
	}

	public void setUser(User inUser)
	{
		fieldUser = inUser;
	}

	public void setComment(String inComments)
	{
		fieldComment = inComments;
	}

	public String getComment()
	{
		return fieldComment;
	}
	
}
