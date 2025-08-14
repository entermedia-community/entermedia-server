package org.entermediadb.ai;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.CatalogEnabled;
import org.openedit.ModuleManager;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.hittracker.HitTracker;
import org.openedit.locks.Lock;
import org.openedit.util.MathUtils;

public class KMeansIndexer implements CatalogEnabled {

	private static final Log log = LogFactory.getLog(KMeansIndexer.class);

	public KMeansIndexer() 
	{
	
	}

	protected String fieldType = "semantic";
	protected String fieldSearchType = "semanticembedding";
	protected String fieldRandomSortBy = null;
	protected String fieldFieldSaveVector = "vectorarray";//vectorarray facedatadoubles

	protected Map<String,String> fieldCustomSettings = null;
	
	public Map<String,String> getCustomSettings()
	{
		if (fieldCustomSettings == null)
		{
			fieldCustomSettings = new HashMap();
		}

		return fieldCustomSettings;
	}


	public String getFieldSaveVector()
	{
		return fieldFieldSaveVector;
	}


	public void setFieldSaveVector(String inFieldSaveVector)
	{
		fieldFieldSaveVector = inFieldSaveVector;
	}


	public void setClusters(Collection<MultiValued> inClusters)
	{
		fieldClusters = inClusters;
	}


	public String getRandomSortBy()
	{
		return fieldRandomSortBy;
	}


	public void setRandomSortBy(String inRandomSortBy)
	{
		fieldRandomSortBy = inRandomSortBy;
	}


	public String getSearchType()
	{
		return fieldSearchType;
	}


	public void setSearchType(String inSearchType)
	{
		fieldSearchType = inSearchType;
	}


	public String getType()
	{
		return fieldType;
	}


	public void setType(String inType)
	{
		fieldType = inType;
	}

	protected MediaArchive fieldMediaArchive;
	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}


	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}


	protected ModuleManager fieldModuleManager;
	protected String fieldCatalogId;
	
	
	public String getCatalogId()
	{
		return fieldCatalogId;
	}


	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}


	public MediaArchive getMediaArchive()
	{
		if (fieldMediaArchive == null)
		{
			fieldMediaArchive = (MediaArchive)getModuleManager().getBean(getCatalogId(),"mediaArchive");
		}
		return fieldMediaArchive;
	}


	public void setMediaArchive(MediaArchive inMediaArchive)
	{
		fieldMediaArchive = inMediaArchive;
	}


	public void reinitClusters(ScriptLogger inLog) 
	{
		//Reload from db
		fieldClusters = null;

		if( getClusters().size() < getSettings().kcount )
		{

			double loop_lower_percentage = 0.97;
			double min_distance = getSettings().init_loop_start_distance;
			
			int toadd = getSettings().kcount - getClusters().size();
			Collection<MultiValued> existingCentroids = new ArrayList(getClusters());
			inLog.info("Adding "  + toadd + " random cluster nodes to " + existingCentroids.size() + " with min_distance of " + min_distance );
			

			while(toadd > 0)
			{
				HitTracker tracker = getMediaArchive().query(getSearchType()).exact("iscentroid",false).sort(getRandomSortBy()).search(); //random enough?
				tracker.enableBulkOperations();
				int maxpagestocheck = tracker.getTotalPages(); //Up to 15 pages * 1000
				if( tracker.isEmpty() )
				{
					throw new OpenEditException("Do a deep reindex on " + getSearchType());
				}
				Collection addedlist = createCentroids(inLog, tracker, min_distance, toadd, existingCentroids);
				toadd = toadd - addedlist.size();
				if( toadd > 0 )
				{
					if( tracker.getPage() > maxpagestocheck)
					{
						inLog.info("Start from page 1 / " + maxpagestocheck);
						tracker.setPage(1); //Start over
					}
					min_distance = min_distance * loop_lower_percentage; //Drop by 3% each time If we add too too close then each node has tons of clusters
					toadd = getSettings().kcount - existingCentroids.size();
					if(min_distance < getSettings().init_loop_lower_limit) //If this gets too low we will have a ton of clusters on the same face
					{
						inLog.info("Distance Got too low! " + min_distance + " Clusters: " + existingCentroids.size() );
						break;
					}
				}
			}
			
			fieldClusters = null; //reload
			if( getClusters().size() < getSettings().kcount )
			{
				inLog.info("Problem creating centroids, Data not random enough. Found: " + getClusters().size() + " centoids but wanted: " + getSettings().kcount + " with a min distance of " + min_distance);
			}

		}
		
		Collection tosave = new ArrayList();
		HitTracker tracker = getMediaArchive().query(getSearchType()).missing("nearbycentroidids").hitsPerPage(500).search();
		tracker.enableBulkOperations();
		//Search for all notes not connected
		
		int totalsaved = 0;
		inLog.info("Start Reindexing " +  tracker.size() + " " + getSearchType() + " into " + getClusters().size() + " clusters");
		long start = System.currentTimeMillis();
		for (Iterator iterator = tracker.iterator(); iterator.hasNext();)
		{
			MultiValued hit = (MultiValued) iterator.next();
			
			List<Double> vectorA = (List<Double>)hit.getValue(getFieldSaveVector());
			if( vectorA == null)
			{
				//User added node
				continue;
			}
			try
			{
//				long end = System.currentTimeMillis();
				setCentroids(hit); //Set em <-----
//				start = System.currentTimeMillis();
//				log.info( "Took " + (start-end) );
			} catch( IllegalArgumentException ex)
			{
				//Bad vectors
				log.error("Could not save vectors " + ex);
				continue;
			}
			
			tosave.add(hit);
			
			if( tosave.size() == 500)
			{
				totalsaved = totalsaved + tosave.size();
				getMediaArchive().saveData(getSearchType(),tosave);
				long end = System.currentTimeMillis();
				double diff = (end - start)/1000D;
				diff = MathUtils.roundDouble(diff, 2);
				inLog.info("Added "  + tosave.size() + " assigned nodes in " + diff + " seconds " + totalsaved + " of " + tracker.size() + " into " + getClusters().size() + " clusters");
				start = System.currentTimeMillis();
				tosave.clear();
			}
		}
		getMediaArchive().saveData(getSearchType(),tosave);
		totalsaved = totalsaved + tosave.size();
		long end = System.currentTimeMillis();
		double diff = (end - start)/1000D;
		diff = MathUtils.roundDouble(diff, 2);
		inLog.info("Complete: "  + totalsaved + " assigned to " + getClusters().size() + " clusters");
	}

	protected Collection<MultiValued> createCentroids(ScriptLogger inLog, HitTracker tracker, double mindistance, int toadd, Collection<MultiValued> existingCentroids)
	{
		int maxpagestocheck = tracker.getTotalPages(); //Up to 5 pages * 1000

		inLog.info("Finding " + toadd  + " centroids. currently have " + existingCentroids.size() + " checking within " + mindistance + " starting in page: " + tracker.getPage() );

		Collection tosave = new ArrayList();
		
		//Make sure none are close to one another. And not the same face at all
		int currentpage = tracker.getPage();
		
		while( maxpagestocheck > 0)
		{
			tracker.setPage(currentpage);
			log.info("Set page " + currentpage + " for distance " + mindistance);
			maxpagestocheck--;
			currentpage++;
			for (Iterator iterator = tracker.getPageOfHits().iterator(); iterator.hasNext();)
			{
				MultiValued hit = (MultiValued)iterator.next();
				if( existingCentroids.contains(hit) || hit.getBoolean("iscentroid") )
				{
					continue;
				}
				List<Double> vectorA = (List<Double>)hit.getValue(getFieldSaveVector());
				if( vectorA == null)
				{
					continue;
				}
				
				double founddistance  = -1;
				if( existingCentroids.isEmpty() )
				{
					founddistance = Double.MAX_VALUE; //Always take the first one
				}
				else
				{
					founddistance = checkDistances(hit,mindistance, existingCentroids);
				}
				if( founddistance != -1 && founddistance > mindistance )
				{
					hit.setValue("iscentroid",true);
					Collection<String> single = new java.util.ArrayList(1);
					single.add(hit.getId());
					hit.setValue("nearbycentroidids",single);
					inLog.info("Init " + existingCentroids.size() + " Centroids within distance, bigger is better " + founddistance );
					tosave.add(hit);
					existingCentroids.add(hit);
				}
				if( toadd == tosave.size() )
				{
					maxpagestocheck = 0; //stop
					break;
				}
			}
		}
		inLog.info("Added " + tosave.size() + " Centroid within min distance:" + mindistance);
		getMediaArchive().saveData(getSearchType(),tosave);

		return tosave;
	}

	/**
	 * This is called only when initiaslizing. To make sure no two clusters are close to one another. Ideally 3x  1.5
	 */
	protected double checkDistances(MultiValued master, double mindistance, Collection<MultiValued> existingCentroids)
	{
		double worstscore = -1;
		for (Iterator iterator2 = existingCentroids.iterator(); iterator2.hasNext();)
		{
			MultiValued other = (MultiValued) iterator2.next();
			if( other.getId().equals( master.getId() ) )
			{
				continue; 
			}
			try
			{
				double distance = findCosineDistance(master, other);
				if (distance <= mindistance)  //To close together. All images are shared on that side
				{
					//Was too close to someone
					return distance;
				}
				if(worstscore == -1 || distance  < worstscore)
				{
					worstscore = distance;
				}
			}
			catch(IllegalArgumentException ex)
			{
				log.error("Cant compare vectors " + ex);
				continue;
			}
		}
		return worstscore;
	
	}


	public  void setCentroids(final MultiValued inRecord) 
	{
		// This method is intended to find the nearest cluster for a given item.
		// Implementation would typically involve calculating distances or similarities
		// between the item and each cluster centroid.
		//remove extras

		if( getClusters().isEmpty())
		{
			//Chicken and egg
			log.info("run initialize");
			return;
		}
		
		List<KMeansCloseCluster> closestclusters = (List<KMeansCloseCluster>)new ArrayList();
	
		for (MultiValued cluster : getClusters())
		{
			double distance = findCosineDistance(inRecord, cluster);
			KMeansCloseCluster close = new KMeansCloseCluster(cluster,distance);
			
			closestclusters.add(close);
		}
		Collections.sort(closestclusters);		

		Collection<String> centroids = new ArrayList();

		KMeansConfiguration settings = getSettings();
		
		int max = Math.min(settings.maxnumberofcentroids,closestclusters.size());
		
		for (int i = 0; i < max; i++) //Starts at 2 but only up to 5 clusters needed
		{
			KMeansCloseCluster cluster = (KMeansCloseCluster) closestclusters.get(i);
			
			if( cluster.distance <= settings.maxdistancetocentroid ) //The More centroid the more hits
			{
				centroids.add( cluster.centroid.getId() ); //must be within within .75
			}
			else if( i == 0 || i == 1 )
			{
				if( cluster.distance <=  settings.maxdistancetocentroid_one) //The More centroid the more hits
				{
					centroids.add( cluster.centroid.getId() ); //must be within within .90
					log.info("Picked one centroid under " + settings.maxdistancetocentroid_one + " , was " + cluster.distance + " was trying for " + settings.maxdistancetocentroid);
				}
				else
				{
					log.info("Could not set a single centroid under " + settings.maxdistancetocentroid_one + ", was " + cluster.distance);
				}
			}
			else
			{
				break;
			}
		}

		//if I cant find any centroids within .9 then be my own so that we dont break the rule
		if( centroids.isEmpty() )
		{
			getClusters().add(inRecord);
			inRecord.setValue("iscentroid",true);
			
			//Its empty so add myself
			Collection<String> single = new java.util.ArrayList(1);
			single.add(inRecord.getId());
			inRecord.setValue("nearbycentroidids",single); //Hes still normal for now. When theysearchthey might divide
			
			//closestclusters.iterator().next();
			
			log.info("Bad: No centroids within " + settings.maxdistancetocentroid + " across " +  getClusters().size() + " centroids");
			getMediaArchive().saveData(getSearchType(),inRecord);
		}
		else
		{
			inRecord.setValue("nearbycentroidids",centroids);
			if( centroids.size() > 15)
			{
				//With large number of records and centroids we Might want to decrease the size since we have so many
				log.info("Too many per face centroids: " + centroids.size() + "/" + getClusters().size() + " on " + inRecord); 
			}
		}
	}

	public Collection<RankedResult> searchNearestItems(List<Double> searchVector)
	{
		List<KMeansCloseCluster> closestclusters = (List<KMeansCloseCluster>)new ArrayList();
		
		for (MultiValued cluster : getClusters())
		{
			List<Double> centroidVector = (List<Double>)cluster.getValue(getFieldSaveVector());

			double distance = findCosineDistance(searchVector, centroidVector);
			KMeansCloseCluster close = new KMeansCloseCluster(cluster,distance);
			
			closestclusters.add(close);
		}
		Collections.sort(closestclusters);	
		KMeansConfiguration settings = getSettings();
		
		Collection goodcentroids = new ArrayList();
		int max = Math.min(settings.maxnumberofcentroids,closestclusters.size());
		
		for (int i = 0; i < max; i++) //Starts at 2 but only up to 5 clusters needed
		{
			KMeansCloseCluster cluster = (KMeansCloseCluster) closestclusters.get(i);
			
			if( cluster.distance <= settings.maxdistancetocentroid ) //The More centroid the more hits
			{
				goodcentroids.add( cluster.centroid.getId() ); //must be within within .75
			}
			else if( i == 0 || i == 1 )
			{
				if( cluster.distance <=  settings.maxdistancetocentroid_one) //The More centroid the more hits
				{
					goodcentroids.add( cluster.centroid.getId() ); //must be within within .90
					log.info("Picked one centroid under " + settings.maxdistancetocentroid_one + " , was " + cluster.distance + " was trying for " + settings.maxdistancetocentroid);
				}
				else
				{
					log.info("Could not set a single centroid under " + settings.maxdistancetocentroid_one + ", was " + cluster.distance);
				}
			}
			else
			{
				break;
			}
		}
		
		HitTracker hits = getMediaArchive().query(getSearchType()).orgroup("nearbycentroidids",goodcentroids).search();
		//Double check these match and also load up Organized modules?
		List<RankedResult> finalmatches = new ArrayList();
		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
		{
			MultiValued embedding = (MultiValued) iterator.next();
			//double check
			List<Double> centroidVector = (List<Double>)embedding.getValue(getFieldSaveVector());

			double distance = findCosineDistance(searchVector, centroidVector);
			if( distance < settings.maxdistancetomatch )  //.6 is good
			{
				RankedResult rank = new RankedResult();
				rank.setDistance(distance);
				rank.setEmbedding(embedding);
				finalmatches.add(rank);  //Add a ranking
			}
		}

		Collections.sort(finalmatches);
		
		return finalmatches;

	}
	public Collection<MultiValued> searchNearestItems(MultiValued inSearch)  //Like face
	{
		// This method is intended to search for the nearest items to a given item.
		// Implementation would typically involve calculating distances or similarities
		// between the search item and each item in the collection.
		
		if( getClusters().isEmpty())
		{
			throw new OpenEditException("Not enought clusters. Run reindexfaces event");
		}
		//inSearch.setValue("iscentroid",false);
		Collection<MultiValued> matches = null;
		
		Lock lock = getMediaArchive().lock(inSearch.getId(), "KMeansSearch");
		long start = System.currentTimeMillis();
		try
		{
			Collection nearbycentroidids = inSearch.getValues("nearbycentroidids");
			if( nearbycentroidids == null || nearbycentroidids.isEmpty() )
			{
				throw new OpenEditException(inSearch + " Has no centroids. reindexfaces");
			}
			HitTracker tracker = getMediaArchive().query(getSearchType()).
					orgroup("nearbycentroidids",nearbycentroidids).
					exact("isremoved",false).hitsPerPage(1000).search();
					
			//if we have too many lets make a new k
			if( tracker.size() > getSettings().maxresultspersearch )
			{
				// Add the new cluster to the list
				//Rebalance centroids
				boolean alreadydivided = false;
				if(inSearch.getBoolean("iscentroid") || (tracker.size() < 2000 &&  nearbycentroidids.size() < 3 ) )
				{	
					alreadydivided = true; //Probably
				}
				if( !alreadydivided)
				{
					matches = divideCluster(inSearch, tracker);
					return matches;
				}
			}		
			Collection<MultiValued> allsimilarroots =	findAllSimilarRoots(inSearch);
			matches = findResultsWithinCentroids(inSearch, allsimilarroots, tracker);
			
			long end = System.currentTimeMillis();
			double seconds = (end-start)/1000d;
			log.info("Search found  " +  matches.size()  + " from " +  tracker  + "  in " + seconds + " seconds");
		}
		finally
		{
			getMediaArchive().releaseLock(lock);
		}
		

		//log.info("Did not divide, found: " + matches.size() + " Missed " + misses + " for " + inSearch.getId() + " in " + seconds + " seconds");
		return matches;
	}
	

	// This method is intended to rebalance centroids in a KMeans clustering algorithm.
	public Collection<MultiValued> divideCluster(MultiValued newcentroidItem, HitTracker allnearestItems)
	{
		Collection<MultiValued> results = new ArrayList(allnearestItems);
		
		log.info("Dividing the cluster from large resultset: " + results.size() + " results for id: " + newcentroidItem.getId() + " all centroids:" +  newcentroidItem.getValues("nearbycentroidids") );
		if( newcentroidItem.getId() == null)
		{
			getMediaArchive().saveData(getSearchType(),newcentroidItem);
		}
		
		//I got too many hits then add my myself as a centroid
		newcentroidItem.setValue("iscentroid",true);

		Collection<MultiValued> allsimilarroots =	findAllSimilarRoots(newcentroidItem);
		
		List<MultiValued> tomove = findResultsWithinCentroids(newcentroidItem, allsimilarroots, results);

		//Save all to be the same click. 
		Collection<String> savecentroids = new HashSet();
		for (Iterator iterator2 = allsimilarroots.iterator(); iterator2.hasNext();)
		{
			MultiValued centroid = (MultiValued) iterator2.next();
			savecentroids.add(centroid.getId());
		}
		
		//This includes everyone who matched important centoids
		for (Iterator iterator = tomove.iterator(); iterator.hasNext();)
		{
			MultiValued moving = (MultiValued) iterator.next();
			if( moving.getId().equals(newcentroidItem.getId() ) )
			{
				continue; //Skip myself
			}
			moving.setValue("nearbycentroidids", savecentroids); //Takeover
		}

		newcentroidItem.setValue("iscentroid",true);
		newcentroidItem.setValue("nearbycentroidids", savecentroids); //Add us at the end
		tomove.add(newcentroidItem);
		
		getMediaArchive().saveData(getSearchType(),tomove);
		
		
		
		log.info("Made a new node with only exact nodes in it: " + results.size() + "->" + tomove.size() + " with new centroid id: " + newcentroidItem.getId() + " saved centroids " + savecentroids);
		
		return tomove;
	}


	protected List<MultiValued> findResultsWithinCentroids(MultiValued inToSearch, Collection<MultiValued> allsimilarroots, Collection<MultiValued> results)
	{
		List<MultiValued> tomove = new ArrayList<MultiValued>(); //I am in here as well
		tomove.addAll(allsimilarroots);

		Set toskip = new HashSet();
		for (Iterator iterator2 = allsimilarroots.iterator(); iterator2.hasNext();)
		{
			MultiValued centroid = (MultiValued) iterator2.next();
			toskip.add(centroid.getId());
		}
		double cutoffdistance = getSettings().maxdistancetomatch;

		for (Iterator iterator = results.iterator(); iterator.hasNext();)
		{
			MultiValued test = (MultiValued) iterator.next();
			if( toskip.contains(test.getId()) )
			{
				continue; //Skip myself
			}
			
			//Loop over any important centroids. Then reset the parents
			for (Iterator iterator2 = allsimilarroots.iterator(); iterator2.hasNext();)
			{
				MultiValued centroid = (MultiValued) iterator2.next();
				double distance = findCosineDistance(centroid, test);
				if (distance <= cutoffdistance) 
				{
					tomove.add(test); //These are as good as connecting to myself. Brad pit as a kid
					break; //Just find one and then we will be resetting EVERYONE in this group to be only the new one
				}
			}
		}
		log.info("found: " + tomove.size() + ", of: " + results.size() + " from similarroots: " + toskip);
		return tomove;
	}

	protected Collection<MultiValued> findAllSimilarRoots(MultiValued searchby)
	{
		Collection<MultiValued> allsimilarroots = new ArrayList();

		allsimilarroots.add(searchby);
		
		Collection<String> ids = searchby.getValues("nearbycentroidids");
		
		for (Iterator iterator2 = ids.iterator(); iterator2.hasNext();)
		{
			String centroidid = (String) iterator2.next();
			if( centroidid.equals(searchby.getId()) )
			{
				continue; //Dont test myself
			}
			if( centroidid == null)
			{
				throw new OpenEditException("Cant compare null nodes " + ids);
			}
			MultiValued centroid = findCentroid(centroidid);
			if( centroid == null)
			{
				log.error("Centroid missing: " + centroidid);
				continue;
			}
			double distance = findCosineDistance(searchby, centroid);
			if (distance <=  getSettings().maxdistancetomatch) 
			{
				allsimilarroots.add(centroid); //These are as good as connecting to myself. Brad pit as a kid
			}	
		}
		return allsimilarroots;
	}

	public MultiValued findCentroid(String inId)
	{
		for (Iterator iterator = getClusters().iterator(); iterator.hasNext();)
		{
			MultiValued multiValued = (MultiValued) iterator.next();
			if( multiValued.getId().equals(inId))
			{
				return multiValued;
			}
		}
		return null;
	}
		
	protected Collection<MultiValued> fieldClusters;
	
	public Collection<MultiValued> getClusters() 
	{
		// This method is intended to load clusters from a database or other storage.
		// Implementation would typically involve querying the database and populating the clusters list.
		if( fieldClusters == null ) 
		{
			HitTracker tracker = getMediaArchive().query(getSearchType()).exact("iscentroid",true).search();
			fieldClusters = new ArrayList<MultiValued>(tracker);
			//	reinitClusters(null);
		}
		return fieldClusters;	
	}

	public KMeansConfiguration getSettings()
	{
		KMeansConfiguration config = (KMeansConfiguration)getMediaArchive().getCacheManager().get(getType(),"kmeansconfig");
		if( config == null)
		{
			fieldClusters = null; //reload em
			
			config = new KMeansConfiguration();
			getMediaArchive().getCacheManager().put(getType(),"kmeansconfig",config);
			
			String value = loadSettingValue( "maxdistancetomatch");
			if( value != null)
			{
				config.maxdistancetomatch = Double.parseDouble(value);
			}
			/*
			 *  Choosing the Number of Centroids (k)
	k ≈ sqrt(n / 2) is a heuristic (where n = total number of record vectors)

	Examples:

	10,000 faces → ~70–100 centroids

	1,000,000 faces → ~700–1000 centroids
	*/
			int totalrecords = getMediaArchive().query(getSearchType()).all().hitsPerPage(1).search().size(); 
			double k = Math.sqrt( totalrecords / 2d); //Higher slows down indexing, more can be added back later as they click
			int min = (int)Math.round(k * 1.50); //Raise by 50% or will be added on demand and that make it worse
			
			String skcount = loadSettingValue("kcount");
			if( skcount != null)
			{
				min = Integer.parseInt( skcount); 
			}
			
			config.kcount = min;
			config.totalrecords = totalrecords;
			
			//Create new nodes when we get over 300 results or more as more likely to have a ton of faces
			
			min =  Math.max(min,300); 
			String smaxresultspersearch = loadSettingValue( "maxresultspersearch");
			if( smaxresultspersearch != null)
			{
				min = Integer.parseInt( smaxresultspersearch); 
			}
			
			config.maxresultspersearch = min; 
			
			
			//.80-.9 = 20-100k
			//		.9 / (t / 20k) = 
					
//			if( totalfaces > 50000 )
//			{
//				newrange = .8;  			 // (totalfaces / 20000.0)); //.90 worked well for 20k so scale it up or down based on total
//			}
			
			String smaxdistancetocentroid = loadSettingValue( "maxdistancetocentroid");
			if( smaxdistancetocentroid != null)
			{
				config.maxdistancetocentroid = Double.parseDouble(smaxdistancetocentroid);
				log.info("Custom size from db " +  getType()  + "_maxdistancetocentroid=" + config.maxdistancetocentroid );
			}

			
			String init_loop_lower_limit = loadSettingValue( "init_loop_lower_limit");
			if( init_loop_lower_limit != null)
			{
				config.init_loop_lower_limit = Double.parseDouble(init_loop_lower_limit);
				log.info("Custom size from db init_loop_lower_limit=" + init_loop_lower_limit );
			}

			String sinit_loop_start_distance = loadSettingValue( "init_loop_start_distance");
			if( sinit_loop_start_distance != null)
			{
				config.init_loop_start_distance = Double.parseDouble(sinit_loop_start_distance);
				log.info("Custom size from db sinit_loop_start_distance=" + sinit_loop_start_distance );
			}

			String smaxdistancetocentroid_one = loadSettingValue( "maxdistancetocentroid_one");
			if( smaxdistancetocentroid_one != null)
			{
				config.maxdistancetocentroid_one = Double.parseDouble(smaxdistancetocentroid_one);
				log.info("Custom size from db maxdistancetocentroid_one=" + config.maxdistancetocentroid_one );
			}
			
			String smaxnumberofcentroids = loadSettingValue( "maxnumberofcentroids ");
			if( smaxnumberofcentroids  != null)
			{
				config.maxnumberofcentroids = Integer.parseInt(smaxnumberofcentroids );
				log.info("Custom size from db maxnumberofcentroids =" + config.maxnumberofcentroids );
			}
			
			log.info("Reloading settings kcount="+ config.kcount  + " maxresultspersearch=" + config.maxresultspersearch + " maxdistancetocentroid=" + config.maxdistancetocentroid );
		}
		return config;
	}


	protected String loadSettingValue(String inName)
	{
		String value = getCustomSettings().get(inName);
		if( value == null)
		{
			value = getMediaArchive().getCatalogSettingValue(getType() + "_" + inName);
		}
		return value;
	}

	
	public boolean compareVectors(List<Double> inputVector, List<Double> inCompareVector, double maxdistance)
	{
		double distance = findCosineDistance(inputVector, inCompareVector);
		if( distance > maxdistance )
		{
			return false;
		}
		return true;
	}

	public double findCosineDistance(List<Double> inputVector, List<Double> compreToV) 
	{
		if (inputVector.size() != compreToV.size()) 
		{
				throw new OpenEditException("Vectors must be the same length.");
		}

		double dotProduct = 0.0;
		double normA = 0.0;
		double normB = 0.0;

		for (int i = 0; i < inputVector.size(); i++) 
		{
			double iv = inputVector.get(i);  //ParserBase line 686 defaults to double for filling a list of values
			double cv = compreToV.get(i);
			
			dotProduct += iv * cv;
			normA += iv * iv;
			normB += cv * cv;
		}
		double diff = (dotProduct / (Math.sqrt(normA) * Math.sqrt(normB)));
		double finalval = 1d - diff;
		return finalval;
	}
	protected double findCosineDistance(MultiValued hita, MultiValued hitb) 
	{
		if(  hita.getId() != null && hita.getId().equals(hitb.getId() ) )
		{
			return 0.0;
		}
		
		List<Double> vectorA = (List<Double>)hita.getValue(getFieldSaveVector());
		List<Double> vectorB = (List<Double>)hitb.getValue(getFieldSaveVector());
		
		if (vectorA == null)
		{
			throw new IllegalArgumentException("Vectors was null "+ hita.getId());
		}
		if (vectorB == null)
		{
			throw new IllegalArgumentException("Vectors was null "+ hitb.getId());
		}
		if (vectorA.size() != vectorB.size())
		{
			throw new IllegalArgumentException("Vectors must be of the same length");
		}

		double dotProduct = 0.0;
		double normA = 0.0;
		double normB = 0.0;

		for (int i = 0; i < vectorA.size(); i++) 
		{
			dotProduct += vectorA.get(i) * vectorB.get(i);
			normA += Math.pow(vectorA.get(i), 2);
			normB += Math.pow(vectorB.get(i), 2);
		}

		if (normA == 0 || normB == 0) {
			return 0.0; // Avoid division by zero
		}

		double similar = dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
		double distance = 1D - similar;
		return Math.abs(distance);
	}

	
	/*
	protected float[] collectFloats(Collection vector) 
	{
		float[] floats = new float[vector.size()];
		int i = 0;
		for (Iterator iterator = vector.iterator(); iterator.hasNext();)
		{
			Object floatobj = iterator.next();
			float f;
			if( floatobj instanceof Float)
			{
				f = (Float)floatobj;
			}
			else
			{
				f = Float.parseFloat(floatobj.toString());
			}
			floats[i++] = f;
		}
		return floats;
	}
	
	 //Used for saving data
	protected static String encodeFloats(float[] vector)
	{
		final int capacity = Float.BYTES * vector.length;
	    final ByteBuffer bb = ByteBuffer.allocate(capacity);
	    for (float v : vector) {
	        bb.putFloat(v);
	    }
	    bb.rewind();
	    final ByteBuffer encodedBB = Base64.getEncoder().encode(bb);

	    return new String(encodedBB.array());
	}
	*/
	public List<Double> collectDoubles(Collection vector) 
	{
		List<Double> floats = new ArrayList(vector.size());
		for (Iterator iterator = vector.iterator(); iterator.hasNext();)
		{
			Object floatobj = iterator.next();
			double f;
			if( floatobj instanceof Double)
			{
				f = (Double)floatobj;
			}
			else if( floatobj instanceof Float)
			{
				f = (Double)floatobj;
			}
			else
			{
				f = Double.parseDouble(floatobj.toString());
			}
			floats.add(f);
		}
		return floats;
	}
	 //Used for saving data
	/*
	protected static String encodeDoubles(double[] vector)
	{
		final int capacity = Double.BYTES * vector.length;
	    final ByteBuffer bb = ByteBuffer.allocate(capacity);
	    for (double v : vector) {
	        bb.putDouble(v);
	    }
	    bb.rewind();
	    final ByteBuffer encodedBB = Base64.getEncoder().encode(bb);

	    return new String(encodedBB.array());
	}
	*/


	public void setCustomSettings(Map<String, String> inCustomsettings)
	{
		fieldCustomSettings = inCustomsettings;
		
	}
}
