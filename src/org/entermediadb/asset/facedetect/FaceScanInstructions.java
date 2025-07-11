package org.entermediadb.asset.facedetect;

import java.util.Collection;

import org.openedit.Data;
import org.openedit.MultiValued;

public class FaceScanInstructions
{
	
	boolean fieldSetParents = true;
	
	public boolean isSetParents()
	{
		return fieldSetParents;
	}
	public void setSetParents(boolean inSetParents)
	{
		fieldSetParents = inSetParents;
	}
	Collection<MultiValued> fieldAllRecords;
	public Collection<MultiValued> getAllRecords()
	{
		return fieldAllRecords;
	}
	public void setAllRecords(Collection<MultiValued> inAllFaces)
	{
		fieldAllRecords = inAllFaces;
	}
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
