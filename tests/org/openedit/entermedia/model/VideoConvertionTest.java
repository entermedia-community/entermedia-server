package org.openedit.entermedia.model;

import org.openedit.entermedia.BaseEnterMediaTest;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.creator.ConvertInstructions;
import org.openedit.entermedia.creator.CreatorManager;
import org.openedit.events.PathEventManager;

import com.openedit.WebPageRequest;
import com.openedit.page.Page;
import com.openedit.util.PathUtilities;

public class VideoConvertionTest extends BaseEnterMediaTest
{
	public void testMpegToFlv()
	{
		ConvertInstructions instructions = new ConvertInstructions();
		instructions.setForce(true);
		instructions.setAssetSourcePath("users/admin/101");
		instructions.setOutputExtension("flv");

		MediaArchive archive = getMediaArchive("entermedia/catalogs/testcatalog");
		CreatorManager manager = archive.getCreatorManager();

		Page converted = manager.createOutput(instructions);
		assertNotNull(converted);
		assertTrue(converted.exists());
		assertTrue(converted.length() > 0);
		assertEquals("flv", PathUtilities.extractPageType(converted.getPath()));
	}

	public void testAviToFlv()
	{
		ConvertInstructions instructions = new ConvertInstructions();
		instructions.setForce(true);
		instructions.setAssetSourcePath("users/admin/103");
		instructions.setOutputExtension("flv");

		MediaArchive archive = getMediaArchive("entermedia/catalogs/testcatalog");
		CreatorManager manager = archive.getCreatorManager();

		Page converted = manager.createOutput(instructions);
		assertNotNull(converted);
		assertTrue(converted.exists());
		assertTrue(converted.length() > 0);
		assertEquals("flv", PathUtilities.extractPageType(converted.getPath()));
	}

	public void testWmvToFlv()
	{
		ConvertInstructions instructions = new ConvertInstructions();
		instructions.setForce(true);
		instructions.setAssetSourcePath("users/admin/102");
		instructions.setOutputExtension("flv");

		MediaArchive archive = getMediaArchive("entermedia/catalogs/testcatalog");
		CreatorManager manager = archive.getCreatorManager();

		Page converted = manager.createOutput(instructions);
		assertNotNull(converted);
		assertTrue(converted.exists());
		assertTrue(converted.length() > 0);
		assertEquals("flv", PathUtilities.extractPageType(converted.getPath()));
	}

	public void testMpegToJpeg()
	{
		ConvertInstructions instructions = new ConvertInstructions();
		instructions.setForce(true);
		instructions.setAssetSourcePath("users/admin/101");
		instructions.setOutputExtension("jpg");

		MediaArchive archive = getMediaArchive("entermedia/catalogs/testcatalog");
		CreatorManager manager = archive.getCreatorManager();

		Page converted = manager.createOutput(instructions);
		assertNotNull(converted);
		assertTrue(converted.exists());
		assertTrue(converted.length() > 0);
		assertEquals("jpg", PathUtilities.extractPageType(converted.getPath()));
	}

	/* We dont do this any more
	public void testCreateAllFlash() throws Exception
	{
		PathEventManager manager = (PathEventManager) getFixture().getModuleManager().getBean("entermedia/catalogs/testcatalog", "pathEventManager");
		WebPageRequest req = getFixture().createPageRequest("/entermedia/catalogs/testcatalog/index.html");

		//TODO: Check for flash video before and after running this event
		Page flash = getPage("/entermedia/catalogs/testcatalog/assets/users/admin/101/video.flv");
		getFixture().getPageManager().removePage(flash);
		assertTrue(!flash.exists());
		manager.runPathEvent("/entermedia/catalogs/testcatalog/events/makeflashpreviews.html", req);
		req.getPageValue("mediaArchive");
		flash = getPage("/entermedia/catalogs/testcatalog/assets/users/admin/101/video.mp4");
		assertTrue(flash.exists());
	}
	*/

	public void xtestCreateAllWatermarks() throws Exception
	{
		PathEventManager manager = (PathEventManager) getFixture().getModuleManager().getBean("entermedia/catalogs/testcatalog", "pathEventManager");
		WebPageRequest req = getFixture().createPageRequest("/entermedia/catalogs/testcatalog/index.html");

		Page watermark = getPage("/entermedia/catalogs/testcatalog/assets/users/admin/101/image150x150wm.jpg");
		getFixture().getPageManager().removePage(watermark);
		manager.runPathEvent("/entermedia/catalogs/testcatalog/events/makewatermarks.html", req);
		req.getPageValue("mediaArchive");
		watermark = getPage("/entermedia/catalogs/testcatalog/assets/users/admin/101/image150x150wm.jpg");
		assertTrue(watermark.exists());

	}

	/** We dont do this any more
	public void testPublishOneLocation() throws Exception
	{

		PathEventManager manager = (PathEventManager) getFixture().getModuleManager().getBean("entermedia/catalogs/testcatalog", "pathEventManager");
		WebPageRequest req = getFixture().createPageRequest("/entermedia/catalogs/testcatalog/index.html");

		Page watermark = getPage("/entermedia/catalogs/testcatalog/publishing/akamia/" + 101 + ".flv");
		getFixture().getPageManager().removePage(watermark);
		req.setRequestParameter("sourcepath", "users/admin/101/");
		req.setRequestParameter("locationid", "akamia");
		req.setRequestParameter("forced", "true");
		manager.runPathEvent("/entermedia/catalogs/testcatalog/events/publishasset.html", req);
		watermark = getPage("/entermedia/catalogs/testcatalog/publishing/akamia/" + 101 + ".flv");
		assertTrue(watermark.exists());
	}
	*/
}
