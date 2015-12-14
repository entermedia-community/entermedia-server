/*
 * Created on May 17, 2006
 */
package org.entermediadb.modules.update;

public class Site
{
	protected String fieldId;
	protected String fieldText;
	protected String fieldHref;
	protected String fieldUsername;
	protected String fieldPassword;
	
	public String getHref()
	{
		return fieldHref;
	}
	public void setHref(String inHref)
	{
		fieldHref = inHref;
	}
	public String getId()
	{
		return fieldId;
	}
	public void setId(String inId)
	{
		fieldId = inId;
	}
	public String getText()
	{
		return fieldText;
	}
	public void setText(String inText)
	{
		fieldText = inText;
	}
	public String getPassword()
	{
		return fieldPassword;
	}
	public void setPassword(String inPassword)
	{
		fieldPassword = inPassword;
	}
	public String getUsername()
	{
		return fieldUsername;
	}
	public void setUsername(String inUsername)
	{
		fieldUsername = inUsername;
	}
	public String toString()
	{
		return getText() + " " + getHref();
	}
}
