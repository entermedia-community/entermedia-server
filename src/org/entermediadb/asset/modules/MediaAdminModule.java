package org.entermediadb.asset.modules;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.upload.FileUpload;
import org.entermediadb.asset.upload.UploadRequest;
import org.entermediadb.elasticsearch.ElasticNodeManager;
import org.entermediadb.events.PathEventManager;
import org.entermediadb.modules.update.Downloader;
import org.entermediadb.workspace.WorkspaceManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.util.JSONParser;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.cache.CacheManager;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.Searcher;
import org.openedit.data.ValuesMap;
import org.openedit.hittracker.HitTracker;
import org.openedit.node.Node;
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
		String catalogid = inReq.findPathValue("catalogid");
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

	public void saveModule(WebPageRequest inReq) 
	{
		
		Data module = (Data) inReq.getPageValue("data");
		if(module != null) {
			String newname = inReq.getRequestParameter("name.value");
			if(newname != null) {
				if(!newname.equals(module.getName())){
					Category cat = getMediaArchive(inReq).getEntityManager().loadDefaultFolderForModule(module, inReq.getUser());
					cat.setName(newname);
					getMediaArchive(inReq).getCategorySearcher().saveCategoryTree(cat);
					
				}
			}
		}
		
		String appid = inReq.findValue("applicationid");
		String catalogid = inReq.findPathValue("catalogid");
		getWorkspaceManager().saveModule(catalogid, appid, module);
		getMediaArchive(inReq).clearAll();
		
		
	}

	public void saveNewModule(WebPageRequest inReq)
	{
		String name = inReq.getRequestParameter("name.value");
		String id = inReq.getRequestParameter("id");
		MediaArchive archive  = getMediaArchive(inReq);
		Data module  = archive.getSearcher("module").createNewData();
		module.setId(id);
		module.setName(name);
		module.setValue("isentity",true);
		//archive.saveData("module", module);
		//Data module = (Data) inReq.getPageValue("data");

		String appid = inReq.findValue("applicationid");
		String catalogid = inReq.findPathValue("catalogid");
		getWorkspaceManager().saveModule(catalogid, appid, module);
		archive.saveData("module", module);
		getMediaArchive(inReq).clearAll();
	}

	
	public void checkModulePath(WebPageRequest inReq) throws Exception
	{
		String moduleid = inReq.findValue("moduleid");
		if( moduleid != null)
		{
			return;
		}
		if( moduleid == null)
		{
			int i = inReq.getPath().indexOf("/views/settings/modules/");
			if( i > 0 )
			{
				moduleid = inReq.getPath().substring(i + "/views/settings/modules/".length());
				moduleid = PathUtilities.extractRootDirectory(moduleid);
			}
		}
		if( moduleid != null)
		{
			//TODO: Speed up loading
			String catalogid = inReq.findPathValue("catalogid");
			Data module = getSearcherManager().getCachedData(catalogid, "module", moduleid);
			if( module != null)
			{
				String appid = inReq.findValue("applicationid");
				getWorkspaceManager().saveModule(catalogid, appid, module);
//				if( appid.contains("mediadb"))
//				{
//					getWorkspaceManager().createMediaDbModule(catalogid,module);
//				}
			}
		}
	}

	public void saveAllModules(WebPageRequest inReq) throws Exception
	{
		String appid = inReq.findValue("applicationid");
		String catalogid = inReq.findPathValue("catalogid");
		
		MediaArchive archive = getMediaArchive(inReq);
		Collection all = archive.getList("module");
		for (Iterator iterator = all.iterator(); iterator.hasNext();)
		{
			Data module = (Data) iterator.next();
			getWorkspaceManager().saveModule(catalogid, appid, module);			
		}
	}

	public void reloadLists(WebPageRequest inReq) throws Exception
	{
		String catalogid = inReq.findPathValue("catalogid");
		
		Collection tables = getSearcherManager().reloadLists(catalogid);
		
		inReq.putPageValue("tables", tables);

	}
	
	public void reloadSettings(WebPageRequest inReq) throws Exception
	{
		String catalogid = inReq.findPathValue("catalogid");
		
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
		NodeManager nodemanager = (NodeManager)getModuleManager().getBean("system","nodeManager");

		nodemanager.connectoToDatabase();
		
		PathEventManager manager = (PathEventManager)getModuleManager().getBean("system", "pathEventManager");
		manager.getPathEvents();

		HitTracker catalogs = getSearcherManager().getList("system","catalog");
		for (Iterator iterator = catalogs.iterator(); iterator.hasNext();)
		{
			try
			{
				Data data = (Data) iterator.next();
				String catalogid = data.getId();
				
				boolean existed = nodemanager.containsCatalog(catalogid);
				
				manager = (PathEventManager)getModuleManager().getBean(catalogid, "pathEventManager");
				manager.getPathEvents();
				
				if( !existed)
				{
					//import any data sitting there for importing
					MediaArchive archive = (MediaArchive)getModuleManager().getBean(catalogid,"mediaArchive");
					
					Page cat = getPageManager().getPage("/" + catalogid + "/_site.xconf" );
					if( !cat.exists())
					{
						
						PageManager pageManager = archive.getPageManager();
						PageSettings homesettings = pageManager.getPageSettingsManager().getPageSettings("/" + catalogid + "/_site.xconf");
						homesettings.setProperty("catalogid", catalogid);
						String fallbackdirectory = "/WEB-INF/base/finder/catalog";
						log.info("Creating catalog: " + catalogid + " Fallback: "+fallbackdirectory);
						homesettings.setProperty("fallbackdirectory", fallbackdirectory);
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
		boolean configonly = Boolean.valueOf( inReq.getRequestParameter("configonly"));	
		
		
		Data snapshot = snaps.createNewData();
		String folder = DateStorageUtil.getStorageUtil().formatDateObj(new Date(), "yyyy-MM-dd-HH-mm-ss");
		snapshot.setValue("folder", folder);
		String name = folder;
		if( configonly)
		{
			name = name + "config";
		}
		snapshot.setName(name);
		snapshot.setValue("site", siteid);
		snapshot.setValue("configonly",configonly);
		snapshot.setValue("snapshotstatus","pendingexport");
		snaps.saveData(snapshot);
		
		log.info("Saving new snapshot with a status of pendingexport " +  siteid);
		
		manager.runSharedPathEvent("/system/events/snapshot/exportsite.html");

		//PathEvent event = manager.getPathEvent("/system/events/data/exportsite.html");
		inReq.putPageValue("site", site);
		
		inReq.putPageValue("snapshot", snapshot);

	}

	public void restoreSiteSnapshot(WebPageRequest inReq)
	{
		String snapid = inReq.getRequestParameter("snapid");
		String configonly = inReq.getRequestParameter("configonly");
		
		Searcher snaps = getSearcherManager().getSearcher("system", "sitesnapshot");
		Data snap = (Data)snaps.searchById(snapid);
		PathEventManager manager = (PathEventManager)getModuleManager().getBean("system", "pathEventManager");

		inReq.putPageValue("status", "Snapshots are pending");
		snap.setValue("snapshotstatus","pendingrestore");
		snap.setValue("configonly",configonly);
		snaps.saveData(snap);
		manager.runSharedPathEvent("/system/events/snapshot/restoresite.html");
		inReq.putPageValue("snapshot", snap);
		
		Data site = getSearcherManager().getData("system","site",snap.get("site"));

		inReq.putPageValue("site", site);
		

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
		
		String siteid = rootpath.substring(1);
		String catalogid = inReq.getRequestParameter("sitecatalogid");
		if( catalogid == null)
		{
			catalogid =  siteid + "/catalog";
		}
		Searcher catsearcher = getSearcherManager().getSearcher("system", "catalog");
		
		Data catalog = (Data)catsearcher.searchById(catalogid);
		if( catalog == null)
		{
			catalog = catsearcher.createNewData();
			catalog.setId(catalogid);
			catalog.setName(sitename);
			catsearcher.saveData(catalog);
		}
		site.setId(siteid);
		site.setValue("catalogid", catalogid);
		site.setName(sitename);
		sites.saveData(site);
		inReq.setRequestParameter("siteid", site.getId());
		
		Page cat = getPageManager().getPage("/" + siteid + "/_site.xconf" );
		if( !cat.exists())
		{
			PageSettings settings = cat.getPageSettings();
			settings.setProperty("catalogid", catalogid);
			String fallbackdirectory = "/WEB-INF/base/eminstitute";
			settings.setProperty("fallbackdirectory", fallbackdirectory);
			settings.setProperty("siteid", siteid);
			getPageManager().getPageSettingsManager().saveSetting(settings);
			getPageManager().clearCache();
		}
//		String frontendid = inReq.getRequestParameter("frontendid");
//		Data frontend= getSearcherManager().getData("system","frontend",frontendid);
//		String url = frontend.get("initurl");
//		if( url != null)
//		{
//			inReq.setRequestParameter("url", url);
//			Data snapshot = downloadSnapshot(inReq);
//			Page root = getPageManager().getPage(rootpath);
//			if( root.exists() )
//			{
//				//dont mess with it
//				snapshot.setValue("snapshotstatus","downloaded");
//			}
//			else
//			{
//				snapshot.setValue("snapshotstatus","pendingrestore");
//			}
//			Searcher snaps = getSearcherManager().getSearcher("system", "sitesnapshot");
//			snaps.saveData(snapshot);
//			PathEventManager manager = (PathEventManager)getModuleManager().getBean("system", "pathEventManager");
//			manager.runSharedPathEvent("/system/events/snapshot/restoresite.html");
//			inReq.putPageValue("snapshot", snapshot);
//		}
		

		
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
		if (snap == null) {
			inReq.redirect("/manager/");
			log.error("Site snapshot missing: " + snapid);
			return;
		}
		Data site = getSearcherManager().getData("system", "site", snap.get("site"));

		String path = "/WEB-INF/data/exports/" + site.get("catalogid") + "/" + snap.get("folder");
		inReq.setRequestParameter("path", path);
		
		path = PathUtilities.extractDirectoryPath(path);
		inReq.setRequestParameter("stripfolders",path);
		
	}
	public Data downloadSnapshot(WebPageRequest inReq) throws Exception
	{
		String zip = inReq.getRequestParameter("url");
		String folder = PathUtilities.extractFileName(zip);
		
		Page temp = getPageManager().getPage("/WEB-INF/temp/" + folder);
		String foldername = temp.getPageName() + "D" + DateStorageUtil.getStorageUtil().formatDateObj(new Date(), "HH-mm-ss");

		getPageManager().removePage(temp);
		File outputFile = new File(temp.getContentItem().getAbsolutePath() );
		log.info("downloading " + zip);
		new Downloader().download(zip, outputFile);
		log.info("downloading finished " + zip);

		//Unzip it to temp folder
		ZipUtil util = new ZipUtil();
		temp = getPageManager().getPage("/WEB-INF/temp/" + folder + "unzip/");
		getPageManager().removePage(temp);
		util.unzip(outputFile.getAbsolutePath(), temp.getContentItem().getAbsolutePath());
		
		
		String siteid = inReq.getRequestParameter("siteid");
		Data site = getSearcherManager().getData("system","site",siteid);

		Page source = getPageManager().getPage("/WEB-INF/temp/" + folder + "unzip/" + PathUtilities.extractPageName(folder));
		String path = "/WEB-INF/data/exports/" + site.get("catalogid") + "/" + foldername;
		Page dest = getPageManager().getPage(path);
		getPageManager().movePage(source, dest);
		Searcher snaps = getSearcherManager().getSearcher("system", "sitesnapshot");
		Data snapshot = snaps.createNewData();
		snapshot.setValue("folder", foldername);
		snapshot.setName(foldername);
		snapshot.setValue("site", siteid);
		snapshot.setValue("snapshotstatus","downloaded");
		snaps.saveData(snapshot);
		
		//PathEvent event = manager.getPathEvent("/system/events/data/exportsite.html");
		inReq.putPageValue("site", site);
		
		inReq.putPageValue("snapshot", snapshot);
		return snapshot;
	}
	public Data uploadSnapshot(WebPageRequest inReq) throws Exception
	{
		String siteid = inReq.getRequestParameter("siteid");
		Data site = getSearcherManager().getData("system","site",siteid);
		UploadRequest req = (UploadRequest)inReq.getPageValue("uploadrequest");
		String filenam = req.getFirstItem().getName();
		String folder = PathUtilities.extractPageName(filenam);
		
		Page source = getPageManager().getPage(req.getFirstItem().getSavedPage().getDirectory() + "/" + folder );
		String foldername = folder + "U" + DateStorageUtil.getStorageUtil().formatDateObj(new Date(), "HH-mm-ss");
		String path = "/WEB-INF/data/exports/" + site.get("catalogid") + "/" + foldername;
		Page dest = getPageManager().getPage(path);
		getPageManager().movePage(source,dest);
		
		Searcher snaps = getSearcherManager().getSearcher("system", "sitesnapshot");
		Data snapshot = snaps.createNewData();
		snapshot.setValue("folder", foldername);
		snapshot.setName(foldername);
		snapshot.setValue("site", siteid);
		snapshot.setValue("snapshotstatus","downloaded");
		snaps.saveData(snapshot);
		
		//PathEvent event = manager.getPathEvent("/system/events/data/exportsite.html");
		inReq.putPageValue("site", site);
		
		inReq.putPageValue("snapshot", snapshot);
		return snapshot;

	}
	public boolean checkAcceptConnections(WebPageRequest inReq) throws Exception
	{
		//Look in DB to see if I am the primary server or not
		//Primary domain
		MediaArchive archive = getMediaArchive(inReq);
		String primaryserverurl = archive.getCatalogSettingValue("primary-server-healthcheck-url");
		Boolean acceptconnections = true;
		if( primaryserverurl != null )
		{
			String me = archive.getNodeManager().getLocalNode().getNodeType();
			
			if(!me.equals(Node.PRIMARY) )
			{
				//Connect to the remote server and make sure it's running ok
				Downloader downloader = new Downloader();
				try
				{
					log.info("nodes is " + me + " Checking " + primaryserverurl + " from " + me);
					String health = downloader.downloadToString(primaryserverurl);
					if( health.contains("\"accepting\":\"true\""))
					{
						acceptconnections = false;
						inReq.putPageValue("message","Primary Server is reachable " + primaryserverurl);
						//Set the status
						log.info("Primary Server is reachable " + primaryserverurl);
						inReq.getResponse().sendError(503,"Primary Server is reachable " + primaryserverurl);
					}
					else
					{
						inReq.putPageValue("message","Primary Server returned false " + health);	
					}
				}
				catch( Exception ex)
				{
					log.error("Server not reachable " + primaryserverurl ,ex);
					inReq.putPageValue("message","Primary server not reachable " + primaryserverurl);
				}
			}
			else
			{
				inReq.putPageValue("message","this is the primary server" );
			}
		}
		else
		{
			inReq.putPageValue("message","primary-server-healthcheck-url not set" );
		}
		inReq.putPageValue("acceptconnections",acceptconnections);
		return acceptconnections;
	}
	public void catalogSnapshot(WebPageRequest inReq)
	{
		String targetcatalogid = inReq.findPathValue("catalogid");
		
		Data site = getSearcherManager().query("system", "site").exact("catalogid", targetcatalogid).searchOne();
		if( site != null)
		{
			inReq.setRequestParameter("id",site.getId());
			createSiteSnapshot(inReq);
		}		
	}
	public void clearCaches(WebPageRequest inReq)
	{
		 CacheManager cache = (CacheManager)getModuleManager().getBean("systemCacheManager");  //prototype
		 if( cache != null)
		 {
			 cache.clearAll();
		 }
		 cache = (CacheManager)getModuleManager().getBean("systemExpireCacheManager"); //shared
		 if( cache != null)
		 {
			 cache.clearAll();
		 }
		 
		 //Bean stuff?
		 String catalogid = inReq.findPathValue("catalogid");
		 if( catalogid != null)
		 {
			 cache = (CacheManager)getModuleManager().getBean(catalogid,"cacheManager"); 
			 if( cache != null)
			 {
				 cache.clearAll();
			 }
			 getMediaArchive(catalogid).getNodeManager().flushDb();
		 }		 
	}

	public void reindexAll(WebPageRequest inReq)
	{
		String catalogid = inReq.findPathValue("catalogid");
		NodeManager manager = (NodeManager)getModuleManager().getBean(catalogid,"nodeManager");

		long start = System.currentTimeMillis();

		try
		{
			if(!manager.reindexInternal(catalogid))
			{
				inReq.putPageValue("mappingerror",true);
			}
		}
		catch ( Throwable ex)
		{
			inReq.putPageValue("exception",ex);
		}
		
		long finish = System.currentTimeMillis();
		String time = String.valueOf( Math.round( (finish - start) / 1000L) );
		inReq.putPageValue("time", time);
	}
	public void listMappings(WebPageRequest inReq)
	{
		String catalogid = inReq.findPathValue("catalogid");
		ElasticNodeManager manager = (ElasticNodeManager)getModuleManager().getBean(catalogid,"nodeManager");
		String map = manager.listAllExistingMapping(catalogid);
		inReq.putPageValue("mappingdebug",map);

	}
	public void makeMaster(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String localmaster = archive.getNodeManager().getLocalClusterId();
		
		//mastereditclusterid
		archive.getNodeManager().setForceSaveMasterCluster(true);
		
		reindexAll(inReq);
		archive.getNodeManager().setForceSaveMasterCluster(false);
	}


	public void createModulePath(WebPageRequest inReq)  throws Exception
	{
		String moduleid = inReq.findValue("moduleid");
		if( moduleid == null)
		{
			moduleid = PathUtilities.extractDirectoryName( inReq.getPath() ); 
			if( moduleid.equals("edit"))
			{
				moduleid = PathUtilities.extractDirectoryName( PathUtilities.extractDirectoryPath(  inReq.getPath() ) );
			}
			if( inReq.getPath().endsWith("/views/modules/" + moduleid + "/index.html") || inReq.getPath().contains("/views/modules/" + moduleid + "/edit/addnew") )
			{
				String applicationid = inReq.findValue("applicationid");
				Page item = getPageManager().getPage("/" + applicationid + "/views/modules/" + moduleid + "/_site.xconf");
				if( !item.exists() )
				{
					String catalogid = inReq.findPathValue("catalogid");
					Data module = getSearcherManager().getData(catalogid, "module", moduleid);
		
					getWorkspaceManager().saveModule(catalogid, applicationid, module);
					getSearcherManager().clear();
					getPageManager().clearCache();
					inReq.putPageValue("content", getPageManager().getPage(inReq.getPath()));
				}			
			}	

		}
		
	}
	
	//Not used. Delete this
	public Data loadHomeModule(WebPageRequest inReq) {
		String catalogid = inReq.findPathValue("catalogid");
		String applicationid = inReq.findValue("applicationid");
		Data module = getSearcherManager().getSearcher(catalogid, "module").query().match("showonnav","true").sort("orderingUp").searchOne(inReq);
		String finalpath;
		if(module == null){
			return null;
		}
		if(module.getId().equals("librarycollection")) {
			finalpath = "/" + applicationid + "/views/collections/index.html";
		}
		else {
			finalpath = "/" + applicationid + "/views/modules/" + module.getId() + "/index.html";
		}
		Page finalp = getMediaArchive(inReq).getPageManager().getPage(finalpath);
		if(finalp.exists()) {
			inReq.redirect(finalpath);
		}
		/*Page requestedPage  = inReq.getPage();
		
		requestedPage.setInnerLayout("/" + applicationid + "/theme/layouts/searchlayout2.html");*/
		return module;
	}
	
	public void scanForCustomizations(WebPageRequest inReq)
	{
		MediaArchive mediaArchive = getMediaArchive(inReq);
		Collection modules = mediaArchive.query("module").all().sort("name").search();
		getWorkspaceManager().scanModuleCustomizations(mediaArchive,modules);
		//getWorkspaceManager().scanHtmlCustomizations(mediaArchive);
	}
	public void customizationsExportEntities(WebPageRequest inReq)
	{
		MediaArchive mediaArchive = getMediaArchive(inReq);
		String[] moduleids = inReq.getRequestParameters("moduleid"); 
		Collection modules = mediaArchive.query("module").orgroup("id",moduleids).sort("name").search();
		getWorkspaceManager().scanModuleCustomizations(mediaArchive,modules);
		
	}
	public void importCustomization(WebPageRequest inReq) throws Exception
	{
		MediaArchive mediaArchive = getMediaArchive(inReq);

		FileUpload command = new FileUpload();
		command.setPageManager(getPageManager());
		UploadRequest properties = command.parseArguments(inReq);
		if (properties == null)
		{
			return;
		}
		if (properties.getFirstItem() == null)
		{
			return;
		}
		Page temp = getPageManager().getPage("/WEB-INF/tmp/unzip");
		getPageManager().removePage(temp);
		properties.saveFirstFileAs("/WEB-INF/tmp/unzip/temporary.zip" , inReq.getUser());

		List files = properties.unzipFiles(true);
		getWorkspaceManager().importCustomizations(mediaArchive,files);
		String appid = inReq.findValue("applicationid");
		
		//group order librarycollections etc.
		//role (settingsgroup), saved searches (savedquery), hot folders, conversion presets, users, orders, collections, libraries, divisions, permissionsapp, preset configuration
		Collection all = mediaArchive.query("module").all().search();
		for (Iterator iterator = all.iterator(); iterator.hasNext();)
		{
			Data module = (Data) iterator.next();
			getWorkspaceManager().saveModule(mediaArchive.getCatalogId(), appid, module);
		}
		getPageManager().clearCache();
		scanForCustomizations(inReq);
	}

	public void copySmartOrganizer(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String id = inReq.getRequestParameter("id");
		Data template = archive.getData("smartorganizer", id);
		
		Searcher s = archive.getSearcher("smartorganizer");
		Data copy = s.createNewData();
		copy.setProperties(template.getProperties());
		copy.setId(null);
		copy.setName(template.getName() + " copy");
		copy.setValue("updatedby",inReq.getUserName());
		copy.setValue("updatedon",new Date());
		copy.setValue("iscurrent",false);
		s.saveData(copy);
	}

	public void smartOrganizerRestore(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String id = inReq.getRequestParameter("restoreid");

		if( id == null)
		{
			return;
		}
		
		Searcher s = archive.getSearcher("smartorganizer");
		Collection existing = s.query().exact("iscurrent",true).search();
		for (Iterator iterator = existing.iterator(); iterator.hasNext();) {
			Data data = (Data) iterator.next();
			data.setValue("iscurrent",false);
			s.saveData(data);
		}
		//s.saveAllData(existing,null);
		
		Data template = archive.getData("smartorganizer", id);
		//template.setValue("updatedby",inReq.getUserName());
		//template.setValue("updatedon",new Date());
		template.setValue("iscurrent",true);
		s.saveData(template);
		
	}

	
	public void smartOrganizerRename(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String id = inReq.getRequestParameter("id");
		Data template = archive.getData("smartorganizer", id);
		
		Searcher s = archive.getSearcher("smartorganizer");
		
		String newname = inReq.getRequestParameter("newname");
		template.setName(newname);
		s.saveData(template);
	}

	
	public void deleteSmartOrganizer(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String id = inReq.getRequestParameter("id");
		Data template = archive.getData("smartorganizer", id);
		if( template != null)
		{
			Searcher s = archive.getSearcher("smartorganizer");
			s.delete(template,inReq.getUser());
		}
	}
	
	
	public void deploySmartOrganizer(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		String id = inReq.getRequestParameter("id");
		Data template = archive.getData("smartorganizer", id);
		if (template == null)
		{
			log.error("Template doesn't exists: " + id);
			return;
		}
		String json = template.get("json");
		if(json == null) {
			log.error("Template is empty: " + template);
			return;
		}
		
		List tosave = new ArrayList();
		
		JSONParser parser = new JSONParser();
		JSONArray jsonarray = null;
		jsonarray = (JSONArray) parser.parseCollection(json);
		if ( jsonarray != null)
		{
			Collection tosavemenu = new ArrayList();

			//Map<String,Data> parents = new HashMap<String,Data>();
			Collection<ParentChildPair> parentschilds = new ArrayList();
			
			for (Iterator iterator = jsonarray.iterator(); iterator.hasNext();) {
				ValuesMap map = new ValuesMap((Map) iterator.next());
				if("folderLabel".equals(map.get("cssClass")))
				{
					JSONObject userdatamap = (JSONObject) parser.parse(map.getString("userData"));
					ValuesMap  userdata  = new ValuesMap(userdatamap);
					String moduleid = userdata.getString("moduleid"); //TODO: get initialmoduleid  to rename
					if( moduleid == null || moduleid.trim().isEmpty())
					{
						continue;
					}
					String modulename = map.getString("text"); 
					Data module = archive.getData("module",moduleid);
					boolean newrecord = false;
					if(module == null)
					{
						newrecord = true;
						module  = archive.getSearcher("module").createNewData();
						module.setId(moduleid);
					}
					module.setName(modulename.replaceAll("\n", "").trim());
					String icon = userdata.getString("moduleicon");
					if( icon != null)
					{
						icon = PathUtilities.extractPageName(icon);
						module.setValue("moduleicon", icon ); //Clean this up on server 
					}
					module.setValue("isentity", true); //Not used anymore?

					if( !newrecord )
					{
						if( !moduleid.equals("searchcategory") )
						{
							module.setValue("enableuploading", true);
						}
						module.setValue("showonsearch", true); 
					}
					//Children
					Collection<String> parentmoduleids = new ArrayList(); 
					Object obj = userdatamap.get("parents");
					if( obj != null)
					{
						if( obj instanceof String )
						{
							parentmoduleids.add((String)obj);
						}
						else
						{
							parentmoduleids.addAll((Collection)obj);
						}
						for (Iterator iterator2 = parentmoduleids.iterator(); iterator2.hasNext();)
						{
							String parentmoduleid = (String) iterator2.next();
							ParentChildPair pair = new ParentChildPair();
							pair.setParentModuleId(parentmoduleid);
							pair.setChildModule(module);
							parentschilds.add(pair);
						}
					}
					tosave.add(module);
					//Menu
					String ordering = userdata.getString("ordering"); 

					if( ordering != null && !ordering.equals("-1" ) )
					{
						Data existingmenu = archive.query("appsection").exact("toplevelentity",moduleid).searchOne();
						if( existingmenu == null)
						{
							existingmenu = archive.getSearcher("appsection").createNewData();
							existingmenu.setValue("toplevelentity",moduleid);
						}
						existingmenu.setValue("name",module.getValue("name"));
						int i = Integer.parseInt(ordering);
						existingmenu.setValue("ordering",i);
						tosavemenu.add(existingmenu);
					
					}
				}
			}
			
//			//Check parents
//			archive.saveData("module", tosave);  //Save children and parents

			String appid = inReq.findValue("applicationid");
			for (Iterator iterator = tosave.iterator(); iterator.hasNext();) 
			{
				Data module = (Data) iterator.next();
				getWorkspaceManager().saveModule(archive.getCatalogId(), appid, module);	 //Save views	
			}
			checkParents(archive,parentschilds);
			archive.saveData("module", tosave);  //Save children and parents
			
			archive.getSearcher("appsection").deleteAll(inReq.getUser()); //
			archive.getSearcher("appsection").saveAllData(tosavemenu,inReq.getUser());
			archive.clearAll();
		}

	}

	private void checkParents(MediaArchive archive, Collection<ParentChildPair> parentschilds)
	{
		for (Iterator<ParentChildPair> iterator = parentschilds.iterator(); iterator.hasNext();) 
		{
			ParentChildPair pair = iterator.next();
			
			//Make field and a one to many view? Add all the "Add New" columns to the view
			Searcher childsearcher = archive.getSearcher(pair.getChildModule().getId());
			PropertyDetails details = childsearcher.getPropertyDetails();
			Data parentmodule = archive.getData("module",pair.getParentModuleId());
			if( parentmodule == null )
			{
				log.error("missing parent module");
				continue;
			}
			if( details.getDetail(pair.getParentModuleId()) == null)
			{
				PropertyDetail newprop = archive.getPropertyDetailsArchive().createDetail(childsearcher.getSearchType(), pair.getParentModuleId(), parentmodule.getName());
				newprop.setValue("name",parentmodule.getValue("name"));  //Int?
				newprop.setDataType("list");
				newprop.setValue("viewtype","entity");
				newprop.setEditable(true);
				newprop.setIndex(true);
				archive.getPropertyDetailsArchive().savePropertyDetail(newprop, pair.getChildModule().getId(), null);
				archive.getPropertyDetailsArchive().clearCache();
				childsearcher.putMappings();
			}
			//Make views
			Searcher viewsearcher = archive.getSearcher("view");

//			String viewid = PathUtilities.makeId(parentmodule.getName());
//			viewid = viewid.toLowerCase();
			
//			String addpath = "/WEB-INF/data/" + archive.getCatalogId() + "/views/" + childmodule.getId() + "/" + childmodule.getId() + "addnew.xml";
//			Page from = getPageManager().getPage(addpath);
//			
//			//entityparent/entitychildparent
//			String targetpath = "/WEB-INF/data/" + archive.getCatalogId() + "/views/" + parentmodule.getId() + "/" + childmodule.getId() + viewid + ".xml";
//			Page to = getPageManager().getPage(targetpath );

//			if( !to.exists())
//			{
//				getPageManager().copyPage(from, to);
//			}
			String viewid = parentmodule.getId() + pair.getChildModule().getId(); 
			Data data =  (Data)viewsearcher.searchById(viewid);
			if( data == null)
			{
				data = viewsearcher.createNewData();
				//Copy the add new
				data.setId(viewid);
				data.setName(pair.getChildModule().getName());

				data.setProperty("moduleid", parentmodule.getId());
				data.setProperty("rendertype", "entitysubmodules"); //POne to manuy
				data.setProperty("rendertable", pair.getChildModule().getId());
				data.setProperty("renderexternalid", parentmodule.getId());
				data.setProperty("systemdefined", "false");
				data.setProperty("ordering", System.currentTimeMillis() + "");
				
//				//Put the XML file in the right place?
//				String addpath = "/WEB-INF/base/finder" + archive.getCatalogId() + "/views/" + childmodule.getId() + "/" + childmodule.getId() + "submoduletable.xml";
//				Page from = getPageManager().getPage(addpath);
//				
//				//entityparent/entitychildparent
//				String targetpath = "/WEB-INF/data/" + archive.getCatalogId() + "/views/" + parentmodule.getId() + "/" + data.getId() + ".xml";
//				Page to = getPageManager().getPage(targetpath );
//				if( !to.exists())
//				{
//					getPageManager().copyPage(from, to);
//				}
				
				viewsearcher.saveData(data);
			}
		}
	}

	public void saveAiMediaDb(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		getWorkspaceManager().createMediaDbAiFunctionEndPoints(catalogid);
	}
	
}
