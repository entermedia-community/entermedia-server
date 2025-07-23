package org.entermediadb.asset.facedetect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.CatalogEnabled;
import org.openedit.ModuleManager;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.hittracker.HitTracker;
import org.openedit.util.MathUtils;

public class KMeansManager implements CatalogEnabled {

	private static final Log log = LogFactory.getLog(KMeansManager.class);

	public KMeansManager() 
	{
	
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

	protected KMeansConfiguration getSettings()
	{
		KMeansConfiguration config = (KMeansConfiguration)getMediaArchive().getCacheManager().get("face","kmeansconfig");
		if( config == null)
		{
			fieldClusters = null; //reload em
			
			config = new KMeansConfiguration();
			String value = getMediaArchive().getCatalogSettingValue("facedetect_max_distance");
			if( value != null)
			{
				config.cutoffdistance = Double.parseDouble(value);
			}
			else
			{
				config.cutoffdistance = 0.5;
			}
			/*
			 *  Choosing the Number of Centroids (k)
	k ≈ sqrt(n / 2) is a heuristic (where n = total number of face vectors)

	Examples:

	10,000 faces → ~70–100 centroids

	1,000,000 faces → ~700–1000 centroids
	*/
			int totalfaces = getMediaArchive().query("faceembedding").all().hitsPerPage(1).search().size(); 
			double k = Math.sqrt( totalfaces / 2d);
			int min = (int)Math.round(k);
			config.kcount = min;
			config.totalrecords = totalfaces;
			
			//Create new nodes when we get over 300 results or more as more likely to have a ton of faces
			config.maxresultspersearch = Math.max(min,300); 
			
			getMediaArchive().getCacheManager().put("face","kmeansconfig",config);
		}
		return config;
	}

	public void reinitClusters(ScriptLogger inLog) 
	{
		//Reload from db

		fieldClusters = null;
		
		if( getClusters().size() < getSettings().kcount )
		{
			int toadd = getSettings().kcount - getClusters().size();
			inLog.info("Adding "  + toadd + " random cluster nodes ");
			HitTracker tracker = getMediaArchive().query("faceembedding").exact("iscentroid",false).hitsPerPage(toadd).sort("face_confidenceDown").search(); //random enough?
			tracker.enableBulkOperations();
			if( tracker.isEmpty() )
			{
				throw new OpenEditException("Do a deep reindex on faceembeddings");
			}
			double min_distance = getSettings().cutoffdistance * 2;
			Collection<MultiValued> found = findCentroids(inLog, tracker,min_distance, toadd);
			getMediaArchive().saveData("faceembedding",found);
			fieldClusters = null;
			toadd = getSettings().kcount - getClusters().size();
			if( found.size() < toadd)
			{
				inLog.info(" is not enough data for a good distance. Needed " + toadd + " found " + found.size());
				min_distance = getSettings().cutoffdistance; //Look closer
				found = findCentroids(inLog, tracker, min_distance, toadd);
				getMediaArchive().saveData("faceembedding",found);
			}
			
			fieldClusters = null; //reload
			if( getClusters().size() < getSettings().kcount )
			{
				inLog.info("Problem creating centroids, try clicking on some results?");
			}

		}
		
		Collection tosave = new ArrayList();
		HitTracker tracker = getMediaArchive().query("faceembedding").missing("nearbycentroidids").hitsPerPage(500).search();
		tracker.enableBulkOperations();
		//Search for all notes not connected 
		int totalsaved = 0;
		inLog.info("Start Reindexing " +  tracker.size() + " faces");
		long start = System.currentTimeMillis();
		for (Iterator iterator = tracker.iterator(); iterator.hasNext();)
		{
			MultiValued hit = (MultiValued) iterator.next();
			
			//Error checking
			List<Double> vectorA = (List<Double>)hit.getValue("facedatadoubles");
			if( vectorA == null)
			{
				//AZgZeJD6n_w3KYxNFTlK
				Asset asset = getMediaArchive().getAsset(hit.get("assetid"));
				if( asset != null)
				{
					asset.setValue("facescanerror",true);
					getMediaArchive().saveAsset(asset);
				}
				continue;
			}
			
			setCentroids(hit); //Set em <-----
			
			
			tosave.add(hit);
			if( tosave.size() == 500)
			{
				totalsaved = totalsaved + tosave.size();
				getMediaArchive().saveData("faceembedding",tosave);
				tosave.clear();
				
				long end = System.currentTimeMillis();
				double diff = (end - start)/1000D;
				diff = MathUtils.roundDouble(diff, 2);
				inLog.info("Added "  + totalsaved + " assigned cluster nodes in " + diff + " seconds");
				start = System.currentTimeMillis();
			}
		}
		getMediaArchive().saveData("faceembedding",tosave);
		totalsaved = totalsaved + tosave.size();
		long end = System.currentTimeMillis();
		double diff = (end - start)/1000D;
		diff = MathUtils.roundDouble(diff, 2);
		inLog.info("Complete: "  + totalsaved + " assigned cluster nodes in " + diff + " seconds");
	}


	protected Collection<MultiValued>  findCentroids(ScriptLogger inLog, HitTracker tracker, double mindistance, int toadd)
	{
		Collection tosave = new ArrayList();
		
		//Make sure none are close to one another. And not the same face at all
		
		int maxchecktimes = getSettings().kcount * 100;

		for (Iterator iterator = tracker.iterator(); iterator.hasNext();)
		{
			MultiValued hit = (MultiValued)iterator.next();
			boolean isfar = checkDistance(hit,mindistance);
			if( isfar )
			{
				hit.setValue("iscentroid",true);
				hit.addValue("nearbycentroidids",hit.getId());
				tosave.add(hit);
			}
			if( toadd == tosave.size() )
			{
				break;
			}
			if( maxchecktimes-- == 0)
			{
				//Gave up looking for 
				inLog.info("Gave up looking for far away nodes");
				break;
			}
		}
		return tosave;
	}

	/**
	 * This is called only when initiaslizing. To make sure no two clusters are close to one another. Ideally 3x  1.5
	 */
	protected boolean checkDistance(MultiValued master, double mindistance)
	{
		for (Iterator iterator2 = getClusters().iterator(); iterator2.hasNext();)
		{
			MultiValued other = (MultiValued) iterator2.next();
			if( other.getId().equals( master.getId() ) )
			{
				continue; 
			}
			double distance = cosineDistance(master, other);
			if (distance <= mindistance)  //To close together. All images are shared on that side
			{
				return false;
			}
		}
		return true;
	
	}


	public void setCentroids(final MultiValued inFace) 
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
		
		List<CloseCluster> closestclusters = (List<CloseCluster>)new ArrayList();
	
		for (MultiValued cluster : getClusters())
		{
			double distance = cosineDistance(inFace, cluster);
			CloseCluster close = new CloseCluster(cluster,distance);
			
			closestclusters.add(close);
		}
		Collections.sort(closestclusters);		

		double upto = getSettings().cutoffdistance * 2;
		
		Collection<String> centroids = new ArrayList();
		for (int i = 0; i < closestclusters.size(); i++)
		{
			CloseCluster cluster = (CloseCluster) closestclusters.get(i);
			if( cluster.distance < upto)  //Could be more than one that are close by
			{
				centroids.add( cluster.centroid.getId() );
			}
		}
		if( centroids.isEmpty() ) //We are all alone within a 2x radious
		{
			CloseCluster first = (CloseCluster)closestclusters.iterator().next();
			centroids.add( first.centroid.getId() ); //Randomly set it  to something
		}

		inFace.setValue("nearbycentroidids",centroids);
		
	}


	public Collection<MultiValued> searchNearestItems(MultiValued inSearch)
	{
		// This method is intended to search for the nearest items to a given item.
		// Implementation would typically involve calculating distances or similarities
		// between the search item and each item in the collection.
		
		if( getClusters().isEmpty())
		{
			throw new OpenEditException("Not enought clusters. Run reindexfaces event");
		}
		
		Collection nearbycentroidids = inSearch.getValues("nearbycentroidids");
		if( nearbycentroidids == null || nearbycentroidids.isEmpty() )
		{
			throw new OpenEditException(inSearch + " Has no centroids. reindexfaces");
		}
		
		HitTracker tracker = getMediaArchive().query("faceembedding").
				orgroup("nearbycentroidids",nearbycentroidids).
				exact("isremoved",false).search();
		
		//if we have too many lets make a new k
		if( tracker.size() > getSettings().maxresultspersearch )
		{
			// Add the new cluster to the list
			//Rebalance centroids
			
			if(inSearch.getBoolean("iscentroid"))
			{	//Took this out because should have already been done to start with when we divided
//				//remove non matching centroids from the circle
//				Collection<MultiValued> matches = compressResults(inSearch, tracker); //limit this group to like minded
//				return matches;
			}
			else
			{
				Collection<MultiValued> matches = divideCluster(inSearch, tracker);
				return matches;
			}
		}
		
		//Filter by distance
		Collection<MultiValued> matches = new ArrayList();
		int misses = 0;
		double cutoff = getSettings().cutoffdistance;
		for (Iterator iterator = tracker.iterator(); iterator.hasNext();)
		{
			MultiValued item = (MultiValued) iterator.next();
			double distance = cosineDistance(inSearch, item);
			if (distance <= cutoff) 
			{
				matches.add(item);
			}
			else
			{
				misses++;
			}
		}
		if( misses > 50 )
		{
			log.info("Misses " + misses + " might need to add more centroids to this area " + inSearch.getId());
		}
		return matches;
	}
	
	protected double cosineDistance(MultiValued hita, MultiValued hitb) 
	{
		if(  hita.getId() != null && hita.getId().equals(hitb.getId() ) )
		{
			return 0.0;
		}
		
		List<Double> vectorA = (List<Double>)hita.getValue("facedatadoubles");
		List<Double> vectorB = (List<Double>)hitb.getValue("facedatadoubles");
		
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

	// This method is intended to rebalance centroids in a KMeans clustering algorithm.
	public Collection<MultiValued> divideCluster(MultiValued newcentroidItem, HitTracker allnearestItems)
	{
		Collection<MultiValued> results = new ArrayList(allnearestItems);
		
		log.info("Dividing the cluster from large resultset: " + results.size() + " results for id: " + newcentroidItem.getId() );
		if( newcentroidItem.getId() == null)
		{
			getMediaArchive().saveData("faceembedding",newcentroidItem);
		}
		
		//I got too many hits then add my myself as a centroid
		
		Collection<MultiValued> allimportantcentroids = new HashSet();

		//Add our new centroid
		newcentroidItem.setValue("iscentroid",true);
		allimportantcentroids.add(newcentroidItem);
		
		double cutoffdistance = getSettings().cutoffdistance;
		for (Iterator iterator = results.iterator(); iterator.hasNext();)
		{
			MultiValued test = (MultiValued) iterator.next();
			if( newcentroidItem.getId().equals(test.getId()) )
			{
				continue; //Dont test myself
			}
				
			//Look for centroids
			if( test.getBoolean("iscentroid") ) 
			{
				double distance = cosineDistance(newcentroidItem, test);
				if (distance <= cutoffdistance) 
				{
					allimportantcentroids.add(test); //These are as good as connecting to myself. Brad pit as a kid
					//Then test each remaining nodes to see if they are exact match to any cluster
				}
			}	
		}
		
		List<MultiValued> tomove = new ArrayList<MultiValued>(); //I am in here as well
		tomove.addAll(allimportantcentroids);
		
		for (Iterator iterator = results.iterator(); iterator.hasNext();)
		{
			MultiValued test = (MultiValued) iterator.next();
			if( newcentroidItem.getId().equals(test.getId()) )
			{
				continue; //Dont test myself
			}
			
			if( !test.getBoolean("iscentroid") )  //Those have already been checked
			{
				//Loop over any important centroids. Then reset the parents
				for (Iterator iterator2 = allimportantcentroids.iterator(); iterator2.hasNext();)
				{
					MultiValued centroid = (MultiValued) iterator2.next();
					double distance = cosineDistance(centroid, test);
					if (distance <= cutoffdistance) 
					{
						tomove.add(test); //These are as good as connecting to myself. Brad pit as a kid
						break; //Just find one and then we will be resetting EVERYONE in this group to be only the new one
					}
				}
			}
		}

		//Save all to be the same click. 
		Collection newgroup = new ArrayList();
		newgroup.add(newcentroidItem.getId());
		
		//This includes everyone who matched important centoids
		for (Iterator iterator = tomove.iterator(); iterator.hasNext();)
		{
			MultiValued moving = (MultiValued) iterator.next();
			moving.setValue("nearbycentroidids", newgroup); //Takeover
		}
		getMediaArchive().saveData("faceembedding",tomove);
		
		log.info("Made a new node with only exact faces in it: " + results.size() + "->" + tomove.size() + " with centroid id: " + newcentroidItem.getId() );
		
		return tomove;
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
	
	protected MultiValued findCluster(String inId)
	{
		for (MultiValued cluster : getClusters())
		{
			if( cluster.getId().equals(inId) )
			{
				return cluster;
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
			HitTracker tracker = getMediaArchive().query("faceembedding").exact("iscentroid",true).search();
			fieldClusters = new ArrayList<MultiValued>(tracker);
			//	reinitClusters(null);
		}
		return fieldClusters;	
	}
	
}
