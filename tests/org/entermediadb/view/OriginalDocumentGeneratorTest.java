package org.entermediadb.view;

import java.io.ByteArrayOutputStream;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.EnterMedia;
import org.entermediadb.asset.MediaArchive;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.generators.Output;

public class OriginalDocumentGeneratorTest extends BaseEnterMediaTest
{
	public OriginalDocumentGeneratorTest( String inName )
	{
		super( inName );
	}
	
	public void testGetPrimaryFile() throws OpenEditException
	{
		String path = "entermedia/catalogs/testcatalog/downloads/originals/users/admin/101/asf_to_mpeg-1.mpg";
		EnterMedia em = getEnterMedia();
		MediaArchive archive = em.getMediaArchive("entermedia/catalogs/testcatalog");
		
		Asset asset = archive.getAssetBySourcePath("users/admin/101");
		assertNotNull(asset);
		
		WebPageRequest request = getFixture().createPageRequest( path );
		getFixture().getModuleManager().executePathActions( request.getPage(), request );
		
		Output out = new Output();
		ByteArrayOutputStream baos = (ByteArrayOutputStream) request.getOutputStream();
		out.setStream(	baos );	
		request.getPage().generate( request , out );
		byte[] bytes = baos.toByteArray();
		
		
		assertEquals( "Was not the correct size", 573444, bytes.length );
	}

	public void testGetSourcePath() throws OpenEditException
	{
		String path = "entermedia/catalogs/testcatalog/downloads/originals/users/admin/CHAPTER_1.pdf/CHAPTER_1.pdf";
		EnterMedia em = getEnterMedia();
		MediaArchive archive = em.getMediaArchive("entermedia/catalogs/testcatalog");
		
		Asset asset = archive.getAssetBySourcePath("users/admin/CHAPTER_1.pdf");
		assertNotNull(asset);
		
		WebPageRequest request = getFixture().createPageRequest( path );
		getFixture().getModuleManager().executePathActions( request.getPage(), request );
		
		Output out = new Output();
		ByteArrayOutputStream baos = (ByteArrayOutputStream) request.getOutputStream();
		out.setStream(	baos );	
		request.getPage().generate( request , out );
		byte[] bytes = baos.toByteArray();
		
		assertEquals( "Was not the correct size", 42718, bytes.length );
	}

	
}
