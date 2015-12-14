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

import java.util.ArrayList;
import java.util.List;

import org.openedit.util.strainer.Filter;

/**
 * This is a default implementation of a {@link WebTreeModel}, which assumes that all its nodes are
 * <code>{@link DefaultWebTreeNode}</code>s.
 *
 * @author Eric Galluzzo
 */
public class DefaultWebTreeModel extends BaseTreeModel
{
	protected DefaultWebTreeNode fieldRoot;
	protected Filter fieldFilter;
	
	/**
	 * Construct a tree model with no root (i.e. no tree).
	 */
	public DefaultWebTreeModel()
	{
	}

	/**
	 * Construct a tree model with the given root node.
	 *
	 * @param inRoot DOCUMENT ME!
	 */
	public DefaultWebTreeModel(DefaultWebTreeNode inRoot)
	{
		fieldRoot = inRoot;
	}

	/* (non-Javadoc)
	 * @see TreeModel#getChild(Object, int)
	 */
	public Object getChild(Object parent, int index)
	{
		return ((DefaultWebTreeNode) parent).getChild(index);
	}
	public List getChildrenInRows(Object inParent, int inColCount)
	{
		//Now break up the page into rows by dividing the count they wanted
		List children = getChildren(inParent);
		double rowscount = (double)children.size() / (double)inColCount;
		
		List rows = new ArrayList();
		for (int i = 0; i < rowscount; i++)
		{
			int start = i*inColCount;
			int end = i*inColCount + inColCount;
			List sublist = children.subList(start,Math.min( children.size(),end ));
			rows.add(sublist);
		}
		return rows;

	}
	public List getChildren(Object parent)
	{
		return ((DefaultWebTreeNode) parent).getChildren();
	}

	/* (non-Javadoc)
	 * @see TreeModel#getChildCount(Object)
	 */
	public int getChildCount(Object parent)
	{
		return ((DefaultWebTreeNode) parent).getChildCount();
	}

	/* (non-Javadoc)
	 * @see TreeModel#getIndexOfChild(Object, Object)
	 */
	public int getIndexOfChild(Object parent, Object child)
	{
		return ((DefaultWebTreeNode) parent).getIndexOfChild((DefaultWebTreeNode) child);
	}

	/* (non-Javadoc)
	 * @see TreeModel#isLeaf(Object)
	 */
	public boolean isLeaf(Object node)
	{
		return ((DefaultWebTreeNode) node).isLeaf();
	}

	/**
	 * Set the root node of this tree model.
	 *
	 * @param inRoot DOCUMENT ME!
	 */
	public void setRoot(DefaultWebTreeNode inRoot)
	{
		fieldRoot = inRoot;
	}

	/* (non-Javadoc)
	 * @see TreeModel#getRoot()
	 */
	public Object getRoot()
	{
		return fieldRoot;
	}
	
	public String getId( Object inNode )
	{
		return String.valueOf( ( (DefaultWebTreeNode) inNode ).getID() );
	}

	public Object getParent(Object inNode)
	{
		if(inNode == null){
			return null;
		}
		DefaultWebTreeNode child = (DefaultWebTreeNode) inNode;
		
		return child.getParent();
	}

	public Object getChildById(String inId)
	{
		return findNodeById(getRoot(), inId);
	}
	
	public Object findNodeById(Object inRoot, String inId)
	{
		if( inId == null)
		{
			return null;
		}
		String test = getId(inRoot);
		if ( test.equals(inId)  )
		{
			return inRoot;
		}
		String start = test;
		int virtual = start.indexOf('~');
		if( virtual > 0)
		{
			start = start.substring(0,virtual);
		}
		if( inId.startsWith(start) || hasLoadedChildren(inRoot) )  //performance optimization
		{
			int count = getChildCount(inRoot);
			for (int i = 0; i < count; i++)
			{
				Object child = getChild(inRoot,i);
				child = findNodeById(child,inId);
				if ( child != null)
				{
					return child;
				}
			}
		}
		return null;
	}
	//can be overriden
	protected boolean hasLoadedChildren(Object inRoot)
	{
		//Only look in nodes with already loaded children
		DefaultWebTreeNode parent = (DefaultWebTreeNode)inRoot;
		return parent.hasLoadedChildren();
	}

	public Filter getFilter()
	{
		return fieldFilter;
	}

	public void setFilter(Filter inFilter)
	{
		fieldFilter = inFilter;
	}


	
	
}
