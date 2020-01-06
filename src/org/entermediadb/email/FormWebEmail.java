package org.entermediadb.email;

import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;

public class FormWebEmail extends WebEmail
{
	protected String fieldBody;

	
	public void send(Map inObjects)
	{
	throw new OpenEditException("Not Implemented for FormWebEmail");
	}
	
	
	public void send() throws OpenEditException
	{
		
		if (isValidMessage())
		{
			try
			{
				String from = getFrom();
				String fromname = getFromName();
				fieldPostMail.postMail(getTo(),getSubject(),null,getBody().toString(),from, fromname);
			}
			catch (MessagingException ex)
			{
				throw new OpenEditException(ex);
			}
		}
		else
		{
			throw new OpenEditException("Invalid message");
		}
	}

	public String getBody()
	{
		return fieldBody;
	}

	public void setBody(String inBody)
	{
		fieldBody = inBody;
	}

	public void loadSettings( WebPageRequest inReq ) throws OpenEditException
	{
		super.loadSettings(inReq);
		
		String body = inReq.getRequestParameter("body");
		setBody(body);
	}
	
	private boolean isValidMessage()
	{
		return (isValidField(getTo()) && isValidField(getBody()) 
				&& isValidField(getFrom()) );
	}
	
	private boolean isValidField(String field[])
	{
		if (field.length <= 0)
		{
			return false;
		}
		
		for (int i = 0; i < field.length; i++)
		{
			if (field[i] == null || field[i].length() <= 0)
			{
				return false;
			}
		}
		return true;
	}
	
	private boolean isValidField(String field)
	{
		return (field != null && field.length() > 0);
	}
}
