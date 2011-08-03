package org.openedit.entermedia.cluster;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermedia.locks.Lock;
import org.entermedia.locks.LockManager;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.util.DateStorageUtil;

import com.openedit.OpenEditException;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;

public class ClusterLockManager implements LockManager
{
	private static final Log log = LogFactory.getLog(ClusterLockManager.class);
	
	protected SearcherManager fieldSearcherManager;
	protected NodeManager fieldNodeManager;

	/* (non-Javadoc)
	 * @see org.entermedia.locks.LockManagerI#lock(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public Lock lock(String inCatId, String inPath, String inOwnerId)
	{
		Searcher searcher = getLockSearcher(inCatId);
		Lock lockrequest = addLock(inPath, inOwnerId, searcher);

		Lock lock = loadLock(inCatId,inPath);
		int tries = 0;
		while( !isOwner(lock, inOwnerId))
		{
			tries++;
			log.info("Could not lock trying again  " + tries);
			if( tries > 9)
			{
				searcher.delete(lockrequest, null);
				throw new OpenEditException("Could not lock file " + inPath + " locked by " + lock.getOwnerId() );
			}
			try
			{
				Thread.sleep(250);
			}
			catch( Exception ex)
			{
				//does not happen
				log.info(ex);
			}
			
			lock = loadLock(inCatId,inPath);
		}
		return lock;
	}

	public boolean isOwner(Lock lock, String inOwnerId)
	{
		if( lock == null)
		{
			throw new OpenEditException("Lock should not be null");
		}
		return lock.isOwner(getNodeManager().getLocalNodeId(),inOwnerId);
	}

	protected Lock addLock(String inPath, String inOwnerId, Searcher searcher)
	{
		Lock lockrequest = (Lock)searcher.createNewData();
		lockrequest.setPath(inPath);
		lockrequest.setOwnerId(inOwnerId);
		lockrequest.setDate(new Date());
		lockrequest.setNodeId(getNodeManager().getLocalNode().getId());
		lockrequest.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
		searcher.saveData(lockrequest, null);
		log.info("locked " + inPath);
		return lockrequest;
	}
	
	/* (non-Javadoc)
	 * @see org.entermedia.locks.LockManagerI#loadLock(java.lang.String, java.lang.String)
	 */
	@Override
	public Lock loadLock(String inCatId, String inPath)
	{
		Searcher searcher = getLockSearcher(inCatId);
		SearchQuery q = searcher.createSearchQuery();
		q.addExact("path",inPath);
		q.addSortBy("date");
		
		HitTracker tracker = searcher.search(q);
		Data first = (Data)tracker.first();
		if( first == null)
		{
			return null;
		}
		Lock lock = new Lock();
		lock.setId(first.getId());
		lock.getProperties().putAll(first.getProperties());
		return lock;
	}
	
	/* (non-Javadoc)
	 * @see org.entermedia.locks.LockManagerI#getLocksByDate(java.lang.String, java.lang.String)
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
	
	protected Searcher getLockSearcher(String inCatalogId)
	{
		Searcher searcher = getSearcherManager().getSearcher(inCatalogId, "lock");
		return searcher;
	}

	/* (non-Javadoc)
	 * @see org.entermedia.locks.LockManagerI#lockIfPossible(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public Lock lockIfPossible(String inCatId, String inPath, String inOwnerId)
	{
		Searcher searcher = getLockSearcher(inCatId);

		Lock lockrequest = addLock(inPath, inOwnerId, searcher);

		Lock lock = loadLock(inCatId,inPath);
		if( isOwner(lock,inOwnerId))
		{
			return lockrequest;
		}
		release(inCatId, lockrequest);
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.entermedia.locks.LockManagerI#release(java.lang.String, org.entermedia.locks.Lock)
	 */
	@Override
	public boolean release(String inCatId, Lock inLock)
	{
		if( inLock != null)
		{
			Searcher searcher = getLockSearcher(inCatId);
			searcher.delete(inLock, null);
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.entermedia.locks.LockManagerI#releaseAll(java.lang.String, java.lang.String)
	 */
	@Override
	public void releaseAll(String inCatalogId, String inPath)
	{
		Lock existing = loadLock(inCatalogId, inPath);
		while( existing != null)
		{
			release(inCatalogId, existing);
			existing = loadLock(inCatalogId, inPath);
		}
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
