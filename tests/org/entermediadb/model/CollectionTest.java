package org.entermediadb.model;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.projects.ProjectManager;
import org.junit.Test;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.ListHitTracker;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;
import org.openedit.util.FileUtils;

public class CollectionTest extends BaseEnterMediaTest
{
	public void testCollectionEdit() throws Exception
	{
		//getMediaArchive().getSearcher("asset").reIndexAll();
		
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

//	public void testCollectionImport() throws Exception
//	{
//		//getMediaArchive().getSearcher("asset").reIndexAll();
//
//		ProjectManager manager = (ProjectManager)getFixture().getModuleManager().getBean(getMediaArchive().getCatalogId(),"projectManager");
//		
//		Searcher librarysearcher = getMediaArchive().getSearcher("library");
//		Data data = (Data)librarysearcher.searchById("admin");
//		if(data == null)
//		{
//			data = librarysearcher.createNewData();
//			data.setId("admin");
//		}
//		data.setProperty("folder", "Users/admin");
//		librarysearcher.saveData(data, null);
//		
//		Asset asset = getMediaArchive().getAsset("104");
//		assertEquals("users/admin/104", asset.getSourcePath() );
//		Category cat = getMediaArchive().getCategory("testcategory");
//		if( cat == null )
//		{
//			cat = getMediaArchive().getCategorySearcher().createCategoryPath("Projects/SomeStuff/Sub1");
//		}
//		asset.clearCategories();
//		asset.addCategory(cat);
//		getMediaArchive().saveAsset(asset, null);
//
//		Data collection = createCollection("testcollection" + new Date());
//
//		WebPageRequest req = getFixture().createPageRequest();
//		manager.addCategoryToCollection(req.getUser(), getMediaArchive(), collection.getId(), cat.getParentId() ); //One higher
//		//Now make a copy of the asset
//		
//		manager.exportCollection(getMediaArchive(), collection.getId(), );
//
//		
////		manager.importCollection(req,getMediaArchive(),collection.getId());
//		
//		boolean foundone = false;
//		Collection assets  = manager.loadAssetsInCollection(req, getMediaArchive(), collection.getId());
//		String onepath = "Users/admin/" + collection.getName() + "/SomeStuff/Sub1/104";
//		for (Iterator iterator = assets.iterator(); iterator.hasNext();)
//		{
//			Data found = (Data) iterator.next();
//			System.out.println(found.getSourcePath());
//			if( onepath.equals(found.getSourcePath()) )
//			{
//				assertNotSame("104",found.getId());
//				foundone = true;
//			}
//		}
//		assertTrue(foundone);
//
//		Data newlibrary = (Data)librarysearcher.searchById("approved");
//		if(newlibrary == null)
//		{
//			newlibrary = librarysearcher.createNewData();
//			newlibrary.setId("approved");
//		}
//		newlibrary.setProperty("folder", "Archive/2016");
//		librarysearcher.saveData(newlibrary, null);
//		manager.exportCollectionTo(req, getMediaArchive(), collection.getId(), newlibrary.getId());
//		
////		"/entermedia-server/webapp/WEB-INF/data/entermedia/catalogs/testcatalog/originals/Archive/2016/testcollectionFri Jun 17 12:51:17 EDT 2016DT 2016/SomeStuff/Sub1/104/im.tiff"
//		Page moved = getPage("/WEB-INF/data/entermedia/catalogs/testcatalog/originals/Archive/2016/" + collection.getName() + "/SomeStuff/Sub1/104/im.tiff");
//		System.out.println(moved.getContentItem().getAbsolutePath());
//		assertTrue(moved.exists());
//		
//	}
	
	@Test
//	public void testCollectionAssets() throws Exception
//	{
//		ProjectManager manager = (ProjectManager)getFixture().getModuleManager().getBean(getMediaArchive().getCatalogId(),"projectManager");
//		
//		WebPageRequest req = getFixture().createPageRequest("/testcatalog/index.html");
//		Searcher lsearcher = getMediaArchive().getSearcher("library");
//		Data library = lsearcher.createNewData();
//		library.setId("testlibrary");
//		library.setName("Test");
//		lsearcher.saveData(library, null);
//		
//		ProfileModule module = (ProfileModule)getFixture().getModuleManager().getBean("ProfileModule");
//		
//		module.loadUserProfile(req);
//		
//		req.getUserProfile().setProperty("last_selected_library", "testlibrary");
//		
//		Searcher lcsearcher = getMediaArchive().getSearcher("librarycollection");
//		Data collection = lcsearcher.createNewData();
//		collection.setId("testcollection");
//		collection.setName("Movie");
//		collection.setProperty("library", "testlibrary");
//		lcsearcher.saveData(collection, null);
//
//
//		int beforecount = 0;
//		Collection<UserCollection> lessfiles = manager.loadCollections(req, getMediaArchive());
//		if( lessfiles != null && lessfiles.size() > 0)
//		{
//			UserCollection hit = lessfiles.iterator().next();
//			beforecount = hit.getAssetCount();
//		}
//		
//		Searcher lcasearcher = getMediaArchive().getSearcher("librarycollectionasset");
//		Data collectionasset = lcasearcher.createNewData();
//		collectionasset.setProperty("asset", "101");
//		collectionasset.setProperty("librarycollection", "testcollection");
//		lcasearcher.saveData(collectionasset, null);
//
//		Collection<UserCollection> files = manager.loadCollections(req, getMediaArchive());
//		assertNotNull( files );
//		assertEquals( files.size(), 1);
//		UserCollection hit = files.iterator().next();
//		
//		assertEquals(beforecount + 1, hit.getAssetCount());
//		
//		
//	}
	

	public void testSnapshotAndImportCategories(){

		MediaArchive archive = getMediaArchive();
		archive.getAssetSearcher().reIndexAll();
		
		ProjectManager manager = (ProjectManager)getFixture().getModuleManager().getBean(archive.getCatalogId(), "projectManager");

//		AssetUtilities utils = getMediaArchive().getAssetImporter().getAssetUtilities();
//		Category root = getMediaArchive().getCategoryArchive().getRootCategory();
		String folder = "/myexportfolder";
//		utils.exportCategoryTree(getMediaArchive(),root, folder);

		
		
		Page samples = archive.getPageManager().getPage("/entermedia/catalogs/testcatalog/importfolder/");
		Page setup = archive.getPageManager().getPage("/WEB-INF/data/entermedia/catalogs/testcatalog/originals/importfolder");
		archive.getPageManager().copyPage(samples, setup);
		Asset existingasset = archive.getAsset("106");
		ContentItem item = archive.getAssetManager().getOriginalContent(existingasset);
		InputStream input = item.getInputStream();
		try
		{
			String md5 = DigestUtils.md5Hex( input );
			existingasset.setValue("md5hex", md5);
		}
		catch ( Exception ex)
		{
			throw new OpenEditException(ex);
		}
		finally
		{
			FileUtils.safeClose(input);
		}
		archive.saveAsset(existingasset, null);
		WebPageRequest inReq = getFixture().createPageRequest("/testcatalog/index.html");
		User user = inReq.getUser();
		getFixture().getEngine().executePathActions(inReq);
		user = inReq.getUser();
		Data library = manager.loadUserLibrary(archive, inReq.getUserProfile());
		
		
		
		Searcher lcsearcher = getMediaArchive().getSearcher("librarycollection");
		Data collection = lcsearcher.createNewData();
		collection.setId("testcollection");
		collection.setName("Movie");
		collection.setValue("library", library.getId());
		lcsearcher.saveData(collection);
		HitTracker assets = archive.getAssetSearcher().fieldSearch("id","101");
		manager.addAssetToCollection(archive, collection.getId(), assets);
		
		//Add tree (This is for an eventual export test)
		Category cat = archive.getCategorySearcher().createCategoryPath("/my/stuff/here");
		Asset other = archive.getAsset("102");
		other.addCategory(cat);
		archive.getCategorySearcher().saveData(cat);
		archive.saveAsset(other,null);
		manager.addCategoryToCollection(null, archive, collection.getId(), cat.getParentId());		
		
	/*	
		//Import a new path
		WebPageRequest req = getFixture().createPageRequest();
//		manager.downloadCollectionToClient(getMediaArchive(), collection.getId(), folder);

		//Make sure we got the same asset as 106
		Category newrootcategory = manager.getRootCategory(archive, collection.getId());

//		HitTracker assetcount = archive.getAssetSearcher().query().match("category", newrootcategory.getId()).search();
//		assertEquals(assetcount.size(), 6);
		
		//This tests matching on MD5
		Data found =  archive.getAssetSearcher().query().match("category", newrootcategory.getId()).match("id", "102").searchOne();
		assertNotNull(found);
		//This tests a new file 
		
		manager.importCollection(req, req.getUser(), archive, collection.getId(), folder, "Unit Test");
		
		newrootcategory = manager.getRootCategory(archive, collection.getId());
		found =  archive.getAssetSearcher().query().match("category", newrootcategory.getId()).searchOne();
		assertNotNull(found);
		
//		found =  archive.getAssetSearcher().query().match("category", newrootcategory.getId()).match("name", "niceday.wmv").searchOne();
//		assertNotNull(found);
//		
//		found =  archive.getAssetSearcher().query().match("category", newrootcategory.getId()).match("description", "bones.jpg").searchOne();
//		assertNotNull(found);
		

		//manager.importCollection(req, req.getUser(), archive, collection.getId(), "/WEB-INF/data/entermedia/catalogs/testcatalog/originals/importfolder" ,"Some Note" );
	

*/
		
	}
	
	
	

	
}
