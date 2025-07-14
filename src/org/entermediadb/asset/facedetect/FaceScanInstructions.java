package org.entermediadb.asset.facedetect;

import java.util.Collection;

import org.openedit.Data;
import org.openedit.MultiValued;

public class FaceScanInstructions
{
	boolean fieldUpdateExistingFace = true;
	
	public boolean isUpdateExistingFace()
	{
		return fieldUpdateExistingFace;
	}
	public void setUpdateExistingFace(boolean inUpdateExistingFace)
	{
		fieldUpdateExistingFace = inUpdateExistingFace;
	}
	boolean fieldFindParents = true;
	
	public boolean isFindParents()
	{
		return fieldFindParents;
	}
	public void setFindParents(boolean inSetParents)
	{
		fieldFindParents = inSetParents;
	}
//	Collection<MultiValued> fieldAllRecords;
//	public Collection<MultiValued> getAllRecords()
//	{
//		return fieldAllRecords;
//	}
//	public void setAllRecords(Collection<MultiValued> inAllFaces)
//	{
//		fieldAllRecords = inAllFaces;
//	}
	public Collection<String> getAllAssetIds()
	{
		return fieldAllAssetIds;
	}
	public void setAllAssetIds(Collection<String> inAssetIds)
	{
		fieldAllAssetIds = inAssetIds;
	}
	Collection<String> fieldAllAssetIds;
	
	
	double fieldConfidenceLimit;
	public double getConfidenceLimit()
	{
		return fieldConfidenceLimit;
	}
	public void setConfidenceLimit(double inConfidenceLimit)
	{
		fieldConfidenceLimit = inConfidenceLimit;
	}
	public double getMinimumFaceSize()
	{
		return fieldMinimumFaceSize;
	}
	public void setMinimumFaceSize(double inMinimumFaceSize)
	{
		fieldMinimumFaceSize = inMinimumFaceSize;
	}
	double fieldMinimumFaceSize;
}
