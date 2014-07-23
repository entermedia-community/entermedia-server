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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.entermedia.locks.LockManagerI#lock(java.lang.String,
	 * java.lang.String, java.lang.String)
	 */
	@Override
	public Lock lock(String inCatId, String inPath, String inOwnerId)
	{
		Searcher searcher = getLockSearcher(inCatId);
		Lock lock = loadLock(inCatId, inPath);
		int tries = 0;
		while (!grabLock(inCatId, inOwnerId, lock))
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
			lock = loadLock(inCatId, inPath);
		}
		return lock;
	}

	public boolean grabLock(String inCatId, String inOwner, Lock lock)
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
			getLockSearcher(inCatId).saveData(lock, null);
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
		lockrequest.setPath(inPath);
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
	public Lock loadLock(String inCatId, String inPath)
	{
		Searcher searcher = getLockSearcher(inCatId);

		SearchQuery q = searcher.createSearchQuery(); 
		q.addExact("path", inPath);
		// q.addSortBy("date");

		HitTracker tracker = searcher.search(q);
		Data first = (Data) tracker.first();

		if (first == null)
		{

			synchronized (searcher)
			{
				tracker = searcher.search(q);
				first = (Data) tracker.first();

			}
			if (first == null)
			{
				Lock lock = createLock(inPath, searcher);
				lock.setNodeId(getNodeManager().getLocalNodeId());
				lock.setDate(new Date());
				searcher.saveData(lock, null);
				return lock;
			}
		}

		first = (Data) searcher.searchById(first.getId());
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
	public HitTracker getLocksByDate(String inCatId, String inPath)
	{
		Searcher searcher = getLockSearcher(inCatId);
		SearchQuery q = searcher.createSearchQuery();
		q.addExact("path", inPath);
		q.addSortBy("date");
		return searcher.search(q);
	}

	public Searcher getLockSearcher(String inCatalogId)
	{
		Searcher searcher = getSearcherManager().getSearcher(inCatalogId, "lock");
		return searcher;
	}

	public boolean isOwner(String inCatId, Lock lock)
	{
		if (lock == null)
		{
			throw new OpenEditException("Lock should not be null");
		}
		if (lock.getId() == null)
		{
			throw new OpenEditException("lock id is currently null");
		}

		Lock owner = loadLock(inCatId, lock.getPath());
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
	public Lock lockIfPossible(String inCatId, String inPath, String inOwnerId)
	{
		Lock lock = loadLock(inCatId, inPath);

		if (lock.isLocked())
		{
			return null;
		}
		if (grabLock(inCatId, inOwnerId, lock))
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
	public boolean release(String inCatId, Lock inLock)
	{
		if (inLock != null)
		{
			Searcher searcher = getLockSearcher(inCatId);
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
	public void releaseAll(String inCatalogId, String inPath)
	{
		Lock existing = loadLock(inCatalogId, inPath);
		release(inCatalogId, existing);
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
