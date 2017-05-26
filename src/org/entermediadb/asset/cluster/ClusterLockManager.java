package org.entermediadb.asset.cluster;

import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.elasticsearch.searchers.LockSearcher;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.Shutdownable;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.locks.Lock;
import org.openedit.locks.LockManager;
import org.openedit.node.NodeManager;

public class ClusterLockManager implements LockManager, Shutdownable
{
	private static final Log log = LogFactory.getLog(ClusterLockManager.class);

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
	@Override
	public Lock lock(String inPath, String inOwnerId)
	{
		Searcher searcher = getLockSearcher();
		Lock lock = loadLock(inPath);
		
		//See if I already have the lock, because I created it or because I called this twice in a row
		
		int tries = 0;
		while (true)
		{
			Lock found = grabLock(lock,inOwnerId, inPath, searcher);
			if( found != null)
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
	public Lock grabLock(Lock lock, String inOwner, String inPath )
	{
		return grabLock(lock, inOwner, inPath,getLockSearcher());
	}
	public Lock grabLock(Lock lock, String inOwner, String inPath, Searcher inSearcher )
	{
		
		String savedid = null;
		if( lock == null)
		{
		//	log.info("Lock was null, creating a new one owner " + inOwner + " path " + inPath + "Thread: " + Thread.currentThread().getId() + "Lock ID" );
			lock = createLock(inPath, inSearcher);
			lock.setNodeId(getNodeManager().getLocalNodeId());
			lock.setDate(new Date());
			lock.setLocked(false);
			lock.setOwnerId(inOwner);
			inSearcher.saveData(lock, null);   
		//	log.info(lock.getId() +" being saved.  Current version " + lock.get(".version") + "Thread: " + Thread.currentThread().getId() + "Lock ID" + lock.getId());
			 savedid = lock.getId();
			//See if anyone else also happen to save a lock and delete the older one
		
			SearchQuery q = inSearcher.createSearchQuery(); 
			q.addExact("sourcepath", inPath);
			q.setHitsPerPage(1);
			HitTracker tracker = inSearcher.search(q); //Make sure there was not a thread waiting
			Iterator iter = tracker.iterator();
			if( !iter.hasNext() )
			{
				throw new OpenEditException("Searching by sourcepath not working" + inPath);
			}
			Data first = (Data)iter.next();
			if(first.get("version") != lock.getVersion() ){
				return null;
			}
			if (tracker.size() > 1) //Someone else also locked
			{
		//		log.info("Deleting lock!  Found a duplicate : version: " +  lock.get(".version") + "Thread: " + Thread.currentThread().getId() + "Lock ID" + lock.getId());
				inSearcher.delete(lock, null);
				return null;
			}
			 
			 
			 
		}
		
		if (lock.isLocked())
		{
	//		log.info("Local was alread locked - returning null" + "Thread: " + Thread.currentThread().getId() + "Lock ID" + lock.getId());
			return null;
		}
		
		
		
		
				
		try
		{
			lock.setOwnerId(inOwner);
			lock.setDate(new Date());
			lock.setNodeId(getNodeManager().getLocalNodeId());
			lock.setLocked(true);
			getLockSearcher().saveData(lock, null);  //Both threads called this
	//		log.info(lock.getId() +"being saved.  Line 139  Current version " + lock.get(".version") + "Thread: " + Thread.currentThread().getId() + " Lock ID " + lock.getId());

		}
		catch (ConcurrentModificationException ex)
		{
	//		log.info("Lock was not available " + lock.get(".version") + "Thread: " + Thread.currentThread().getId() + " Lock ID " + lock.getId());

			return null;
		}
		catch (OpenEditException ex)
		{
			if (ex.getCause() instanceof ConcurrentModificationException)
			{
				return null;
			}
			throw ex;
		}
	//	log.info(lock.getId() +"being returned.  Line 154  Current version " + lock.get(".version") + "Thread: " + Thread.currentThread().getId() +  "Lock ID" + lock.getId());

		return lock;

	}

	protected Lock createLock(String inPath, Searcher searcher)
	{
		Lock lockrequest = (Lock) searcher.createNewData();
		lockrequest.setSourcePath(inPath);
		lockrequest.setLocked(false);
		return lockrequest;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.entermediadb.locks.LockManagerI#loadLock(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public Lock loadLock(String inPath)
	{
//		return loadLock(inPath, false, null);
//	}
//	public Lock loadLock(String inPath, boolean lockIt, String inOwner)
//	{
		Searcher searcher = getLockSearcher();

		SearchQuery q = searcher.createSearchQuery(); 
		q.addExact("sourcepath", inPath);
		q.setHitsPerPage(1);
		HitTracker tracker = searcher.search(q);
		if( tracker.size() == 0)
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
		Lock lock = new Lock();
		lock.setId(first.getId());
		lock.getProperties().putAll(first.getProperties());
		return lock;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.entermediadb.locks.LockManagerI#getLocksByDate(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public HitTracker getLocksByDate(String inPath)
	{
		Searcher searcher = getLockSearcher();
		SearchQuery q = searcher.createSearchQuery();
		q.addExact("sourcepath", inPath);
		q.addSortBy("date");
		return searcher.search(q);
	}

	public Searcher getLockSearcher()
	{
		Searcher searcher = getSearcherManager().getSearcher(getCatalogId(),"lock");
		return searcher;
	}

	public boolean isOwner(Lock lock)
	{
		if (lock == null)
		{
			throw new OpenEditException("Lock should not be null");
		}
		if (lock.getId() == null)
		{
			throw new OpenEditException("lock id is currently null");
		}

		Lock owner = loadLock(lock.getSourcePath());
		if (owner == null)
		{
			throw new OpenEditException("Owner lock is currently null");
		}
		if (lock.getOwnerId() == null)
		{
			return false;
		}
		boolean sameowner = lock.getOwnerId().equals(owner.getOwnerId());
		return sameowner;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.entermediadb.locks.LockManagerI#lockIfPossible(java.lang.String,
	 * java.lang.String, java.lang.String)
	 */
	@Override
	public Lock lockIfPossible(String inPath, String inOwnerId)
	{
		log.info(Thread.currentThread().getId() + " is trying to lock " + inPath);
		try
		{
			Lock lock = loadLock(inPath);
			if(lock != null && lock.isLocked())
			{
				log.error("Locked " + lock + " " + inPath);
				return null;
			}
			lock = grabLock(lock, inOwnerId, inPath);
			return lock;
		}
		catch( Throwable ex)
		{
			log.error(ex);
			return null;
		}
	
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.entermediadb.locks.LockManagerI#release(java.lang.String,
	 * org.entermediadb.locks.Lock)
	 */
	@Override
	public boolean release(Lock inLock)
	{
		if (inLock != null)
		{
			Searcher searcher = getLockSearcher();
			inLock.setLocked(false);
			//inLock.setProperty("version", (String) null); //Once this is saved other people can go get it
		//	log.info(inLock.getId() +" being released Current version " + inLock.get(".version") + " Thread: " + Thread.currentThread().getId());
			searcher.saveData(inLock, null);
		//	log.info(inLock.getId() +" being saved on release.  Current version " + inLock.get(".version") + "Thread: " + Thread.currentThread().getId());

			return true;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.entermediadb.locks.LockManagerI#releaseAll(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public void releaseAll(String inPath)
	{
		Lock existing = loadLock( inPath);
		release( existing);
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
	
	@Override
	public void shutdown()
	{
//		LockSearcher searcher = (LockSearcher)getLockSearcher();
//		try
//		{
//			searcher.clearStaleLocks();
//		}
//		catch( Throwable ex)
//		{
//			log.info(ex);
//		}
	}
}
