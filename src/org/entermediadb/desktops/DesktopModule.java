package org.entermediadb.desktops;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.find.FolderManager;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;

public class DesktopModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(DesktopModule.class);

	public FolderManager getFolderManager(WebPageRequest inReq) {
		MediaArchive archive = getMediaArchive(inReq);
		FolderManager manager = (FolderManager) getModuleManager().getBean(archive.getCatalogId(), "folderManager");
		inReq.putPageValue("folderManager", manager);
		return manager;
	}
	public void startDownload(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		FolderManager manager = getFolderManager(inReq);
		
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
			if( cat == null)
			{
				log.info("No such category");
				return;
			}
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
		FolderManager manager = getFolderManager(inReq);
		
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
	
	
	public void loadDesktop(WebPageRequest inReq)
	{
		if(inReq.getRequest() == null) {
			return;
		}
		
		String isDesktopParameter = inReq.getRequestParameter("desktop");
		Desktop desktop = (Desktop) inReq.getPageValue("desktop");
		if(desktop == null || isDesktopParameter != null) 
		{
			if( "false".equals(isDesktopParameter) )
			{
				inReq.removeSessionValue("desktop");
				inReq.putPageValue("desktop", null);
				FolderManager manager = getFolderManager(inReq);
				manager.getDesktopManager().removeDesktop(inReq.getUserName());
				return;
			}
			
			String isDesktop = inReq.getRequest().getHeader("User-Agent");
			if(Boolean.parseBoolean(isDesktopParameter) || (isDesktop.contains("eMediaLibrary") && inReq.getUser() != null)) 
			{
				FolderManager manager = getFolderManager(inReq);
				desktop = manager.getDesktopManager().getDesktop(inReq.getUserName());
				if(desktop == null) {
					desktop = manager.getDesktopManager().connectDesktop(inReq.getUser());
				}
				inReq.putSessionValue("desktop", desktop);
			}
		}
	}
	
}
