package org.openedit.entermedia.data;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;

import org.dom4j.Element;
import org.openedit.Data;
import org.openedit.entermedia.BaseEnterMediaTest;
import org.openedit.entermedia.cluster.DataFileSearcher;
import org.openedit.entermedia.cluster.LocalDataManager;
import org.openedit.entermedia.cluster.RemoteDataManager;

import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.util.FileUtils;

public class DistributedDataTest extends BaseEnterMediaTest
{
	//Lucene 2.9.1 Does not support minute resolution of date searches
	/* 
	public void XXtestDateSearch() throws Exception
	{
		LocalDataManager data = (LocalDataManager)getFixture().getModuleManager().getBean("distributedDataManager");
		DataFileSearcher searcher = (DataFileSearcher)data.getSearcherManager().getSearcher("entermedia", "dataFile");
		SearchQuery query = searcher.createSearchQuery();
		
		//11:30:07 AM
		GregorianCalendar cal = new GregorianCalendar();
		//cal.add(Calendar.DAY_OF_YEAR, -1);
		cal.set(Calendar.HOUR,5);
		cal.set(Calendar.MINUTE,00);
		cal.set(Calendar.SECOND,03);
		//20100714153744
		
		query.addAfter("lastmodified",cal.getTime());
		
		HitTracker tracker = searcher.search(query);
		assertTrue(tracker.size() > 0);
		for (Iterator iterator = tracker.iterator(); iterator.hasNext();)
		{
			Data hit = (Data) iterator.next();
			System.out.println(hit.get("lastmodified"));
		}
	}
	*/
	
	public void XXtestLocalFileChanges() throws Exception
	{
		LocalDataManager data = (LocalDataManager)getFixture().getModuleManager().getBean("localDataManager");
		Date now = new Date();
		HitTracker changes = data.listChangesSince("entermedia",now);
	///	assertEquals(0, changes.size());

		File thumb = new File(getRoot(), "../etc/testassets/test2.jpg");

		File file = new File(getRoot(), "/WEB-INF/data/entermedia/testdata/generated/test/thumb1234.jpg");

		new FileUtils().copyFiles(thumb, file);
		file.setLastModified(now.getTime()+ 5000 * 60);
		Thread.sleep(100);
		
		changes = data.listChangesSince("entermedia",now);
		assertEquals(1, changes.size());
	}

	//This will require another cluster running on 8888
	public void testRemoteFileChanges() throws Exception
	{
		RemoteDataManager manager = (RemoteDataManager)getFixture().getModuleManager().getBean("remoteDataManager");
		
		Data server = manager.findNextRemoteServer("entermedia", getFixture().createPageRequest().getSiteRoot());
		assertNotNull(server);
		Element changes = manager.importChanges("entermedia",server);
		assertNotNull( changes );
		//manager.importChanges(server.getId(),"entermedia",changes);
	}
}
