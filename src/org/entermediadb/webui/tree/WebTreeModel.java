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

import java.util.List;

import org.openedit.hittracker.HitTracker;


/**
 * This is the model for a <code>{@link WebTree}</code>.
 *
 * @author Eric Galluzzo
 */
public interface WebTreeModel
{
	/**
	 * Returns the child of <code>parent</code> at index <code>index</code> in the parent's child
	 * array. <code>parent</code> must be a node previously obtained from this data source. This
	 * should not return <code>null</code> if <code>index</code> is a valid index for
	 * <code>parent</code> (that is <code>index &gt;= 0 && index &lt;
	 * getChildCount(parent)</code>).
	 *
	 * @param parent A node in the tree, obtained from this data source
	 *
	 * @return The child of <code>parent</code> at index <code>index</code>
	 */
	public Object getChild(Object parent, int index);

	public List getChildren(Object parent);

	public List getChildrenById(String inId);
	
	public HitTracker getHitTracker(String inId);

	/**
	 * Improve the performance by using a tree logic
	 * @param inRoot
	 * @param inId
	 * @return
	 */
	public Object findNodeById(Object inRoot, String inId);

	public List getChildrenInRows(Object inParent, int inColCount);

	/**
	 * Returns the number of children of <code>parent</code>.  Returns 0 if the node is a leaf or
	 * if it has no children. <code>parent</code> must be a node previously obtained from this
	 * data source.
	 *
	 * @param parent A node in the tree, obtained from this data source
	 *
	 * @return The number of children of the node <code>parent</code>
	 */
	public int getChildCount(Object parent);

	/**
	 * Returns the index of <code>child</code> in <code>parent</code>. If <code>parent</code> is
	 * <code>null</code> or <code>child</code> is <code>null</code>, returns -1.
	 *
	 * @param parent A node in the tree, obtained from this data source
	 * @param child The node we are interested in
	 *
	 * @return The index of the child in the parent, or -1 if either <code>child</code> or
	 * 		   <code>parent</code> is <code>null</code>
	 */
	public int getIndexOfChild(Object parent, Object child);

	/**
	 * Returns <code>true</code> if <code>node</code> is a leaf.  It is possible for this method to
	 * return <code>false</code> even if <code>node</code> has no children.  A directory in a
	 * filesystem, for example, may contain no files; the node representing the directory is not a
	 * leaf, but it also has no children.
	 *
	 * @param node A node in the tree, obtained from this data source
	 *
	 * @return <code>true</code> if <code>node</code> is a leaf
	 */
	public boolean isLeaf(Object node);

	/**
	 * Returns the root of the tree.  Returns <code>null</code> only if the tree has no nodes.
	 *
	 * @return the root of the tree
	 */
	public Object getRoot();
	
	/**
	 * Returns a unique ID for the given node.  This ID should be unique
	 * throughout the tree.
	 * 
	 * @param inNode  The node
	 * 
	 * @return  The node's ID
	 */
	public String getId( Object inNode );

	public Object getParent(Object inNode);

	public List getParentPaths(String inNodeId);

	public List getParentPaths(String inNodeId, String inRootNodeId);

	public Object getChildById(String inId);

	public boolean hasChildren(Object inNode);

	public boolean hasFolderChildren(Object inNode);
	
}
