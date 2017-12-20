package utils

import org.entermediadb.asset.MediaArchive
import org.openedit.node.NodeManager;

public void runit()
{
	NodeManager nodeManager = moduleManager.getBean("elasticNodeManager");
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	String latest = mediaArchive.getCatalogSettingValue("latestrestoredsnap");
	
	String id = nodeManager.restoreLatest(mediaArchive.getCatalogId(), latest);
	if(id != null){
		log.info("Restored newer snapshot" + id);
		mediaArchive.saveCatalogSettingValue("latestrestoredsnap", id);
	} else {
		
		log.info("No new snapshot detected");
	}
	
}

runit();

