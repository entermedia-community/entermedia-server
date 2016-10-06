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
 */
package org.entermediadb.webui.tree;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.openedit.util.PathUtilities;

/**
 * This is the base implemation of a JavaScript renderer for output to the web.
 *
 * @author Matt Avery
 */
public abstract class BaseTreeRenderer implements TreeRenderer
{
	protected WebTree fieldWebTree;
	protected String fieldHome;

	protected String fieldImageDir = "/openedit/images/tree";
	protected Set fieldExpandedNodes;
	protected Object fieldSelectedNode;
	protected Object fieldLastCutoffNode;

	protected String fieldUrlPrefix = "";
	protected String fieldUrlPostfix = "";
	protected boolean  fieldFoldersLinked;
	protected boolean fieldRenderLeaves = true;
	protected boolean fieldAllowSelections = false;
	protected Set fieldSelectedNodes; 
	protected boolean fieldEditable;
	
	
	public boolean isEditable()
	{
		return fieldEditable;
	}


	public void setEditable(boolean inEditable)
	{
		fieldEditable = inEditable;
	}


	public BaseTreeRenderer()
	{
		
	}


	public boolean isAllowSelections()
	{
		return fieldAllowSelections;
	}

	public void setAllowSelections(boolean inAllowSelections)
	{
		fieldAllowSelections = inAllowSelections;
	}

	
	public Set getSelectedNodes()
	{
		if (fieldSelectedNodes == null)
		{
			fieldSelectedNodes = new HashSet();
		}
		return fieldSelectedNodes;
	}
	public boolean isIdSelected(String inNodeId)
	{
		return getSelectedNodes().contains(inNodeId); // TODO: look at this
	}
	public boolean isNodeSelected(Object inNode)
	{
		if( inNode == getSelectedNode() )
		{
			return true;
		}
		String inId = getId(inNode);
		return getSelectedNodes().contains(inId);
	}
	protected String getId(Object inNode)
	{
		return getWebTree().getModel().getId(inNode);
	}

	public void setSelectedNodes(Set inSelectedNodes)
	{
		fieldSelectedNodes = inSelectedNodes;
	}
	public void selectNode(Object inNode)
	{
		if( inNode != null )
		{
			getSelectedNodes().add(getId(inNode));
			
			Object parent = getWebTree().getModel().getParent(inNode);
			if( parent != null )
			{
				expandNode(parent);
			}
		}
	}
	public void unSelectNode(Object inNode)
	{
		getSelectedNodes().remove(getId(inNode));
	}
	public void selectNodes(Collection inNodes)
	{
		Set newselection = new HashSet();
		if(inNodes != null){
		for (Iterator iterator = inNodes.iterator(); iterator.hasNext();)
		{
			Object object = (Object) iterator.next();
			newselection.add(getId(object));
			Object parent = getWebTree().getModel().getParent(object);
			if( parent != null )
			{
				expandNode(parent);
			}
		}
		}
		setSelectedNodes(newselection);
		
	}

	
	public boolean isRenderLeaves()
	{
		return fieldRenderLeaves;
	}
	public void setRenderLeaves(boolean inRenderLeaves)
	{
		fieldRenderLeaves = inRenderLeaves;
	}
	public BaseTreeRenderer(WebTree inWebTree)
	{
		fieldWebTree = inWebTree;
	}
	public String getHome()
	{
		if ( fieldHome == null )
		{
			fieldHome = "";
		}
		return fieldHome;
	}

	public void setHome(String inHome)
	{
		fieldHome = inHome;
	}

	public String getExpandNodeCommand()
	{
		return "WebTree.expandTreeNode";
	}

	public String getImageDir()
	{
		return fieldImageDir;
	}
	public void setImageDir(String inImageDir)
	{
		fieldImageDir = inImageDir;
	}
	public void setWebTree(WebTree tree)
	{
		fieldWebTree = tree;
	}

	public WebTree getWebTree()
	{
		return fieldWebTree;
	}
	protected Set getExpandedNodes()
	{
		if (fieldExpandedNodes == null)
		{
			fieldExpandedNodes = new HashSet();
		}

		return fieldExpandedNodes;
	}

	@Override
	public void toggleNode(Object inNode)
	{
		if( hasBeenExpanded(inNode) )
		{
			collapseNode(inNode);
		}
		else
		{
			expandNode(inNode);
		}
	}
	public void expandNode(Object inNode)
	{
		if( inNode == null)
		{
			return;
		}
		String path = toUrl(inNode);
		if ( !getExpandedNodes().contains(path))
		{
			getExpandedNodes().add(path);
			//get all the parents somehow
			Object parent = getWebTree().getModel().getParent( inNode);
			if ( parent != null)
			{
				expandNode(parent);
			}
		}
	}
	
	/* (non-javadoc)
	 * @see org.entermediadb.webui.tree.TreeRenderer#collapseNode(java.lang.Object)
	 */
	public void collapseNode(Object inNode)
	{
		String path = toUrl(inNode);

		getExpandedNodes().remove(path);
	}

	public boolean hasBeenExpanded(Object inNode)
	{
		//Make sure it is expanded
		Object parent = inNode;
		while( parent != null)
		{
			String path = toUrl(inNode);
			if( getExpandedNodes().contains(path) )
			{
				parent = getWebTree().getModel().getParent(parent);
				//If we get to the root and it is selected still then we are ok!
				if( parent == null)
				{
					return true;
				}
			}
			else
			{
				return false;
			}
		}
		return false;
	}

	protected String quotes(String inString)
	{
		inString = inString.replace("\"", "\\\"");
		return "\"" + inString + "\"";
	}
	
	public String renderAsString()
	{
		StringBuffer js = new StringBuffer(  "var tree = new Tree( \n" );
		
		// Ensure the root node is always expanded.
		expandNode( getWebTree().getModel().getRoot() );

		js.append( renderNewFolder( getWebTree().getModel().getRoot()));
		js.append( ", " );
		js.append( quotes(getWebTree().getId()) );
		js.append( ", \"" );
		js.append(  getHome() );
		js.append( getImageDir() + "/\" );\n");
		if ( getUrlPrefix() != null)
		{
			js.append( "tree.setViewerUrl( '" + getHome() + getUrlPrefix() + "' );\n" );
		}
		if ( getUrlPostfix() != null)
		{
			js.append( "tree.setViewerUrlEnding( '" + getUrlPostfix() + "' );\n" );
		}
		js.append( "tree.showFolderData= " + isFoldersLinked() + ";\n");
		if ( getSelectedNode() != null)
		{
			js.append("tree.setSelectedNodeById(" );
			js.append(quotes(getWebTree().getModel().getId( getSelectedNode() ) ));
			js.append(");\n");
		}
		return js.toString();
	}
	public String renderAsString(String inNodeId)
	{
		return null;
	}
	public Object setSelectedNodeByUrl(String inPath)
	{
		if ( inPath == null)
		{
			return null;
		}
		
		if ( inPath.endsWith("/") && inPath.length() > 1 )
		{
			inPath = inPath.substring(0, inPath.length() - 1);
		}

		Object node = null;
		
		String root = toUrl( getWebTree().getModel().getRoot() );
		if ( PathUtilities.match(root, inPath ) )
		{
			node = getWebTree().getModel().getRoot();
		}
		else
		{		
			node = findNodeByUrl(getWebTree().getModel().getRoot(), inPath);
		}
		setSelectedNode(node);
		if  ( node != null)
		{
			//expand the parent
			Object parent = getWebTree().getModel().getParent(node);
			if ( parent != null)
			{
				expandNode(parent);				
			}
		}
		return node;
	}

	public Object findNodeByUrl(Object inRoot, String inUrl)
	{
		if(inUrl == null){
			return null;
		}
		int count = getWebTree().getModel().getChildCount(inRoot);
		for (int i = 0; i < count; i++)
		{
			Object child = getWebTree().getModel().getChild(inRoot,i);
			
			String test = toUrl( child );
			if ( PathUtilities.match(test,inUrl  ) )
			{
				return child;
			}

			//this test must be a directory
			if( !inUrl.startsWith(test))
			{
				continue;
			}
			child = findNodeByUrl(child,inUrl);
			if ( child != null)
			{
				return child;
			}
		}
		return null;
	}
	
	public void setSelectedNodeById(String inId)
	{
		if ( inId == null)
		{
			return;
		}
		Object node = getWebTree().getModel().getChildById(inId);
		setSelectedNode(node);
		if  ( node != null)
		{
			//expand the parent
			node = getWebTree().getModel().getParent(node);
			if ( node != null)
			{
				expandNode(node);				
			}
		}

	}

	
	protected String renderNewFolder(Object inNode)
	{
		StringBuffer js = new StringBuffer(
				"new Node( " + quotes( toName( inNode ) ) + ", " + quotes( toUrl( inNode ) ) + ", ");

		js.append(quotes(getWebTree().getModel().getId(inNode)));

		if ( getWebTree().getModel().isLeaf( inNode ) )
		{
			js.append(",null");
		}
		else
		{
			js.append(renderChildren(inNode));
		}

		js.append( "," + getWebTree().getModel().isLeaf( inNode ) );
		
		String iconsset = customIconSet(inNode);
		if( iconsset != null )
		{
			js.append(',');
			js.append("\"" + iconsset + "\")");
		}
		else
		{
			js.append(")");
		}

		return js.toString();
	}
	/**
	 * Allows a certain node to have a custom icon set. Icon sets can be defined in the tree.js
	 * @param inNode
	 * @return
	 */
	protected String customIconSet(Object inNode)
	{
		return null;
	}
	protected String renderChildren(Object inNode)
	{
		//	Use null for our folder icon, and the JavaScript tree will substitute either folder.gif or
		//	folderopen.gif, depending on whether the node is open or closed.
		StringBuffer js = new StringBuffer();
		
		if ( hasBeenExpanded( inNode ) )
		{
//			String collapseUrl = quotes("?" + "WebTreeName=" + getWebTree().getName() + "&oe-action=" +
//				getCollapseNodeCommand() + "&nodeID=" + getWebTree().getModel().getId(inNode));
//			js.append(collapseUrl +", new Array(\n");

			js.append(", new Array(\n");

			int c = getWebTree().getModel().getChildCount( inNode );
			for (int i = 0; i < c; i++ )
			{
				if ( i > 0 )
				{
					js.append(",\n");
				}
				Object childNode = getWebTree().getModel().getChild( inNode, i );
				js.append(renderNewFolder(childNode));
			}

			js.append(" ) ");
		}
		else
		{
//			String expandUrl = quotes("?" + "WebTreeName=" + getWebTree().getName() + "&oe-action=" +
//				getExpandNodeCommand() + "&nodeID=" + getWebTree().getModel().getId(inNode));

			// Might need to make this configurable later on.
			// Different controllers may want to use different commands here.
			js.append(",null " );
			
		//				js.append(
		//		quotes("?" + "WebTreeName=" + getWebTree().getName() + "&oe-action=" +
		//			getExpandNodeCommand() + "&nodeID=" + getWebTree().getModel().getId(inNode)) + ", null, ");
			
		}

		return js.toString();
	}
	
	/**
	 * The name is used on the tree display
	 * @param inNode
	 * @return
	 */
	public abstract String toName(Object inNode);
	
	/**
	 * The url will be triggered when a node is clicked
	 * @param inNode
	 * @return
	 */
	public abstract String toUrl(Object inNode);

	public String getCollapseNodeCommand()
	{
		return "WebTree.collapseTreeNode";
	}

	public String getUrlPrefix()
	{
		return fieldUrlPrefix;
	}
	public void setUrlPrefix(String inUrlPrefix)
	{
		fieldUrlPrefix = inUrlPrefix;
	}
	public boolean isFoldersLinked()
	{
		return fieldFoldersLinked;
	}
	public void setFoldersLinked(boolean inFoldersLinked)
	{
		fieldFoldersLinked = inFoldersLinked;
	}

	public Object getSelectedNode()
	{
		return fieldSelectedNode;
	}
	
	public String getSelectedId()
	{
		Object selected = getSelectedNode();
		if( selected == null)
		{
			return null;
		}
		return getWebTree().getModel().getId(selected);
	}
	
	public void setSelectedNode(Object inSelectedNode)
	{
		fieldSelectedNode = inSelectedNode;
		Object parent = getWebTree().getModel().getParent(inSelectedNode);
		if( parent != null )
		{
			expandNode(parent);
		}

	}
	public String getUrlPostfix()
	{
		return fieldUrlPostfix;
	}
	public void setUrlPostfix(String inUrlPostfix)
	{
		fieldUrlPostfix = inUrlPostfix;
	}
	public Object getLastCutoffNode()
	{
		return fieldLastCutoffNode;
	}
	public void setLastCutoffNode(Object inLastCutoffNode)
	{
		fieldLastCutoffNode = inLastCutoffNode;
	}
	public boolean isShowPathInfo(Object inNode)
	{
		//inNode == /a/b/c.html
		//Cutoff == /a
		//First make sure it is not expanded
	
		Object container = inNode;
		if( getWebTree().getModel().isLeaf(container) )
		{
			container = getWebTree().getModel().getParent(container);
		}
		if( hasBeenExpanded(container))
		{
			return false;
		}
		//See if we are below the cut off. If we are below or equal then show the node (return true)
		if( fieldLastCutoffNode != null)
		{
			Object parent = inNode;
			while( parent != null)
			{
				if( parent == fieldLastCutoffNode)
				{
					return true;
				}
				parent = getWebTree().getModel().getParent(parent);
			}
			return false;
		}
		return true; //Show it since it is not expanded
	}
}
