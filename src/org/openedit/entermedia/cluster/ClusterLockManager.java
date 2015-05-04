package org.openedit.entermedia.cluster;

import java.util.ConcurrentModificationException;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermedia.locks.Lock;
import org.entermedia.locks.LockManager;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;

import com.openedit.OpenEditException;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;

public class ClusterLockManager implements LockManager
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
	 * @see org.entermedia.locks.LockManagerI#lock(java.lang.String,
	 * java.lang.String, java.lang.String)
	 */
	@Override
	public Lock lock(String inPath, String inOwnerId)
	{
		//Searcher searcher = getLockSearcher(inCatId);
		Lock lock = loadLock(inPath);
		int tries = 0;
		while (!grabLock(inOwnerId, lock))
		{
			tries++;
			if (tries > 9)
			{
				throw new OpenEditException("Could not lock file " + inPath + " locked by " + lock.getNodeId() + " " + lock.getOwnerId());
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
		return lock;
	}

	public boolean grabLock(String inOwner, Lock lock)
	{
		if (lock == null)
		{
			throw new OpenEditException("Lock should not be null");
		}

		if (lock.isLocked())
		{
			return false;
		}
		// set owner
		try
		{
			lock.setOwnerId(inOwner);
			lock.setDate(new Date());
			lock.setNodeId(getNodeManager().getLocalNodeId());
			lock.setLocked(true);
			getLockSearcher().saveData(lock, null);
		}
		catch (ConcurrentModificationException ex)
		{
			return false;
		}
		catch (OpenEditException ex)
		{
			if (ex.getCause() instanceof ConcurrentModificationException)
			{
				return false;
			}
			throw ex;
		}
		return true;

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
	 * @see org.entermedia.locks.LockManagerI#loadLock(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public Lock loadLock(String inPath)
	{
		Searcher searcher = getLockSearcher();

		SearchQuery q = searcher.createSearchQuery(); 
		q.addMatches("sourcepath", inPath);
		//q.addSortBy("date"); //We just have one now
		
		HitTracker tracker = searcher.search(q);
		tracker.setHitsPerPage(1);
		Data first = (Data) tracker.first();

		if (first == null)
		{
			synchronized (searcher)
			{
				tracker = searcher.search(q); //Make sure there was not a thread waiting
				tracker.setHitsPerPage(1);
				first = (Data) tracker.first();
				if (first == null)
				{
					Lock lock = createLock(inPath, searcher);
					lock.setNodeId(getNodeManager().getLocalNodeId());
					lock.setDate(new Date());
					searcher.saveData(lock, null);
					return lock;
				}
			}
		}

		first = (Data) searcher.loadData(first); //TODO: Replace this with some
		//kind of createNewData option
		Lock lock = new Lock();
		lock.setId(first.getId());
		lock.getProperties().putAll(first.getProperties());
		return lock;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.entermedia.locks.LockManagerI#getLocksByDate(java.lang.String,
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
	 * @see org.entermedia.locks.LockManagerI#lockIfPossible(java.lang.String,
	 * java.lang.String, java.lang.String)
	 */
	@Override
	public Lock lockIfPossible(String inPath, String inOwnerId)
	{
		Lock lock = loadLock(inPath);

		if (lock.isLocked())
		{
			return null;
		}
		if (grabLock(inOwnerId, lock))
		{
			return lock;
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.entermedia.locks.LockManagerI#release(java.lang.String,
	 * org.entermedia.locks.Lock)
	 */
	@Override
	public boolean release(Lock inLock)
	{
		if (inLock != null)
		{
			Searcher searcher = getLockSearcher();
			inLock.setLocked(false);
			inLock.setProperty("version", (String) null);
			searcher.saveData(inLock, null);
			return true;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.entermedia.locks.LockManagerI#releaseAll(java.lang.String,
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
}
