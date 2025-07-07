package org.entermediadb.asset.facedetect;

public class FaceScanInstructions
{
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
