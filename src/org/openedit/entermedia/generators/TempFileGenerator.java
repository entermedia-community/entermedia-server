/*
 * Created on Jul 21, 2006
 */
package org.openedit.entermedia.generators;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

import com.openedit.Generator;
import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.generators.BaseGenerator;
import com.openedit.generators.Output;
import com.openedit.util.FileUtils;

public abstract class TempFileGenerator   extends BaseGenerator implements Generator
{
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
				res.setContentLength((int)len);
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
