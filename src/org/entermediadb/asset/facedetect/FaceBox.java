package org.entermediadb.asset.facedetect;

import java.util.List;

import org.json.simple.JSONArray;
import org.openedit.Data;

public class FaceBox
{
	public String toJsonArray()
	{
		String json = JSONArray.toJSONString( getBoxArea());
		return json;
	}
	public String getId()
	{
		return getEmbeddedData().getId();
	}
	public String getAssetId()
	{
		return getEmbeddedData().get("assetid");
	}
	protected Data fieldEmbeddedData;
	public Data getEmbeddedData()
	{
		return fieldEmbeddedData;
	}
	public void setEmbeddedData(Data inEmbeddedData)
	{
		fieldEmbeddedData = inEmbeddedData;
	}
	public Data getPerson()
	{
		return fieldPerson;
	}
	public void setPerson(Data inPerson)
	{
		fieldPerson = inPerson;
	}
	
	public double getTimecodeStartSeconds()
	{
		return fieldTimecodeStartSeconds;
	}
	public void setTimecodeStartSeconds(double inTimecodeStartSeconds)
	{
		fieldTimecodeStartSeconds = inTimecodeStartSeconds;
	}
	protected Data fieldPerson;
	protected List<Integer> fieldBoxArea;
	
	public List<Integer> getBoxArea()
	{
		return fieldBoxArea;
	}
	public void setBoxArea(List<Integer> inBoxArea)
	{
		fieldBoxArea = inBoxArea;
	}

	protected double fieldTimecodeStartSeconds;
}
