package org.entermediadb.model;

import org.entermediadb.asset.BaseEnterMediaTest;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;

public class ModuleSearchTest extends BaseEnterMediaTest
{
	
	public void testLuceneIds() throws Exception
	{
		WebPageRequest req = getFixture().createPageRequest("/entermedia/index.html");
		
		Searcher searcher = (Searcher) getMediaArchive().getSearcher("order");
		
		Data order = searcher.createNewData();
		order.setId("search101");
		order.setProperty("sharewithemail", "TESTING" );
		order.setSourcePath("testsearch/search101");
		searcher.saveData(order,null);

		SearchQuery q = searcher.createSearchQuery();
		q.addMatches("sharewithemail", "TESTing" );
		HitTracker exact = searcher.search(q);
		assertEquals(exact.size() , 1);
		
		
		
		order = searcher.createNewData();
		order.setId("SEARCH102");
		order.setSourcePath("testsearch/search102");
		searcher.saveData(order,null);


		q = searcher.createSearchQuery();
		q.addOrsGroup("id", "search101 SEARCH102" );
		HitTracker three = searcher.search(q);
		assertEquals(three.size() , 2);
		
	
	}
	
}
