package org.entermediadb.model;

import java.util.HashMap;
import java.util.Map;

import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConverterManager;
import org.entermediadb.asset.convert.MediaConverter;
import org.openedit.page.Page;
import org.openedit.util.PathUtilities;

public class ConvertionTest extends BaseEnterMediaTest
{
	
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
