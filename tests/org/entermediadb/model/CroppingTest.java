package org.entermediadb.model;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.ConvertStatusModule;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.page.Page;

public class CroppingTest extends BaseEnterMediaTest
{

	public void testConversion() throws Exception
	{
		MediaArchive archive = getMediaArchive();
		archive.getAssetSearcher().reIndexAll();
		
		Searcher taskSearcher = getMediaArchive().getSearcherManager().getSearcher("entermedia/catalogs/testcatalog", "conversiontask");
		taskSearcher.deleteAll(null);
		
		Asset asset = archive.getAssetBySourcePath("users/admin/105");
		
		//select a random preset
		Searcher presetSearcher = getMediaArchive().getSearcherManager().getSearcher("entermedia/catalogs/testcatalog","convertpreset");
		Data preset = (Data)presetSearcher.searchById("largeimage");
		
		
		//kick off the preset conversion with a destination id
		WebPageRequest req = (WebPageRequest)getFixture().createPageRequest("/entermedia/catalogs/testcatalog/index.html");
		
		req.setRequestParameter("sourcepath", asset.getSourcePath());
		req.setRequestParameter("preset", preset.getId());
		req.setRequestParameter("force", "true");
		ConvertStatusModule module = (ConvertStatusModule)getFixture().getModuleManager().getModule("ConvertStatusModule");

		String destinationFile = preset.get("outputfile");
		Page output = getPage("/WEB-INF/data/entermedia/catalogs/testcatalog/generated/" + asset.getSourcePath() + "/" + destinationFile);
		getMediaArchive().getPageManager().removePage(output);
		assertFalse( output.exists());

		
		module.addConvertRequest(req); //this should kick off the groovy event by firing a path event?

		Thread.sleep(1000);
		
				
		 output = getPage("/WEB-INF/data/entermedia/catalogs/testcatalog/generated/" + asset.getSourcePath() + "/" + destinationFile);
		assertTrue( output.exists());

	
		long length = output.getContentItem().getLength();
		
		
		
		req.setRequestParameter("field", new String[] {"x1", "y1", "prefheight", "prefwidth", "force", "crop"});
		req.setRequestParameter("x1.value", "10");
		req.setRequestParameter("y1.value", "10");
		req.setRequestParameter("prefwidth.value", "100");
		req.setRequestParameter("prefheight.value", "100");
		req.setRequestParameter("force.value", "true");
		req.setRequestParameter("crop.value", "true");
		
		module.addConvertRequest(req); //this should kick off the groovy event by firing a path event?
		Thread.sleep(1000);
		
		long length2 = output.getContentItem().getLength();
		assertNotSame(length, length2);

	}
	
	
	
}
