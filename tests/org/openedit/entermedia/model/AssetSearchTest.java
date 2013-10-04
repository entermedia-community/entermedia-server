package org.openedit.entermedia.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openedit.Data;
import org.openedit.data.PropertyDetail;
import org.openedit.data.lucene.LuceneHitTracker;
import org.openedit.data.lucene.LuceneSearchQuery;
import org.openedit.entermedia.BaseEnterMediaTest;
import org.openedit.entermedia.search.AssetSearcher;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.FilterNode;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;

public class AssetSearchTest extends BaseEnterMediaTest
{
	
	public void xtestLuceneIds() throws Exception
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
	
	
	public void testFacetedSearch() throws Exception
	{
		WebPageRequest req = getFixture().createPageRequest("/entermedia/index.html");
		
		AssetSearcher searcher = getMediaArchive().getAssetSearcher();
		Data asset = searcher.createNewData();
		asset.setId("facet101");
		asset.setSourcePath("testsearch/search101");
		asset.setName("facetedasset");
		asset.setProperty("assettype", "audio");
		searcher.saveData(asset,null);
		
		
		asset = searcher.createNewData();
		asset.setId("facet102");
		asset.setSourcePath("testsearch/search102");
		asset.setName("facetedasset");
		asset.setProperty("assettype", "video");
		searcher.saveData(asset,null);
		
		
		asset = searcher.createNewData();
		asset.setId("facet103");
		asset.setSourcePath("testsearch/search103");
		asset.setName("facetedasset");
		asset.setProperty("assettype", "image");
		searcher.saveData(asset,null);
		
		asset = searcher.createNewData();
		asset.setId("facet104");
		asset.setSourcePath("testsearch/search104");
		asset.setName("facetedasset");
		asset.setProperty("assettype", "audio");
		searcher.saveData(asset,null);
		
		asset = searcher.createNewData();
		asset.setId("facet105");
		asset.setSourcePath("testsearch/search105");
		asset.setName("facetedasset");
		asset.setProperty("assettype", null);
		searcher.saveData(asset,null);
		
		LuceneSearchQuery q = (LuceneSearchQuery) searcher.createSearchQuery();		
		q.addMatches("name", "facetedasset");
		
		LuceneHitTracker hits = (LuceneHitTracker) searcher.search(q);
		assertEquals(5, hits.size());
		
		
		
		List <FilterNode> facets = hits.getFilters();
		assertTrue(facets.size() > 0);
	//	searcher.reIndexAll();
		hits = (LuceneHitTracker) searcher.search(q);
		facets = hits.getFilters();
		
		
		
		
//		//
//		String[] f=  {"assettype","audio"}; 
//		 
//		         
		FilterNode parent = new FilterNode();
		FilterNode child = new FilterNode();
		parent.addChild(child);
		child.setId("assettype/audio");
		child.setProperty("label", "assettype/audio");
		child.setSelected(true);
		ArrayList list = new ArrayList();
		list.add(parent);
		q.setFacetValues(list);

		
//		facet.setValues(f);
//		FacetValues values = new FacetValues();
//		values.addFacet(facet);
//		q.setFacetValues(values);
//		
		
		LuceneHitTracker facetedhits = (LuceneHitTracker) searcher.search(q);
		assertEquals(2 ,facetedhits.size());
		searcher.reIndexAll();
		
		 facetedhits = (LuceneHitTracker) searcher.search(q);
		 facets = hits.getFilters();
 
		assertEquals(2 ,facetedhits.size());
		FilterNode child2 = new FilterNode();
		child2.setId("assetype/image");
		child2.setProperty("label", "assettype/video");
		parent.addChild(child2);
		child2.setSelected(true);
		
		 facetedhits = (LuceneHitTracker) searcher.search(q);
		assertEquals(3 ,facetedhits.size());
		
	}
	
	
}
