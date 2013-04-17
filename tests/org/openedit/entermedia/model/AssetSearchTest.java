package org.openedit.entermedia.model;

import java.util.Iterator;
import java.util.List;

import org.openedit.Data;
import org.openedit.entermedia.BaseEnterMediaTest;
import org.openedit.entermedia.search.AssetSearcher;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;

public class AssetSearchTest extends BaseEnterMediaTest
{
	
	public void testLuceneIds() throws Exception
	{
		WebPageRequest req = getFixture().createPageRequest("/entermedia/index.html");
		
		AssetSearcher searcher = getMediaArchive().getAssetSearcher();
		Data asset = searcher.createNewData();
		asset.setId("search101");
		asset.setSourcePath("testsearch/search101");
		asset.setName("search101");
		searcher.saveData(asset,null);
		
		asset = searcher.createNewData();
		asset.setId("search102");
		asset.setSourcePath("testsearch/search102");
		asset.setName("search102");
		searcher.saveData(asset,null);

		asset = searcher.createNewData();
		asset.setId("search103");
		asset.setSourcePath("testsearch/search103");
		asset.setName("search103");

		searcher.saveData(asset,null);
		
		SearchQuery q = searcher.createSearchQuery();
		q.addOrsGroup("id", "search101 search102 search103" );
		q.addSortBy("name");
		HitTracker three = searcher.search(q);
		assertEquals(three.size() , 3);
		
		asset.setProperty("caption", "caption" + System.currentTimeMillis());
		searcher.saveData(asset,null);

		three = searcher.search(q);
		three.setHitsPerPage(2);
		assertEquals(three.size() , 3);
		boolean found = false;
		for (Iterator iterator = three.iterator(); iterator.hasNext();) 
		{
			Data hit = (Data) iterator.next();
			String caption = hit.getName();
			if( caption.equals(asset.getName()))
			{
				found = true;
			}
		}
		assertTrue(found);

		List hits = three.getPageOfHits();
		assertNotNull(hits);
		assertEquals(hits.size(), 2);
		three.setPage(2);
		hits = three.getPageOfHits();
		assertEquals(hits.size(), 1);
		
	}
	
}
