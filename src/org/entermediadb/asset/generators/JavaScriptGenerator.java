package org.entermediadb.asset.generators;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Date;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.WebPageRequest;
import org.openedit.config.Script;
import org.openedit.generators.Output;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;
import org.openedit.util.FileUtils;

public class JavaScriptGenerator extends TempFileGenerator
{
	private static Log log = LogFactory.getLog(JavaScriptGenerator.class);
	protected PageManager fieldPageManager;
	
	
	public PageManager getPageManager()
	{
		return fieldPageManager;
	}


	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}


	@Override
	public void generate(WebPageRequest inContext, Page inPage, Output inOut)
	{
		HttpServletResponse res = inContext.getResponse();
		HttpServletRequest req = inContext.getRequest();

			//Loop over
			String path = inPage.get("frompath");
			Page rootpage = getPageManager().getPage(path, false);
			
			//Check on the last mod date. If file has changed then write out new file before sending
			
			long mostrecentmod = 0;
			
			for (Iterator iterator = rootpage.getScripts().iterator(); iterator.hasNext();)
			{		
				Script script= (Script) iterator.next();
				if(!skip(script.getSrc()))
				{
					Page file = getPageManager().getPage(script.getSrc());
					long modifield = file.lastModified();
					if( modifield > mostrecentmod )
					{
						mostrecentmod = modifield;
					}
				}
			}
			

			String since = req.getHeader("If-Modified-Since");
			if( since != null && since.endsWith("GMT"))
			{
				//304 Not Modified
				try
				{
					Date old = getLastModFormat().parse(since);
					if( mostrecentmod <= old.getTime())
					{
						//log.info("if since"  + since);
						res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
						return;
					}
				}
				catch( Exception ex)
				{
					log.error(since);
				}
			}

		//Something modified. Save file again
		try
		{
			if( mostrecentmod > inPage.getLastModified().getTime())
			{
				saveLocally(rootpage, inPage, inOut);
			}
			sendBack(inPage, mostrecentmod, inOut, res);
		}
		catch ( Throwable ex)
		{
			log.error("Could not save",ex);
		}
		
		
	}


	protected boolean skip(String script)
	{
		return script == null || script.isEmpty() || script.startsWith("http");
	}


	protected void sendBack(Page inPage, long mostrecentmod, Output inOut, HttpServletResponse res) throws UnsupportedEncodingException, IOException
	{
		long length = inPage.length();
		if( length > -1)
		{
			res.setContentLength((int)length);
		}
		res.setDateHeader("Last-Modified",mostrecentmod);

		InputStreamReader reader = null;
		try
		{
			if ( inPage.getCharacterEncoding() != null )
			{
				reader = new InputStreamReader( inPage.getInputStream(), inPage.getCharacterEncoding() );
			}
			else
			{
				reader = new InputStreamReader( inPage.getInputStream() );
			}
			//If you get an error about content length then your character encoding is not correct. Use UTF-8
			//maybe we need to write with the correct encoding then the files should match
			getOutputFiller().fill(reader, inOut.getWriter());
		}
		finally
		{
			FileUtils.safeClose(reader);
		}
//			#foreach( $script in $content.getScriptPaths() )
//			#if($script.indexOf("jquery-3.3.1.min.js") > -1)
//				<script src="#if(!$script.startsWith("http"))$home#end$script" onload="if (!window.jQuery) window.$ = window.jQuery = module.exports;"></script>
//			#else
//				<script type="text/javascript" src="#if(!$script.startsWith("http"))$home#end$script"></script>
//			#end
//		#end
	}


	protected void saveLocally(Page rootpage, Page inPage, Output inOut) throws FileNotFoundException, IOException
	{
		synchronized( inPage )
		{
			Page tmpfile = getPageManager().getPage( inPage.getContentItem().getAbsolutePath() + ".tmp.js" );
			
			Writer out = new OutputStreamWriter( tmpfile.getContentItem().getOutputStream(), inPage.getCharacterEncoding() );
			
			for (Iterator iterator = rootpage.getScripts().iterator(); iterator.hasNext();)
			{		
				Script script= (Script) iterator.next();
				if(!skip(script.getSrc()))
				{
					Page infile = getPageManager().getPage(script.getSrc());
					InputStreamReader reader = null;
					if ( infile.getCharacterEncoding() != null )
					{
						reader = new InputStreamReader( infile.getInputStream(), infile.getCharacterEncoding() );
					}
					else
					{
						reader = new InputStreamReader( infile.getInputStream() );
					}
					try
					{
						out.write(System.lineSeparator() + System.lineSeparator() + "/** javascriptGenerator : " + script + System.lineSeparator()  + "  **/" + System.lineSeparator() );
						getOutputFiller().fill(reader,out);
					}
					finally
					{
						FileUtils.safeClose(reader);
					}
				}
			}
			FileUtils.safeClose(out);
			//rename
			getPageManager().removePage(inPage);
			getPageManager().movePage(tmpfile, inPage);
		}
	}

}
