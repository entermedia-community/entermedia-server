package utils

import org.elasticsearch.snapshots.SnapshotInfo
import org.entermediadb.asset.MediaArchive
import org.openedit.node.NodeManager;

public void runit()
{
	NodeManager nodeManager = moduleManager.getBean("elasticNodeManager");
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	String latest = mediaArchive.getCatalogSettingValue("latestrestoredsnap");
	
	
	
	
	
	
	String id = restoreLatest(mediaArchive,mediaArchive.getCatalogId(), latest);
	if(id != null){
		log.info("Restored newer snapshot" + id);
		mediaArchive.setCatalogSettingValue("latestrestoredsnap", id);
	} else {
		
		log.info("No new snapshot detected");
	}
	
}

runit();

public String restoreLatest(MediaArchive inArchive, String inCatalogId, String lastrestored) {
	
	List snapshots = inArchive.getNodeManager().listSnapShots(inCatalogId);
	if(snapshots.size() == 0) {
		return null;
	}
	SnapshotInfo info = (SnapshotInfo) snapshots.get(0);
	if(lastrestored == null) {
		lastrestored="";
	}
	
	if(lastrestored.equals(info.name())) {
		return null;
	}
		
	
	inArchive.getNodeManager().restoreSnapShot(inCatalogId, info.name());
	
	return info.name();
	
}
