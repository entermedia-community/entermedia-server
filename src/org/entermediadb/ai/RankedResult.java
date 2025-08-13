package org.entermediadb.ai;

import org.openedit.MultiValued;

public class RankedResult
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
}
