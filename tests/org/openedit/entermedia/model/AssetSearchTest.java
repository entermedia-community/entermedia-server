package org.openedit.entermedia.model;

import java.util.Collection;
import java.util.Iterator;

import org.openedit.Data;
import org.openedit.entermedia.BaseEnterMediaTest;
import org.openedit.entermedia.search.AssetSearcher;

import com.openedit.WebPageRequest;
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
		asset.setProperty("caption","search101");
		searcher.saveData(asset,null);
		
		asset = searcher.createNewData();
		asset.setId("search102");
		asset.setSourcePath("testsearch/search102");
		asset.setProperty("caption","search102");
		searcher.saveData(asset,null);

		asset = searcher.createNewData();
		asset.setId("search103");
		asset.setSourcePath("testsearch/search103");
		asset.setProperty("caption","search103");
		searcher.saveData(asset,null);
		
		SearchQuery q = searcher.createSearchQuery();
		q.addOrsGroup("id", "search101 search102 search103" );
		Collection three = searcher.search(q);
		assertEquals(three.size() , 3);
		
		asset.setProperty("caption", "caption" + System.currentTimeMillis());
		searcher.saveData(asset,null);

		three = searcher.search(q);
		assertEquals(three.size() , 3);
		boolean found = false;
		for (Iterator iterator = three.iterator(); iterator.hasNext();) 
		{
			Data hit = (Data) iterator.next();
			String caption = hit.get("caption");
			if( caption.equals(asset.get("caption")))
			{
				found = true;
			}
		}
		assertTrue(found);
	}
	
}
