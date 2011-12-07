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
import org.openedit.entermedia.savedqueries.SavedQueryManager;
import org.openedit.util.DateStorageUtil;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.SearchQuery;


public class SavedQueryTest extends BaseEnterMediaTest
{
	
	public void testLoad()
	{
		SavedQueryManager manager = (SavedQueryManager)getStaticFixture().getModuleManager().getBean("savedQueryManager");
		WebPageRequest req = getStaticFixture().createPageRequest("/entermedia/catalogs/testcatalog/index.html");
		Collection hits = manager.loadSavedQueryList(req);
		assertNotNull(hits);
		
	}
	
	public void testSave() throws Exception
	{
		SavedQueryManager manager = (SavedQueryManager)getStaticFixture().getModuleManager().getBean("savedQueryManager");
		WebPageRequest req = getStaticFixture().createPageRequest("/entermedia/catalogs/testcatalog/index.html");
		String catalogId = "entermedia/catalogs/testcatalog";
		Searcher assetsearcher = manager.getSearcherManager().getSearcher(catalogId, "asset");
		SearchQuery query = assetsearcher.createSearchQuery();
		query.setName("tester");
		query.addMatches("department","sales");

		Collection hits = manager.loadSavedQueryList(req);

		Data data = manager.saveQuery(catalogId, query, req.getUser());

		Collection newhits = manager.loadSavedQueryList(req);
		assertTrue(newhits.size() ==  hits.size() + 1);
		
		SearchQuery newone = manager.loadSearchQuery(catalogId, data,false, req.getUser());
		assertTrue(newone.getTerms().size() > 0);
	}	
	
	public void testSaveSubQuery() throws Exception
	{
		SavedQueryManager manager = (SavedQueryManager)getStaticFixture().getModuleManager().getBean("savedQueryManager");

		String catalogId = "entermedia/catalogs/testcatalog";
		Searcher assetsearcher = manager.getSearcherManager().getSearcher(catalogId, "asset");
		SearchQuery query = assetsearcher.createSearchQuery();
		
		query.setName("tester");
		query.addMatches("caption","sales");
		query.addMatches("caption","support");

		WebPageRequest req = getStaticFixture().createPageRequest("/entermedia/catalogs/testcatalog/index.html");
		Data data = manager.saveQuery(catalogId, query, req.getUser());
		
		SearchQuery newone = manager.loadSearchQuery(catalogId, data,true,req.getUser());
		assertTrue(newone.getChildren().size() == 1);
		
		SearchQuery subquery = (SearchQuery)newone.getChildren().get(0);
		assertEquals(2, subquery.getTerms().size());

		
		
	}
}
