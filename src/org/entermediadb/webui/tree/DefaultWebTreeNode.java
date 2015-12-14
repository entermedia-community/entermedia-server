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

import org.openedit.data.BaseData;
import org.openedit.util.strainer.Filter;


/**
 * This class represents a node in a <code>{@link DefaultWebTreeModel}</code>. It has the
 * attributes most people will need: name, URL, and icon URL.  Most of the methods in
 * <code>DefaultWebTreeModel</code> delegate to this class.
 *
 * @author Eric Galluzzo
 */
public class DefaultWebTreeNode extends BaseData
{
	protected static int staticNextID = 0;
	protected List fieldChildren;
	protected String fieldIconURL;
	protected boolean fieldLeaf;
	protected Filter fieldFilter;
	protected DefaultWebTreeNode fieldParent;
	protected String fieldIconSet;
	
	/**
	 * Create a tree node with the given name, without an icon or link.
	 *
	 * @param inName DOCUMENT ME!
	 */
	public DefaultWebTreeNode(String inName)
	{
		setName(inName);
		setId( String.valueOf(staticNextID++) );
	}

	public DefaultWebTreeNode(String inId, String inName)
	{
		setName(inName);
		setId(inId);
	}

	/**
	 * Get the child at the given position in this node.
	 *
	 * @param inIndex DOCUMENT ME!
	 *
	 * @return
	 */
	public DefaultWebTreeNode getChild(int inIndex)
	{
		return (DefaultWebTreeNode) getChildren().get(inIndex);
	}

	/**
	 * Get the number of children that this node has.
	 *
	 * @return
	 */
	public int getChildCount()
	{
		return getChildren().size();
	}

	/**
	 * Gets the children.
	 *
	 * @return Returns a List
	 */
	public List getChildren()
	{
		if (fieldChildren == null)
		{
			fieldChildren = new ArrayList();
		}

		return fieldChildren;
	}

	/**
	 * Gets the ID of this node, which is unique within the VM for the life of the VM (unless you
	 * do weird things with classloaders).
	 *
	 * @return Returns an int
	 */


	/**
	 * Sets the icon URL.
	 *
	 * @param iconURL The icon URL to set
	 */
	public void setIconURL(String iconURL)
	{
		fieldIconURL = iconURL;
	}

	/**
	 * Gets the icon URL.
	 *
	 * @return Returns a String
	 */
	public String getIconURL()
	{
		return fieldIconURL;
	}

	/**
	 * Get the index of the given child in this node
	 *
	 * @param inChild DOCUMENT ME!
	 *
	 * @return The index, or -1 if the child could not be found
	 */
	public int getIndexOfChild(DefaultWebTreeNode inChild)
	{
		return getChildren().indexOf(inChild);
	}

	/**
	 * Set whether this node is a leaf.
	 *
	 * @param inLeaf DOCUMENT ME!
	 */
	public void setLeaf(boolean inLeaf)
	{
		fieldLeaf = inLeaf;
	}

	/**
	 * Determine whether this node is a leaf node.
	 *
	 * @return
	 */
	public boolean isLeaf()
	{
		return fieldLeaf;
	}


	/**
	 * Gets the URL.
	 *
	 * @return Returns a String
	 */
	public String getURL()
	{
		if ( getParent() != null)
		{
			String p =getParent().getURL();
			if ( p.endsWith("/"))
			{
				return  p + getName();
			}
			else
			{
				return  p + "/" + getName();
			}
		}
		else
		{
			return getName(); //the root does not need a special URL since it is part of the base path
		}
	}

	/**
	 * Add the given child to this node's children.
	 *
	 * @param inNode DOCUMENT ME!
	 */
	public void addChild(DefaultWebTreeNode inNode)
	{
		getChildren().add(inNode);
		inNode.setParent(this);
	}

	/**
	 * DOCME
	 */
	public void reloadChildren()
	{
		//nothing to do?
	}
	public DefaultWebTreeNode getParent()
	{
		return fieldParent;
	}
	public void setParent(DefaultWebTreeNode inParent)
	{
		fieldParent = inParent;
	}

	public Filter getFilter()
	{
		return fieldFilter;
	}

	public void setFilter(Filter inFilter)
	{
		fieldFilter = inFilter;
	}
	public boolean hasLoadedChildren()
	{
		return fieldChildren != null;
	}

	public String getIconSet()
	{
		return fieldIconSet;
	}

	public void setIconSet(String inIconSet)
	{
		fieldIconSet = inIconSet;
	}

	public String getID() {
		return getId();
	}
}
