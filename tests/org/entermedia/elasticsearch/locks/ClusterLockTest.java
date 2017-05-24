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
		ClusterLockManager manager = (ClusterLockManager) getStaticFixture().getModuleManager().getBean(catid,
				"lockManager");

		String path = "/entermedia/catalogs/testcatalog/assets/users/101/index.html";

		manager.releaseAll(path);

		Lock lock = manager.lock(path, "admin");
		assertNotNull(lock);

		Lock nopossible = manager.lockIfPossible(path, "sfdsf");
		assertNull(nopossible);

		LockSearcher searcher = (LockSearcher) manager.getLockSearcher();
		searcher.clearStaleLocks();
		Thread.sleep(3000);
		lock = manager.loadLock(path);
		assertNull(lock);

		// clear
		Lock yespossible = manager.lockIfPossible(path, "sfdsf");
		assertNotNull(yespossible);
		manager.release(yespossible);

		ExecutorManager exec = (ExecutorManager) getStaticFixture().getModuleManager().getBean("executorManager");
		ArrayList list = new ArrayList();
		ArrayList runnables = new ArrayList();
		
		
		for (int i = 0; i < 200; i++) {
			final Integer count = i;
			
			runnables.add(new Runnable() {

				@Override
				public void run() {
					try {
						Thread.sleep(count*10);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
					//	e.printStackTrace();
					}
					Lock lock = manager.lockIfPossible("tryfail", "sfdsf");
					if(lock != null){
						//Thread.sleep(count);
						list.add(count);
						
						manager.release(lock);
						lock = manager.lockIfPossible("tryfail", "sfdsf");
						manager.release(lock);

					}

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

		Lock lockfirst = manager.lock(path, "testing");
		String version = lockfirst.get(".version");
		assertNotNull(version);

		Lock locksecond = manager.loadLock(path);
		locksecond.setOwnerId("fastdude");
		manager.getLockSearcher().saveData(locksecond, null);

		String version2 = locksecond.get(".version");
		assertNotNull(version2);

		lockfirst.setOwnerId("slowdude");
		boolean failed = false;
		try {
			manager.getLockSearcher().saveData(lockfirst, null);
		} catch (ConcurrentModificationException ex) {
			failed = true;
		}
		assertTrue(failed);
	}
}
