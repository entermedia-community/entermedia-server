package org.entermediadb.projects;

import java.util.Date;

public class Event
{
	protected String fieldLabel;
	public String getLabel()
	{
		return fieldLabel;
	}
	public void setLabel(String inLabel)
	{
		fieldLabel = inLabel;
	}
	public Date getDate()
	{
		return fieldDate;
	}
	public void setDate(Date inDate)
	{
		fieldDate = inDate;
	}
	public String getParentId()
	{
		return fieldParentId;
	}
	public void setParentId(String inParentId)
	{
		fieldParentId = inParentId;
	}
	public String getType()
	{
		return fieldType;
	}
	public void setType(String inType)
	{
		fieldType = inType;
	}
	public String getId()
	{
		return fieldId;
	}
	public void setId(String inId)
	{
		fieldId = inId;
	}
	protected Date fieldDate;
	protected String fieldParentId;
	protected String fieldType;
	protected String fieldId;
	
}
