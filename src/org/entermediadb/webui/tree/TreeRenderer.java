/*
 * Created on Oct 29, 2003
 */
package org.entermediadb.webui.tree;

import java.util.Collection;


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
	public void toggleNode(Object inNode);
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
	
	public boolean isIdSelected(String inNodeId);
	public boolean isNodeSelected(Object inNode);
	public void selectNode(Object inNode);
	public void unSelectNode(Object inNode);

	public void selectNodes(Collection inNodes);
	public Collection getSelectedNodes();

}
