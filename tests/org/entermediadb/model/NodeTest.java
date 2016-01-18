package org.entermediadb.model;

import org.entermediadb.asset.BaseEnterMediaTest;
import org.openedit.node.NodeManager;

public class NodeTest extends BaseEnterMediaTest
{
	public void testListLocal()
	{
		NodeManager manager = (NodeManager)getBean("nodeManager");
		assertEquals("singlenode",manager.getLocalNode().getId() );
	}
}
