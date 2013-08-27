package org.openedit.entermedia.data;

import java.io.File;

import org.openedit.Data;
import org.openedit.data.BaseData;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.data.XmlFileSearcher;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.BaseEnterMediaTest;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.modules.AssetEditModule;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;

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
		XmlFileSearcher searcher = getNewSearcher(catalog, "usagehistory");
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
		XmlFileSearcher searcher = getNewSearcher("entermedia/catalogs/testcatalog", "reviewers");
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
		
		searcher.deleteRecord(data);
		after = searcher.search(query);
		assertNotNull(after);
		assertEquals(before.size(), after.size());
	}

	protected XmlFileSearcher getNewSearcher(String inCatalogId, String inSearchType)
	{
		XmlFileSearcher searcher = (XmlFileSearcher) getBean("xmlFileSearcher");
		searcher.setCatalogId(inCatalogId);
		searcher.setSearchType(inSearchType);
		return searcher;
	}
	
	/*
	 * 3. A way to store Asset metadata in XML files instead of xconf files
	 */
	public void xxxtestAssetArchiving() throws Exception
	{
		Asset asset = new Asset();
		String catalog = "entermedia/catalogs/test";
		asset.setCatalogId(catalog);
		String sourcepath = "another/new/sourcepath";
		asset.setSourcePath(sourcepath);
		MediaArchive archive = getMediaArchive(catalog);
		asset.addCategory(archive.getCategoryArchive().getRootCategory());
		asset.addKeyword("test");
		asset.setFolder(false);
		asset.setName("xml");
		
		XmlFileSearcher searcher = getNewSearcher(catalog, "asset");
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
		newxml.setProperty("notes", "Here are my notes.< ");
		newxml.setProperty("notes", "Here are my notes. >");
		newxml.setProperty("notes", "Here are my notes.\nFull of");
		xmlsearcher.saveData(newxml, null);
		xmlsearcher.clearIndex();
		
		Data newlines = (Data)xmlsearcher.searchById(newxml.getId());
		String lines = newlines.get("notes");
		assertEquals(  "Here are my notes.\nFull of", lines );
		
	}
}
