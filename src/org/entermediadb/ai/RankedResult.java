package org.entermediadb.ai;

import org.openedit.MultiValued;

public class RankedResult implements  Comparable<RankedResult>
{
	protected MultiValued fieldEmbedding;
	
	public MultiValued getEmbedding()
	{
		return fieldEmbedding;
	}
	public void setEmbedding(MultiValued inEmbedding)
	{
		fieldEmbedding = inEmbedding;
	}
	
	public String getModuleId()
	{
		return getEmbedding().get("moduleid");
	}
	public String getEntityId()
	{
		return getEmbedding().get("dataid");
	}
	
	protected MultiValued fieldEntity;
	public MultiValued getEntity()
	{
		return fieldEntity;
	}
	public void setEntity(MultiValued inEntity)
	{
		fieldEntity = inEntity;
	}
	public double getDistance()
	{
		return fieldDistance;
	}
	public void setDistance(double inDistance)
	{
		fieldDistance = inDistance;
	}
	protected double fieldDistance;

	@Override
	public int compareTo(RankedResult inO)
	{
		int i = Double.compare(getDistance(), inO.getDistance());
		return i;
	}
}
