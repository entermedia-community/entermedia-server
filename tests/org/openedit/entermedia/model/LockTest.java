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
		LockManager manager = (LockManager)getFixture().getModuleManager().getBean("lockManager");
		
		String path = "/entermedia/catalogs/testcatalog/assets/users/101/index.html";
		manager.releaseAll("entermedia/catalogs/testcatalog",path);
		
		Lock lock = manager.lock("entermedia/catalogs/testcatalog", path, "admin");
		assertNotNull(lock);
		assertTrue(manager.isOwner(lock,"admin"));

		manager.release("entermedia/catalogs/testcatalog",lock);
		lock = manager.loadLock("entermedia/catalogs/testcatalog", path);
		assertNull(lock);

		//clear
		//manager.lockIfPossible(inCatId, inPath, inOwnerId)
		//manager.release(inCatId, inPath, inOwnerId)
	}
	
	public void testAssetIsAlreadyLocked() throws Exception
	{
		LockManager manager = (LockManager)getFixture().getModuleManager().getBean("lockManager");
		
		String path = "/entermedia/catalogs/testcatalog/assets/users/101/index.html";
		manager.releaseAll("entermedia/catalogs/testcatalog",path);

		Lock lock = manager.lock("entermedia/catalogs/testcatalog", path, "admin");
		assertNotNull(lock);
		assertTrue(manager.isOwner(lock,"admin"));

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
		final LockManager manager = (LockManager)getFixture().getModuleManager().getBean("lockManager");
		
		final String path = "/entermedia/catalogs/testcatalog/assets/users/101/index.html";
		manager.releaseAll("entermedia/catalogs/testcatalog",path);

		final Lock lock = manager.lock("entermedia/catalogs/testcatalog", path, "admin");
		assertNotNull(lock);
		assertTrue(manager.isOwner(lock,"admin"));

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
