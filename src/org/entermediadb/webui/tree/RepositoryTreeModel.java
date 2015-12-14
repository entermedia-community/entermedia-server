/*
Copyright (c) 2003 eInnovation Inc. All rights reserved

This library is free software; you can redistribute it and/or modify it under the terms
of the GNU Lesser General Public License as published by the Free Software Foundation;
either version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Lesser General Public License for more details.
*/

package org.entermediadb.webui.tree;

import org.openedit.OpenEditException;
import org.openedit.OpenEditRuntimeException;
import org.openedit.PageAccessListener;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;
import org.openedit.repository.ContentItem;
import org.openedit.repository.Repository;
import org.openedit.repository.RepositoryException;


/**
 * This model represents a tree of site content.
 *
 * @author Matt Avery, mavery@einnovation.com
 */
public class RepositoryTreeModel extends DefaultWebTreeModel implements PageAccessListener
{
	protected Repository fieldRepository;
	protected String fieldRootPath;
	protected PageManager fieldPageManager;

	/**
	 * Constructor for PageTreeModel.
	 *
	 * @param inSiteContext DOCUMENT ME!
	 */
	public RepositoryTreeModel(Repository inRepository)
	{
		this( inRepository, "/");
	}

	public RepositoryTreeModel( Repository inRepository, String inRootPath )
	{
		super();
		fieldRepository = inRepository;
		fieldRootPath = inRootPath;
	}


	/* (non-Javadoc)
	 * @see TreeModel#getRoot()
	 */
	public Object getRoot()
	{
		if (fieldRoot == null)
		{
			reload();
		}

		return fieldRoot;
	}


	/**
	 * Find the page tree node at the given path.  This method will not automatically expand any
	 * node in the tree if it is not already expanded.
	 *
	 * @param inPath The path (e.g. "abc/def/ghi.html")
	 *
	 * @return The node at the given path, or <code>null</code> if no node could be found that
	 * 		   matched the given path
	 */
	public RepositoryTreeNode findNode(String inPath)
	{
		return ((RepositoryTreeNode) getRoot()).findNode(inPath);
	}

	/**
		 *
		 */
	public void reload()
	{
		ContentItem rootItem;
		try
		{
			rootItem = getRepository().get( getRootPath() );
			//TODO: Add all the ignore paths depending on permissions
			//At least handle directory level permissions _site.xconf
			//TODO: Add user, page manager create basewebrequests
		}
		catch( RepositoryException e )
		{
			 throw new OpenEditRuntimeException(e);
		}
		RepositoryTreeNode newRoot = new RepositoryTreeNode( getRepository(), rootItem, "_");
		newRoot.fieldFilter = getFilter();
		newRoot.fieldPageManager = getPageManager();
		newRoot.setUrl(getRootPath());
		fieldRoot = newRoot;
		
	}
	public Repository getRepository()
	{
		return fieldRepository;
	}
	public void setRepository( Repository repository )
	{
		fieldRepository = repository;
	}
	public String getRootPath()
	{
		return fieldRootPath;
	}
	public void setRootPath( String rootPath )
	{
		fieldRootPath = rootPath;
	}


	/**
	 * DOCUMENT ME!
	 *
	 * @param inPage
	 * @param inRevision
	 *
	 * @throws OpenEditException
	 */
	public void pageAdded(Page inPage)
	{
		reload();
	}

	/**
	 * DOCUMENT ME!
	 *
	 * @param inPage
	 * @param inRevision
	 *
	 * @throws OpenEditException
	 */
	public void pageModified(Page inPage)
	{
		reload();
	}

	/**
	 * DOCUMENT ME!
	 *
	 * @param inPage
	 * @param inRevision
	 *
	 * @throws OpenEditException
	 */
	public void pageRemoved(Page inPage)
	{
		reload();
	}

	/**
	 * DOCUMENT ME!
	 *
	 * @param inPage
	 *
	 * @throws OpenEditException
	 */
	public void pageRequested(Page inPage)
	{
		// do nothing
	}
	
	protected boolean hasLoadedChildren(Object inRoot)
	{
		//Only look in nodes with already loaded children
		RepositoryTreeNode parent = (RepositoryTreeNode)inRoot;
		if( parent.fieldChildren == null)
		{
			return false;
		}

		return true;
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}
}
