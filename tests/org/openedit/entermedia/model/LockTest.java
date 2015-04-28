package org.openedit.entermedia.model;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.entermedia.locks.Lock;
import org.entermedia.locks.LockManager;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.entermedia.BaseEnterMediaTest;
import org.openedit.util.DateStorageUtil;


public class LockTest extends BaseEnterMediaTest
{
	
	public void testLocks()
	{
		String catid = "entermedia/catalogs/testcatalog";
		LockManager manager = (LockManager)getStaticFixture().getModuleManager().getBean(catid,"lockManager");
		
		String path = "/entermedia/catalogs/testcatalog/assets/users/101/index.html";
		manager.releaseAll(path);
		
		Lock lock = manager.lock( path, "admin");
		assertNotNull(lock);
		assertTrue(manager.isOwner(lock));

		manager.release(lock);
		lock = manager.loadLock(path);
		assertFalse(lock.isLocked());

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
		boolean failed = false;
		try
		{
			new Thread(new Runnable()
			{
				public void run()
				{	
					manager.release(lock);
				}
			}).start();
			
			Lock lock2 = manager.lock(path, "testuser");
			
		}
		catch( Exception ex)
		{
			failed = true;
		}
		assertFalse("Should have released",failed);

		
	}

	
	
}
