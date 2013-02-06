package org.openedit.entermedia.model;

import java.util.Iterator;
import java.util.List;

import org.openedit.Data;
import org.openedit.data.XmlFileSearcher;
import org.openedit.entermedia.BaseEnterMediaTest;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;

public class ModuleSearchTest extends BaseEnterMediaTest
{
	
	public void testLuceneIds() throws Exception
	{
		WebPageRequest req = getFixture().createPageRequest("/entermedia/index.html");
		
		XmlFileSearcher searcher = (XmlFileSearcher) getMediaArchive().getSearcher("order");
		
		Data order = searcher.createNewData();
		order.setId("search101");
		order.setSourcePath("testsearch/search101");
		searcher.saveData(order,null);
		
		order = searcher.createNewData();
		order.setId("SEARCH102");
		order.setSourcePath("testsearch/search102");
		searcher.saveData(order,null);

		
		SearchQuery q = searcher.createSearchQuery();
		q.addOrsGroup("id", "search101 SEARCH102" );
		HitTracker three = searcher.search(q);
		assertEquals(three.size() , 1);
		
	
	}
	
}
