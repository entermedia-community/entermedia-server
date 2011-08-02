package org.openedit.entermedia.model;

import org.openedit.entermedia.BaseEnterMediaTest;
import org.openedit.entermedia.cluster.NodeManager;

public class NodeTest extends BaseEnterMediaTest
{
	public void testListLocal()
	{
		NodeManager manager = (NodeManager)getBean("nodeManager");
		assertEquals("singlenode",manager.getLocalNode().getId() );
	}
}
