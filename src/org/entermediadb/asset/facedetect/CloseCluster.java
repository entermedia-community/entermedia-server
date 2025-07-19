package org.entermediadb.asset.facedetect;

import org.openedit.MultiValued;

public class CloseCluster implements Comparable<CloseCluster>
{
	public CloseCluster(MultiValued inCluster, double inDistance)
	{
		distance = inDistance;
		centroid = inCluster;
	}
	double distance;
	MultiValued centroid;
	
	@Override
	public int compareTo(CloseCluster inO)
	{
		int i = Double.compare(distance, inO.distance);
		return i;
	}
}
