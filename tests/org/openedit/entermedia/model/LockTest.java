package org.openedit.entermedia.model;

import org.entermedia.locks.Lock;
import org.entermedia.locks.LockManager;

import com.openedit.BaseTestCase;

public class LockTest extends	 BaseTestCase
{
	public void testLocks()
	{
		LockManager manager = (LockManager)getBean("lockManager");
		Lock lock = manager.lockIfPossible("entermedia/catalog/testcatalog","/somepath.xml", "testprocess");
		assertNotNull(lock);

		Lock lock2 = manager.lockIfPossible("entermedia/catalog/testcatalog","/somepath.xml", "testprocess2");
		assertNull(lock);

		
	}
}
