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

import java.io.Serializable;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * This class is a Web tree, which can render a <code>{@link WebTreeModel}</code> to an XML
 * document.
 *
 * @author Eric Galluzzo
 */
public class WebTree implements Serializable
{
	private static final Log log = LogFactory.getLog(WebTree.class);
	
	protected transient String fieldName;
	protected transient String fieldId;
	
	public String getRootId()
	{
		String id = getModel().getId( getModel().getRoot() );
		return id;
	}

	protected transient WebTreeModel fieldModel;
	protected transient TreeRenderer fieldTreeRenderer;

	public WebTree()
	{
	}

	/**
	 * Create a new WebTree which listens to the given model.
	 *
	 * @param inModel DOCUMENT ME!
	 */
	public WebTree(WebTreeModel inModel)
	{
		setModel(inModel);
		//expandNode(getModel().getRoot());
	}

	public boolean isEmpty()
	{
		if ((getModel() == null) || (getModel().getChildCount(getModel().getRoot()) == 0))
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 * Sets the model.
	 *
	 * @param model The model to set
	 */
	public void setModel(WebTreeModel model)
	{
		fieldModel = model;
		if ( fieldModel != null && fieldTreeRenderer != null)
		{
    		getTreeRenderer().expandNode( fieldModel.getRoot() );
		}
	}

	/**
	 * Gets the model.
	 *
	 * @return Returns a WebTreeModel
	 */
	public WebTreeModel getModel()
	{
		if (fieldModel == null)
		{
			fieldModel = new DefaultWebTreeModel();
		}

		return fieldModel;
	}

	public Object getChildChildren(int inCount)
	{
		List children = getModel().getChildren(getModel().getRoot() );
		if( children.size() < inCount)
		{
			Object parent = children.get(inCount);
			return getModel().getChildren(parent);
		}
		return null;
	}
	
	
	
	/**
	 * DOCUMENT ME!
	 *
	 * @param inName
	 */
	public void setName(String inName)
	{
		fieldName = inName;
	}

	/**
	 * DOCUMENT ME!
	 *
	 * @return
	 */
	public String getName()
	{
		if (fieldName == null)
		{
			fieldName = "WebTree";
		}

		return fieldName;
	}

	/**
	 * DOCME
	 *
	 * @return DOCME
	 */
	public String renderAsJavaScript()
	{		
		return render();
	}
	public String render()
	{
		TreeRenderer renderer = getTreeRenderer();
		return renderer.renderAsString();
	}
	public String render(String inNodeId)
	{
		if( inNodeId == null)
		{
			log.error(getName() + " was passed in a null node id ");
			inNodeId = "_";
		}
		TreeRenderer renderer = getTreeRenderer();
		return renderer.renderAsString(inNodeId);
	}

	public TreeRenderer getTreeRenderer()
	{
		if (fieldTreeRenderer == null)
		{
			fieldTreeRenderer = new WebTreeNodeTreeRenderer(this);
		}
		return fieldTreeRenderer;	
	}

	public void setTreeRenderer(TreeRenderer inTreeRenderer)
	{
		fieldTreeRenderer = inTreeRenderer;
	}

	public String toString()
	{
		return getTreeRenderer().renderAsString();
	}

	public String getId()
	{
		return fieldId;
	}

	public void setId(String inId)
	{
		fieldId = inId;
	}
	
	public boolean isChildSelected(Object inChild) 
	{
		Object check = getTreeRenderer().getSelectedNode();
		while( check != null)
		{
			if( check == inChild)
			{
				return true;
			}
			check = getModel().getParent(check);
		}
		return false;
	}
	
	public Object selectNodeByUrl(String inURL)
	{
		Object node = getTreeRenderer().setSelectedNodeByUrl(inURL);
		return node;
	}
	
	
	public boolean isExpanded(Object inNode){
		return getTreeRenderer().hasBeenExpanded(inNode);
	}
	
}
