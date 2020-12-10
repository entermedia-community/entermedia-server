package org.entermediadb.desktops;

import java.util.Date;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
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
		String categoryid = inReq.getRequestParameter("categoryid");
		
		Searcher linksearcher = archive.getSearcher("userdownloads");
		Data link = linksearcher.createNewData();
		
		Asset asset = null;
		Category cat = null;
		if( assetid != null)
		{
			asset = archive.getCachedAsset(assetid);
			link.setSourcePath(asset.getSourcePath());
			link.setValue("assetid",asset.getId());
		}
		else
		{
			cat = archive.getCategory(categoryid);
			link.setSourcePath(cat.getSourcePath());
			link.setValue("categoryid",cat.getId());
		}
		link.setValue("date",new Date());
		link.setValue("user",inReq.getUser());
		linksearcher.saveData(link,inReq.getUser());


		//TODO: Desktop to start download this
		Desktop desktop = manager.getDesktopManager().getDesktop(inReq.getUserName());
		if(asset != null)
		{
			desktop.downloadAsset(archive, link);
		}
		else
		{
			desktop.downloadCategory(archive, link);
		}
//		if( desktop.isBusy())
//		{
//			log.info("Desktop still busy");
//			return;
//		}

	}

	
	public void open(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		ProjectManager manager = getProjectManager(inReq);
		
		String downloadid = inReq.getRequestParameter("userdownloadid");
		Data userdownload = archive.query("userdownloads").id(downloadid).searchOne();

		//TODO: Desktop to start download this
		Desktop desktop = manager.getDesktopManager().getDesktop(inReq.getUserName());
		if( userdownload.get("assetid") != null)
		{
			desktop.openAsset(archive, userdownload.get("assetid"));
		}
		else
		{
			Category cat = archive.getCategory(userdownload.get("categoryid") );
			desktop.openCategory(archive, cat);			
		}

	}

	
}
