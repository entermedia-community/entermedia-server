package org.entermediadb.asset.scanner;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.fileupload.disk.DiskFileItem;

public class UploadDiskFileItem extends DiskFileItem
{
	protected File fieldTempFile;
	protected String fieldDestinationPath;
	protected long fieldSeek;
	
	public UploadDiskFileItem(String inFieldName, String inContentType, boolean inIsFormField, String inFileName, int inSizeThreshold, File inRepository)
	{
		super(inFieldName, inContentType, inIsFormField, inFileName, inSizeThreshold, inRepository);
	}

	public void setDestinationPath(String inDestinationPath)
	{
		fieldDestinationPath = inDestinationPath;
	}
	public OutputStream getOutputStream() throws IOException
	{
		// item.putProperty("offset", inReq.getRequestParameter("offset"));
		// log.info("Uploading from offset: " + item.get("offset"));
		OutputStream fos;
		File file = getTempFile();
		if(fieldSeek > 0 && file.length() == fieldSeek)
		{
			fos = new FileOutputStream(file, true);
		}
		else
		{
			fos = new FileOutputStream(getTempFile());
		}
		return new BufferedOutputStream(fos)
				{
					public void write(byte[] bytes, int one, int two) throws IOException
					{
						//Put breakpoint here to slow down progress SLOWDOWN
//						try
//						{
//							Thread.sleep(100);
//						}
//						catch( Exception ex)
//						{
//							
//						}
						super.write(bytes,one,two);
					}
				};
	}

	public InputStream getInputStream() throws IOException
	{
		return new FileInputStream(getTempFile());
	}
	public boolean isInMemory()
	{
		return false;
	}
	public long getSize()
	{
		return getTempFile().length();
	}
	protected File getTempFile()
	{
		if (fieldTempFile == null)
		{
			fieldTempFile = new File(fieldDestinationPath);
			fieldTempFile.getParentFile().mkdirs();
		}
		return fieldTempFile;
	}
}
