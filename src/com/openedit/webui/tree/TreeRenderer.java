/*
 * Created on Oct 29, 2003
 */
package com.openedit.webui.tree;


/**
 * @author cburkey
 *
 */
public interface TreeRenderer
{
	String renderAsString();
	String renderAsString(String inNodeId);
	public void expandNode(Object inNode);
	public boolean hasBeenExpanded(Object inNode);
	/**
	 * @param inObject
	 */
	void collapseNode(Object inQueue);
	void setSelectedNode(Object inChildNode);
	Object getSelectedNode();
	Object getLastCutoffNode();
	void setLastCutoffNode(Object inCutOff);
	
	public Object setSelectedNodeByUrl(String inPath);
	public Object findNodeByUrl(Object inRoot, String inUrl);
	
	//void setRenderLeaves(boolean inRenderLeaves);
	//boolean isChildSelected(Object inParent);
}
