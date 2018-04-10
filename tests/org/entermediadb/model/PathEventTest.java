package org.entermediadb.model;

import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.PathEventModule;
import org.entermediadb.events.PathEventManager;
import org.openedit.WebPageRequest;

public class PathEventTest extends BaseEnterMediaTest
{
	public void testRepeat() throws Exception
	{		
		MediaArchive archive = getMediaArchive();		
		String runpath = "/" + archive.getCatalogId() + "/events/tests/testevent.html";
		PathEventManager manager = (PathEventManager) archive.getModuleManager().getBean(archive.getCatalogId(), "pathEventManager");
		long start = System.currentTimeMillis();
		manager.runSharedPathEvent(runpath); //this one set to run
		manager.runSharedPathEvent(runpath); //This one sets it to run again
		manager.runSharedPathEvent(runpath); //ignore
		manager.runSharedPathEvent(runpath); //ignore
		long end = System.currentTimeMillis();
		Thread.currentThread().sleep(1200);
		//Should have only run twice
		long total = end - start; 
		assertTrue( total < 400L);
		assertEquals(1000,PathEventModule.sleepcount);
	}
}
