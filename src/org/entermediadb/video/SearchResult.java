package org.entermediadb.video;

public class SearchResult
{

	public String getType()
	{
		return fieldType;
	}
	public void setType(String inType)
	{
		fieldType = inType;
	}

	public String getLabel()
	{
		return fieldLabel;
	}
	public void setLabel(String inLabel)
	{
		fieldLabel = inLabel;
	}
	String fieldType;
	double fieldStartTime;
	public double getStartTime()
	{
		return fieldStartTime;
	}
	public void setStartTime(double inStartTime)
	{
		fieldStartTime = inStartTime;
	}
	String fieldLabel;
}
