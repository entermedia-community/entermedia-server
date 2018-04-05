package org.entermediadb.sitemonitor;

public class Stat
{
	private String fieldName;
	private Object fieldValue;
	private boolean isError;
	private String errorMsg;
	
	public Stat()
	{
		isError = false;
		errorMsg = null;
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
	
	public boolean isError()
	{
		return isError;
	}

	public void setError(boolean inIsError)
	{
		isError = inIsError;
	}

	public String getErrorMsg()
	{
		return errorMsg;
	}

	public void setErrorMsg(String inErrorMsg)
	{
		errorMsg = inErrorMsg;
	}

}
