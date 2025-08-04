package org.entermediadb.asset.facedetect;

import java.util.Collection;
import java.util.Map;

import org.openedit.MultiValued;

public class FaceScanInstructions
{
	boolean fieldSkipExistingFaces = true;
	
	public boolean isSkipExistingFaces()
	{
		return fieldSkipExistingFaces;
	}
	public void setSkipExistingFaces(boolean inUpdateExistingFace)
	{
		fieldSkipExistingFaces = inUpdateExistingFace;
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
	
	public Map<String,Collection<MultiValued>> getExistingFacesByAssetId()
	{
		return fieldExistingFacesByAssetId;
	}
	public void setExistingFacesByAssetId(Map<String,Collection<MultiValued>> inAssetIds)
	{
		fieldExistingFacesByAssetId = inAssetIds;
	}
	protected Map<String,Collection<MultiValued>> fieldExistingFacesByAssetId;
	
	
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
