package org.openedit.entermedia.creator;

import org.openedit.data.BaseData;

public class ConvertResult extends BaseData
{
	protected boolean fieldOk;
	protected boolean fieldComplete;
	protected String fieldError;
	
	public boolean isComplete() 
	{
		return fieldComplete;
	}
	
	public void setComplete(boolean inComplete) 
	{
		fieldComplete = inComplete;
	}
	
	public String getError()
	{
		return fieldError;
	}
	
	public void setError(String inError)
	{
		fieldError = inError;
	}
	public boolean isError()
	{
		return fieldError != null;
	}
	protected String fieldOutputPath;
	
	public boolean isOk()
	{
		return fieldOk;
	}
	public void setOk(boolean inOk)
	{
		fieldOk = inOk;
	}
	public String getOutputPath()
	{
		return fieldOutputPath;
	}
	public void setOutputPath(String inOutputPath)
	{
		fieldOutputPath = inOutputPath;
	}
	
}
