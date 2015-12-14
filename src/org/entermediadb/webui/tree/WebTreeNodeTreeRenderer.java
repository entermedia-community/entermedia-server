/*
Copyright (c) 2003 eInnovation Inc. All rights reserved

This library is free software; you can redistribute it and/or modify it under the terms
of the GNU Lesser General Public License as published by the Free Software Foundation;
either version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Lesser General Public License for more details.
*/

/*
 * Created on May 29, 2003
 *
 * Render a JavaScript representation of the web tree.
 */
package org.entermediadb.webui.tree;

import java.util.Iterator;
import java.util.List;

/**
 * DOCUMENT ME!
 *
 * @author Matt Avery
 */
public class WebTreeNodeTreeRenderer extends HtmlTreeRenderer
{
	protected boolean fieldFriendlyNames = false;
	
	public WebTreeNodeTreeRenderer(WebTree inWebTree)
	{
		super( inWebTree );
	}

	public boolean isFriendlyNames()
	{
		return fieldFriendlyNames;
	}

	public void setFriendlyNames(boolean inFriendlyNames)
	{
		fieldFriendlyNames = inFriendlyNames;
	}

	/* (non-javadoc)
	 * @see org.entermediadb.webui.tree.BaseTreeRenderer#toName(java.lang.Object)
	 */
	public String toName(Object inNode)
	{
		DefaultWebTreeNode node = (DefaultWebTreeNode)inNode;
		String name = node.getName();
		if( isFriendlyNames())
		{
			name = name.replace('_', ' ');
			name = name.substring(0,1).toUpperCase() + name.substring(1);
				
			int p = name.lastIndexOf('.');
			if ( p > 0 )
			{
				name = name.substring(0,p);
			}			
		}
		return name;
	}
	protected String customIconSet(Object inNode)
	{
		DefaultWebTreeNode node = (DefaultWebTreeNode)inNode;
		return node.getIconSet();
	}
	public String toUrl(Object inNode)
	{
		DefaultWebTreeNode node = (DefaultWebTreeNode)inNode;

		return node.getURL();
	}

	/* (non-javadoc)
	 * @see org.entermediadb.webui.tree.BaseTreeRenderer#toId(java.lang.Object)
	 */
	public String toId(Object inNode)
	{
		DefaultWebTreeNode node = (DefaultWebTreeNode)inNode;

		return node.getId();
	}

	public void expandAll(Object inRoot)
	{
		expandNode(inRoot);
		List children = getWebTree().getModel().getChildren(inRoot);
		for (Iterator iterator = children.iterator(); iterator.hasNext();)
		{
			Object object = (Object) iterator.next();
			expandAll(object);
		}
	}

}
