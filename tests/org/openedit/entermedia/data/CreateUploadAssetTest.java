package org.openedit.entermedia.data;

import java.io.File;

import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.modules.AssetSyncModule;

import com.openedit.BaseTestCase;
import com.openedit.WebPageRequest;

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
		
		assertEquals(asset.getProperty("uploadstatus"), "pending");
		
		
	}
	
}
