package org.entermediadb.data;

import java.io.File;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.scanner.ExiftoolMetadataExtractor;
import org.entermediadb.asset.xmp.XmpWriter;
import org.openedit.repository.filesystem.FileItem;

public class XmpTest extends BaseEnterMediaTest{
	
	public void testXmpWriting() throws Exception
	{
		Asset asset = new Asset(getMediaArchive());
		asset.addKeyword("test1");
		asset.addKeyword("test2");
		asset.setSourcePath("testassets/Indesign.indd");
		File assetfile = new File(getRoot(), "../etc/testassets/Indesign.indd");

		XmpWriter writer = (XmpWriter) getBean("xmpWriter");
		assertNotNull(writer);
		writer.saveMetadata(getMediaArchive(), asset);
		
		Asset newasset = new Asset(getMediaArchive());
		
		ExiftoolMetadataExtractor reader= (ExiftoolMetadataExtractor)getBean("exiftoolMetadataExtractor");
		MediaArchive mediaArchive = getMediaArchive();
		
		FileItem item = new FileItem();
		item.setPath("/etc/testassets/Indesign.indd");
		item.setFile(assetfile);
		
		reader.extractData(mediaArchive, item, newasset);
	//	assertEquals(2, newasset.getKeywords().size());
		assertTrue(newasset.getKeywords().contains("test1"));
		assertTrue(newasset.getKeywords().contains("test2"));
	}

	public void testCustomXmp() throws Exception
	{
		Asset asset = new Asset(getMediaArchive());
		asset.setValue("entermedia-exif", "EnterMedia");
			
		
		asset.setSourcePath("testassets/dog.jpg");
		File assetfile = new File(getRoot(), "../etc/testassets/dog.jpg");

		XmpWriter writer = (XmpWriter) getBean("xmpWriter");
		assertNotNull(writer);
		writer.saveMetadata(getMediaArchive(), asset);
		
		Asset newasset = new Asset(getMediaArchive());
		
		ExiftoolMetadataExtractor reader= (ExiftoolMetadataExtractor)getBean("exiftoolMetadataExtractor");
		MediaArchive mediaArchive = getMediaArchive();
		
		FileItem item = new FileItem();
		item.setPath("/etc/testassets/dog.jpg");
		item.setFile(assetfile);
		
		reader.extractData(mediaArchive, item, newasset);
		assertEquals(newasset.getValue("entermedia-exif"), "EnterMedia");

	}
	

	
	
	
	

}
