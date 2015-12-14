package org.entermediadb.events;

import java.util.Iterator;

import org.openedit.OpenEditException;
import org.openedit.config.XMLConfiguration;

public abstract class Action extends PropertyContainer
{
	protected String fieldName;
	protected String fieldType;

	public void configure(XMLConfiguration inConfig) throws OpenEditException
	{
		String name = inConfig.getAttribute("name");
		setName(name);
	}

	public String getName()
	{
		return fieldName;
	}
	public void setName(String inName)
	{
		fieldName = inName;
	}
	
	public abstract boolean execute(BaseTask inTask) throws OpenEditException;
	//public abstract boolean isComplete(VideoTask inTask) throws OpenEditException;

	
	public String getType()
	{
		return fieldType;
	}

	public void setType(String inType)
	{
		fieldType = inType;
	}
	
	/*
	 * Substitutes properties in for any variables in the String
	 */
	public String resolveString(String inString)
	{
		String newString = new String(inString);
		if (getProperties() != null)
		{
			for (Iterator iterator = getProperties().keySet().iterator(); iterator.hasNext();)
			{
				String key = (String) iterator.next();
				String val = getProperty(key);
				newString = newString.replaceAll("\\$\\{" + key + "\\}", val);
			}
		}
		return newString;
	}
}
