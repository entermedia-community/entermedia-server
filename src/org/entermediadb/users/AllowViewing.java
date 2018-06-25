/*
Copyright (c) 2003 eInnovation Inc. All rights reserved

This library is free software; you can redistribute it and/or modify it under the terms
of the GNU Lesser General Public License as published by the Free Software Foundation;
either version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Lesser General Public License for more details.
*/

package org.entermediadb.users;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.config.Configuration;
import org.openedit.page.Page;
import org.openedit.page.PageSettings;
import org.openedit.page.Permission;
import org.openedit.page.manage.PageManager;
import org.openedit.util.PathUtilities;
import org.openedit.util.strainer.Filter;


/**
 * This action enforces that the currently logged-in user has a certain specified permission, and
 * redirects to the login page otherwise.  A sample configuration would look like this:
 * <pre>
 *   &lt;path-action path="/openedit/*" name="enforceAdminPrivilege"&gt;
 *     &lt;login-path&gt;/openedit/authentication/logon.html&lt;/login-path&gt;
 *     &lt;permission&gt;wsp.administration&lt;/permission&gt;
 *     &lt;exclude&gt;/openedit/authentication/logon.html&lt;/exclude&gt;
 *     &lt;exclude&gt;/openedit/dologon.html&lt;/exclude&gt;
 *     &lt;exclude&gt;/openedit/editors/*&lt;/exclude&gt;
 *   &lt;/path-action&gt;
 * </pre>
 *
 * @author Eric Galluzzo
 */
public class AllowViewing
{
	protected static final String DEFAULT_LOGIN_PATH = "/system/components/authentication/logon.html";
	protected static final String DEFAULT_ADMIN_PERMISSION = "oe.administration";

	private static final Log log = LogFactory.getLog(AllowViewing.class);

	protected String fieldLoginPath;
	protected List fieldExcludes;
	protected PageManager fieldPageManager;
	protected boolean fieldForbid;
	

	public boolean isForbid()
	{
		return fieldForbid;
	}

	public void setForbid(boolean inForbid)
	{
		fieldForbid = inForbid;
	}

	/* (non-Javadoc)
	 * @see org.openedit.action.Command#execute(java.util.Map, java.util.Map)
	 */
	public void execute( WebPageRequest inReq ) throws OpenEditException
	{
		configure(inReq);
		Page page = (Page) inReq.getPage(); //urlUtils.requestPath();
		String requestPath = page.getPath();

		if (!inExcludeList(requestPath))
		{
			Permission filter = inReq.getPage().getPermission("view"); 		
			if ( (filter != null) )			
			{
				if ( !filter.passes( inReq ))
				{
					 if( isForbid() )
					 {
						if( inReq.getResponse() != null )
						{
							inReq.getResponse().setStatus(HttpServletResponse.SC_FORBIDDEN);
							inReq.setHasRedirected(true);
						}
					 }
					 else
					 {
						log.error(inReq.getUserName() + " has no view permission" + filter + " for " + page.getPath() + " sending redirect to login. Profile ");
						if(inReq.getUserProfile() != null)
						{
							log.error("Profile was " + inReq.getUserProfile() );
						}
						inReq.putPageValue("oe-exception", "You do not have permission to view "+ page.getPath()  );
	
						 //this is the original page someone might have been on. Used in login
						 inReq.putSessionValue("originalEntryPage",inReq.getContentPage().getPath() );
						 String fullOriginalEntryPage = (String)inReq.getSessionValue("fullOriginalEntryPage");
						 if( fullOriginalEntryPage == null)
						 {
							 inReq.putSessionValue("fullOriginalEntryPage",inReq.getPathUrlWithoutContext() );
						 }
						 inReq.redirect( getLoginPath() );
					 }
				}
			}
			else
			{
				log.info("No view restrictions have been set for " + requestPath);
			}
		}

	}

	/**
	 * Determine whether the request path is in the exclude list in the given configuration.
	 *
	 * @param inPath The request
	 *
	 * @return <code>true</code> if the path is excluded, <code>false</code> if not
	 */
	protected boolean inExcludeList(String inPath)
	{
		for (Iterator iter = getExcludes().iterator(); iter.hasNext();)
		{
			String path = (String)iter.next();

			if (PathUtilities.match(inPath, path))
			{
				log.debug(
					"Excluded path " + inPath + " from " + getClass().getName() +
					" because it matched " + path);

				return true;
			}
		}
		if ( inPath.equals( getLoginPath() ) )
		{
			return true;
		}
		String relative = PathUtilities.resolveRelativePath(getLoginPath(), inPath);
		if ( inPath.equals( relative ) )
		{
			return true;
		}
		return false;
	}
	/* (non-Javadoc)
	 * @see org.openedit.command.Command#load(com.anthonyeden.lib.config.Configuration)
	 */
	public void configure( WebPageRequest inReq )
	{
		fieldLoginPath = inReq.findValue( "login-path" );
		
		Configuration element = inReq.getCurrentAction().getConfig();
		PageSettings settings = inReq.getPage().getPageSettings();
		for (Iterator iter = element.getChildren("exclude").iterator(); iter.hasNext();)
		{
			Configuration excludeElem = (Configuration) iter.next();
			String path = excludeElem.getValue();
			path = settings.replaceProperty(path);
			getExcludes().add( path );
		}
		String forbid = element.getAttribute("forbid");
		setForbid(Boolean.valueOf(forbid));
	}
	
	protected String getLoginPath()
	{
		if (fieldLoginPath == null)
		{
			fieldLoginPath = DEFAULT_LOGIN_PATH;
		}
		return fieldLoginPath;
	}
	
	protected List getExcludes()
	{
		if (fieldExcludes == null)
		{
			fieldExcludes = new ArrayList();
		}
		return fieldExcludes;
	}
	public PageManager getPageManager()
	{
		return fieldPageManager;
	}
	public void setPageManager( PageManager pageManager )
	{
		fieldPageManager = pageManager;
	}

	/**
	 * Determine whether the given user passes the given filter.
	 *
	 * @param inReq The user to query
	 * @param inFilter The filter through which to pass the user
	 *
	 * @return boolean  <code>true</code> if the user passes, <code>false</code> if not
	 *
	 * @throws OpenEditException If the filter threw an exception
	 */
	protected boolean userPassesFilter( Filter inFilter )
		throws OpenEditException
	{
		return ((inFilter == null) || inFilter.passes( this ));
	}

}
