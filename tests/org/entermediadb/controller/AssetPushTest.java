package org.entermediadb.controller;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.AssetSyncModule;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;

public class AssetPushTest extends BaseEnterMediaTest
{

	public void testPushAsset() throws Exception
	{
		AssetSyncModule mod = (AssetSyncModule)getFixture().getModuleManager().getModule("AssetSyncModule");
		WebPageRequest req = getFixture().createPageRequest("/entermedia/catalogs/testcatalog/events/sync/pushassets.html");

		MediaArchive archive = mod.getMediaArchive(req);
		Asset target = archive.getAssetBySourcePath("users/admin/101");
		Searcher pushsearcher = archive.getSearcherManager().getSearcher("entermedia/catalogs/testcatalog", "pushrequest");
		Data request = pushsearcher.createNewData();
		request.setId("testpush");
		request.setProperty("assetid", target.getId());
		request.setProperty("status", "pending");
		request.setSourcePath(target.getSourcePath());
		request.setProperty("sourcefolder", "test");
		
		Searcher hot =  archive.getSearcherManager().getSearcher("entermedia/catalogs/testcatalog", "hotfolder");
		Data local = (Data) hot.searchById("test");
		if(local == null){
			local = hot.createNewData();
		}
		local.setId("test");
		local.setProperty("auto", "true");
		request.setProperty("hotfolder", "test");
		local.setProperty("convertpreset", "original largeimage thumbimage");
		hot.saveData(local, null);
		
		pushsearcher.saveData(request, null);
	
		getFixture().getEngine().executePageActions(req);
		getFixture().getEngine().executePathActions(req);
		
		
		request = (Data) pushsearcher.searchById("testpush");
		assertEquals("status", "complete");
		
		
	}

	

}
