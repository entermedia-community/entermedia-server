package org.entermediadb.desktops;

import java.util.Date;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.projects.ProjectManager;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;

public class DesktopModule extends BaseMediaModule
{
	public ProjectManager getProjectManager(WebPageRequest inReq) {
		MediaArchive archive = getMediaArchive(inReq);
		ProjectManager manager = (ProjectManager) getModuleManager().getBean(archive.getCatalogId(), "projectManager");
		inReq.putPageValue("projectmanager", manager);
		return manager;
	}
	public void startDownload(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		ProjectManager manager = getProjectManager(inReq);
		
		String assetid = inReq.getRequestParameter("assetid");
		
		Asset asset = archive.getAsset(assetid);
		
		Searcher linksearcher = archive.getSearcher("userdownloads");
		Data link = linksearcher.createNewData();
		
		link.setSourcePath(asset.getSourcePath());
		link.setValue("assetid",asset.getId());
		link.setValue("date",new Date());
		link.setValue("user",inReq.getUser());
		linksearcher.saveData(link,inReq.getUser());


		//TODO: Desktop to start download this
		Desktop desktop = manager.getDesktopManager().getDesktop(inReq.getUserName());
		
//		if( desktop.isBusy())
//		{
//			log.info("Desktop still busy");
//			return;
//		}

	}
	
}
