package org.entermediadb.model;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.MediaArchive;
import org.openedit.page.Page;

public  class ZipTest extends BaseEnterMediaTest
{
	protected MediaArchive fArchive;

	public ZipTest( String inName )
	{
		super( inName );
	}

	public void testZipCategory() throws Exception
	{
		Page testFile = getPage("/system/images/icons/about_16x16.png");
		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream("testme.zip")); 

		ZipEntry entry = new ZipEntry("testme/testfile.html");
		byte[] buf = new byte[1024];
		int len;
		//Create a new Zip entry with the file's name.
		//Create a buffered input stream out of the file
		//we're trying to add into the Zip archive.
		BufferedInputStream in = new BufferedInputStream(
				testFile.getContentItem().getInputStream());
		zos.putNextEntry(entry);
		//Read bytes from the file and write into the Zip archive.
		while ((len = in.read(buf)) >= 0) {
			zos.write(buf, 0, len);
		}
		//Close the input stream.
		in.close();
		//Close this entry in the Zip stream.
		zos.closeEntry();
		zos.close();
	}
}
