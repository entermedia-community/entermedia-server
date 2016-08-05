package org.entermediadb.data;

import java.io.File;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.AssetSyncModule;
import org.openedit.BaseTestCase;
import org.openedit.WebPageRequest;

public class CreateUploadAssetTest extends BaseTestCase
{
	public void testCreateUploads() throws Exception 
	{
		AssetSyncModule mod = (AssetSyncModule)getFixture().getModuleManager().getModule("AssetSyncModule");
		WebPageRequest req = getFixture().createPageRequest("/entermedia/catalogs/testcatalog/index.html");

		MediaArchive archive = mod.getMediaArchive(req);
		Asset asset = archive.getAssetBySourcePath("newassets/admin/testassets/test1.jpg");
		if( asset != null)
		{
			archive.getAssetSearcher().delete(asset, req.getUser());
		}
		
		File testasset = new File( getRoot().getParentFile().getAbsolutePath() + "/etc/testassets/test1.jpg");
		
		req.setRequestParameter("localfilepath", new String[]{testasset.getAbsolutePath()});
		req.setRequestParameter("parentpath", new String[]{"/testassets"});
		req.setRequestParameter("filesize", new String[]{String.valueOf( testasset.length() )});
		
		mod.createAssetFromLocalPaths(req);
		
		asset = archive.getAssetBySourcePath("users/admin/testassets/test1.jpg");
		assertNotNull(asset);
		
		assertEquals(asset.get("uploadstatus"), "pending");
		
		
	}
	
}
