package org.entermediadb.desktops;

import java.util.Date;
import java.util.Map;

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

import com.google.common.base.Splitter;

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
		Desktop desktop = loadDesktop(inReq);
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
		Desktop desktop = loadDesktop(inReq);
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
	
	
	public Desktop loadDesktop(WebPageRequest inReq)
	{
		if(inReq.getRequest() == null || inReq.getUser() == null) {
			return null;
		}
		
		String isDesktopParameter = inReq.getRequestParameter("desktop");
		
		Desktop desktop = (Desktop) inReq.getPageValue("desktop");

		if(isDesktopParameter != null) 
		{
			if( "false".equals(isDesktopParameter) ) //Not used anymore
			{
				inReq.removeSessionValue("desktop");
				inReq.putPageValue("desktop", null);
				FolderManager manager = getFolderManager(inReq);
				manager.getDesktopManager().removeDesktop(inReq.getUserName());
				return null;
			}
			else if( Boolean.parseBoolean(isDesktopParameter))
			{
				FolderManager manager = getFolderManager(inReq);
				desktop = manager.getDesktopManager().loadDesktop(inReq.getUser(),System.getenv("HOSTNAME"));
				inReq.putSessionValue("desktop", desktop);
			}
		}
		
		
		String useragent = inReq.getRequest().getHeader("User-Agent");
		if(useragent.contains("eMediaLibrary")) 
		{
			if(desktop == null) 
			{
				FolderManager manager = getFolderManager(inReq);
				desktop = manager.getDesktopManager().loadDesktop(inReq.getUser(), useragent);
				inReq.putPageValue("desktop", desktop);
				inReq.putSessionValue("desktop", desktop);
			}

			String values = useragent.substring(useragent.indexOf("eMediaLibrary") + 13, useragent.length());
			Map<String, String> map = Splitter.on( " " ).withKeyValueSeparator( '/' ).split( values );
			String computername = map.get("ComputerName");
			if( computername != null) 
			{					
				desktop.setComputerName(computername);
			}
			String desktopversion = map.get("APIVersion");
			if( desktopversion != null)
			{
				desktop.setDesktopApiVersion((Integer.parseInt(desktopversion))); 
			}
		}
		
		return desktop;
	}
	
}
