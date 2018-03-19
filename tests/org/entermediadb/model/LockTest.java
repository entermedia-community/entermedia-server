package org.entermediadb.model;

import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.cluster.IdManager;
import org.openedit.locks.Lock;
import org.openedit.locks.LockManager;


public class LockTest extends BaseEnterMediaTest
{
	
	public void testLocks()
	{
		String catid = "entermedia/catalogs/testcatalog";
		LockManager manager = (LockManager)getStaticFixture().getModuleManager().getBean(catid,"lockManager");
		
		String path = "/entermedia/catalogs/testcatalog/assets/users/101/index.html";
		//manager.releaseAll(path);

		Lock lock = manager.loadLock( path);
		assertNotNull(lock);
		manager.release(lock);

//		Lock lock = manager.lock( path, "admin");
//		assertNotNull(lock);
//		assertTrue(manager.isOwner(lock));
//
//		Lock lock2 = manager.loadLock(path);
//		assertTrue(lock2.isLocked());
//
//		
//		manager.release(lock);
//		lock = manager.loadLock(path);
//		assertFalse(lock.isLocked());

		//clear
		//manager.lockIfPossible(inCatId, inPath, inOwnerId)
		//manager.release(inCatId, inPath, inOwnerId)
	}
	
	public void testAssetIsAlreadyLocked() throws Exception
	{
		String catid = "entermedia/catalogs/testcatalog";
		LockManager manager = (LockManager)getStaticFixture().getModuleManager().getBean(catid,"lockManager");

		
		String path = "/entermedia/catalogs/testcatalog/assets/users/101/index.html";
		manager.releaseAll(path);

		Lock lock = manager.lock(path, "admin");
		assertNotNull(lock);
		assertTrue(manager.isOwner(lock));

		//this should fail
		boolean failed = false;
		try
		{
			 manager.lock(path, "testuser");
		}
		catch( Exception ex)
		{
			failed = true;
		}
		assertTrue("Should have given up after 2.5 seconds",failed);
		
	}

	public void testWaitForLock() throws Exception
	{
		String catid = "entermedia/catalogs/testcatalog";
		final LockManager manager = (LockManager)getStaticFixture().getModuleManager().getBean(catid,"lockManager");
		
		final String path = "/entermedia/catalogs/testcatalog/assets/users/101/index.html";
		manager.releaseAll(path);
		
		final Lock lock = manager.lock(path, "admin");
		assertNotNull(lock);
		assertTrue(manager.isOwner(lock));

		//this should pass
		boolean passed = false;
		try
		{
			new Thread(new Runnable()
			{
				public void run()
				{	
					try
					{
						Thread.sleep(500);
					}
					catch( Throwable ex)
					{
						
					}
					manager.release(lock);
				}
			}).start();
			
			Lock lock2 = manager.lock(path, "testuser");
			passed = true;
		}
		catch( Exception ex)
		{
			passed = false;
		}
		assertTrue("Should have released",passed);

		
	}
	
	public void testIds() throws Exception
	{
		String catid = "entermedia/catalogs/testcatalog";
		final IdManager manager = (IdManager)getStaticFixture().getModuleManager().getBean(catid,"idManager");
		manager.getIdSearcher().deleteAll(null);
		
		String test = manager.nextId("test");
		String test2 = manager.nextId("test");
		assertNotSame( test2, test);
		
		
		
	}
	

	
	
}
