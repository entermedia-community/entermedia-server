package snapshot

import org.entermediadb.asset.MediaArchive
import org.openedit.node.NodeManager;

public void runit()
{
	NodeManager nodeManager = moduleManager.getBean("elasticNodeManager");
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	
	String id = nodeManager.createDailySnapShot("system", true);
	context.putPageValue("snapid",id);
	log.info("Created " + id);
}

runit();

