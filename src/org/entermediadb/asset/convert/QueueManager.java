package org.entermediadb.asset.convert;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.locks.Lock;
import org.openedit.util.ExecutorManager;

public class QueueManager implements ConversionEventListener
{
	private static final Log log = LogFactory.getLog(QueueManager.class);
	//Runs every 5 minutes or when new uploads come in or when an asset finishes and we need the next one
	//Synchnize the checking
	//Search for all tasks that arfe not ones I am already working on
	//Looks to see if there are any available runners, if so
	//then Does a search on the queue sorted by assetid
	//Locks the asset, Bundles an asset into a runnable and hands it off to a runner
	//Each thread finishes, releases the lock and updates the tasks then fires shared event to run more conversions
	protected AssetConversions ISLOCKED = new AssetConversions();
	protected MediaArchive fieldMediaArchive;
	protected ModuleManager fieldModuleManager;
	protected Map fieldRunningAssetConversions = new ConcurrentHashMap();
	protected String fieldCatalogId;
	protected int fieldTotalPending;
	
	public int getTotalPending()
	{
		return fieldTotalPending;
	}

	public void setTotalPending(int inTotalPending)
	{
		fieldTotalPending = inTotalPending;
	}

	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
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

	public synchronized void checkQueue()
	{
		if (!hasAvailableProcessor())
		{
			log.info("No available processors");
			return;
		}

		//Lock searching for tasks
		try
		{
			Searcher tasksearcher = getMediaArchive().getSearcher("conversiontask");
			Searcher itemsearcher = getMediaArchive().getSearcher("orderitem");
			Searcher presetsearcher = getMediaArchive().getSearcher("convertpreset");
	
			SearchQuery query = tasksearcher.createSearchQuery();
			query.addOrsGroup("status", "new submitted retry missinginput");
			query.addSortBy("assetidDown");
			query.addSortBy("ordering");
			//TODO: Exclude any existing asseids we are already processing
			if (hasRunningConversions())
			{
				query.addNots("assetid", getRunningAssetIds());
			}
	
			HitTracker newtasks = tasksearcher.search(query);
			newtasks.enableBulkOperations();
			newtasks.setHitsPerPage(500);  //Just enought to fill up the queue
			//newtasks.enableBulkOperations();
			//newtasks.setHitsPerPage(25); //We want to make sure scroll does not expire 
			//newtasks.setHitsPerPage(20000);  //This is a problem. Since the data is being edited while we change pages we skip every other page. Only do one page at a time
			setTotalPending(newtasks.size());
			if (newtasks.size() > 0)
			{
				log.info("processing " + newtasks.size() + " new submitted retry missinginput conversions");
			}
			else
			{
				return;
			}
			//log.info("Thread checking: " + Thread.currentThread().getName() + " class:" + hashCode() );
			long count = 0;
			Map assetstoprocess = new HashMap();
			for (Iterator iterator = newtasks.iterator(); iterator.hasNext();)
			{
				Data hit = (Data) iterator.next();
				
				//If locked skip it, someone is already processing it
				String assetid = hit.get("assetid"); //Since each converter locks the asset we want to group these into one sublist
	
				if (assetid == null)
				{
					log.info("No assetid set");
					Data missingdata = tasksearcher.loadData(hit);
					missingdata.setProperty("status", "error");
					missingdata.setProperty("errordetails", "asset id is null");
					tasksearcher.saveData(missingdata, null);
					continue;
				}
	
				AssetConversions existing = (AssetConversions) assetstoprocess.get(assetid);
				if (existing == null)
				{
					//lock and create
					if (count >= availableProcessors())
					{
						break;
					}
					Asset asset = getMediaArchive().getAsset( assetid );
					if( asset == null )
					{
						Data missingdata = tasksearcher.loadData(hit);
						missingdata.setProperty("status", "error");
						missingdata.setProperty("errordetails", "asset not found " + assetid);
						tasksearcher.saveData(missingdata, null);
						assetstoprocess.put(assetid, ISLOCKED);
						continue;
					}
					Lock lock = fieldMediaArchive.getLockManager().lockIfPossible("assetconversions/" + assetid, "CompositeConvertRunner.run");
					if (lock == null)
					{
						assetstoprocess.put(assetid, ISLOCKED);
						log.info("Asset is already being processed " + assetid + " in catalog " + getMediaArchive().getCatalogId());
						continue;
					}
					count++;
					existing = new AssetConversions(getMediaArchive(), lock);
					existing.setAsset(asset);
					existing.setEventListener(this);
					assetstoprocess.put(assetid, existing);
				}
				if (existing == ISLOCKED)
				{
					continue;
				}
				ConversionTask task = createRunnable(tasksearcher, presetsearcher, itemsearcher, hit);
				existing.addTask(task);
	
			}
			for (Iterator iterator = assetstoprocess.keySet().iterator(); iterator.hasNext();)
			{
				String assetid = (String) iterator.next();
				AssetConversions tasks = (AssetConversions) assetstoprocess.get(assetid);
				if (tasks != ISLOCKED)
				{
					queueConversion(tasks);
				}
			}
			//log.info("Thread finished: " + Thread.currentThread().getName() );
		}
		catch ( Throwable ex)
		{
			log.error("Could not process queue ", ex);
		}
	}

	private boolean hasRunningConversions()
	{
		return fieldRunningAssetConversions.isEmpty() == false;
	}

	private Set getRunningAssetIds()
	{
		return fieldRunningAssetConversions.keySet();
	}

	private boolean hasAvailableProcessor()
	{
		return availableProcessors() > 0;
	}

	private int availableProcessors()
	{
		int total = getThreads().getAvailableProcessors();
		total = total - fieldRunningAssetConversions.size();
		return total;
	}
	public int runningProcesses()
	{
		return fieldRunningAssetConversions.size();
	}

	protected ConversionTask createRunnable(Searcher tasksearcher, Searcher presetsearcher, Searcher itemsearcher, Data hit)
	{
		ConversionTask runner = (ConversionTask)getModuleManager().getBean(getCatalogId(),"conversionTask",false);
		runner.mediaarchive = getMediaArchive();
		runner.tasksearcher = tasksearcher;
		runner.presetsearcher = presetsearcher;
		runner.itemsearcher = itemsearcher;
		runner.hit = hit;
		return runner;
	}

	private void queueConversion(AssetConversions inAssetconversions)
	{
		//log.info("ADDING" + inAssetconversions.getAssetId());
		fieldRunningAssetConversions.put(inAssetconversions.getAssetId(), inAssetconversions);
		getThreads().execute("conversions", inAssetconversions);
	}

	public void finishedConversions(AssetConversions inAssetconversions)
	{
		try
		{
			fieldRunningAssetConversions.remove(inAssetconversions.getAssetId());
			getMediaArchive().releaseLock(inAssetconversions.getLock());
			//log.info("RELEASED" + inAssetconversions.getAssetId());
			getMediaArchive().conversionCompleted(inAssetconversions.getAsset());
			//getMediaArchive().fireMediaEvent("conversions/conversioncomplete",null,inAssetconversions.getAsset());
			//log.info("Thread complete: " + Thread.currentThread().getName() );
			checkQueue();
		}
		catch ( Exception ex)
		{
			log.error("Problem finishing conversions ",ex);
		}
	}
	public void ranConversions(AssetConversions inAssetConversions)
	{
		fieldRunningAssetConversions.remove(inAssetConversions.getAssetId());
		getMediaArchive().releaseLock(inAssetConversions.getLock());
		//log.info("RELEASED after run" + inAssetConversions.getAssetId());

	}
	public ExecutorManager getThreads()
	{
		ExecutorManager queue = (ExecutorManager) getModuleManager().getBean(getMediaArchive().getCatalogId(), "executorManager");
		return queue;
	}



}
