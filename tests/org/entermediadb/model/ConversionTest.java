package org.entermediadb.model;

import java.util.HashMap;
import java.util.Map;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.convert.ConversionManager;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.entermediadb.asset.convert.TranscodeTools;
import org.entermediadb.asset.generators.ConvertGenerator;
import org.openedit.WebPageRequest;
import org.openedit.generators.Output;
import org.openedit.page.Page;

public class ConversionTest extends BaseEnterMediaTest
{
	
	
	//conversion convertion  
	public void testPreset() throws Exception
	{
		MediaArchive archive = getMediaArchive("entermedia/catalogs/testcatalog");
		Asset asset = archive.getAsset("105");
		assertNotNull(asset);
		ConversionManager manager = archive.getTranscodeTools().getManagerByRenderType("image");
		ConvertInstructions instructions = manager.createInstructions(asset, "image1024x769.jpg");				
		ConvertResult result = manager.createOutput(instructions);
		
		assertTrue(result.isOk());
		assertNotNull(result.getOutput());
	}
	public void testWidthHeight() throws Exception
	{
		MediaArchive archive = getMediaArchive("entermedia/catalogs/testcatalog");
		Map settings = new HashMap();
		settings.put("prefwidth", "100");
		settings.put("prefheight", "100");
		ConvertResult result = archive.getTranscodeTools().createOutputIfNeeded(null, settings, "users/admin/105", "video1024x768.jpg");
		assertTrue(result.isOk());
		assertNotNull(result.getOutput());
		//Make sure we save the right final file output
		assertEquals("/WEB-INF/data/entermedia/catalogs/testcatalog/generated/users/admin/105/image100x100.jpg",result.getOutput().getPath(), result.getOutput().getPath() );
		
	}
	public void testVideo() throws Exception
	{
		MediaArchive archive = getMediaArchive("entermedia/catalogs/testcatalog");
		Asset asset = archive.getAsset("101"); //mpg
		assertNotNull(asset);
		ConversionManager manager = archive.getTranscodeTools().getManagerByRenderType("video");
		ConvertInstructions instructions = manager.createInstructions(asset, "video.mp4");
		ConvertResult result = manager.createOutput(instructions);
		
		assertTrue(result.isOk());
		assertNotNull(result.getOutput());
	}
	public void testVideoImageOffset() throws Exception
	{
		MediaArchive archive = getMediaArchive("entermedia/catalogs/testcatalog");
		Asset asset = archive.getAsset("101"); //mpg
		assertNotNull(asset);
		ConversionManager manager = archive.getTranscodeTools().getManagerByRenderType("video");
		ConvertInstructions instructions = manager.createInstructions(asset, "image1024x768.jpg");
		instructions.setProperty("timeoffset","3");
		instructions.setForce(true);
		ConvertResult result = manager.createOutput(instructions);
		
		assertTrue(result.isOk());
		assertNotNull(result.getOutput());
	}

	public void testDynamicVideoImageOffset() throws Exception
	{
		MediaArchive archive = getMediaArchive("entermedia/catalogs/testcatalog");
		Asset asset = archive.getAsset("101"); //mpg
		assertNotNull(asset);
		
		Page page = archive.getPageManager().getPage("/WEB-INF/data/entermedia/catalogs/testcatalog/generated/" + asset.getSourcePath() + "/image200x200offset3.jpg");		
		archive.getPageManager().removePage(page);
		
		
		Page page2 = archive.getPageManager().getPage("/WEB-INF/data/entermedia/catalogs/testcatalog/generated/" + asset.getSourcePath() + "/image200x200offset5.jpg");		
		archive.getPageManager().removePage(page2);
		
		WebPageRequest inReq = getFixture().createPageRequest("/testcatalog/views/modules/asset/downloads/preview/thumb/" + asset.getSourcePath() + "/thumb.jpg?timeoffset=3");
		getFixture().getEngine().executePageActions(inReq);
		getFixture().getEngine().executePathActions(inReq);
		
		
		ConvertGenerator generator = (ConvertGenerator) archive.getModuleManager().getBean("ConvertGenerator");
		Output output = new Output();
		output.setStream(inReq.getOutputStream());
		generator.generate(inReq, inReq.getPage(),output );
		
		
		inReq = getFixture().createPageRequest("/testcatalog/views/modules/asset/downloads/preview/thumb/" + asset.getSourcePath() + "/thumb.jpg?timeoffset=5");
		getFixture().getEngine().executePageActions(inReq);
		getFixture().getEngine().executePathActions(inReq);
		
		 output = new Output();
		output.setStream(inReq.getOutputStream());
		generator.generate(inReq, inReq.getPage(),output );	
		
		
			
		assertTrue(page.exists());
		assertTrue(page2.exists());
		
		assertNotSame(page.length(), page2.length());

		
	}
	
	
	
	
	
	///views/modules/asset/downloads/preview/widethumb/submitted/admin/Broad.City.S01E06.HDTV.x264-EXCELLENCE.mp4/thumb.jpg?timeoffset=407&assetid=102
	
	
	
	
	/*
	public void testPdfToJpeg()
	{

		MediaArchive archive = getMediaArchive("entermedia/catalogs/testcatalog");
		ConverterManager manager = archive.getCreatorManager();
		MediaConverter creater = manager.getMediaCreatorByOutputFormat("jpg");
		Map map = new HashMap();
		
		for (int i = 0; i < 1000; i++)
		{
			ConvertInstructions inst = creater.createInstructions(map, archive, "jpg", "users/admin/105");			
		}
		
		ConvertInstructions instructions = new ConvertInstructions();
		instructions.setForce(true);
		instructions.setAssetSourcePath("users/admin/105");
		instructions.setOutputExtension("jpg");

		
				
		Page converted = manager.createOutput(instructions);
		assertNotNull(converted);
		assertTrue(converted.exists());
		assertTrue(converted.length() > 0);
		assertEquals("jpg", PathUtilities.extractPageType(converted.getPath()));
	}
	*/
	
	/*
	
	
	public void testCropInstructions()
	{

		MediaArchive archive = getMediaArchive("entermedia/catalogs/testcatalog");
		ConverterManager manager = archive.getCreatorManager();
		MediaConverter creater = manager.getMediaCreatorByOutputFormat("jpg");
		Map map = new HashMap();
	
		ConvertInstructions instructions = new ConvertInstructions();
		instructions.setForce(true);
		instructions.setAssetSourcePath("users/admin/105");
		instructions.setOutputExtension("jpg");
		instructions.setCrop(true);
		instructions.setProperty("prefwidth", "100");
		instructions.setProperty("prefheight", "100");
		
		instructions.setProperty("x1","10");
		instructions.setProperty("y1","10");
		
		
		
		Page converted = manager.createOutput(instructions);
		assertNotNull(converted);
		assertTrue(converted.exists());
		assertTrue(converted.length() > 0);
		assertEquals("jpg", PathUtilities.extractPageType(converted.getPath()));
	}
	
	
	
	public void testTiffToEps()
	{
		ConvertInstructions instructions = new ConvertInstructions();
		instructions.setForce(true);
		instructions.setAssetSourcePath("users/admin/104");
		instructions.setOutputExtension("eps");
		instructions.setInputExtension("tiff");

		MediaArchive archive = getMediaArchive("entermedia/catalogs/testcatalog");
		ConverterManager manager = archive.getCreatorManager();
		
		Page converted = manager.createOutput(instructions);
		assertNotNull(converted);
		assertTrue(converted.exists());
		assertTrue(converted.length() > 0);
		assertEquals("eps", PathUtilities.extractPageType(converted.getPath()));
	}
	*/
	/**
	public void xtestConvertInddFile() throws Exception
	{
		Asset asset = createAsset();
		asset.setFolder(true);
		asset.setPrimaryFile("Indesign.indd");
		asset.setProperty("fileformat", "indd");
		Page path = getMediaArchive().getOriginalDocument(asset);

		File assetDir = new File(path.getContentItem().getAbsolutePath());
		assetDir.getParentFile().mkdirs();
		File testFile = new File(getRoot().getAbsoluteFile().getParentFile(), "/etc/testassets/Indesign.indd");
		if (testFile.exists())
		{
			new FileUtils().copyFiles(testFile, assetDir);
		}
		
		User user = getFixture().createPageRequest().getUser();
		getMediaArchive().saveAsset(asset, user);

		//Create the Asset? Then 
		ImageMagickImageCreator converter = (ImageMagickImageCreator)getFixture().getModuleManager().getBean("imageMagickImageCreator");
		MediaArchive mediaarchive = getMediaArchive();
				
		ConvertInstructions instructions = new ConvertInstructions();
		instructions.setAssetSourcePath(asset.getSourcePath());
		instructions.setInputExtension("indd");
		instructions.setOutputExtension("jpg");
		instructions.setMaxScaledSize(50, 50);
		converter.populateOutputPath(mediaarchive, instructions);
		Page junk = getPage(instructions.getOutputPath());
		converter.convert(mediaarchive, asset, junk, instructions);

		assertNotNull(junk);
		assertTrue(junk.exists());
		assertTrue(junk.length() > 0);

	}
	**/
}
