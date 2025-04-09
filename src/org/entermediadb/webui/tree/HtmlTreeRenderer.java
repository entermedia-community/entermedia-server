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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * This is a placeholder for a future version
 *
 */
public abstract class HtmlTreeRenderer extends BaseTreeRenderer
{
	private static final Log log = LogFactory.getLog(HtmlTreeRenderer.class);
	protected boolean fieldExpandingAll = false;
	protected String fieldIconHome;
	protected int fieldIconWidth = 16;
	public int getIconWidth()
	{
		return fieldIconWidth;
	}
	public void setIconWidth(int inIconWidth)
	{
		fieldIconWidth = inIconWidth;
	}
	/*
	 * Gets the root directory for icons.
	 */
	public String getIconHome() {
		if (fieldIconHome == null)
			fieldIconHome = "/system/images/tree/";
		return fieldIconHome;
	}
	/**
	 * Sets the root directory for icons.
	 * @param inIconHome the root directory for icons. If <code>null</code> defaults to <code>/openedit/images/tree/</code>.
	 * 	
	 */
	public void setIconHome(String inIconHome) {
		fieldIconHome = inIconHome;
		if (inIconHome!=null && !fieldIconHome.endsWith("/"))
			fieldIconHome = fieldIconHome + "/";
	}
	public HtmlTreeRenderer(WebTree inWebTree)
	{
		fieldWebTree = inWebTree;
	}
	public HtmlTreeRenderer()
	{
		
	}

	public String renderAsString()
	{
		StringBuffer js = new StringBuffer( );
		
		// Ensure the root node is always expanded.
		Object obj = getWebTree().getModel().getRoot();
		expandNode( obj );

		//Amazingly... In IE6.0 you cannot name your div id the same as a globally defined javascript variable
		js.append("<div class=\"dtree\" id='root" + getWebTree().getName() + "'>");
		js.append( renderFolder( getWebTree().getModel().getRoot(),0)); //line levels and blank levels
		js.append("</div>");
		return js.toString();
	}
	
	public String renderAsString(String inNodeId)
	{
		StringBuffer js = new StringBuffer( );
		WebTreeModel model = getWebTree().getModel();
		int levels = 0;
		Object child = model.getChildById(inNodeId);
		if( child != null)
		{
			Object parent = child;
			while (parent != null && parent != model.getRoot())
			{
				levels++;
				parent = model.getParent(parent);
			}
			js.append( renderFolder( child,levels)); //line levels and blank levels
		}
		return js.toString();
	}

	protected String renderFolder(Object inNode, int level)
	{
		//	Use null for our folder icon, and the JavaScript tree will substitute either folder.png or
		//	folderopen.png, depending on whether the node is open or closed.
		StringBuffer js = new StringBuffer();
		String id = getWebTree().getModel().getId(inNode);
		String name = toName(inNode);
		String path = toUrl(inNode);
//		String post = "";
//		if( isLast)
//		{
//			post = "bottom";
//		}
		String selected = "node";
		boolean select = false;
//		if( fieldLastCutoffNode != null)
//		{
			if( getSelectedNode() == inNode )
			{
				select = true;
			}
			else
			{
				if( inNode == getLastCutoffNode() )
				{
					//If the selected node is visible then no need to make the cut off visible
					Object parent = getSelectedNode();
					if( parent != null)
					{
						parent = getWebTree().getModel().getParent(parent);
						if( !hasBeenExpanded(parent) )
						{
							select = true;
						}
					}
				}
			}
		if( select )
		{
			selected = "nodeSel";
		}
		String icon = customIconSet(inNode);
		if( icon == null)
		{
			icon = "";
		}
		int wide = getIconWidth();

		boolean folder = !getWebTree().getModel().isLeaf(inNode);

		if ( folder )
		{
			boolean expanded = hasBeenExpanded( inNode );
			boolean shouldexpand = false;

			boolean isroot = false;
			if (inNode == getWebTree().getModel().getRoot())
			{
				isroot = true;
			}	

			int padding = (level)*wide;
			if( padding == 0)
			{
				padding = wide/3;
			}

			renderRow(inNode, js, id, name, path, icon, expanded, shouldexpand, isroot, level , padding, select);
		}
		else if( isRenderLeaves())
		{
			renderLeaf(level, js, id, name, path, select, icon, wide);
		}
		return js.toString();
	}
	protected void renderLeaf(int level, StringBuffer js, String id, String name, String path, boolean select, String icon, int wide)
	{
//		js.append("\n	</div>\n");
//			if (entire)
//			{
//			} 
//			else {
			js.append("<div  id='" + getWebTree().getName() + "treerow" + id + "'  >");
			js.append("\n<div class='" + (select ? "dTreeNodeSel ":"") + "treerowtext'  id='" + getWebTree().getName() + "treerowtext" + id + "'  path='" + path + "' nodeid='"+ id +"' >");
			js.append("\n<div class='treerowinside' style='padding-left: " + (level)*wide + "px;'>");
			js.append("<img alt='' src='" + getHome() + getIconHome() + "rightempty.png' id='jpageTree" + id + "' />");
//			}
		//indent(js,level+2);
		js.append("<a title='" + path + "' href='#" + path + "' onclick=\"return " + getWebTree().getName() +".jumpToNode('" + path + "','" + id + "');\"  >");
		//js.append("<img src='" + getHome() + getIconHome() + icon + "page.png' class='treeicon' id='" + getWebTree().getName() + "ipageTree" + id + "' alt=''  />" + name + "</a></div></div></div>");
		js.append(name + "</a></div></div></div>");
	}
	protected void renderRow(Object inNode, StringBuffer js, String id, String name, String path, String icon, boolean expanded, boolean shouldexpand, boolean isroot,int level, int padding, boolean select)
	{
		js.append("<div  id='" + getWebTree().getName() + "treerow" + id + "'  >");
		js.append("<div class='" + (select ? "dTreeNodeSel ":"") + "treerowtext'  id='" + getWebTree().getName() + "treerowtext" + id + "'  path='" + path + "' nodeid='"+ id +"'>");
		js.append("<div  class='treerowinside' style='padding-left: " + padding + "px;' >");

		//Has children so make it a link to open and close
		if( (!isroot && isRenderLeaves() &&  getWebTree().getModel().hasChildren(inNode) ) 
				|| (!isroot && !isRenderLeaves() && getWebTree().getModel().hasFolderChildren(inNode)))
		{
			js.append("<a class='node' href='#" + path + "' onclick=\"return " + getWebTree().getName() +".toggleNode('" + path + "','");
			js.append( getWebTree().getId() );
			js.append("','" + id + "');\" >");
			if( expanded )	
			{
				//if( level > 0)
				if (inNode != getWebTree().getModel().getRoot())
				{
					js.append("<img alt='' src='" + getHome() + getIconHome() + "down.png' id='jpageTree" + id + "' />");
				}
				//js.append("<img alt='' class='treeicon' src='" + getHome() + getIconHome() + icon + "folderopen.png' id='ipageTree" + id + "' />" );
			}
			else
			{
				shouldexpand = true;
				if (inNode != getWebTree().getModel().getRoot())
				{
					js.append("<img alt='' src='" + getHome() + getIconHome() + "right.png' id='jpageTree" + id + "' />");
				}	
			}
			js.append("</a>");
		}
		else
		{
			if( !isroot)
			{
				js.append("<img alt='' src='" + getHome() + getIconHome() + "right.png' id='jpageTree" + id + "' />" );
			}
		}
		js.append("<a title='" + path + "' href='#" + path + "' onclick=\"");
		if( shouldexpand )
		{
			js.append("return " + getWebTree().getName() +".jumpAndOpenNode('" + path + "','");
			js.append( getWebTree().getId() );
			js.append("','" + id + "');\"");				
		}
		else
		{
			js.append("	return " + getWebTree().getName() +".jumpToNode('" + path + "','" + id + "');\" ");				
			//js.append("	return jumpAndOpenNode('" + path + "','" + id + "');\" ");
		}
		js.append( " id='" + getWebTree().getName() + "spageTree" + id + "'>" + name + "</a>");
		js.append("</div>\n");
		js.append("</div>\n");

		if ( expanded )
		{
			int c = getWebTree().getModel().getChildCount( inNode );
			for (int i = 0; i < c; i++ )
			{
				Object childNode = getWebTree().getModel().getChild( inNode, i );
				try
				{
					js.append(renderFolder(childNode,level + 1 ));
				}
				catch ( Exception ex)
				{
					//js.append("Error: " + ex);
					log.error(ex);
				}
			}				
		}
		js.append("</div>\n");

	}
}
