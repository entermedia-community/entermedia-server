package org.entermediadb.video;

import java.util.Map;

public class Clip
{
	protected Map fieldData;
	
	public Map getData()
	{
		return fieldData;
	}
	public void setData(Map inData)
	{
		fieldData = inData;
	}
	public double getStart()
	{
		Double d = (Double)getData().get("timecodestart");
		if( d == null)
		{
			return 0d;
		}
		return d;
	}
	public double getLength()
	{
		Double d = (Double)getData().get("timecodelength");
		if( d == null)
		{
			return 0d;
		}
		return d;
	}
	
	public Object getValue(String inKey)
	{
		return getData().get(inKey);
	}
}
