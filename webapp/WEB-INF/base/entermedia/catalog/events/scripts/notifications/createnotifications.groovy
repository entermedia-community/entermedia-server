package notifications

import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.push.PushManager

public void init(){
	log.info("------ Running Asset Approved Notification ------");
	MediaArchive archive = context.getPageValue("mediaarchive");
	PushManager pushmanager = (PushManager)archive.getModuleManager().getBean("pushManager");
	pushmanager.pullApprovedAssets(context,archive);
	log.info("------ Finished Asset Approved Notification ------");
}

init();