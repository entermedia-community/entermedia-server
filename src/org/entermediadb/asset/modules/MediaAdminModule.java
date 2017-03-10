package org.entermediadb.asset.modules;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.events.PathEventManager;
import org.entermediadb.modules.update.Downloader;
import org.entermediadb.workspace.WorkspaceManager;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.node.NodeManager;
import org.openedit.page.Page;
import org.openedit.page.PageProperty;
import org.openedit.page.PageSettings;
import org.openedit.page.manage.PageManager;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.PathUtilities;
import org.openedit.util.ZipUtil;

public class MediaAdminModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(MediaAdminModule.class);
	protected WorkspaceManager fieldWorkspaceManager;
	protected PageManager fieldPageManager;

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	public WorkspaceManager getWorkspaceManager()
	{
		return fieldWorkspaceManager;
	}

	public void setWorkspaceManager(WorkspaceManager inWorkspaceManager)
	{
		fieldWorkspaceManager = inWorkspaceManager;
	}

	public void listThemes(WebPageRequest inReq)
	{
		String skinsPath = "/themes";
		List children = getPageManager().getChildrenPaths(skinsPath, true);
		Map skins = new HashMap();

		for (Iterator iterator = children.iterator(); iterator.hasNext();)
		{
			String path = (String) iterator.next();
			Page theme = getPageManager().getPage(path);
			if (theme.isFolder() && theme.get("themename") != null)
			{
				skins.put("/themes/" + theme.getName(), theme.get("themename"));
			}
		}
		inReq.putPageValue("themes", skins);
	}

	public void changeTheme(WebPageRequest inReq) throws Exception
	{
		String layout = inReq.getRequestParameter("theme");
		if (layout == null)
		{
			return;
		}
		String path = inReq.getRequestParameter("path");
		if (path == null)
		{
			return;
		}
		// "/" + inReq.findValue("applicationid");
		Page page = getPageManager().getPage(path); // This is the root level
													// for this album
		PageProperty skin = new PageProperty("themeprefix");
		if ("default".equals(layout))
		{
			page.getPageSettings().removeProperty("themeprefix");
		}
		else
		{
			skin.setValue(layout);
			page.getPageSettings().putProperty(skin);
		}
		getPageManager().saveSettings(page);
	}

	public void deployUploadedApp(WebPageRequest inReq) throws Exception
	{
		Page uploaded = getPageManager().getPage("/WEB-INF/temp/importapp.zip");
		String catid = inReq.getRequestParameter("appcatalogid");
		String destinationid = inReq.getRequestParameter("destinationappid");
		if (destinationid.startsWith("/"))
		{
			destinationid = destinationid.substring(1);
		}
		getWorkspaceManager().deployUploadedApp(catid, destinationid, uploaded);
	}

	public void deleteCatalog(WebPageRequest inReq) throws Exception
	{
		Searcher searcher = getSearcherManager().getSearcher("system", "catalog");
		//searchtype=catalog&id=$appcatalogid
		String st = inReq.getRequestParameter("searchtype");
		String id = inReq.getRequestParameter("id");
		
		if( id.length() < 2)
		{
			throw new OpenEditException("Invalid ID");
		}
		
		Data catalog = (Data)searcher.searchById(id);
		if( catalog != null)
		{
			searcher.delete(catalog, null);
		}
		if( id.endsWith("catalog"))
		{
			Page home = getPageManager().getPage("/" + id.substring(0, id.lastIndexOf("/catalog") ), true);
			getPageManager().removePage(home);
		}
		else
		{
			Page home = getPageManager().getPage("/" + id, true);
			getPageManager().removePage(home);			
		}
		

		Page data = getPageManager().getPage("/WEB-INF/data/" + id, true);
		getPageManager().removePage(data);

		NodeManager nodeManager = (NodeManager)getModuleManager().getBean(id,"nodeManager");
		nodeManager.deleteCatalog(id);
		
	}

	public void deployApp(WebPageRequest inReq) throws Exception
	{
		String appcatalogid = inReq.getRequestParameter("appcatalogid");
		Searcher searcher = getSearcherManager().getSearcher(appcatalogid, "app");

		Data site = null;
		String id = inReq.getRequestParameter("id");
		if (id == null)
		{
			site = searcher.createNewData();
		}
		else
		{
			site = (Data) searcher.searchById(id);
		}
		String frontendid = inReq.findValue("frontendid");
		if (frontendid == null)
		{
			throw new OpenEditException("frontendid was null");
		}
		String deploypath = inReq.findValue("deploypath");
		if (!deploypath.startsWith("/"))
		{
			deploypath = "/" + deploypath;
		}
		site.setProperty("deploypath", deploypath);

		String module = inReq.findValue("module");
		site.setProperty("module", module);

		String name = inReq.findValue("sitename");
		site.setName(name);

		// site.setProperty("frontendid",frontendid);

		searcher.saveData(site, inReq.getUser());
		Data frontend = getSearcherManager().getData("system", "frontend", frontendid);
		Page copyfrompage = getPageManager().getPage(frontend.get("path"));
		// Page copyfrompage =
		// getPageManager().getPage("/WEB-INF/base/manager/components/newworkspace");

		Page topage = getPageManager().getPage(deploypath);
		if (!topage.exists())
		{
			getPageManager().copyPage(copyfrompage, topage);
		}
		topage = getPageManager().getPage(topage.getPath(), true);

		topage.getPageSettings().setProperty("catalogid", appcatalogid);

		String appid = deploypath;
		if (appid.startsWith("/"))
		{
			appid = appid.substring(1);
		}
		if (appid.endsWith("/"))
		{
			appid = appid.substring(0, appid.length() - 1);
		}
		topage.getPageSettings().setProperty("applicationid", appid);
		//topage.getPageSettings().setProperty("appmodule", site.get("module"));

		getPageManager().saveSettings(topage);

	}

	public void saveRows(WebPageRequest inReq) throws Exception
	{
		String catalogid = inReq.findValue("catalogid");
		Searcher searcher = getSearcherManager().getSearcher(catalogid, "catalogsettings");

		String[] fields = inReq.getRequestParameters("field");
		for (int i = 0; i < fields.length; i++)
		{
			Data existing = (Data) searcher.searchById(fields[i]);
			if (existing == null)
			{
				// log.error("No default value" + fields[i]);
				// continue;
				existing = searcher.createNewData();
				existing.setId(fields[i]);
			}
			boolean save = false;
			String[] values = inReq.getRequestParameters(fields[i] + ".value");
			if (values != null && values.length > 0)
			{
				if (values.length == 1)
				{
					if (!values[0].equals(existing.get("value")))
					{
						save = true;
						existing.setProperty("value", values[0]);
					}
				}
				else
				{
					save = true;
					StringBuffer buffer = new StringBuffer();
					for (int j = 0; j < values.length; j++)
					{
						buffer.append(values[j]);
						if (j + 1 < values.length)
						{
							buffer.append(' ');
						}
					}
					existing.setProperty("value", buffer.toString());
				}
			}
			else
			{
				if (existing.get("value") != null)
				{
					save = true;
					existing.setProperty("value", null);
				}
			}
			if (save)
			{
				searcher.saveData(existing, inReq.getUser());
				MediaArchive archive = getMediaArchive(inReq);
				archive.getCacheManager().remove("catalogsettings", existing.getId());
			}
		}
	}

	public void saveModule(WebPageRequest inReq) throws Exception
	{
		Data module = (Data) inReq.getPageValue("data");

		String appid = inReq.findValue("applicationid");
		String catalogid = inReq.findValue("catalogid");
		getWorkspaceManager().saveModule(catalogid, appid, module);
	}

	public void reloadSettings(WebPageRequest inReq) throws Exception
	{
		String catalogid = inReq.findValue("catalogid");
		
		Collection tables = getSearcherManager().reloadLoadedSettings(catalogid);
		
		inReq.putPageValue("tables", tables);

	}

	public void toggleSearchLogs(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		if( archive.getSearcherManager().getShowSearchLogs(archive.getCatalogId()))
		{
			archive.getSearcherManager().setShowSearchLogs(archive.getCatalogId(),false);
			archive.setCatalogSettingValue("log_all_searches","false");
		}
		else
		{
			archive.getSearcherManager().setShowSearchLogs(archive.getCatalogId(),true);
			archive.setCatalogSettingValue("log_all_searches","true");
		}
	}	
	public void initCatalogs(WebPageRequest inReq)
	{
		PathEventManager manager = (PathEventManager)getModuleManager().getBean("system", "pathEventManager");
		manager.getPathEvents();

		HitTracker catalogs = getSearcherManager().getList("system","catalog");
		for (Iterator iterator = catalogs.iterator(); iterator.hasNext();)
		{
			try
			{
				Data data = (Data) iterator.next();
				String catalogid = data.getId();
				
				NodeManager nodemanager = (NodeManager)getModuleManager().getBean(catalogid,"nodeManager");
				boolean existed = nodemanager.containsCatalog(catalogid);
				
				manager = (PathEventManager)getModuleManager().getBean(catalogid, "pathEventManager");
				manager.getPathEvents();
				
				if( !existed)
				{
					//import any data sitting there for importing
					MediaArchive archive = (MediaArchive)getModuleManager().getBean(catalogid,"mediaArchive");
					
					Page cat = getPageManager().getPage("/" + catalogid + "/site.xconf" );
					if( !cat.exists())
					{
						
						PageManager pageManager = archive.getPageManager();
						PageSettings homesettings = pageManager.getPageSettingsManager().getPageSettings("/" + catalogid + "/_site.xconf");
						homesettings.setProperty("catalogid", catalogid);
						homesettings.setProperty("fallbackdirectory","/entermedia/catalog");
						pageManager.getPageSettingsManager().saveSetting(homesettings);
						pageManager.clearCache();
					}
					
				}
			}
			catch ( Exception ex)
			{
				log.error(ex);
			}
		}
	}

	public void createSiteSnapshot(WebPageRequest inReq)
	{
		//Use the archive
		String siteid = inReq.getRequestParameter("id");
		Data site = getSearcherManager().getData("system","site",siteid);

		Searcher snaps = getSearcherManager().getSearcher("system", "sitesnapshot");

		PathEventManager manager = (PathEventManager)getModuleManager().getBean("system", "pathEventManager");

		HitTracker exports = snaps.query().match("snapshotstatus","pendingexport").search();
		if( exports.size() > 0)
		{
			inReq.putPageValue("status", "Snapshots are already pending");
			manager.runSharedPathEvent("/system/events/snapshot/exportsite.html");
			return;
		}
			
		
		Data snapshot = snaps.createNewData();
		String folder = DateStorageUtil.getStorageUtil().formatDateObj(new Date(), "yyyy-MM-dd-HH-mm-ss");
		snapshot.setValue("folder", folder);
		
		snapshot.setName(folder);
		snapshot.setValue("site", siteid);
		snapshot.setValue("snapshotstatus","pendingexport");
		snaps.saveData(snapshot);
		manager.runSharedPathEvent("/system/events/snapshot/exportsite.html");

		//PathEvent event = manager.getPathEvent("/system/events/data/exportsite.html");
		inReq.putPageValue("site", site);
		
		inReq.putPageValue("snapshot", snapshot);

	}

	public void restoreSiteSnapshot(WebPageRequest inReq)
	{
		String snapid = inReq.getRequestParameter("snapid");
		Searcher snaps = getSearcherManager().getSearcher("system", "sitesnapshot");
		Data snap = (Data)snaps.searchById(snapid);
		PathEventManager manager = (PathEventManager)getModuleManager().getBean("system", "pathEventManager");

		inReq.putPageValue("status", "Snapshots are already pending");
		snap.setValue("snapshotstatus","pendingrestore");
		snaps.saveData(snap);
		manager.runSharedPathEvent("/system/events/snapshot/restoresite.html");
		inReq.putPageValue("snapshot", snap);

	}
	
	public void deploySite(WebPageRequest inReq) throws Exception
	{
		//make us a catalog id
		//Download a snapshot? no use snapshots by URL to do that. Show a URL and allow paste of URL
		String sitename = inReq.getRequestParameter("sitename");
		
		Searcher sites = getSearcherManager().getSearcher("system", "site");
		Data site = sites.createNewData();
		String rootpath = inReq.getRequestParameter("rootpath");
		site.setValue("rootpath", rootpath);
		
		String catalogid = inReq.getRequestParameter("sitecatalogid");
		if( catalogid == null)
		{
			catalogid = rootpath.substring(1) + "/catalog";
		}
		site.setValue("catalogid", catalogid);
		site.setName(sitename);
		sites.saveData(site);
		inReq.setRequestParameter("siteid", site.getId());
		
//		Searcher snaps = getSearcherManager().getSearcher("system", "sitesnapshot");
//
//		Data snapshot = snaps.createNewData();
//		String folder = DateStorageUtil.getStorageUtil().formatDateObj(new Date(), "yyyy-MM-dd-HH-mm-ss");
//		snapshot.setValue("folder", folder);
//		snapshot.setName(folder + " site added"); //Init
//		snapshot.setValue("site", site.getId());
//		snapshot.setValue("snapshotstatus","pendingrestore");
//		snaps.saveData(snapshot);
//		
//		PathEventManager manager = (PathEventManager)getModuleManager().getBean("system", "pathEventManager");
//		manager.runSharedPathEvent("/system/events/snapshot/restoresite.html");
//		inReq.putPageValue("snapshot", snapshot);
		
	}
	public void zipSnapshot(WebPageRequest inReq) throws Exception
	{
		//ZipGenerator
		//Check on the path and user
		//Cancel if not the right user. Otherwise put in as temp user varible
		String key = inReq.getRequestParameter("entermedia.key");
		User admin = getUserManager(inReq).getUser("admin");
		String hash = getUserManager(inReq).getStringEncryption().getPasswordMd5(admin.getPassword());
		if (!key.equals(hash))
		{
			inReq.redirect("/manager/");
			log.error("Invalid key");
			return;
		}
		inReq.setUser(admin);

		String snapid = inReq.getContentPage().getDirectoryName();
		Data snap = getSearcherManager().getData("system", "sitesnapshot",snapid);
		Data site = getSearcherManager().getData("system", "site", snap.get("site"));

		String path = "/WEB-INF/data/exports/" + site.get("catalogid") + "/" + snap.get("folder");
		inReq.setRequestParameter("path", path);
		
		path = PathUtilities.extractDirectoryPath(path);
		inReq.setRequestParameter("stripfolders",path);
		
	}
	public void downloadSnapshot(WebPageRequest inReq) throws Exception
	{
	
		String zip = inReq.getRequestParameter("url");
		String folder = PathUtilities.extractFileName(zip);
		
		Page temp = getPageManager().getPage("/WEB-INF/temp/" + folder);
		getPageManager().removePage(temp);
		File outputFile = new File(temp.getContentItem().getAbsolutePath() );
		new Downloader().download(zip, outputFile);

		String siteid = inReq.getRequestParameter("siteid");
		Data site = getSearcherManager().getData("system","site",siteid);
		
		String path = "/WEB-INF/data/exports/" + site.get("catalogid") + "/";
		ZipUtil util = new ZipUtil();
		util.unzip(outputFile.getAbsolutePath(), getPageManager().getRepository().get(path).getAbsolutePath());

		Searcher snaps = getSearcherManager().getSearcher("system", "sitesnapshot");
		Data snapshot = snaps.createNewData();
		snapshot.setValue("folder", temp.getPageName());
		snapshot.setName(temp.getPageName() + " downloaded");
		snapshot.setValue("site", siteid);
		snapshot.setValue("snapshotstatus","downloaded");
		snaps.saveData(snapshot);
		
		//PathEvent event = manager.getPathEvent("/system/events/data/exportsite.html");
		inReq.putPageValue("site", site);
		
		inReq.putPageValue("snapshot", snapshot);
	}
}
