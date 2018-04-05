package org.entermediadb.sitemonitor;

public class Stat
{
	private String fieldName;
	private Object fieldValue;
	
	public Stat()
	{
	}

	public String getName()
	{
		return fieldName;
	}

	public void setName(String inName)
	{
		fieldName = inName;
	}

	public Object getValue()
	{
		return fieldValue;
	}

	public void setValue(Object inObject)
	{
		fieldValue = inObject;

	}
	
}
