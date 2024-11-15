package org.entermediadb.asset.generators;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.WebPageRequest;
import org.openedit.config.Script;
import org.openedit.generators.Output;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;
import org.openedit.repository.filesystem.FileItem;
import org.openedit.util.FileUtils;

public class JavaScriptGenerator extends TempFileGenerator
{
	private static Log log = LogFactory.getLog(JavaScriptGenerator.class);
	protected PageManager fieldPageManager;
	protected Map<String,Long> cachedSizeCounts = new HashMap();
	
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
			String appid = inPage.get("applicationid");
			Page rootpage = getPageManager().getPage("/" + appid + "/", false);  //Not a real page
			
			//Check on the last mod date. If file has changed then write out new file before sending
			long mostrecentmod = 0;
			long totalsize = 0;
			
//			String applicationid = inContent.get("applicationid");
//			String apppath = "/" + applicationid + "/_site.xconf";
//			PageSettings site = getPageSettingsManager().getPageSettings(apppath);
//			List<String> appscripts = loadScriptPathsFor(site);
//			return appscripts;

			List<Script> scripts = getPageManager().getScriptsForApp(rootpage);
			if(scripts == null)
			{
				return;
			}
			
			boolean deferonly = Boolean.parseBoolean(inPage.getProperty("deferjs"));
			for (Iterator iterator = scripts.iterator(); iterator.hasNext();)
			{		
				Script script= (Script) iterator.next();
				
				if(include(script,deferonly))
				{
					String path = inPage.replaceProperty(script.getSrc());
					Page file = getPageManager().getPage(path); //Cached
					totalsize = totalsize + file.length();
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
						//res.setHeader("ETag",length + "_" + mostrecentmod);
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
			Long oldtotal = cachedSizeCounts.get(inPage.getPath());
			if( oldtotal == null)
			{
				oldtotal = -1L;
			}
			if(oldtotal != totalsize ||  mostrecentmod != inPage.getLastModified().getTime())
			{
				saveLocally(scripts, deferonly, inPage, inOut, mostrecentmod);
				cachedSizeCounts.put(inPage.getPath(),totalsize); //TODO check count change or size change
			}
			sendBack(inPage, deferonly, mostrecentmod, inOut, res);
		}
		catch ( Throwable ex)
		{
			log.error("Could not save",ex);
		}
		
		
	}


	protected boolean include(Script script, boolean deferonly)
	{
		String html = script.getSrc();
		boolean logic =  html != null && !html.isEmpty() && !html.startsWith("http");
		if( logic )
		{
			if( script.isDefer()  == deferonly )
			{
				return true;
			}
		}
		return false;
	}


	protected void sendBack(Page inPage, boolean deferonly, long mostrecentmod, Output inOut, HttpServletResponse res) throws UnsupportedEncodingException, IOException
	{
		long length = inPage.length();
		if( length > -1)
		{
			res.setContentLength((int)length);
		}
		res.setDateHeader("Last-Modified",mostrecentmod);
		//res.setHeader("ETag",length + "_" + mostrecentmod); //Too strong of a cache

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


	protected void saveLocally(List<Script> scriptpaths,boolean deferonly, Page inPage, Output inOut, long mostrecentmod) throws FileNotFoundException, IOException
	{
		synchronized( inPage )
		{
			Page tmpfile = getPageManager().getPage( inPage.getContentItem().getAbsolutePath() + ".tmp.js" );
			
			Writer out = new OutputStreamWriter( tmpfile.getContentItem().getOutputStream(), inPage.getCharacterEncoding() );
			
			for (Iterator iterator = scriptpaths.iterator(); iterator.hasNext();)
			{		
				Script script= (Script) iterator.next();
				
				if(include(script,deferonly))
				{
					String path = inPage.replaceProperty(script.getSrc());
					Page infile = getPageManager().getPage(path);
					InputStreamReader reader = null;
					if (!infile.exists()) 
					{
						out.write(System.lineSeparator() + "/** " + System.lineSeparator() + " EnterMediaDB javascriptGenerator : 404 NOT FOUND" + script.getSrc() + System.lineSeparator() + script.getPath() + System.lineSeparator()  + "  **/" + System.lineSeparator() + System.lineSeparator() );
						out.write(System.lineSeparator()  + System.lineSeparator() + System.lineSeparator());
						continue;
					}
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
						out.write(System.lineSeparator() + "/** " + System.lineSeparator() + " EnterMediaDB javascriptGenerator : " + script.getSrc() + System.lineSeparator() + script.getPath() + System.lineSeparator()  + " Modified: " + infile.getLastModified() + "  **/" + System.lineSeparator() + System.lineSeparator() );
						getOutputFiller().fill(reader,out);
						out.write(System.lineSeparator()  + System.lineSeparator() + System.lineSeparator());
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
			//tmpfile.getContent().set
			getPageManager().movePage(tmpfile, inPage);
			if( inPage.getContentItem() instanceof FileItem)
			{
				FileItem savedata = (FileItem)inPage.getContentItem();
				savedata.getFile().setLastModified(mostrecentmod);
			}
		}
	}

}
