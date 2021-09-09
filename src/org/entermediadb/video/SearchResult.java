package org.entermediadb.video;

import org.entermediadb.asset.util.MathUtils;

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
	long fieldStartTime;
	public long getStartTime()
	{
		return fieldStartTime;
	}
	public void setStartTime(long inStartTime)
	{
		fieldStartTime = inStartTime;
	}
	String fieldLabel;

	public String getStartSecondsAndHours()
	{
		return MathUtils.toDuration(getStartTime());
	}
	
}
