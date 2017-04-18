package org.entermedia.elasticsearch.searchers;

import java.util.ArrayList;
import java.util.List;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.EnterMedia;
import org.entermediadb.elasticsearch.searchers.BaseElasticSearcher;
import org.entermediadb.projects.ProjectManager;
import org.junit.Test;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.FilterNode;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.ListHitTracker;

public class CollectionTest  extends BaseEnterMediaTest
{
	public void testVerifyConfiguration()
	{
		BaseElasticSearcher searcher = (BaseElasticSearcher)getMediaArchive().getSearcherManager().getSearcher("entermedia/catalogs/testcatalog", "librarycollectionasset");
		assertNotNull("asset searcher is NULL!", searcher);
	}
	public void testCollectionEdit() throws Exception
	{
		Searcher csearcher  = getMediaArchive().getSearcher("librarycollection");
		Data collection = csearcher.createNewData();
		collection.setName("test");
		csearcher.saveData(collection, null);
		
		Searcher lsearcher  = getMediaArchive().getSearcher("librarycollectionasset");
		ProjectManager manager = (ProjectManager)getFixture().getModuleManager().getBean(getMediaArchive().getCatalogId(),"projectManager");

		Asset found = getMediaArchive().getAsset("101");
		
		ListHitTracker tracker = new ListHitTracker();
		tracker.add(found);
		manager.addAssetToCollection(getMediaArchive(), collection.getId(), tracker);
		//lsearcher.flush();
		//getMediaArchive().getAssetSearcher().flush();
		
		WebPageRequest req = getFixture().createPageRequest();
		HitTracker assets = manager.loadAssetsInCollection(req, getMediaArchive(), collection.getId(),null);
		assertEquals(1, assets.size());
		
	}
	
	@Test
	public void testCreateNewData()
	{
		//getMediaArchive().getAssetSearcher().reIndexAll();
		
		Asset newasset = new Asset(getMediaArchive());
		newasset.setId("101");
		newasset.setName("Test 101");
		newasset.setSourcePath("users/101");
		getMediaArchive().saveAsset(newasset, null);

		newasset = new Asset(getMediaArchive());
		newasset.setId("102");
		newasset.setName("Test 102");
		newasset.setSourcePath("users/102");
		getMediaArchive().saveAsset(newasset, null);

		
		Searcher lsearcher = getMediaArchive().getSearcherManager().getSearcher("entermedia/catalogs/testcatalog", "library");
		Data data = lsearcher.createNewData();
		data.setId("library101"); //mixed case
		data.setName("Library 101");
		lsearcher.saveData(data, null);
		
		Searcher lcsearcher = getMediaArchive().getSearcherManager().getSearcher("entermedia/catalogs/testcatalog", "librarycollection");
		data = lcsearcher.createNewData();
		data.setId("libraryCollection101"); //mixed case
		data.setName("Collection 101");
		data.setProperty("library", "library101");
		lcsearcher.saveData(data, null);

		BaseElasticSearcher lcasearcher = (BaseElasticSearcher)getMediaArchive().getSearcherManager().getSearcher("entermedia/catalogs/testcatalog", "librarycollectionasset");
		lcasearcher.reIndexAll();
		Data lca = lcasearcher.createNewData();
		lca.setProperty("librarycollection", "libraryCollection101");
		lca.setProperty("asset", "101");
		lcasearcher.saveData(lca, null);
		
		lca = lcasearcher.createNewData();
		lca.setProperty("librarycollection", "libraryCollection101");
		lca.setProperty("asset", "102");
		lcasearcher.saveData(lca, null);
		
		
//		Searcher collectionassetsearcher = getSearcherManager().getSearcher(getCatalogId(),"librarycollectionasset");
		
		//Build list of ID's
		List ids = new ArrayList();
		ids.add("libraryCollection101");
		
		HitTracker collectionassets = lcasearcher.query().orgroup("librarycollection",ids).named("sidebar").search(); 
		FilterNode collectionhits = collectionassets.findFilterNode("librarycollection");
		int assetcount = collectionhits.getCount("libraryCollection101");
		assertTrue( assetcount > 0);
		//Now get the filters
		
	}

	public EnterMedia getEnterMedia(String inApplicationId)
	{
		EnterMedia media = (EnterMedia)getStaticFixture().getModuleManager().getBean(inApplicationId, "enterMedia");
		media.setApplicationId(inApplicationId);
		return media;
	}
}
