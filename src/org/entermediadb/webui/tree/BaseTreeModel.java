package org.entermediadb.webui.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.ListHitTracker;

public abstract class BaseTreeModel implements WebTreeModel
{
	public HitTracker getHitTracker(String inId)
	{
		HitTracker tracker = new ListHitTracker(getChildrenById(inId));
		return tracker;
	}
	
	public HitTracker getLeaves(String inId)
	{
		HitTracker tracker = new ListHitTracker();
		for (Iterator iterator = getChildrenById(inId).iterator(); iterator.hasNext();)
		{
			Object node = (Object) iterator.next();
			if (isLeaf(node))
			{
				tracker.add(node);
			}
		}
		return tracker;
	}
	
	public HitTracker getNonLeaves(String inId)
	{
		HitTracker tracker = new ListHitTracker();
		for (Iterator iterator = getChildrenById(inId).iterator(); iterator.hasNext();)
		{
			Object node = (Object) iterator.next();
			if (!isLeaf(node))
			{
				tracker.add(node);
			}
		}
		return tracker;
	}

	public List getChildrenById(String inId)
	{
		Object parent = findNodeById(getRoot(), inId);
		if( parent == null)
		{
			return null;
		}
		return getChildren(parent);
	}
	public List getParentPaths(String inId, String inRootNodeId)
	{
		List parents = new ArrayList();
		Object child =  findNodeById(getRoot(), inId); //last node
		Object top =  null;
		if( inRootNodeId != null )
		{
			top = findNodeById(getRoot(), inRootNodeId); //last node
		}
		while( child != null )
		{
			parents.add(child);
			if( child == top)
			{
				break;
			}
			child = getParent(child);
		}
		Collections.reverse(parents);
		return parents;
	}
	public List getParentPaths(String inId)
	{
		return getParentPaths(inId,0);
	}
	public List getParentPaths(String inId, int inDeepLevel)
	{
		List parents = new ArrayList();
		Object child =  findNodeById(getRoot(), inId);
		while( child != null )
		{
			parents.add(child);
			child = getParent(child);
		}
		Collections.reverse(parents);
		if( inDeepLevel > 0 )
		{
			if( inDeepLevel > parents.size() )
			{
				parents = Collections.EMPTY_LIST;
			}
			else
			{
				parents  = parents.subList(inDeepLevel, parents.size());
			}
		}
		return parents;
	}
	public boolean hasChildren(Object inNode)
	{
		return getChildCount(inNode) > 0;
	}

	public boolean hasFolderChildren(Object inNode)
	{
		for (Iterator iterator = getChildren(inNode).iterator(); iterator.hasNext();)
		{
			Object child = (Object) iterator.next();
			if( !isLeaf(child))
			{
				return true;
			}
		}
		return false;
	}
}
