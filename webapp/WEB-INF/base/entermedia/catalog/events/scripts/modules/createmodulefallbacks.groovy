package modules

import org.entermediadb.asset.MediaArchive
import org.entermediadb.workspace.WorkspaceManager
import org.openedit.hittracker.HitTracker

public void init(){
	MediaArchive archive = context.getPageValue("mediaarchive");
	WorkspaceManager manager= archive.getModuleManager().getBean(archive.getCatalogId(), "workspaceManager");
	HitTracker modules = archive.getList("module");
	String appid = context.findValue("applicationid");
	
	//group order librarycollections etc.
	//role (settingsgroup), saved searches (savedquery), hot folders, conversion presets, users, orders, collections, libraries, divisions, permissionsapp, preset configuration
	
	modules.each{
		manager.createModuleFallbacks( appid, it);
	}
}


init();