package org.openedit.entermedia.model;

import java.io.File;

import org.openedit.entermedia.Asset;
import org.openedit.entermedia.BaseEnterMediaTest;
import org.openedit.entermedia.scanner.MetaDataReader;

public class MetaDataReaderTest extends BaseEnterMediaTest {

	public MetaDataReaderTest(String inName) {
		super(inName);
	}
	public void xxxtestSpeed() throws Exception 
	{
		
		MetaDataReader reader = (MetaDataReader) getBean("metaDataReader");
		Asset asset = new Asset();
		File file = new File("/media/D603-EA1D/Sample EM/Content Archive/Highlights/2011/HL_12_11/HL_DEC_2011_PRESS_PDFS/HL_12_11_05_VERSE.pdf");		
		reader.populateAsset(getMediaArchive(), file, asset);
		reader.populateAsset(getMediaArchive(), file, asset);
		reader.populateAsset(getMediaArchive(), file, asset);
	}
	
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
			p = new Asset();
			reader.populateAsset(getMediaArchive(),testFile, p);
			for (int j = 0; j < keywords[i].length; j++)
			{
				assertTrue(p.getKeywords().contains(keywords[i][j]));
			}
		}
	}
}
