package org.entermediadb.webui.tabs;

import java.util.Date;


public class Tab implements Comparable
{
	protected String fieldId;
	protected String fieldName;
	protected String fieldPath;
	protected int fieldMaxLevel;
	protected Date fieldTimeAdded;
	
	public String getName()
	{
		if( fieldName == null)
		{
			String name = getPath();
			int MAX = 20;
			if( name != null && name.length() > MAX)
			{
				name = ".." + name.substring(name.length() - MAX, name.length());
			}
			return name;
		}
		return fieldName;
	}
	public void setName(String inName)
	{
		fieldName = inName;
	}
	public String getPath()
	{
		return fieldPath;
	}
	public void setPath(String inLink)
	{
		fieldPath = inLink;
	}
	public String getId()
	{
		return fieldId;
	}
	public void setId(String inId)
	{
		fieldId = inId;
	}
	public int getMaxLevel()
	{
		return fieldMaxLevel;
	}
	public void setMaxLevel(int inMaxLevel)
	{
		fieldMaxLevel = inMaxLevel;
	}
	public int compareTo(Object inO)
	{
		Tab inTab = (Tab)inO;
		
//		String path = inTab.getPath();
//		int inDeep = path.split("/").length;
//		int meDeep = getPath().split("/").length;
//		
//		if( inDeep > meDeep)
//		{
//			return -1;
//		}
//		else if( inDeep < meDeep)
//		{
//			return 1;
//		}
		
//		int i = getPath().toLowerCase().compareTo(inTab.getPath().toLowerCase());
		//System.out.println(getPath() +" was " + i + " to " + inTab.getPath());
		int i = getTimeAdded().compareTo(inTab.getTimeAdded());
		if( i > 0)
		{
			return 0 - i;
		}
		else if( i < 0)
		{
			return i * -1;
		}
		return 0;
	}
	public Date getTimeAdded()
	{
		return fieldTimeAdded;
	}
	public void setTimeAdded(Date inTimeAdded)
	{
		fieldTimeAdded = inTimeAdded;
	}
}
