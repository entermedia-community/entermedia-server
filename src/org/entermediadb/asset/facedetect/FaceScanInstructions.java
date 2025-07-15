package org.entermediadb.asset.facedetect;

import java.util.Collection;
import java.util.Map;

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

	protected Map<String,MultiValued> fieldChildChunk; //Used to compare with then we loop again
	public Map<String, MultiValued> getChildChunk()
	{
		return fieldChildChunk;
	}
	public void setChildChunk(Map<String, MultiValued> inChildChunk)
	{
		fieldChildChunk = inChildChunk;
	}
	public Map<String, MultiValued> getParentChunk()
	{
		return fieldParentChunk;
	}
	public void setParentChunk(Map<String, MultiValued> inParentChunk)
	{
		fieldParentChunk = inParentChunk;
	}
	protected Map<String,MultiValued> fieldParentChunk; //Used to compare with then we loop again

	public MultiValued loadData(String inId)
	{
		MultiValued  found = getChildChunk().get(inId);
		if( found == null)
		{
			found = getParentChunk().get(inId);
		}
		return found;
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
