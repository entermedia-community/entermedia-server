package org.entermediadb.asset.facedetect;

import org.entermediadb.asset.Asset;
import org.openedit.Data;
import org.openedit.data.ValuesMap;

public class FaceAsset
{
	public Asset getAsset()
	{
		return fieldAsset;
	}
	protected void setAsset(Asset inAsset)
	{
		fieldAsset = inAsset;
	}
	protected Asset fieldAsset;
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
	public Data getFaceProfileGroup()
	{
		return fieldFaceProfileGroup;
	}
	protected void setFaceProfileGroup(Data inFaceProfileGroup)
	{
		fieldFaceProfileGroup = inFaceProfileGroup;
	}
	public String getFaceProfileGroupId()
	{
		return getFaceProfileGroup().getId();
	}

}
