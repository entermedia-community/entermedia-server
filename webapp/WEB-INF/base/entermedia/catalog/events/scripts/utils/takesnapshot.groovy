package utils

import org.entermediadb.asset.MediaArchive
import org.openedit.node.NodeManager;

public void runit()
{
	NodeManager nodeManager = moduleManager.getBean("nodeManager");
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	
	String id = nodeManager.createDailySnapShot(mediaArchive.getCatalogId());
	context.putPageValue("snapid",id);
	log.info("Created " + id);
}

runit();

