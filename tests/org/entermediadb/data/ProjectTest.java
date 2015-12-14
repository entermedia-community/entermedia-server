package org.entermediadb.data;

import java.util.Collection;

import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.modules.ProfileModule;
import org.entermediadb.projects.ProjectManager;
import org.entermediadb.projects.UserCollection;
import org.junit.Test;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;

public class ProjectTest extends BaseEnterMediaTest
{
	@Test
	public void testCollectionAssets() throws Exception
	{
		ProjectManager manager = (ProjectManager)getFixture().getModuleManager().getBean(getMediaArchive().getCatalogId(),"projectManager");
		
		WebPageRequest req = getFixture().createPageRequest("/testcatalog/index.html");
		Searcher lsearcher = getMediaArchive().getSearcher("library");
		Data library = lsearcher.createNewData();
		library.setId("testlibrary");
		library.setName("Test");
		lsearcher.saveData(library, null);
		
		ProfileModule module = (ProfileModule)getFixture().getModuleManager().getBean("ProfileModule");
		
		module.loadUserProfile(req);
		
		req.getUserProfile().setProperty("last_selected_library", "testlibrary");
		
		Searcher lcsearcher = getMediaArchive().getSearcher("librarycollection");
		Data collection = lcsearcher.createNewData();
		collection.setId("testcollection");
		collection.setName("Movie");
		collection.setProperty("library", "testlibrary");
		lcsearcher.saveData(collection, null);


		int beforecount = 0;
		Collection<UserCollection> lessfiles = manager.loadCollections(req);
		if( lessfiles != null && lessfiles.size() > 0)
		{
			UserCollection hit = lessfiles.iterator().next();
			beforecount = hit.getAssetCount();
		}
		
		Searcher lcasearcher = getMediaArchive().getSearcher("librarycollectionasset");
		Data collectionasset = lcasearcher.createNewData();
		collectionasset.setProperty("asset", "101");
		collectionasset.setProperty("librarycollection", "testcollection");
		lcasearcher.saveData(collectionasset, null);

		Collection<UserCollection> files = manager.loadCollections(req);
		assertNotNull( files );
		assertEquals( files.size(), 1);
		UserCollection hit = files.iterator().next();
		
		assertEquals(beforecount + 1, hit.getAssetCount());
		
		
	}
	
}