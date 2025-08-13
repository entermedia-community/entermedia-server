package org.entermediadb.ai.semantics;

import java.util.Set;

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
	public Set<String> getExistingEntityIds()
	{
		return fieldExistingEntityIds;
	}
	public void setExistingEntityIds(Set<String> inDataIds)
	{
		fieldExistingEntityIds = inDataIds;
	}
	protected Set<String> fieldExistingEntityIds;
	
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
