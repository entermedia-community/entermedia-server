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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.naming.LinkException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.webui.tree.DefaultWebTreeNode;
import org.openedit.util.PathUtilities;


/**
 * This class represents a node in a {@link LinkTreeModel}.
 *
 * @author Matt Avery, mavery@einnovation.com
 */
public class LinkNode extends DefaultWebTreeNode implements Comparable
{
	//public static final DefaultWebTreeNode ERROR_NODE = new DefaultWebTreeNode( "Error accessing tree node." );
	protected Set fieldIgnoreTypes;
	protected Link fieldLink;

	private static final Log log = LogFactory.getLog(LinkNode.class);
	/**
	 * Create a new <code>PageTreeNode</code>.
	 *
	 * @param inFile The file that this node represents
	 * @param inPath The path to the file, relative to the site context root
	 * @throws LinkException
	 */
	public LinkNode( Link inLink )
	{
		super(  inLink.getId(), inLink.getText() );
		fieldLink = inLink;
		setLeaf(!getLink().hasChildren());
	}
	
	/**
	 * Get the first child of this node with the given name.
	 *
	 * @param inName The node name
	 *
	 * @return The node, or <code>null</code> if no such child could be found
	 */
	public LinkNode getChild(String inName)
	{
		for (Iterator iter = getChildren().iterator(); iter.hasNext();)
		{
			LinkNode child = (LinkNode) iter.next();

			if ((child.getName() != null) && child.getName().equals(inName))
			{
				return child;
			}
		}

		return null;
	}

	/* (non-Javadoc)
	 * @see DefaultWebTreeNode#getChildren()
	 */
	public List getChildren() 
	{
		if (fieldChildren == null)
		{
			fieldChildren = new ArrayList();
			reloadChildren();
		}

		return fieldChildren;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(Object)
	 */
	public int compareTo(Object o)
	{
		if (o == null)
		{
			return 1;
		}

		if (o instanceof LinkNode)
		{
			LinkNode node = (LinkNode) o;

			return getLink().getPath().compareTo(node.getLink().getPath());
		}
		else
		{
			return 0;
		}
	}

	/**
	 * Find the descendant of this node with the given path.
	 *
	 * @param inPath The path to find
	 *
	 * @return The node at the given path, or <code>null</code> if it could not be found
	 */
	public LinkNode findNode(String inPath)
	{
		// Quick initial checks...
		if (!inPath.startsWith("/"))
		{
			inPath = "/" + inPath;
		}

		if (inPath.equals("") || inPath.equals("/"))
		{
			return this;
		}

		int beforeSlashIndex = 0;

		if (inPath.startsWith("/"))
		{
			beforeSlashIndex = 1;
		}

		int nextSlashIndex = inPath.indexOf('/', beforeSlashIndex);

		if (nextSlashIndex < 0)
		{
			nextSlashIndex = inPath.length();
		}

		String childName = inPath.substring(beforeSlashIndex, nextSlashIndex);

		LinkNode child = getChild(childName);

		if (child == null)
		{
			return null;
		}
		else
		{
			return child.findNode(inPath.substring(nextSlashIndex));
		}
	}

	/**
	 * Reload the children of this page tree node.
	 */
	public void reloadChildren() 
	{
		getChildren().clear();

		if (getLink().hasChildren())
		{
			List childItems = getLink().getChildren();
			for ( Iterator iterator = childItems.iterator(); iterator.hasNext(); )
			{
				Link childItem = (Link) iterator.next();

				String name = childItem.getPath();
				boolean okToAdd = true;

				if( getIgnoreTypes() != null)
				{
					//we want to ignore some files in this directory ie. CVS
					for (Iterator iter = getIgnoreTypes().iterator(); iter.hasNext();)
					{
						String key = (String) iter.next();
	
						if (PathUtilities.match(name, key))
						{
							okToAdd = false;
							break;
						}
					}
				}
				if (okToAdd)
				{

					LinkNode child = createNode( childItem );
					child.setParent(this);

					getChildren().add(child);
				}
			}

			// Make sure the files appear in lexicographically increasing
			// order, with all the directories appearing before all the files.
		}
	}

	protected void setIgnoreTypes(Set inIgnoreTypes)
	{
		fieldIgnoreTypes = inIgnoreTypes;
	}

	protected Set getIgnoreTypes()
	{
		return fieldIgnoreTypes;
	}

	/**
	 * Method createPageTreeNode.
	 *
	 * @param childFile
	 * @param path
	 *
	 * @return PageTreeNode
	 */
	protected LinkNode createNode(Link childItem) 
	{
		LinkNode node = new LinkNode( childItem);
		node.setIgnoreTypes(getIgnoreTypes());
		return node;
	}

	public Link getLink()
	{
		return fieldLink;
	}
	public void setLink( Link Link )
	{
		fieldLink = Link;
	}
	public String getURL()
	{
		return getLink().getId();
	}
	public boolean hasLoadedChildren()
	{
		return getLink().getChildren() != null;
	}

}
