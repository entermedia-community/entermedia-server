/*
 * Created on May 12, 2006
 */
package com.openedit.modules.update;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;

import com.openedit.OpenEditException;
import com.openedit.util.FileUtils;
import com.openedit.util.OutputFiller;

public class Downloader
{

	public void download(String inUrl, String inOutput) throws OpenEditException
	{
		download(inUrl,new File( inOutput));
	}
	
	public void download(String inStrUrl, File outputFile) throws OpenEditException
	{
		try
		{
			URL url = new URL(inStrUrl);
			URLConnection con = url.openConnection();
			con.setUseCaches(false);
			con.connect(); 
			
			//*** create new output file
	        //*** make a growable storage area to read into 
			outputFile.getParentFile().mkdirs();
	        FileOutputStream out = new FileOutputStream(outputFile);
	        //*** read in url connection stream into input stream
	        InputStream in = con.getInputStream();
	        //*** fill output stream
	        new OutputFiller().fill(in,out);
	        //*** close output stream
	        FileUtils.safeClose(out);
	        //*** close input stream
	        FileUtils.safeClose(in);
		}
		catch ( Exception ex)
		{
			throw new OpenEditException(ex);
		}
	}
	
	public String downloadToString(String inUrl)
	{
		try
		{
			URL url = new URL(inUrl);
			URLConnection con = url.openConnection();
			con.setUseCaches(false);
			con.connect(); 
			
			//*** create new output file
	        //*** make a growable storage area to read into 
	        StringWriter out = new StringWriter();
	        //*** read in url connection stream into input stream
	        InputStream in = con.getInputStream();
	        //*** fill output stream
	        new OutputFiller().fill(new InputStreamReader(in),out);
	        //*** close output stream
	        FileUtils.safeClose(out);
	        //*** close input stream
	        FileUtils.safeClose(in);
	        return out.toString();
		}
		catch ( Exception ex)
		{
			throw new OpenEditException(ex);
		}
		
	}
}
