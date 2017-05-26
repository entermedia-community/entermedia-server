package org.entermedia.elasticsearch.locks;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.cluster.ClusterLockManager;
import org.entermediadb.elasticsearch.searchers.LockSearcher;
import org.entermediadb.events.PathEventManager;
import org.entermediadb.model.LockTest;
import org.openedit.locks.Lock;
import org.openedit.util.ExecutorManager;

public class ClusterLockTest extends LockTest {
	// Had some problems with the very first lock not being saved ok
	
	
	
	protected static final Log log = LogFactory.getLog(ClusterLockTest.class);

	public void testLock() throws Exception {
		String catid = "entermedia/catalogs/testcatalog";
		final ClusterLockManager manager = (ClusterLockManager) getStaticFixture().getModuleManager().getBean(catid,
				"lockManager");

		final String path = "/entermedia/catalogs/testcatalog/assets/users/101/index.html";

		//manager.releaseAll(path);

		LockSearcher searcher = (LockSearcher) manager.getLockSearcher();
		searcher.clearStaleLocks();
		Thread.sleep(3000);
		Lock stub = manager.lock(path, "sfdsf");
		manager.release(stub);
		// clear

		ExecutorManager exec = (ExecutorManager) getStaticFixture().getModuleManager().getBean("executorManager");
		ArrayList list = new ArrayList();
		ArrayList runnables = new ArrayList();
		
		
		for (int i = 0; i < 50; i++) {
			final Integer count = i;
			
			runnables.add(new Runnable() {

				@Override
				public void run() {
					Lock yespossible = null;
					try {
						yespossible = manager.lockIfPossible(path, "sfdsf");
						if( yespossible != null)
						{
							log.info("Publishing...");
							Thread.sleep(70);
						}	
					} catch (InterruptedException e) {
					}
					finally
					{
						if( yespossible != null )
						{
							manager.release(yespossible);
						}
					}
//					Lock lock = manager.lockIfPossible("tryfail", "sfdsf");
//					if(lock != null){
//						//Thread.sleep(count);
//						list.add(count);
//						
//						manager.release(lock);
//						lock = manager.lockIfPossible("tryfail", "sfdsf");
//						manager.release(lock);
//
//					}

				}
			});
			
			

		}
		exec.execute("unlimited", runnables);
		
		log.info(list);
		

	}

	public void testVersion() throws Exception {
		String catid = "entermedia/catalogs/testcatalog";
		ClusterLockManager manager = (ClusterLockManager) getStaticFixture().getModuleManager().getBean(catid,
				"lockManager");
		String path = "/entermedia/catalogs/testcatalog/assets/users/101/index.html";

//		manager.releaseAll(path);
//
//		LockSearcher searcher = (LockSearcher) manager.getLockSearcher();
//		searcher.clearStaleLocks();
//		Thread.sleep(3000);

		
		Lock lockfirst = manager.lock(path, "testing");
		String version = lockfirst.get(".version");
		assertNotNull(version);

		Lock locksecond = manager.loadLock(path);
		locksecond.setOwnerId("fastdude");

		String version2 = locksecond.get(".version");
		assertNotNull(version2);

		log.info("First save " + locksecond.getId() + " " + locksecond.get(".version"));
		log.info(lockfirst.getId() + " " + lockfirst.get(".version"));
		
		manager.getLockSearcher().saveData(locksecond, null);

		String version3 = locksecond.get(".version");
		assertNotNull(version3);

		lockfirst.setOwnerId("slowdude");
		boolean failed = false;
		try {
			log.info("Second Save " + locksecond.getId() + " " + locksecond.get(".version"));
			log.info(lockfirst.getId() + " " + lockfirst.get(".version"));
			manager.getLockSearcher().saveData(lockfirst, null);
		} catch (ConcurrentModificationException ex) {
			failed = true;
		}
		if( !failed)
		{
			assertTrue(failed);
		}
	}
}
