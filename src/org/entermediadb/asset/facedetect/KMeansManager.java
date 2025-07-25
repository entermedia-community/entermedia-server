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


	public void reinitClusters(ScriptLogger inLog) 
	{
		//Reload from db
		fieldClusters = null;
		
		if( getClusters().size() < getSettings().kcount )
		{
			int toadd = getSettings().kcount - getClusters().size();
			inLog.info("Adding "  + toadd + " random cluster nodes ");
			double min_distance = getSettings().maxdistancetomatch * 2;
			Collection<MultiValued> existingCentroids = new ArrayList(getClusters());
			
			while(toadd > 0)
			{
				HitTracker tracker = getMediaArchive().query("faceembedding").exact("iscentroid",false).sort("face_confidence").search(); //random enough?
				tracker.enableBulkOperations();
				if( tracker.isEmpty() )
				{
					throw new OpenEditException("Do a deep reindex on faceembeddings");
				}
				findCentroids(inLog, tracker,min_distance, toadd, existingCentroids);
				min_distance = min_distance * 0.95; //If we add too too close then each node has tons of clusters
				toadd = getSettings().kcount - existingCentroids.size();
				if(min_distance < .70) //If this gets too low we will have a ton of clusters on the same face
				{
					inLog.info("Got too low! " + min_distance);
					break;
				}
			}
			
			fieldClusters = null; //reload
			if( getClusters().size() < getSettings().kcount )
			{
				inLog.info("Problem creating centroids, Data not random enough. Found: " + getClusters().size() + " centoids but wanted: " + getSettings().kcount + " with a min distance of " + min_distance);
			}

		}
		
		Collection tosave = new ArrayList();
		HitTracker tracker = getMediaArchive().query("faceembedding").missing("nearbycentroidids").hitsPerPage(500).search();
		tracker.enableBulkOperations();
		//Search for all notes not connected
		
		int totalsaved = 0;
		inLog.info("Start Reindexing " +  tracker.size() + " faces into " + getClusters().size() + " clusters");
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
				getMediaArchive().saveData("faceembedding",tosave);
				long end = System.currentTimeMillis();
				double diff = (end - start)/1000D;
				diff = MathUtils.roundDouble(diff, 2);
				inLog.info("Added "  + tosave.size() + " assigned cluster nodes in " + diff + " seconds " + totalsaved + " of " + tracker.size() + " into " + getClusters().size() + " clusters");
				start = System.currentTimeMillis();
				tosave.clear();
			}
		}
		getMediaArchive().saveData("faceembedding",tosave);
		totalsaved = totalsaved + tosave.size();
		long end = System.currentTimeMillis();
		double diff = (end - start)/1000D;
		diff = MathUtils.roundDouble(diff, 2);
		inLog.info("Complete: "  + totalsaved + " assigned cluster nodes in " + diff + " seconds into " + getClusters().size() + " clusters");
	}

	protected Collection<MultiValued> findCentroids(ScriptLogger inLog, HitTracker tracker, double mindistance, int toadd, Collection<MultiValued> existingCentroids)
	{
		int maxchecktimes = getSettings().kcount * 200;

		inLog.info("Finding centroids Need: " + toadd + " have " + existingCentroids.size() + " checking within " + mindistance  + " search up to " + maxchecktimes);

		Collection tosave = new ArrayList();
		
		//Make sure none are close to one another. And not the same face at all
		

		for (Iterator iterator = tracker.iterator(); iterator.hasNext();)
		{
			MultiValued hit = (MultiValued)iterator.next();
			if( existingCentroids.contains(hit) || hit.getBoolean("iscentroid") )
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
				inLog.info("Init added Centroid with min distance, bigger is better " + founddistance );
				tosave.add(hit);
				existingCentroids.add(hit);
			}
			if( toadd == tosave.size() )
			{
				break;
			}
			if( maxchecktimes-- == 0)
			{
				//Gave up looking for 
				inLog.info("Gave up looking for far away nodes at " + mindistance);
				break;
			}
		}
		inLog.info("Added " + tosave.size() + " Centroid within min distance:" + mindistance);
		getMediaArchive().saveData("faceembedding",tosave);

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
				double distance = cosineDistance(master, other);
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


	public  void setCentroids(final MultiValued inFace) 
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

		Collection<String> centroids = new ArrayList();

		for (int i = 0; i < closestclusters.size(); i++) //Starts at 2
		{
			CloseCluster cluster = (CloseCluster) closestclusters.get(i);
			
			if( cluster.distance <= getSettings().maxdistancetocentroid ) //The More centroid the more hits
			{
				centroids.add( cluster.centroid.getId() ); //must be within within .75
			}
			else
			{
				break;
			}
		}

		//if I cant find any centroids within .6 then be my own so that we dont break the rule
		if( centroids.isEmpty() )
		{
			getClusters().add(inFace);
			inFace.setValue("iscentroid",true);
			Collection<String> single = new java.util.ArrayList(1);
			single.add(inFace.getId());
			inFace.setValue("nearbycentroidids",single);
			
			//closestclusters.iterator().next();
			
			log.info("Bad: added another Centroid  " +  getClusters().size() );
			getMediaArchive().saveData("faceembedding",inFace);
		}
		else
		{
			inFace.setValue("nearbycentroidids",centroids);
			if( centroids.size() > 15)
			{
				//With large number of records and centroids we Might want to decrease the size since we have so many
				log.info("Failed to limit centroids, decrease maxdistancetocentroid " + centroids.size() + " on " + inFace); 
			}
		}
		
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
		inSearch.setValue("iscentroid",false);
		
		Collection nearbycentroidids = inSearch.getValues("nearbycentroidids");
		if( nearbycentroidids == null || nearbycentroidids.isEmpty() )
		{
			throw new OpenEditException(inSearch + " Has no centroids. reindexfaces");
		}
		long start = System.currentTimeMillis();
		HitTracker tracker = getMediaArchive().query("faceembedding").
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
				Collection<MultiValued> matches = divideCluster(inSearch, tracker);
				return matches;
			}
		}		
		
		Collection<MultiValued> allimportantcentroids =	findImportantCentroids(inSearch);
		Collection<MultiValued> matches = findResultsWithinCentroids(inSearch, allimportantcentroids, tracker);

		long end = System.currentTimeMillis();
		double seconds = (end-start)/1000d;
		log.info("Search found  " + tracker + " -> " + matches.size() + "  in " + seconds + " seconds");

		//log.info("Did not divide, found: " + matches.size() + " Missed " + misses + " for " + inSearch.getId() + " in " + seconds + " seconds");
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

	// This method is intended to rebalance centroids in a KMeans clustering algorithm.
	public Collection<MultiValued> divideCluster(MultiValued newcentroidItem, HitTracker allnearestItems)
	{
		Collection<MultiValued> results = new ArrayList(allnearestItems);
		
		log.info("Dividing the cluster from large resultset: " + results.size() + " results for id: " + newcentroidItem.getId() + " all centroids:" +  newcentroidItem.getValues("nearbycentroidids") );
		if( newcentroidItem.getId() == null)
		{
			getMediaArchive().saveData("faceembedding",newcentroidItem);
		}
		
		//I got too many hits then add my myself as a centroid
		newcentroidItem.setValue("iscentroid",true);

		Collection<MultiValued> allimportantcentroids =	findImportantCentroids(newcentroidItem);
		
		List<MultiValued> tomove = findResultsWithinCentroids(newcentroidItem, allimportantcentroids, results);

		//Save all to be the same click. 
		Collection<String> savecentroids = new HashSet();
		for (Iterator iterator2 = allimportantcentroids.iterator(); iterator2.hasNext();)
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
				newcentroidItem.setValue("iscentroid",true);
			}
			moving.setValue("nearbycentroidids", savecentroids); //Takeover
		}
		getMediaArchive().saveData("faceembedding",tomove);
		
		log.info("Made a new node with only exact faces in it: " + results.size() + "->" + tomove.size() + " with new centroid id: " + newcentroidItem.getId() + " saved centroids " + savecentroids);
		
		return tomove;
	}


	protected List<MultiValued> findResultsWithinCentroids(MultiValued inToSearch, Collection<MultiValued> allimportantcentroids, Collection<MultiValued> results)
	{
		List<MultiValued> tomove = new ArrayList<MultiValued>(); //I am in here as well
		tomove.addAll(allimportantcentroids);
		
		double cutoffdistance = getSettings().maxdistancetomatch;

		int misses = 0;
		for (Iterator iterator = results.iterator(); iterator.hasNext();)
		{
			MultiValued test = (MultiValued) iterator.next();
			if( inToSearch.getId().equals(test.getId()) )
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
					else
					{
						misses++;
					}
				}
			}
		}
		log.info("found: " + results.size() + ", missed: " + misses + " for " + inToSearch.getId());
		return tomove;
	}

	protected Collection<MultiValued> findImportantCentroids(MultiValued searchby)
	{
		Collection<MultiValued> allimportantcentroids = new ArrayList();
		//Add our new centroid
		if( searchby.getBoolean("iscentroid") )
		{
			allimportantcentroids.add(searchby);
		}
		
		Collection<String> ids = searchby.getValues("nearbycentroidids");
		
		for (Iterator iterator2 = ids.iterator(); iterator2.hasNext();)
		{
			String centroidid = (String) iterator2.next();
			if( centroidid.equals(searchby.getId()) )
			{
				continue; //Dont test myself
			}
			MultiValued centroid = findCentroid(centroidid);
			double distance = cosineDistance(searchby, centroid);
			if (distance <=  getSettings().maxdistancetomatch) 
			{
				allimportantcentroids.add(centroid); //These are as good as connecting to myself. Brad pit as a kid
			}	
		}
		return allimportantcentroids;
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
			HitTracker tracker = getMediaArchive().query("faceembedding").exact("iscentroid",true).search();
			fieldClusters = new ArrayList<MultiValued>(tracker);
			//	reinitClusters(null);
		}
		return fieldClusters;	
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
				config.maxdistancetomatch = Double.parseDouble(value);
			}
			else
			{
				config.maxdistancetomatch = 0.5;
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
			int min = (int)Math.round(k*.9); //Lowered by 10% will be added on demand or make it worse
			config.kcount = min;
			config.totalrecords = totalfaces;
			
			//Create new nodes when we get over 300 results or more as more likely to have a ton of faces
			config.maxresultspersearch = Math.max(min,300); 
			
			getMediaArchive().getCacheManager().put("face","kmeansconfig",config);
			
			config.maxdistancetocentroid = .90; //Stay close by 87 added >150 centroids  .875 made 130/152  bigger the number the less are made by individuals
		}
		return config;
	}

}
