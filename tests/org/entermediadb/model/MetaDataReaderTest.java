package org.entermediadb.model;

import java.io.File;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.BaseAsset;
import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.scanner.MetaDataReader;
import org.entermediadb.asset.search.AssetSearcher;
import org.openedit.Data;
import org.openedit.data.PropertyDetail;
import org.openedit.data.Searcher;
import org.openedit.repository.filesystem.FileItem;

public class MetaDataReaderTest extends BaseEnterMediaTest {

	public MetaDataReaderTest(String inName) {
		super(inName);
	}
//	public void xxxtestSpeed() throws Exception 
//	{
//		
//		MetaDataReader reader = (MetaDataReader) getBean("metaDataReader");
//		Asset asset = new BaseAsset();
//		File file = new File("/media/D603-EA1D/Sample EM/Content Archive/Highlights/2011/HL_12_11/HL_DEC_2011_PRESS_PDFS/HL_12_11_05_VERSE.pdf");		
//		reader.populateAsset(getMediaArchive(), file, asset);
//		reader.populateAsset(getMediaArchive(), file, asset);
//		reader.populateAsset(getMediaArchive(), file, asset);
//	}
	
	public void testEpsXmp()
	{
		File testDir = new File(getRoot().getAbsoluteFile().getParentFile().getPath() + "/etc/testassets");
		
		String[] testFiles = new String[] {"test2.eps"};
		String[][] knownKeywords = new String[][] {
				{"electropunt", "televisores", "antenas", "marca", "reparaciones"}
		};
		
		checkFiles(testDir, testFiles, knownKeywords);
	}

	public void testJpegXmp()
	{
		File testDir = new File(getRoot().getAbsoluteFile().getParentFile().getPath() + "/etc/testassets");
		
		String[] testFiles = new String[] {"test1.jpg","test2.jpg","test3.jpg","test4.jpg","test5.jpg"};
		String[][] knownKeywords = new String[][] {
				{"psicología", "dudas", "cabeza"},
				{"gimnasio", "femenino", "cuerpo", "belleza"},
				{"oficinas", "empresa"},
				{"paneles solares", "b/n", "energias renovables"},
				{"peugeot","marca","coche"}
		};
		
		checkFiles(testDir, testFiles, knownKeywords);
	}
	
	public void testTiffXmp()
	{
		File testDir = new File(getRoot().getAbsoluteFile().getParentFile().getPath() + "/etc/testassets");
		
		String[] testFiles = new String[] {"test1.tif"};
		String[][] knownKeywords = new String[][] {
				{"XMP", "Blue Square", "test file", "Photoshop", ".tif"}
		};
		
		checkFiles(testDir, testFiles, knownKeywords);
	}
	
	private void checkFiles(File rootDir, String[] fileNames, String[][] keywords)
	{
		Asset p;
		MetaDataReader reader = (MetaDataReader) getBean("metaDataReader");
		
		assertTrue(rootDir.exists() && rootDir.isDirectory());
		
		for (int i = 0; i < fileNames.length; i++)
		{
			File testFile = new File(rootDir, fileNames[i]);
			assertTrue(testFile.exists() && testFile.canRead());
			p = new BaseAsset(getMediaArchive());
			FileItem item = new FileItem();
			item.setPath(testFile.getName());
			item.setFile(testFile);

			reader.populateAsset(getMediaArchive(),item, p);
			for (int j = 0; j < keywords[i].length; j++)
			{
				assertTrue(p.getKeywords().contains(keywords[i][j]));
			}
		}
	}
	
	public void testDita()
	{
		File testDir = new File(getRoot().getAbsoluteFile().getParentFile().getPath() + "/etc/testassets/dita/topics");
		MetaDataReader reader = (MetaDataReader) getBean("metaDataReader");
		getMediaArchive().getSearcher("fileformat").reIndexAll();

		File testFile = new File(testDir, "c_about_defining_payload.dita");
		assertTrue(testFile.exists() && testFile.canRead());
		Asset p = new BaseAsset(getMediaArchive());
		FileItem item = new FileItem();
		item.setPath(testFile.getName());
		item.setFile(testFile);

		reader.populateAsset(getMediaArchive(),item, p);
		Object value = p.get("fulltext");
		assertNotNull("fulltext", value);
	}
	
	
	public void XtestListLookups()
	{
		File testDir = new File(getRoot().getAbsoluteFile().getParentFile().getPath() + "/etc/testassets");
		
	
		Asset p;
		MetaDataReader reader = (MetaDataReader) getBean("metaDataReader");
	
		File testFile = new File(testDir, "location.jpg");
		MediaArchive  archive = getMediaArchive();
		p = new BaseAsset(archive);
		FileItem item = new FileItem();
		item.setPath(testFile.getName());
		item.setFile(testFile);
		
	
		Searcher locations = archive.getSearcher("location");
		locations.deleteAll(null);
		
		Data lookup = locations.createNewData();
		lookup.setId("campusscene");
		lookup.setName("Campus Scenes");
		locations.saveData(lookup, null);
		reader.populateAsset(getMediaArchive(),item, p);
		assertTrue(p.get("location").equals("campusscene"));
		
		AssetSearcher searcher = archive.getAssetSearcher();
		PropertyDetail location = searcher.getDetail("location");
		location.setProperty("autocreatefromexif", "true");
		locations.deleteAll(null);
		reader.populateAsset(getMediaArchive(),item, p);
		assertNotNull(p.get("location"));
		
		assertNotSame(p.get("location"),"Campus Scenes");

		location.setProperty("autocreatefromexif", "false");
		searcher.getPropertyDetails().addDetail(location);
		locations.deleteAll(null);
		reader.populateAsset(getMediaArchive(),item, p);
		assertNotNull(p.get("location"));
		
		assertTrue(p.get("location").equals("Campus Scenes"));

		
	}
}
