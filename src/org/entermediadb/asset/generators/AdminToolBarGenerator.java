/*
 * Created on Dec 28, 2004
 */
package org.entermediadb.asset.generators;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.generators.Output;
import org.openedit.page.Page;
import org.openedit.profile.UserProfile;
import org.openedit.users.User;

/**
 * Inserts a decoration header just after the opening "body" tag
 * and inserts a decoration footer just before the closing "body" tag.
 * 
 * @author Matthew Avery, mavery@einnovation.com
 */
public class AdminToolBarGenerator extends BaseToolBarGenerator
{
	public static Log log = LogFactory.getLog(AdminToolBarGenerator.class);
	protected ModuleManager fieldModuleManager;
	
	protected String fieldSearch;
	
	public AdminToolBarGenerator()
	{
	}
	
	public void setSearch( String inHtmlTag )
	{
		fieldSearch = inHtmlTag;
	}
	public void generate( WebPageRequest inContext, Page inPage, Output inOut ) throws OpenEditException
	{
		User user = inContext.getUser();
		
		if( user == null )
		{
			getWraps().generate(inContext, inPage, inOut);
			return;
		}
		Boolean showtoolbar = false;
		
		
		showtoolbar = (Boolean)inContext.getPageValue("canshowadmintoolbar");
		if( showtoolbar == null )
		{
			showtoolbar = false;
			UserProfile profile = inContext.getUserProfile();
			if( profile != null)
			{
				String show = profile.get("admintoolbar");
				showtoolbar = Boolean.parseBoolean(show);
			}
			if( !showtoolbar)
			{
				showtoolbar = user.hasPermission("oe.administration");
			}
			if( !showtoolbar)
			{
				showtoolbar = user.isInGroup("administrators");
			}
		}
		
		if(!showtoolbar){
			if(inContext.getSessionValue("realuser") != null){
				showtoolbar = true;
			}
		}
//		if( !showtoolbar)
//		{
//			String mode = (String)user.getProperty("oe_edit_mode");
//			if( "edit".equals( mode) || "debug".equals(mode) )
//			{
//				showtoolbar = true;
//			}
//		}
//		if( showtoolbar )
//		{
//			String mode = inContext.getRequestParameter("oe_edit_mode");
//			if( "preview".equals(mode) )
//			{
//				showtoolbar = false;
//			}
//		}
		
		if( showtoolbar )
		{
			String toolbar = inContext.getContentProperty("showadmintoolbar");
			if( toolbar != null)
			{
				showtoolbar = Boolean.parseBoolean(toolbar);
			}
		}
		
		if( !showtoolbar )
		{
			getWraps().generate(inContext, inPage, inOut);
			return;
		}
		//show the toolbar
		
		Output oldOut = inContext.getPageStreamer().getOutput();
		//check login

		ByteArrayOutputStream scapture = null;
		try
		{
			 scapture = new ByteArrayOutputStream();
				Writer capture = new OutputStreamWriter(scapture, inPage.getCharacterEncoding() );
				Output fakeout = new Output(capture, scapture );
				inContext.getPageStreamer().setOutput(fakeout);
				
				getWraps().generate(inContext, inPage, fakeout);
						
			inContext.getPageStreamer().setOutput(oldOut);
			
			fakeout.getWriter().flush();
			String pageContent = scapture.toString(inPage.getCharacterEncoding());
			int start = pageContent.indexOf(getSearch());
			if( start == -1)
			{
				start = pageContent.indexOf(getSearch().toUpperCase());
			}
			if ( start == -1)
			{
				log.error("No hit found for " + getSearch());
				writePage( pageContent, inOut );
				return;
			}
			int end = pageContent.indexOf("\n", start);
		
			inOut.getWriter().write( pageContent.substring(0,end +1 ) );
			Page header = getPageManager().getPage(getHeaderPath() );

			header.generate(inContext, inOut);
				
			inOut.getWriter().write(pageContent.substring( end));
			inOut.getWriter().flush();
		} 
		catch ( Exception ex)
		{
			inContext.getPageStreamer().setOutput(oldOut);
			if( ignoreError(ex) )
			{
				log.info("Browser canceled request");
				return;
			}
			//write out any errors
			if( ex instanceof OpenEditException)
			{
				throw (OpenEditException)ex;
			}
			throw new OpenEditException(ex);
		}
		
	}

	public String getSearch()
	{
		return fieldSearch;
	}

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}


}
