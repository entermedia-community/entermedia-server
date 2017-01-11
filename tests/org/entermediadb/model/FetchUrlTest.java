package org.entermediadb.model;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.MediaArchive;
import org.openedit.WebPageRequest;
import org.openedit.page.Page;


public class FetchUrlTest extends BaseEnterMediaTest
{
	public void testYouTube() throws Exception
	{
		String url = "http://www.youtube.com/watch?v=EEWE65hKQTo";
		MediaArchive archive = getMediaArchive();

		String basepath = archive.getCatalogHome() + "/data/originals/users/admin/youtube.com/EEWE65hKQTo";
		Page original = getPage(basepath + "/imported.flv"); //attachment
		getFixture().getPageManager().removePage(original);

		WebPageRequest req = getFixture().createPageRequest(archive.getCatalogHome() + "/data/originals");
		///This API should fire an event:
		Asset asset = getMediaArchive().getAssetImporter().createAssetFromFetchUrl(archive, url, req.getUser(),"some/sourcepath",  "testfile.mp4", null);
		assertEquals( asset.getSourcePath(), "users/admin/youtube.com/EEWE65hKQTo");

		assertFalse(original.exists()); //Should not exists until the event fires
		
		//The event should run this API
		//getMediaArchive().getAssetImporter().fetchMediaForAsset(archive, asset, req.getUser());
		Thread.sleep(5000);

		original = getPage(basepath + "/imported.flv"); //attachment
		assertTrue(original.exists());
		
		assertEquals("Sony Vegas Pro 8 - Text Effect 3D", asset.get("title"));
		
		Page thumb = getPage( basepath + "/thumb.jpg");
		assertTrue(thumb.exists());

		//they are all attachments to the main folder
	}
}
