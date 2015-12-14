package org.entermediadb.view;

import java.io.ByteArrayOutputStream;

import org.entermediadb.asset.BaseEnterMediaTest;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.generators.Output;

public class ConvertDocumentGeneratorTest extends BaseEnterMediaTest
{
	boolean wrote = false; 
	public ConvertDocumentGeneratorTest( String inName )
	{
		super( inName );
	}
	
	public void testConvertToEPS() throws OpenEditException
	{
		String path = "/media/catalogs/photo/downloads/converted/cache/newassets/admin/101/converted.eps";
		WebPageRequest request = getFixture().createPageRequest( path );
		
		Output out = new Output();
		ByteArrayOutputStream baos = (ByteArrayOutputStream) request.getOutputStream();
		out.setStream(	baos );
		request.getPage().generate( request , out );
		byte[] bytes = baos.toByteArray();
		assertTrue(bytes.length > 1000);
	}
}
