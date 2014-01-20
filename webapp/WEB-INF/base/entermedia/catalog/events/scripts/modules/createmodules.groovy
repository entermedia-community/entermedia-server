package modules;

import org.entermedia.workspace.WorkspaceManager
import org.openedit.entermedia.MediaArchive

import com.openedit.hittracker.HitTracker


public void init(){
	MediaArchive archive = context.getPageValue("mediaarchive");
	WorkspaceManager manager= archive.getModuleManager().getBean(archive.getCatalogId(), "workspaceManager");
	HitTracker modules = archive.getList("module");
	String appid = context.findValue("applicationid");
	
	modules.each{
		manager.saveModule(archive.getCatalogId(), appid, it);
	}
}


init();