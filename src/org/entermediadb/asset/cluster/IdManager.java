package org.entermediadb.asset.cluster;

import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.locks.Lock;
import org.openedit.node.NodeManager;

public class IdManager 
{
	private static final Log log = LogFactory.getLog(IdManager.class);

	protected SearcherManager fieldSearcherManager;
	protected NodeManager fieldNodeManager;
	protected String fieldCatalogId;

	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.entermediadb.locks.LockManagerI#lock(java.lang.String,
	 * java.lang.String, java.lang.String)
	 */
	
	public UniqueId lock(String inPath)
	{
		Searcher searcher = getIdSearcher();
		UniqueId lock = loadLock(inPath);

		//See if I already have the lock, because I created it or because I called this twice in a row

		int tries = 0;
		while (true)
		{
			UniqueId found = grabLock(lock,  inPath, searcher);
			if (found != null)
			{
				return found;
			}
			tries++;
			if (tries > 9)
			{
				break;
			}
			try
			{
				Thread.sleep(250);
			}
			catch (Exception ex)
			{
				// does not happen
				log.info(ex);
			}
			log.info("Could not lock " + inPath + " trying again  " + tries);
			lock = loadLock(inPath);
		}
		throw new OpenEditException("Could not lock file " + inPath + " locked by " + lock.getNodeId() + " " + lock.getOwnerId());
	}

	public UniqueId grabLock(UniqueId lock, String inPath)
	{
		return grabLock(lock,  inPath, getIdSearcher());
	}

	public UniqueId grabLock(UniqueId lock,  String inPath, Searcher inSearcher)
	{
		if (lock == null)
		{
			//	log.info("Lock was null, creating a new one owner " + inOwner + " path " + inPath + "Thread: " + Thread.currentThread().getId() + "Lock ID" );
			lock = createLock(inPath, inSearcher);
			lock.setNodeId(getNodeManager().getLocalNodeId());
			lock.setDate(new Date());
			lock.setLocked(false);
			inSearcher.saveData(lock, null);
			//	log.info(lock.getId() +" being saved.  Current version " + lock.get(".version") + "Thread: " + Thread.currentThread().getId() + "Lock ID" + lock.getId());
			//See if anyone else also happen to save a lock and delete the older one

			SearchQuery q = inSearcher.createSearchQuery();
			q.addExact("sourcepath", inPath);
			q.setHitsPerPage(1);
			HitTracker tracker = inSearcher.search(q); //Make sure there was not a thread waiting
			Iterator iter = tracker.iterator();
			if (!iter.hasNext())
			{
				throw new OpenEditException("Searching by sourcepath not working" + inPath);
			}
			Data first = (Data) iter.next();
			if (first.get("version") != lock.getVersion())
			{
				return null;
			}
			if (tracker.size() > 1) //Someone else also locked
			{
				log.info("Deleting lock!  Found a duplicate : version: " + lock.get(".version") + "Thread: " + Thread.currentThread().getId() + "Lock ID" + lock.getId());
				inSearcher.delete(lock, null);
				return null;
			}

		}

		if (lock.isLocked())
		{
			//log.info("Local was alreadY locked - returning null. CatalogID:" + inSearcher.getCatalogId() + " Thread: " + Thread.currentThread().getId() + "Lock ID" + lock.getId() + " Searcher: " + inSearcher.getClass());
			return null;
		}

		try
		{
			lock.setDate(new Date());
			lock.setNodeId(getNodeManager().getLocalNodeId());
			lock.setLocked(true);
			getIdSearcher().saveData(lock, null); //Both threads called this
			//		log.info(lock.getId() +"being saved.  Line 139  Current version " + lock.get(".version") + "Thread: " + Thread.currentThread().getId() + " Lock ID " + lock.getId());

		}
		catch (ConcurrentModificationException ex)
		{
			log.info("Lock was not available " + lock.get(".version") + "Thread: " + Thread.currentThread().getId() + " Lock ID " + lock.getId());

			return null;
		}
		catch (OpenEditException ex)
		{
			log.info("saving lock conflict"); //May not be an issue
			if (ex.getCause() instanceof ConcurrentModificationException)
			{
				
				return null;
			}
			throw ex;
		}
		//	log.info(lock.getId() +"being returned.  Line 154  Current version " + lock.get(".version") + "Thread: " + Thread.currentThread().getId() +  "Lock ID" + lock.getId());

		return lock;

	}

	protected UniqueId createLock(String inPath, Searcher searcher)
	{
		UniqueId lockrequest = (UniqueId) searcher.createNewData();
		lockrequest.setSourcePath(inPath);
		lockrequest.setLocked(false);
		lockrequest.setValue("countvalue", new Long(1L));
		return lockrequest;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.entermediadb.locks.LockManagerI#loadLock(java.lang.String,
	 * java.lang.String)
	 */
	
	public UniqueId loadLock(String inPath)
	{
		//		return loadLock(inPath, false, null);
		//	}
		//	public Lock loadLock(String inPath, boolean lockIt, String inOwner)
		//	{
		Searcher searcher = getIdSearcher();

		SearchQuery q = searcher.createSearchQuery();
		q.addExact("sourcepath", inPath);
		q.setHitsPerPage(1);
		HitTracker tracker = searcher.search(q);
		if (tracker.size() == 0)
		{
			return null;
		}
		Data first = (Data) tracker.first();

		if (first == null)
		{
			return null;
		}
		//first = (Data) searcher.loadData(first); //This should already create a lock
		//kind of createNewData option
		UniqueId lock = new UniqueId();
		lock.setId(first.getId());
		lock.getProperties().putAll(first.getProperties());
		lock.setValue("countvalue", first.getValue("countvalue"));
		return lock;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.entermediadb.locks.LockManagerI#getLocksByDate(java.lang.String,
	 * java.lang.String)
	 */
	public HitTracker getLocksByDate(String inPath)
	{
		Searcher searcher = getIdSearcher();
		SearchQuery q = searcher.createSearchQuery();
		q.addExact("sourcepath", inPath);
		q.addSortBy("date");
		return searcher.search(q);
	}

	public Searcher getIdSearcher()
	{
		Searcher searcher = getSearcherManager().getSearcher(getCatalogId(), "uniqueid");
		return searcher;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.entermediadb.locks.LockManagerI#lockIfPossible(java.lang.String,
	 * java.lang.String, java.lang.String)
	 */
	
	

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.entermediadb.locks.LockManagerI#release(java.lang.String,
	 * org.entermediadb.locks.Lock)
	 */
	public boolean release(UniqueId inLock)
	{
		if (inLock != null)
		{
			Searcher searcher = getIdSearcher();
			inLock.setLocked(false);
			//inLock.setProperty("version", (String) null); //Once this is saved other people can go get it
			//	log.info(inLock.getId() +" being released Current version " + inLock.get(".version") + " Thread: " + Thread.currentThread().getId());
			searcher.saveData(inLock, null);
			//	log.info(inLock.getId() +" being saved on release.  Current version " + inLock.get(".version") + "Thread: " + Thread.currentThread().getId());

			return true;
		}
		return false;
	}

	

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}

	public NodeManager getNodeManager()
	{
		return fieldNodeManager;
	}

	public void setNodeManager(NodeManager inNodeManager)
	{
		fieldNodeManager = inNodeManager;
	}

	
	
	public String nextId(String inType){
		
		UniqueId counter = lock(inType);
		
		Long current = null;
		try
		{
			Object val = counter.getValue("countvalue");
			if(val instanceof Integer){
				current = ((Integer)val).longValue();
			} 
			if(val instanceof Long){
				current = (Long) val;
			}
			if(current == null){
				current = 1L;
			}
			
			current++;
			counter.setValue("countvalue", current);
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}
		finally{
			release(counter);
		}
		return String.valueOf(current);
		
		
	}
	
	public Number nextNumber(String inType){
		
		UniqueId counter = lock(inType);
		
		Long current = null;
		try
		{
			Object val = counter.getValue("countvalue");
			if(val instanceof Integer){
				current = ((Integer)val).longValue();
			} 
			if(val instanceof Long){
				current = (Long) val;
			}
			if(current == null){
				current = 1L;
			}
			
			current++;
			counter.setValue("countvalue", current);
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}
		finally{
			release(counter);
		}
		return current;
		
	}
	
	
	
}
