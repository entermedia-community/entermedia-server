package org.entermediadb.asset.facedetect;

import java.util.Map;

import org.openedit.Data;
import org.openedit.data.ValuesMap;

public class FaceAsset
{
	public Data getAsset()
	{
		return fieldAsset;
	}
	protected void setAsset(Data inAsset)
	{
		fieldAsset = inAsset;
	}
	protected Data fieldAsset;
	protected ValuesMap fieldFaceLocationData;
	
	public ValuesMap getFaceLocationData()
	{
		return fieldFaceLocationData;
	}
	protected void setFaceLocationData(ValuesMap inFaceLocationData)
	{
		fieldFaceLocationData = inFaceLocationData;
	}
	protected Data fieldFaceProfileGroup;
	protected Data getFaceProfileGroup()
	{
		return fieldFaceProfileGroup;
	}
	protected void setFaceProfileGroup(Data inFaceProfileGroup)
	{
		fieldFaceProfileGroup = inFaceProfileGroup;
	}
	protected String getFaceProfileGroupId()
	{
		return getFaceProfileGroup().getId();
	}

}
