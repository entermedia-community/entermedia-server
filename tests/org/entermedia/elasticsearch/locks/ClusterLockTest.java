package org.entermedia.elasticsearch.locks;

import java.util.ConcurrentModificationException;

import org.entermediadb.asset.cluster.ClusterLockManager;
import org.entermediadb.elasticsearch.searchers.LockSearcher;
import org.entermediadb.model.LockTest;
import org.openedit.locks.Lock;


public class ClusterLockTest extends LockTest
{
	//Had some problems with the very first lock not being saved ok
	public void testLock()
	{
		String catid = "entermedia/catalogs/testcatalog";
		ClusterLockManager manager = (ClusterLockManager)getStaticFixture().getModuleManager().getBean(catid,"lockManager");
		
		String path = "/entermedia/catalogs/testcatalog/assets/users/101/index.html";
		
		manager.releaseAll(path);
		
		Lock lock = manager.lock(path, "admin");
		assertNotNull(lock);

		Lock nopossible = manager.lockIfPossible(path, "sfdsf");
		assertNull(nopossible);

		LockSearcher searcher = (LockSearcher)manager.getLockSearcher();
		searcher.clearStaleLocks();
		
		lock = manager.loadLock(path);
		assertNull(lock);

		//clear
		Lock yespossible = manager.lockIfPossible(path, "sfdsf");
		assertNotNull(yespossible);
		manager.release(yespossible);
	}

	public void testVersion() throws Exception
	{
		String catid = "entermedia/catalogs/testcatalog";
		ClusterLockManager manager = (ClusterLockManager)getStaticFixture().getModuleManager().getBean(catid,"lockManager");
		String path = "/entermedia/catalogs/testcatalog/assets/users/101/index.html";
		
		Lock lockfirst = manager.lock(path,"testing");
		String version = lockfirst.get(".version");
		assertNotNull(version);

		Lock locksecond = manager.loadLock(path);
		locksecond.setOwnerId("fastdude");
		manager.getLockSearcher().saveData(locksecond, null);

		String version2 = locksecond.get(".version");
		assertNotNull(version2);
		
		lockfirst.setOwnerId("slowdude");
		boolean failed = false;
		try
		{
			manager.getLockSearcher().saveData(lockfirst, null);
		}
		catch( ConcurrentModificationException ex)
		{
			failed = true;
		}
		assertTrue(failed);
	}
}
