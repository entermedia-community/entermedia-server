package org.openedit.entermedia.controller;



import java.util.List;

import org.openedit.data.PropertyDetail;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.BaseEnterMediaTest;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.modules.MultiSearchModule;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.CompositeHitTracker;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.hittracker.Term;

public class MultiSearchModuleTest extends BaseEnterMediaTest
{

	public MultiSearchModuleTest(String inName)
	{
		super(inName);
	}


	 public void testFieldSearch() throws Exception
	 {
		String catalog1 = "entermedia/catalogs/audio";
		String catalog2 = "entermedia/catalogs/video";
		WebPageRequest req = getFixture().createPageRequest("/entermedia/search/multiresults.html");
		
		MultiSearchModule module = (MultiSearchModule) getFixture().getModuleManager().getModule("MultiSearchModule");		
		MediaArchive archive = getMediaArchive(catalog1);
		Asset p = createAsset(archive);
		archive.saveAsset(p,req.getUser());
		archive = getMediaArchive(catalog2);
		p = createAsset(archive);
		archive.saveAsset(p,req.getUser());
		
		req.setRequestParameter("applicationid", "entermedia");
		
		req.setRequestParameter("field",new String[] {catalog1 + ":asset/details:name"});
		req.setRequestParameter("operation",new String[] {"startswith"});
		req.setRequestParameter("name.value", "*" );
		
		req.setRequestParameter("searchtype", "asset" );
		req.setRequestParameter("catalogid",new String[] {catalog1,catalog2});
		
		module.multiSearch(req);
		CompositeHitTracker allhits = (CompositeHitTracker) req.getPageValue("hits");
		assertNotNull(allhits);
		
		HitTracker onehits = (HitTracker) allhits.getSubTracker(catalog1);
		assertNotNull(onehits);
		HitTracker twohits = (HitTracker) allhits.getSubTracker(catalog2);
	
		assertNotNull(twohits);
		assertEquals(allhits.size(), onehits.size() + twohits.size());
		assertTrue(allhits.size() >0);
		SearchQuery query = allhits.getSearchQuery();
		List terms = query.getTerms(catalog1, "asset/details", "name");
		assertTrue(terms.size() >0);
		
		PropertyDetail detail = ((Term) terms.get(0)).getDetail();
		assertNotNull(detail);
		assertEquals(catalog1, detail.getCatalogId());
		assertEquals("asset/details", detail.getView());
		assertEquals("name", detail.getId());
				
		archive.getAssetArchive().deleteAsset(p);
	}
	 
	 public void testAddTerm() throws Exception
	 {
		WebPageRequest req = getFixture().createPageRequest();
		MultiSearchModule module = (MultiSearchModule) getFixture().getModuleManager().getModule("MultiSearchModule");
		req.setRequestParameter("applicationid", "entermedia");
		req.setRequestParameter("catalogid", "entermedia/catalogs/testcatalog");
		
		
		SearchQuery query = module.loadCurrentQuery(req);
		query.getTerms().clear();
		
		req.setRequestParameter("fieldid", "caption");
		req.setRequestParameter("view", "asset/content");
		module.addTerm(req);
		req.setRequestParameter("termid", "caption_0");
		req.setRequestParameter("view", "asset/content");
		module.addTerm(req);
		module.loadCurrentQuery(req);
		query = (SearchQuery) req.getPageValue("query");
		assertEquals(1, query.getChildren().size());
		SearchQuery childQuery = (SearchQuery) query.getChildren().get(0);
		assertEquals("caption", childQuery.getId());
		assertEquals(2, childQuery.getTerms().size());
	 }	
}
