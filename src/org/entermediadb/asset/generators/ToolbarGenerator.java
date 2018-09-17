/*
 * Created on May 19, 2006
 */
package org.entermediadb.asset.generators;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.generators.Output;
import org.openedit.page.Page;
import org.openedit.users.User;

public class ToolbarGenerator extends BaseToolBarGenerator
{
	private static final Log log = LogFactory.getLog(ToolbarGenerator.class);
	
	public void generate(WebPageRequest inPageRequest, Page inPage, Output inOut) throws OpenEditException
	{
		boolean added = addHeader(inPageRequest, inOut);

		if (added)
		{
			Page header = getPageManager().getPage( getHeaderPath() );
			if( header.exists() )
			{
				inPageRequest.putPageValue("editPage",inPage);
				header.generate(inPageRequest, inOut);
			}
		}	
		
		getWraps().generate(inPageRequest, inPage, inOut);
		
		if ( added && getFooterPath() != null )
		{
			addFooter(inPageRequest, inOut);
		}
	}	
	public boolean addHeader( WebPageRequest inPageRequest, Output inOut ) throws OpenEditException
	{		
		User user = inPageRequest.getUser();
		
		if (user == null)
		{
			return false;
		}
		Page requestedPage  = inPageRequest.getPage();
		if( requestedPage.getPath().equals(getHeaderPath()) || requestedPage.getInnerLayout() == null)
		{
			return false;
		}
		if( requestedPage.getPath().equals(getFooterPath()))
		{
			return false;
		}
		String val = (String) inPageRequest.getSessionValue("oe_edit_mode");
		if(val == null){
			 val = user.get("oe_edit_mode");	
		}
		
		
		
		
		if( "preview".equals( val ) )
		{
			return false;
		}

		
		
		if ("editing".equals(val) &&  !requestedPage.isBinary() && inPageRequest.isEditable() )
		{
			return true;
		}

		//boolean debug = Boolean.parseBoolean(user.get("showdebug"));

		if( "debug".equals(val) && requestedPage.isHtml())
		{
			String show = inPageRequest.getPageProperty("showdebug");
			if( show != null)
			{
				return Boolean.parseBoolean(show);
			}
			return true; //we probably don't need to check for the header
		}
		
		
	
		
//		String show = inPageRequest.getRequestParameter("showtoolbar");
//		if ( show != null && !Boolean.parseBoolean(show))
//		{
//			return false;
//		}

			return false;
	}
	public boolean addFooter(WebPageRequest inPageRequest, Output inOut) throws OpenEditException
	{
		Page requestedPage  = inPageRequest.getPage();
		if ( requestedPage.isBinary())
		{
			return false;
		}
		Page footer = getPageManager().getPage( getFooterPath() );

		inPageRequest.putPageValue("editPage",requestedPage);
		footer.generate(inPageRequest, inOut);
		return true;
	}
	/**
	 * This seems to complex. The only thing needed now is editable checks
	 * @param inPageRequest
	 * @return
	 * @throws OpenEditException
	 
	protected boolean checkFlags( WebPageRequest inPageRequest ) throws OpenEditException
	{
		PageAction inAction = inPageRequest.getCurrentAction();

		String flagKey = inAction.getConfig().getChildValue( "flag" );
		
		Page requestedPage  = (Page)inPageRequest.getPage();
		String propertyFlag = requestedPage.getProperty( flagKey );
		String pageValueFlag = (String)inPageRequest.getPageValue( flagKey );
		String requestParameterFlag = inPageRequest.getRequestParameter( flagKey );
		log.debug( "Checking decoration flag " + flagKey + " for page " + requestedPage );
		if ( "false".equals( propertyFlag )
		  || "false".equals( pageValueFlag )
		  || "false".equals( requestParameterFlag ) )
		{
			return false;
		}
		
		String permission = inAction.getConfig().getChildValue( "permission" );
		if ( permission != null )
		{
			if ( inPageRequest.getUser() == null)
			{
				return false;				
			}
			else
			{
				log.debug("Checking user");
				return inPageRequest.getUser().hasPermission(permission);
			}
		}
		return true;
	}
	*/
	

}
