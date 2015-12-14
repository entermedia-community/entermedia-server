/*
Copyright (c) 2003 eInnovation Inc. All rights reserved

This library is free software; you can redistribute it and/or modify it under the terms
of the GNU Lesser General Public License as published by the Free Software Foundation;
either version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Lesser General Public License for more details.
*/

package org.entermediadb.links;

import java.util.List;

import org.entermediadb.webui.tree.DefaultWebTreeModel;


/**
 * This model represents a tree of site content.
 *
 * @author Matt Avery, mavery@einnovation.com
 */
public class LinkTreeModel extends DefaultWebTreeModel
{
	protected LinkTree fieldLinkTree;
	protected String fieldRootPath;
	long lastMod = 1001;

	/**
	 * Constructor for PageTreeModel.
	 *
	 * @param inSiteContext DOCUMENT ME!
	 */
	public LinkTreeModel(LinkTree inLinkTree)
	{
		this( inLinkTree, null);
	}

	public LinkTreeModel( LinkTree inLinkTree, String inRootPath )
	{
		super();
		fieldLinkTree = inLinkTree;
		fieldRootPath = inRootPath;
	}


	/* (non-Javadoc)
	 * @see TreeModel#getRoot()
	 */
	public Object getRoot()
	{
		if (fieldRoot == null )
		{
			reload();
		}

		return fieldRoot;
	}

	public List getChildren(Object parent)
	{
//		if( getLinkTree().getLastModified() != lastMod )
//		{
//			reload();
//		}
		return super.getChildren(parent);
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
	public LinkNode findNode(String inPath)
	{
		return ((LinkNode) getRoot()).findNode(inPath);
	}

	/**
		 *
		 */
	public void reload()
	{
		Link rootItem;
		if( getRootPath() == null)
		{
			rootItem = getLinkTree().getRootLink();
		}
		else
		{
			rootItem = getLinkTree().getLink( getRootPath() );
		}
		LinkNode newRoot = new LinkNode( rootItem);
		fieldRoot = newRoot;
		lastMod = getLinkTree().getLastModified();
	}
	/**
	 * @param inString
	 */
	public void ignore(String inString)
	{
		LinkNode node = (LinkNode)getRoot();
		node.getIgnoreTypes().add( inString);
	}
	public LinkTree getLinkTree()
	{
		return fieldLinkTree;
	}
	public void setLinkTree( LinkTree LinkTree )
	{
		fieldLinkTree = LinkTree;
	}
	public String getRootPath()
	{
		return fieldRootPath;
	}
	public void setRootPath( String rootPath )
	{
		fieldRootPath = rootPath;
	}
	
	protected boolean hasLoadedChildren(Object inRoot)
	{
		//Only look in nodes with already loaded children
		LinkNode parent = (LinkNode)inRoot;
		return parent.hasLoadedChildren();
	}

}
