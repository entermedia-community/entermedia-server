package org.entermediadb.ai.semantics;

import java.util.Collection;
import java.util.Map;

import org.openedit.MultiValued;

public class SemanticInstructions
{
	boolean fieldSkipExistingRecords = true;
	
	public boolean isSkipExistingRecords()
	{
		return fieldSkipExistingRecords;
	}
	public void setSkipExistingRecords(boolean inUpdateExistingFace)
	{
		fieldSkipExistingRecords = inUpdateExistingFace;
	}
	public Map<String,Collection<MultiValued>> getExistingFacesByDataId()
	{
		return fieldExistingFacesByDataId;
	}
	public void setExistingFacesByDataId(Map<String,Collection<MultiValued>> inDataIds)
	{
		fieldExistingFacesByDataId = inDataIds;
	}
	protected Map<String,Collection<MultiValued>> fieldExistingFacesByDataId;
	
	double fieldConfidenceLimit;
	public double getConfidenceLimit()
	{
		return fieldConfidenceLimit;
	}
	public void setConfidenceLimit(double inConfidenceLimit)
	{
		fieldConfidenceLimit = inConfidenceLimit;
	}
}
