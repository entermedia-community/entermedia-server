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
		LockManager manager = (LockManager)getStaticFixture().getModuleManager().getBean("lockManager");
		
		String path = "/entermedia/catalogs/testcatalog/assets/users/101/index.html";
		String catid = "entermedia/catalogs/testcatalog";
		manager.releaseAll(catid,path);
		
		Lock lock = manager.lock(catid, path, "admin");
		assertNotNull(lock);
		assertTrue(manager.isOwner(catid, lock));

		manager.release(catid,lock);
		lock = manager.loadLock(catid, path);
		assertFalse(lock.isLocked());

		//clear
		//manager.lockIfPossible(inCatId, inPath, inOwnerId)
		//manager.release(inCatId, inPath, inOwnerId)
	}
	
	public void testAssetIsAlreadyLocked() throws Exception
	{
		LockManager manager = (LockManager)getStaticFixture().getModuleManager().getBean("lockManager");

		String catid = "entermedia/catalogs/testcatalog";
		
		String path = "/entermedia/catalogs/testcatalog/assets/users/101/index.html";
		manager.releaseAll("entermedia/catalogs/testcatalog",path);

		Lock lock = manager.lock("entermedia/catalogs/testcatalog", path, "admin");
		assertNotNull(lock);
		assertTrue(manager.isOwner(catid,lock));

		//this should fail
		boolean failed = false;
		try
		{
			 manager.lock("entermedia/catalogs/testcatalog", path, "testuser");
		}
		catch( Exception ex)
		{
			failed = true;
		}
		assertTrue("Should have given up after 2.5 seconds",failed);
		
	}

	public void testWaitForLock() throws Exception
	{
		final LockManager manager = (LockManager)getStaticFixture().getModuleManager().getBean("lockManager");
		
		final String path = "/entermedia/catalogs/testcatalog/assets/users/101/index.html";
		manager.releaseAll("entermedia/catalogs/testcatalog",path);
		String catid = "entermedia/catalogs/testcatalog";
		
		final Lock lock = manager.lock("entermedia/catalogs/testcatalog", path, "admin");
		assertNotNull(lock);
		assertTrue(manager.isOwner(catid,lock));

		//this should pass
		boolean failed = false;
		try
		{
			new Thread(new Runnable()
			{
				public void run()
				{	
					manager.release("entermedia/catalogs/testcatalog", lock);
				}
			}).start();
			
			Lock lock2 = manager.lock("entermedia/catalogs/testcatalog", path, "testuser");
			
		}
		catch( Exception ex)
		{
			failed = true;
		}
		assertFalse("Should have released",failed);

		
	}

	
	
}
