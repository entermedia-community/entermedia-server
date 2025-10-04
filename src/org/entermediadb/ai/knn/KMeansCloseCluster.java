package org.entermediadb.ai.knn;

import org.openedit.MultiValued;

public class KMeansCloseCluster implements Comparable<KMeansCloseCluster>
{
	public KMeansCloseCluster(MultiValued inCluster, double inDistance)
	{
		distance = inDistance;
		centroid = inCluster;
	}
	double distance;
	MultiValued centroid;
	
	@Override
	public int compareTo(KMeansCloseCluster inO)
	{
		int i = Double.compare(distance, inO.distance);
		return i;
	}
}
