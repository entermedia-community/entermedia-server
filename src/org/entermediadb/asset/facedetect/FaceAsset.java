package org.entermediadb.asset.facedetect;

import org.openedit.Data;

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
