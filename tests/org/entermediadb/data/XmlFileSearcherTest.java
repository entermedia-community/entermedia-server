package org.entermediadb.data;

import java.io.File;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.AssetEditModule;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.BaseData;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;

public class XmlFileSearcherTest extends BaseEnterMediaTest
{
	protected SearcherManager getSearcherManager()
	{
		return (SearcherManager) getBean("searcherManager");
	}
	
	/*
	 * 1. HitTracker HistoryModule.getUsageHistory(WebPageRequest)
	 */
	public void testUsageHistory() throws Exception
	{
		String catalog = "entermedia/catalogs/testcatalog";
		MediaArchive archive = getMediaArchive(catalog);
		String sourcepath = "source/path/to/new/asset";
		Asset asset = archive.createAsset(sourcepath);
		archive.saveAsset(asset, null);
		
		Data historyRecord = new BaseData();
		historyRecord.setSourcePath(asset.getSourcePath());
		historyRecord.setProperty("message", "New record");
		historyRecord.setProperty("assetid", asset.getId());
		Searcher searcher = getNewSearcher(catalog, "usagehistory");
		searcher.saveData(historyRecord, null);
		
		SearchQuery idQuery = searcher.createSearchQuery();
		idQuery.addMatches("assetid", asset.getId());
		HitTracker idResults = searcher.search(idQuery);
		
		SearchQuery spQuery = searcher.createSearchQuery();
		searcher.reIndexAll();
		spQuery.addMatches("sourcepath", asset.getSourcePath());
		HitTracker spResults = searcher.search(spQuery);
		
		assertNotNull(idResults);
		assertNotNull(spResults);
		assertEquals(idResults.size(), spResults.size());
		assertTrue(idResults.size() > 0);
		
		searcher.reIndexAll();
		spResults = searcher.search(spQuery);
		assertNotNull(idResults);
		assertNotNull(spResults);
		assertEquals(idResults.size(), spResults.size());
		assertTrue(idResults.size() > 0);
		
		
		
		
		
		Data historyBack = (Data) searcher.searchById(historyRecord.getId());
		assertNotNull(historyBack);
		assertEquals(historyRecord.getId(), historyBack.getId());
		assertEquals(historyRecord.getSourcePath(), historyBack.getSourcePath());
		assertEquals(historyRecord.get("assetid"), historyBack.get("assetid"));
		assertEquals(historyRecord.get("message"), historyBack.get("message"));
	}
	
	/*
	 * High level test for usage history
	 */
	public void testUsageHistoryModule()
	{
		Asset asset = createAsset();
		MediaArchive archive = getMediaArchive();
		archive.saveAsset(asset, null);
		
		WebPageRequest req = getFixture().createPageRequest(archive.getCatalogHome() + "/index.html");
		req.setRequestParameter("assetid", asset.getId());
		req.setRequestParameter("message", "Test usage");
		req.setRequestParameter("projectno", "none");
		req.setRequestParameter("formno", "Form");
		
		AssetEditModule aem = (AssetEditModule) getBean("AssetEditModule");
		aem.saveUsageHistory(req);
		aem.loadUsageHistory(req);
		HitTracker hits = (HitTracker) req.getPageValue("history");
		assertNotNull(hits);
		assertEquals(1, hits.size());
	}
	
	/*
	 * 2. A way to search for Asset Reviewers like we do in State Farm Marketing by passing in a productid
	 */
	public void testAssetReviewers() throws Exception
	{
		Searcher searcher = getNewSearcher("entermedia/catalogs/testcatalog", "reviewers");
		SearchQuery query = searcher.createSearchQuery();
		String sourcepath = "path/to/test/asset"; //Just needs to be unique and might as well match the assetsourcepath
		query.addMatches("sourcepath", sourcepath);
		HitTracker before = searcher.search(query);
		assertNotNull(before);
		
		Data data = new BaseData(); //this is a reviewer record
		data.setId(searcher.nextId()); //Just needs to be unique 
		data.setProperty("assetid", "100");
		data.setProperty("department", "0");
		data.setProperty("contact", "me");
		data.setProperty("datesent", "1/1/2009");
		data.setProperty("status", "0");
		data.setSourcePath(sourcepath); 
		searcher.saveData(data, getFixture().createPageRequest().getUser());
		
		//#1 Old
		//File dataFile = new File(getRoot(), "/entermedia/catalogs/testcatalog/assets/" + sourcepath + "/data/reviewers.xml");
		
		//#2 Hybrid
		File dataFile = new File(getRoot(), "/entermedia/catalogs/testcatalog/data/metadata/" + sourcepath + "/reviewers.xml");
		
		//Replace .xconf usage
		//File dataFile = new File(getRoot(), "/entermedia/catalogs/testcatalog/data/metadata/" + sourcepath + "/properties.xml");

		//Replace .xconf usage
		//File dataFile = new File(getRoot(), "/entermedia/catalogs/testcatalog/data/lists/sizes.xml");

		
		assertTrue(dataFile.exists());
		
		HitTracker after = searcher.search(query);
		assertNotNull(after);
		assertEquals(before.size() + 1, after.size());
		
		searcher.delete(data,null);
		after = searcher.search(query);
		assertNotNull(after);
		assertEquals(before.size(), after.size());
	}

	protected Searcher getNewSearcher(String inCatalogId, String inSearchType)
	{
		Searcher searcher = (Searcher) getBean("Searcher");
		searcher.setCatalogId(inCatalogId);
		searcher.setSearchType(inSearchType);
		return searcher;
	}
	
	/*
	 * 3. A way to store Asset metadata in XML files instead of xconf files
	 */
	public void xxxtestAssetArchiving() throws Exception
	{
		String catalog = "entermedia/catalogs/test";
		MediaArchive archive = getMediaArchive(catalog);
		Asset asset = new Asset(archive);
		//asset.setCatalogId(catalog);
		String sourcepath = "another/new/sourcepath";
		asset.setSourcePath(sourcepath);
		asset.addCategory(archive.getCategoryArchive().getRootCategory());
		asset.addKeyword("test");
		asset.setFolder(false);
		asset.setName("xml");
		
		Searcher searcher = getNewSearcher(catalog, "asset");
		/*
		 * searcher.setIndexer(AssetLuceneIndexer) ?
		 * searcher.setXml{Parser, Processor, Loader, ...}(AssetXmlParser) ?
		 */
		searcher.saveData(asset, null);
		
	}
	
	/*
	 * 4. Save and load Email data? LuceneEmailSearcher
	 */
	public void testLibrarySave()
	{
		Searcher xmlsearcher = getMediaArchive().getSearcher("library");
		Data newxml = xmlsearcher.createNewData();
		newxml.setProperty("notes", "Here are my notes Full of stuf");
		
		newxml.setProperty("notes", "Here are my notes.\"Full");
		newxml.setProperty("notes", "Here are my notes.&");
		xmlsearcher.saveData(newxml, null);
		xmlsearcher.clearIndex();
		
		newxml.setProperty("notes", "Here are my notes.< ");
		newxml.setProperty("notes", "Here are my notes. >");
		xmlsearcher.saveData(newxml, null);
		xmlsearcher.clearIndex();
		
		newxml.setProperty("notes", "Here are my notes.\nFull of");
		xmlsearcher.saveData(newxml, null);
		xmlsearcher.clearIndex();
		
		
		Data newlines = (Data)xmlsearcher.searchById(newxml.getId());
		String lines = newlines.get("notes");
		assertEquals(  "Here are my notes.\nFull of", lines );
		
		newxml.setProperty("notes", "Here are my notes Full of stuf");
		xmlsearcher.saveData(newxml, null);
		xmlsearcher.clearIndex();
		
		newlines = (Data)xmlsearcher.searchById(newxml.getId());
		lines = newlines.get("notes");
		assertEquals(  "Here are my notes Full of stuf", lines );
		
		
	}
}
