package org.entermediadb.model;

import java.util.ArrayList;
import java.util.List;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.projects.ProjectManager;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.ListHitTracker;

public class CollectionTest extends BaseEnterMediaTest
{
	public void testCollectionEdit() throws Exception
	{
		//getMediaArchive().getSearcher("asset").getAllHits();
		//getMediaArchive().getSearcher("librarycollectionasset").getAllHits();
		
		Searcher csearcher  = getMediaArchive().getSearcher("librarycollection");
		Data collection = csearcher.createNewData();
		collection.setName("test");
		csearcher.saveData(collection, null);
		
		//Searcher lsearcher  = getMediaArchive().getSearcher("librarycollectionasset");
		ProjectManager manager = (ProjectManager)getFixture().getModuleManager().getBean(getMediaArchive().getCatalogId(),"projectManager");

		Asset found = getMediaArchive().getAsset("101");
		assertNotNull(found);
		ListHitTracker tracker = new ListHitTracker();
		tracker.add(found);
		manager.addAssetToCollection(getMediaArchive(), collection.getId(), tracker);

		//getMediaArchive().getAssetSearcher().getAllHits();

//		manager.addAssetToCollection(getMediaArchive(), collection.getId(), tracker);
		List all = new ArrayList();
		all.add(found.getId());
		//Collection existing = lsearcher.query().match("librarycollection", collection.getId()).orgroup("asset", all).search();

		WebPageRequest req = getFixture().createPageRequest();
		HitTracker assets = manager.loadAssetsInCollection(req, getMediaArchive(), collection.getId());
		assertEquals(1, assets.size());
		
	}
}
