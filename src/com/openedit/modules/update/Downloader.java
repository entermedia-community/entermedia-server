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
		FileOutputStream out = null;
		InputStream in  = null;
		try
		{
			URL url = new URL(inStrUrl);
			URLConnection con = url.openConnection();
		  //this helps prevent 403 errors.
			con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB;     rv:1.9.2.13) Gecko/20101203 Firefox/3.6.13 (.NET CLR 3.5.30729)");

			con.setUseCaches(false);
			con.connect(); 
			
			//*** create new output file
	        //*** make a growable storage area to read into 
			outputFile.getParentFile().mkdirs();
	        out = new FileOutputStream(outputFile);
	        //*** read in url connection stream into input stream
	        in = con.getInputStream();
	        //*** fill output stream
	        new OutputFiller().fill(in,out);
		}
		catch ( Exception ex)
		{
			throw new OpenEditException(ex);
		}
		finally
		{
			//*** close output stream
			FileUtils.safeClose(out);
	        //*** close input stream
	        FileUtils.safeClose(in);
		}
	}
	
	public String downloadToString(String inUrl)
	{
		StringWriter out = null;
		InputStream in  = null;
		try
		{
			URL url = new URL(inUrl);
			URLConnection con = url.openConnection();
		    con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB;     rv:1.9.2.13) Gecko/20101203 Firefox/3.6.13 (.NET CLR 3.5.30729)");

			con.setUseCaches(false);
			con.connect(); 
			
			//*** create new output file
	        //*** make a growable storage area to read into 
	        out = new StringWriter();
	        //*** read in url connection stream into input stream
	        in = con.getInputStream();
	        //*** fill output stream
	        new OutputFiller().fill(new InputStreamReader(in),out);
	        return out.toString();
		}
		catch ( Exception ex)
		{
			throw new OpenEditException(ex);
		}
		finally
		{
			//*** close output stream
	        FileUtils.safeClose(out);
	        //*** close input stream
	        FileUtils.safeClose(in);
		}
	}
}
