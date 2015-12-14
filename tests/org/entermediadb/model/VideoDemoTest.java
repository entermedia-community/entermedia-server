package org.entermediadb.model;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.ConvertStatusModule;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.page.Page;

public class VideoDemoTest extends BaseEnterMediaTest
{

	public void testConversion() throws Exception
	{
		MediaArchive archive = getMediaArchive();
		archive.getAssetSearcher().reIndexAll();
		
		Searcher taskSearcher = getMediaArchive().getSearcherManager().getSearcher("entermedia/catalogs/testcatalog", "conversiontask");
		taskSearcher.deleteAll(null);
		
		Asset asset = archive.getAssetBySourcePath("users/admin/101");
		
		//select a random preset
		Searcher presetSearcher = getMediaArchive().getSearcherManager().getSearcher("entermedia/catalogs/testcatalog","conversions/convertpresets");
		Data preset = (Data)presetSearcher.searchById("1");
		
		
		//kick off the preset conversion with a destination id
		WebPageRequest req = (WebPageRequest)getFixture().createPageRequest("/entermedia/catalogs/testcatalog/index.html");
		req.setRequestParameter("sourcepath", asset.getSourcePath());
		req.setRequestParameter("preset", preset.getId());
		
		ConvertStatusModule module = (ConvertStatusModule)getFixture().getModuleManager().getModule("ConvertStatusModule");
		
		module.addConvertRequest(req); //this should kick off the groovy event by firing a path event?

		Thread.sleep(1000);
		
		String destinationFile = preset.get("outputfile");
				
		Page output = getPage("/WEB-INF/data/entermedia/catalogs/testcatalog/generated/" + asset.getSourcePath() + "/" + destinationFile);
		assertTrue( output.exists());

		Thread.sleep(1000);
		Page published = getPage("/WEB-INF/publish/entermedia/catalogs/testcatalog/" + asset.getSourcePath() + "/" + destinationFile);
		assertTrue( published.exists());

	}
	public void testRhozet() throws Exception
	{
		MediaArchive archive = getMediaArchive();
		archive.getAssetSearcher().reIndexAll();
		Asset asset = archive.getAssetBySourcePath("users/admin/101");
		
		//select a random preset
		Searcher presetsearcher  = getMediaArchive().getSearcherManager().getSearcher("entermedia/catalogs/testcatalog","conversions/convertpresets");
		Data preset = (Data) presetsearcher.searchById("rhozet-test");
		
		
		//kick off the preset conversion with a destination id
		WebPageRequest req = (WebPageRequest)getFixture().createPageRequest("/entermedia/catalogs/testcatalog/index.html");
		req.setRequestParameter("sourcepath", asset.getSourcePath());
		req.setRequestParameter("preset", "rhozet-test");
		
		ConvertStatusModule module = (ConvertStatusModule)getFixture().getModuleManager().getModule("ConvertStatusModule");
		
		module.addConvertRequest(req); //this should kick off the groovy event by firing a path event?

		Thread.sleep(1000);
		
		String destinationFile = preset.get("outputfile");
				
		Page output = getPage("/WEB-INF/data/entermedia/catalogs/testcatalog/generated/" + asset.getSourcePath() + "/" + destinationFile);
		assertTrue( output.exists());
		
	}
	public void testPublishing() throws Exception
	{
		//select a random destination
		//HitTracker destinations = getMediaArchive().getSearcherManager().getList("entermedia/catalogs/testcatalog","publishing/publishdestinations");
		//Data destination = destinations.get(2);
		
		
		//String destinationpath = destination.get("destinationsourcepath");
		//Page output = getPage("/WEB-INF/data/entermedia/catalogs/testcatalog/generated/" + asset.getSourcePath() + "/" + destinationFile);
		//assertTrue( output.exists());
	}
	
	
	public void testOrdersAndConversions() throws Exception
	{
		//select a random destination
		//HitTracker destinations = getMediaArchive().getSearcherManager().getList("entermedia/catalogs/testcatalog","publishing/publishdestinations");
		//Data destination = destinations.get(2);
		
		
		//String destinationpath = destination.get("destinationsourcepath");
		//Page output = getPage("/WEB-INF/data/entermedia/catalogs/testcatalog/generated/" + asset.getSourcePath() + "/" + destinationFile);
		//assertTrue( output.exists());
	}
	
	
}
