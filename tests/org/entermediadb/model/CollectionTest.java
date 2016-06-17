package org.entermediadb.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.Category;
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
		
		Data collection = createCollection("test");
		
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

	protected Data createCollection(String inName)
	{
		Searcher csearcher  = getMediaArchive().getSearcher("librarycollection");
		Data collection = csearcher.createNewData();
		//collection.setId("testcollection");
		collection.setName(inName);
		collection.setProperty("library","admin");
		csearcher.saveData(collection, null);
		return collection;
	}
	
	public void testCollectionImport() throws Exception
	{
		ProjectManager manager = (ProjectManager)getFixture().getModuleManager().getBean(getMediaArchive().getCatalogId(),"projectManager");
		
		Searcher librarysearcher = getMediaArchive().getSearcher("library");
		Data data = (Data)librarysearcher.searchById("admin");
		if(data == null)
		{
			data = librarysearcher.createNewData();
			data.setId("admin");
		}
		data.setProperty("folder", "Users/admin");
		librarysearcher.saveData(data, null);
		
		Asset asset = getMediaArchive().getAsset("101");
		Category cat = getMediaArchive().getCategory("testcategory");
		if( cat == null )
		{
			cat = getMediaArchive().getCategoryArchive().createCategoryTree("Projects/SomeStuff/Sub1");
			asset.clearCategories();
			asset.addCategory(cat);
			getMediaArchive().saveAsset(asset, null);
		}
		Data collection = createCollection("testcollection" + new Date());

		WebPageRequest req = getFixture().createPageRequest();
		manager.addCategoryToCollection(req.getUser(), getMediaArchive(), collection.getId(), cat.getParentId() ); //One higher
		//Now make a copy of the asset
		manager.importCollection(req,getMediaArchive(),collection.getId());
		
		boolean foundone = false;
		Collection assets  = manager.loadAssetsInCollection(req, getMediaArchive(), collection.getId());
		String onepath = "Users/admin/" + collection.getName() + "/SomeStuff/Sub1/asf_to_mpeg-1.mpg";
		for (Iterator iterator = assets.iterator(); iterator.hasNext();)
		{
			Data found = (Data) iterator.next();
			if( onepath.equals(found.getSourcePath()) )
			{
				foundone = true;
			}
		}
		assertTrue(foundone);
	}

	public void testCollectionExport() throws Exception
	{
		
	}
	
//	public void testCollectionExport() throws Exception
//	{
//		ProjectManager manager = (ProjectManager)getFixture().getModuleManager().getBean(getMediaArchive().getCatalogId(),"projectManager");
//		
//		String collectionid = "testcollection"; 
//		String libraryid = "testlibray";
//
//		WebPageRequest req = getFixture().createPageRequest();
//		manager.addCategoryToCollection(req.getUser(),getMediaArchive(),collectionid,"testcategory");
//		manager.moveCollectionTo(req,getMediaArchive(),collectionid,libraryid);
//		
//	}
}
