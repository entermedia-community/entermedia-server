package org.entermediadb.model;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.convert.ConversionManager;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.entermediadb.asset.convert.TranscodeTools;
import org.entermediadb.events.PathEventManager;
import org.joda.time.convert.ConverterManager;
import org.openedit.WebPageRequest;
import org.openedit.page.Page;
import org.openedit.util.PathUtilities;

public class VideoConvertionTest extends BaseEnterMediaTest
{
	public void testMpegToFlv()
	{
		MediaArchive archive = getMediaArchive("entermedia/catalogs/testcatalog");
		TranscodeTools manager = archive.getTranscodeTools();
		
		ConvertInstructions instructions = new ConvertInstructions(archive);
		instructions.setForce(true);
		instructions.setAssetSourcePath("users/admin/101");
		instructions.setOutputExtension("flv");

		ConversionManager videotool = manager.getManagerByFileFormat("flv");
		ConvertResult result = videotool.createOutput(instructions);
		
		assertNotNull(result.isComplete());
		assertTrue(result.getOutput().exists());
		assertTrue(result.getOutput().getLength() > 0);
		assertEquals("flv", PathUtilities.extractPageType(result.getOutput().getPath()));
	}

	public void testAviToFlv()
	{

		MediaArchive archive = getMediaArchive("entermedia/catalogs/testcatalog");
		TranscodeTools manager = archive.getTranscodeTools();

		ConvertInstructions instructions = new ConvertInstructions(archive);
	//	instructions.setForce(true);
		instructions.setAssetSourcePath("users/admin/103");
		instructions.setOutputExtension("flv");

		ConversionManager videotool = manager.getManagerByFileFormat("flv");
		ConvertResult result = videotool.createOutput(instructions);
		
		assertNotNull(result.isComplete());
		assertTrue(result.getOutput().exists());
		assertTrue(result.getOutput().getLength() > 0);
		assertEquals("flv", PathUtilities.extractPageType(result.getOutput().getPath()));

	}

//	public void testWmvToFlv()
//	{
//
//		MediaArchive archive = getMediaArchive("entermedia/catalogs/testcatalog");
//		TranscodeTools manager = archive.getTranscodeTools();
//		
//		ConvertInstructions instructions = new ConvertInstructions(archive);
//		instructions.setForce(true);
//		instructions.setAssetSourcePath("users/admin/102");
//		instructions.setOutputExtension("flv");
//
//		Page converted = manager.createOutput(instructions);
//		assertNotNull(converted);
//		assertTrue(converted.exists());
//		assertTrue(converted.length() > 0);
//		assertEquals("flv", PathUtilities.extractPageType(converted.getPath()));
//	}

	
	public void testMpegToJpeg()
	{
		MediaArchive archive = getMediaArchive("entermedia/catalogs/testcatalog");
		TranscodeTools manager = archive.getTranscodeTools();

		ConversionManager videotool = manager.getManagerByFileFormat("mp4");
		Asset asset = archive.getAsset("101");
		assertTrue("Asset is missing",asset != null);
		ConvertInstructions instructions1 = videotool.createInstructions(asset, "video.mp4");
		instructions1.setForce(true);
		
		ConvertResult result = videotool.createOutput(instructions1);
		
		assertNotNull(result.isComplete());
		assertTrue(result.getOutput().exists());
		assertTrue(result.getOutput().getLength() > 0);
		assertEquals("mp4", PathUtilities.extractPageType(result.getOutput().getPath()));

		ConvertInstructions instructions = videotool.createInstructions(asset, "image1024x768.jpg");
	//	instructions.setForce(true);
		//instructions.setAssetSourcePath("users/admin/101");

		result = videotool.createOutput(instructions);
		assertNotNull(result.isComplete());
		assertTrue(result.getOutput().exists());
		assertTrue(result.getOutput().getLength() > 0);
		assertEquals("jpg", PathUtilities.extractPageType(result.getOutput().getPath()));

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
