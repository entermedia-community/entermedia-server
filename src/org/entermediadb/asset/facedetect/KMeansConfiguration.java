package org.entermediadb.asset.facedetect;

public class KMeansConfiguration
{
	protected int kcount;
	protected int totalrecords;
	protected double maxdistancetomatch = .52;
	protected double maxdistancetocentroid = .865;
	protected double maxdistancetocentroid_one = .89;
	protected int maxresultspersearch;
	//protected int maxpagestocheck = 40;
	protected  double init_loop_start_distance = 1.0; //Start at 1. Its rare and nice starting point
	protected  double init_loop_lower_limit = .88;

}
