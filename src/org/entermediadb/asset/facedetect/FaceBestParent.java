package org.entermediadb.asset.facedetect;

import org.openedit.MultiValued;

public class FaceBestParent
{
	protected MultiValued fieldFace;
	public MultiValued getFace()
	{
		return fieldFace;
	}
	public void setFace(MultiValued inFace)
	{
		fieldFace = inFace;
	}
	public double getBestParentScore()
	{
		return fieldBestParentScore;
	}
	public void setBestParentScore(double inBestParentScore)
	{
		fieldBestParentScore = inBestParentScore;
	}
	public MultiValued getBestParentFace()
	{
		return fieldBestParentFace;
	}
	public void setBestParentFace(MultiValued inBestParentFace)
	{
		fieldBestParentFace = inBestParentFace;
	}
	protected double fieldBestParentScore = 100;
	protected MultiValued fieldBestParentFace;
}
