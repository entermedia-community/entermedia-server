package org.openedit.entermedia.model;

import org.openedit.entermedia.Asset;
import org.openedit.entermedia.AssetSourcePathCreator;
import org.openedit.entermedia.BaseEnterMediaTest;


public class SourcePathTest extends BaseEnterMediaTest
{

	
	public void testLookup() throws Exception
	{
		Asset asset = new Asset();
		String originalpath= "\\\\server\\share\\mystuff\\here\\1234.gif";
		
		AssetSourcePathCreator creator = new AssetSourcePathCreator();
		String sourcepath = creator.createSourcePath(asset, originalpath);
		assertEquals("server/share/mystuff/here/1234.gif",sourcepath);

		
		originalpath= "\\\\SERVER\\share\\mystuff\\here\\1234.gif";		
		sourcepath = creator.createSourcePath(asset, originalpath);
		assertEquals("server/share/mystuff/here/1234.gif",sourcepath);

		
		originalpath = "\\\\vault.bunch.of.stuff\\here\\1234";
		sourcepath = creator.createSourcePath(asset, originalpath);
		assertEquals("vault/here/1234",sourcepath);

		originalpath = "D:\\mystuff\\here\\1234.gif";
		sourcepath = creator.createSourcePath(asset, originalpath);
		assertEquals("D/mystuff/here/1234.gif",sourcepath);

		originalpath = "mam4 (D:)\\mystuff\\here\\1234.gif";
		sourcepath = creator.createSourcePath(asset, originalpath);
		assertEquals("D/mystuff/here/1234.gif",sourcepath);

		//Linux with windows
//		originalpath = "/mystuff/here/1234.pdf";
//		asset.setProperty("pagenumber", "2");
//		sourcepath = creator.createSourcePath(asset, originalpath);
//		assertEquals("mystuff/here/1234_page2.pdf",sourcepath);
//
//		originalpath = "D:\\vault\\here\\1234";
//		asset.setProperty("pagenumber", null);
//		asset.setProperty("fileformat", "pdf");
//		sourcepath = creator.createSourcePath(asset, originalpath);
//		assertEquals("D/vault/here/1234.pdf",sourcepath);
		
		
	}
	
}
