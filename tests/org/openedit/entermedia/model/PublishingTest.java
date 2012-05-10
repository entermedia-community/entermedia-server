package org.openedit.entermedia.model;

import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.BaseEnterMediaTest;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.publishing.PublishManager;

import com.openedit.WebPageRequest;

public class PublishingTest extends BaseEnterMediaTest
{

	public void testPublishAssets() throws Exception
	{
		//take an album,copy all the assets, give an ID, send email, save order, clear cart
		WebPageRequest req = getFixture().createPageRequest("/entermedia/catalogs/testcatalog/events/publishassets.html");
		req.setRequestParameter("forced", "true");
		MediaArchive archive = getMediaArchive();
		PublishManager manager = (PublishManager) archive.getModuleManager().getBean(archive.getCatalogId(), "publishManager");
		manager.clearLastRun();
		archive.getAssetSearcher().reIndexAll();
		Searcher publishlocations = archive.getSearcherManager().getSearcher(archive.getCatalogId(), "publishlocations");
		Data location = (Data) publishlocations.searchById("testlocation");
		if(location == null){
			location =publishlocations.createNewData();
		}
		//<publishlocation id="akamia" path="/home/ian/akamia" type="manual" permission="publishtoakamai">Streaming</publishlocation>
		location.setProperty("path", "/test/publishing/folder");
		location.setProperty("type", "automatic");
		location.setProperty("id", "testpublishing");
		location.setProperty("inputfile", "preview.flv");
		publishlocations.saveData(location, req.getUser());
		Asset test = archive.getAsset("101");
		test.setProperty("publishdate", null);
		archive.saveAsset(test, null);
	
		getFixture().getEngine().executePageActions(req);
		getFixture().getEngine().executePathActions(req);
		
		test = archive.getAsset("101");
		assertNotNull(test.get("publishdate"));
		
		
		
	}

	
	public void testSftpPublishing() throws Exception
	{
		//take an album,copy all the assets, give an ID, send email, save order, clear cart
		WebPageRequest req = getFixture().createPageRequest("/entermedia/catalogs/testcatalog/events/publishassets.html");
		req.setRequestParameter("forced", "true");
		MediaArchive archive = getMediaArchive();
		PublishManager manager = (PublishManager) archive.getModuleManager().getBean(archive.getCatalogId(), "publishManager");
		manager.clearLastRun();
		archive.getAssetSearcher().reIndexAll();
		Searcher publishlocations = archive.getSearcherManager().getSearcher(archive.getCatalogId(), "publishlocations");
		Data location = (Data) publishlocations.searchById("testlocation");
		if(location == null){
			location =publishlocations.createNewData();
			location.setId("testlocation");
		}
		//<publishlocation id="akamia" path="/home/ian/akamia" type="manual" permission="publishtoakamai">Streaming</publishlocation>
		location.setProperty("path", "/test/publishing/folder");
		location.setProperty("type", "automatic");
		location.setProperty("id", "sftptest");
		location.setProperty("inputfile", "preview.flv");
		publishlocations.saveData(location, req.getUser());
		Asset test = archive.getAsset("101");
		test.setProperty("publishdate", null);
		archive.saveAsset(test, null);
	
		getFixture().getEngine().executePageActions(req);
		getFixture().getEngine().executePathActions(req);
		
//		test = archive.getAsset("101");
//		assertNotNull(test.get("publishdate"));
		
		
		
	}
	
	
	
	public void testAttachmentPublishing() throws Exception
	{
		//take an album,copy all the assets, give an ID, send email, save order, clear cart
		WebPageRequest req = getFixture().createPageRequest("/entermedia/catalogs/testcatalog/events/publishassets.html");
		req.setRequestParameter("forced", "true");
		MediaArchive archive = getMediaArchive();
		PublishManager manager = (PublishManager) archive.getModuleManager().getBean(archive.getCatalogId(), "publishManager");
		manager.clearLastRun();
		archive.getAssetSearcher().reIndexAll();
		Searcher publishlocations = archive.getSearcherManager().getSearcher(archive.getCatalogId(), "publishlocations");
		Data location = (Data) publishlocations.searchById("testlocation");
		if(location == null){
			location =publishlocations.createNewData();
			location.setId("testlocation");
		}
		//<publishlocation id="akamia" path="/home/ian/akamia" type="manual" permission="publishtoakamai">Streaming</publishlocation>
		location.setProperty("path", "/test/publishing/folder");
		location.setProperty("type", "automatic");
		location.setProperty("id", "sftptest");
		location.setProperty("inputfile", "preview.flv");
		publishlocations.saveData(location, req.getUser());
		Asset test = archive.getAsset("101");
		test.setProperty("publishdate", null);
		archive.saveAsset(test, null);
	
		getFixture().getEngine().executePageActions(req);
		getFixture().getEngine().executePathActions(req);
		
//		test = archive.getAsset("101");
//		assertNotNull(test.get("publishdate"));
		
		
		
	}
	
	
	
}
