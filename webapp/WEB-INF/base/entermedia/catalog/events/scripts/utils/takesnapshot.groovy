package utils;

import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.cluster.NodeManager
	


public void runit()
{
	NodeManager nodeManager = moduleManager.getBean("nodeManager");
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	
	String id = nodeManager.createDailySnapShot(mediaArchive.getCatalogId());
	context.putPageValue("snapid",id);
	log.info("Created " + id);
}

runit();

