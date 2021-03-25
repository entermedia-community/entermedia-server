/*
 * Created on Jul 21, 2006
 */
package org.entermediadb.asset.generators;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Locale;

import javax.servlet.http.HttpServletResponse;

import org.openedit.Generator;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.generators.BaseGenerator;
import org.openedit.generators.Output;
import org.openedit.util.FileUtils;
import org.openedit.util.SimpleDateFormatPerThread;

public abstract class TempFileGenerator   extends BaseGenerator implements Generator
{
	protected SimpleDateFormatPerThread fieldLastModFormat;
	
	public SimpleDateFormatPerThread getLastModFormat() 
	{
		if( fieldLastModFormat  == null)
		{
			//Tue, 05 Jan 2010 14:20:51 GMT  -- just english
			fieldLastModFormat = new SimpleDateFormatPerThread("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
			//log.info( fieldLastModFormat.format(new Date()) );
		}
		return fieldLastModFormat;
	}

	
	public void generate(WebPageRequest inReq, File inFile, Output inOut) throws OpenEditException
	{
		//sometimes we can specify the length of the document
		HttpServletResponse res = inReq.getResponse();
		
		//only bother if we are the content page and not in development
		if ( res != null )
		{
			long len = inFile.length();
			if ( len != -1)
			{
				//res.setHeader("Content-Length", String.valueOf(len));
				if( len < Integer.MAX_VALUE)
				{
					res.setContentLength((int)len);
				}	
			}
			
			res.setDateHeader("Last-Modified",inFile.lastModified());
			long now = System.currentTimeMillis();			
			boolean cache = true;
			String nocache = inReq.getRequestParameter("cache");
			if( nocache != null ) 
			{
				cache = Boolean.parseBoolean(nocache);
			}
			else
			{
				//is this recenlty modified?
				//3333333recent99  + 24 hours (mil * sec * min * hours) will be more than now
				cache = inFile.lastModified() + (1000 * 60 * 60 * 24 ) < now;
			}
			
			if(  cache )
			{
				res.setDateHeader("Expires", now + (1000 * 60 * 60 * 24 )); //sec * min * hour * 48 Hours				
			}
			else
			{
				res.setDateHeader("Expires", now - (1000 * 60 * 60 * 24)); //expired 24 hours ago
			}
		}

		InputStream in = null;
		try
		{
			in = new FileInputStream(inFile);
			getOutputFiller().fill(in, inOut.getStream());
		}
		catch ( Exception eof )
		{
			if( ignoreError(eof))
			{
				//ignored
				return;
			}
			throw new OpenEditException(eof);
		}
		finally
		{
			FileUtils.safeClose(in);
		}
	}
	
	public boolean canGenerate(WebPageRequest inReq)
	{
		return true;
	}

}
