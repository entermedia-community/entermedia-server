package org.entermediadb.asset.importer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.scanner.AssetImporter;
import org.entermediadb.asset.scanner.HotFolderManager;
import org.entermediadb.asset.util.TimeParser;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebServer;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.event.WebEvent;
import org.openedit.locks.Lock;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;
import org.openedit.repository.ContentItem;
import org.openedit.repository.Repository;
import org.openedit.repository.filesystem.FileRepository;
import org.openedit.repository.filesystem.XmlVersionRepository;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.EmStringUtils;
import org.openedit.util.Exec;
import org.openedit.util.ExecResult;
import org.openedit.util.PathUtilities;

public class BaseHotFolderManager implements HotFolderManager
{
	private static final Log log = LogFactory.getLog(BaseHotFolderManager.class);

	protected PageManager fieldPageManager;
	protected SearcherManager fieldSearcherManager;
	protected WebServer fieldWebServer;
	protected FolderMonitor fieldFolderMonitor;
	
	public FolderMonitor getFolderMonitor()
	{
		return fieldFolderMonitor;
	}

	public void setFolderMonitor(FolderMonitor inFolderMonitor)
	{
		fieldFolderMonitor = inFolderMonitor;
	}

	public WebServer getWebServer()
	{
		return fieldWebServer;
	}

	public void setWebServer(WebServer inWebServer)
	{
		fieldWebServer = inWebServer;
	}


	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}


	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}


	public PageManager getPageManager()
	{
		return fieldPageManager;
	}


	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}


	public void saveMounts(String inCatalogId)
	{
		//remove any old hot folders for this catalog
		getWebServer().reloadMounts();
		List configs = new ArrayList(getPageManager().getRepositoryManager().getRepositories());
		String originalpath = "/WEB-INF/data/" + inCatalogId + "/originals";

		List newrepos = new ArrayList();
		for (Iterator iterator = configs.iterator(); iterator.hasNext();)
		{
			Repository config = (Repository) iterator.next();
			if( config.getPath().startsWith(originalpath))
			{
				getPageManager().getRepositoryManager().removeRepository(config.getPath());
			}
		}
		Collection folders = loadFolders(inCatalogId);
		for (Iterator iterator = folders.iterator(); iterator.hasNext();)
		{
			Data folder = (Data) iterator.next();
			String external = folder.get("externalpath");
			String type = folder.get("hotfoldertype");

			if( external != null || "s3".equals(type) || "syncthing".equals(type))
			{
				String toplevelfolder =  folder.get("subfolder");
				
				if(type == null ||"mount".equals(type))
				{
					type = "mount";
					
				}
				String fullpath = originalpath + "/" + toplevelfolder;
				//String versioncontrol = folder.get("versioncontrol");
				Repository created = createRepo(type);
				created.setPath(fullpath);
				created.setExternalPath(external);
				created.setFilterIn(folder.get("includes"));
				created.setFilterOut(folder.get("excludes"));
				//add varliables
				
				for (Iterator iterator2 = folder.keySet().iterator(); iterator2.hasNext();) {
					
					String key = (String) iterator2.next();
					created.setProperty(key, (String) folder.get(key)); //
				}
				
				newrepos.add(created);
			}	
		}		
		configs = getPageManager().getRepositoryManager().getRepositories();
		configs.addAll(newrepos);
		
		
		getWebServer().saveMounts(configs);
		//getPageManager().getRepositoryManager().setRepositories(configs);
		//save the file
	}
	
	protected Repository createRepo(String inType)
	{
		Repository repo;
		if("version".equals(inType) )
		{
			repo = new XmlVersionRepository();
			repo.setRepositoryType("versionRepository");
		}
		else if( "s3".equals(inType))
		{
			repo = (Repository) getSearcherManager().getModuleManager().getBean("S3Repository");
			repo.setRepositoryType("S3Repository");
		}
		else
		{
			repo = new FileRepository();
		}
		return repo;
	}

	protected Repository findRepoByPath(List inConfigs, String inFullpath)
	{

		for (Iterator iterator = inConfigs.iterator(); iterator.hasNext();)
		{
			Repository config = (Repository) iterator.next();
			if (config.getPath().startsWith(inFullpath)) 
			{
				return config;
			}
		}
		return null;
	}


	public Collection loadFolders(String inCatalogId)
	{
		Searcher hfsearcher = getFolderSearcher(inCatalogId);
		return hfsearcher.query().all().sort("orderingDown").sort("lastscanstart").search();
		//return hfsearcher.getAllHits();
	}


	public Searcher getFolderSearcher(String inCatalogId)
	{
		return getSearcherManager().getSearcher(inCatalogId, "hotfolder");
	}

	public Data getFolderByPathEnding(String inCatalogId, String inFolder)
	{		
		for (Iterator iterator = loadFolders(inCatalogId).iterator(); iterator.hasNext();)
		{
			Data folder = (Data) iterator.next();
			String subfolder = folder.get("subfolder");
			if(inFolder.equals(subfolder) )
			{
				return folder;
			}
			
		}
		return null;
	}


	/* (non-Javadoc)
	 * @see org.entermediadb.asset.scanner.HotFolderManager2#deleteFolder(java.lang.String, org.openedit.Data)
	 */
	@Override
	public void deleteFolder(String inCatalogId, Data inExisting)
	{
		String type = inExisting.get("hotfoldertype");
		getFolderSearcher(inCatalogId).delete(inExisting, null);
		if( "syncthing".equals(type))
		{
			updateSyncThingFolders(inCatalogId);
		}
		
	}


	/* (non-Javadoc)
	 * @see org.entermediadb.asset.scanner.HotFolderManager2#saveFolder(java.lang.String, org.openedit.Data)
	 */
	@Override
	public void saveFolder(String inCatalogId, Data inNewrow)
	{
		String type = inNewrow.get("hotfoldertype");
		if("syncthing".equals(type))
		{
			String toplevelfolder = inNewrow.get("subfolder");
			Page toplevel = getPageManager().getPage("/WEB-INF/data/" + inCatalogId + "/hotfolders/" + toplevelfolder +"/" );
			if(!toplevel.exists()){
				getPageManager().putPage(toplevel);
			}
			inNewrow.setProperty("externalpath",toplevel.getContentItem().getAbsolutePath() );
			getFolderSearcher(inCatalogId).saveData(inNewrow, null);
			updateSyncThingFolders(inCatalogId);
		}
		else if( "googledrive".equals(type))
		{
			String toplevelfolder = inNewrow.get("subfolder");
			String email = inNewrow.get("email");
			MediaArchive archive = (MediaArchive)getSearcherManager().getModuleManager().getBean(inCatalogId,"mediaArchive");

			ContentItem hotfolderpath =  archive.getContent( "/WEB-INF/data/" + archive.getCatalogId() + "/workingfolders/"+ email + "/" + toplevelfolder );
			File file = new File( hotfolderpath.getAbsolutePath() );
			file.mkdirs();
			inNewrow.setProperty("externalpath",file.getAbsolutePath());				
			getFolderSearcher(inCatalogId).saveData(inNewrow, null);
			archive.fireMediaEvent("hotfolder/googledrivesaved", "hotfolder", inNewrow.getId(), null);
			
		}
		else if( "resiliodrive".equals(type))
		{
			String toplevelfolder = inNewrow.get("subfolder");
			MediaArchive archive = (MediaArchive)getSearcherManager().getModuleManager().getBean(inCatalogId,"mediaArchive");
			
			ContentItem hotfolderpath =  archive.getContent( "/WEB-INF/data/" + archive.getCatalogId() + "/workingfolders/"+ toplevelfolder );
			File file = new File( hotfolderpath.getAbsolutePath() );
			file.mkdirs();
			inNewrow.setProperty("externalpath",file.getAbsolutePath());				
			getFolderSearcher(inCatalogId).saveData(inNewrow, null);
			archive.fireMediaEvent("hotfolder/resiliodrivesaved", "hotfolder", inNewrow.getId(), null);
		}
		else 
		{
			String toplevelfolder = null;
			
			//save subfolder with the value of the end of externalpath
			String epath =  type = inNewrow.get("externalpath");
			if(epath!= null )
			{
				epath = epath.trim();
				epath = epath.replace('\\', '/');
				if( epath.endsWith("/"))
				{
					epath = epath.substring(0,epath.length() - 1);
				}
				toplevelfolder = PathUtilities.extractDirectoryName(epath + "/junk.html");
			}	
			if( toplevelfolder == null )
			{
				toplevelfolder = inNewrow.getName();
			}	
			inNewrow.setProperty("subfolder",toplevelfolder);
			getFolderSearcher(inCatalogId).saveData(inNewrow, null);
		}		
		
	}	
	@Override
	public List<String> importHotFolder(MediaArchive inArchive, Data inFolder)
	{
		return importHotFolder(inArchive,inFolder,null);
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.scanner.HotFolderManager2#importHotFolder(org.entermediadb.asset.MediaArchive, org.openedit.Data)
	 */
	public List<String> importHotFolder(MediaArchive inArchive, Data inFolder, String inSubChangePath)
	{
		String base = "/WEB-INF/data/" + inArchive.getCatalogId() + "/originals";
		String name = inFolder.get("subfolder");
		String path = base + "/" + name;

		AssetImporter importer = createImporter(inArchive,inFolder,path);

		Date started = new Date();
		
		boolean checkformod = false;
		if( inSubChangePath != null )
		{
			path = path + "/" + inSubChangePath;
			checkformod = true;
		}
		importer.fireHotFolderEvent(inArchive, "update", "start", "Scanning " + path, null);
		log.info(path + " scan started. mod check = " + checkformod);
		
		List<String> paths = importer.processOn(base, path, checkformod, inArchive, null);
		importer.fireHotFolderEvent(inArchive, "update", "finish", String.valueOf( paths.size()), null);
		inFolder.setProperty("lastscanstart", DateStorageUtil.getStorageUtil().formatForStorage(started));
		getFolderSearcher(inArchive.getCatalogId()).saveData(inFolder, null);

		long taken = ((new Date().getTime() - started.getTime())/6000L);
		log.info(inFolder + " Imported " + paths.size() + " in " + taken + " milli-seconds" );
		
		return paths;
	}
	
	protected AssetImporter createImporter(MediaArchive inArchive, Data inFolder, String path)
	{
		AssetImporter importer = (AssetImporter)getWebServer().getModuleManager().getBean("assetImporter");
		
		String excludes = inFolder.get("excludes");
		if( excludes != null )
		{
			List<String> list = EmStringUtils.split(excludes);
//			for (int i = 0; i < list.size(); i++)
//			{
//				String row = list.get(i).trim();
//				if( row.startsWith("/") &&  !row.startsWith(path))
//				{
//					row = path + row;
//				}
//				list.set(i, row);
//			}
			importer.setExcludeMatches(list);
		}
		importer.setIncludeMatches(inFolder.get("includes"));
		String attachments = inFolder.get("attachmenttrigger");
		if( attachments != null )
		{
			Collection attachmentslist = EmStringUtils.split(attachments);
			importer.setAttachmentFilters(attachmentslist);
		}
		
		return importer;
	}

	protected boolean checkMod(MediaArchive inArchive, Data inFolder)
	{
		long sincedate = 0;
		String since = inFolder.get("lastscanstart");
		if( since != null )
		{
			sincedate = DateStorageUtil.getStorageUtil().parseFromStorage(since).getTime();
		}
		boolean skipmodcheck = false;
		if( since != null )
		{
			long now = System.currentTimeMillis();
			String mod = inArchive.getCatalogSettingValue("importing_modification_interval");
			if( mod == null)
			{
				mod = "1d";
			}
			long time = new TimeParser().parse(mod);
			sincedate = sincedate + time; //once a week
			if( sincedate > now )
			{
				skipmodcheck = true;
			}
		}
		return skipmodcheck;
	}
	
//	public void addGoogleFolders(String inCatalogId)
//	{
//		Collection hotfolders = loadFolders(inCatalogId);
//		
//		for (Iterator iterator = hotfolders.iterator(); iterator.hasNext();) {
//			Data folder = (Data) iterator.next();
//			String type = folder.get("hotfoldertype");
//			if( !"googledrive".equals(type))
//			{
//				continue;
//			}
//			String toplevelfolder = folder.get("subfolder");
//			String email = folder.get("email");
//			String key = folder.get("accesskey");
//			//String email = folder.get("email");
//			String externalpath = folder.get("externalpath");
//			
//		}
//		//TODO: get
//	}
	protected Exec fieldExec;
	protected Exec getExec()
	{
		if( fieldExec == null)
		{
			fieldExec = (Exec) getWebServer().getModuleManager().getBean("exec");
		}
		return fieldExec;
	}
	public void updateSyncThingFolders(String inCatalogId)	
	{
		Collection hotfolders = loadFolders(inCatalogId);
		
		//TODO: get login/key information from system/systemsettings
		Data server = getSearcherManager().getData("system","systemsettings","syncthing_server_address");
		if( server == null)
		{
			return;
		}
		String serverapi = getSearcherManager().getData("system","systemsettings","syncthing_server_apikey").get("value");
		String serverdeviceid = getSearcherManager().getData("system","systemsettings","syncthing_server_deviceid").get("value");
		
		String postUrl = "http://" + server.get("value") + "/rest/system/config";
		
		try
		{
			CloseableHttpClient httpclient = HttpClients.createDefault();
			HttpGet httpGet = new HttpGet(postUrl);
			httpGet.addHeader("X-API-Key", serverapi);
			CloseableHttpResponse response1 = httpclient.execute(httpGet);
			if( response1.getStatusLine().getStatusCode() != 200 )
			{
				throw new OpenEditException("SyncThing Server error " + response1.getStatusLine().getStatusCode());
			}
			String returned = EntityUtils.toString(response1.getEntity());
			JSONObject config = (JSONObject) new JSONParser().parse(returned);

			//Save it
			//TODO: Make a map of existing device ids
			Set existingdevices = new HashSet();
			List devices = (List) config.get("devices");
			
			for (Iterator iterator = devices.iterator(); iterator.hasNext();) {
				Map device = (Map) iterator.next();
				existingdevices.add(device.get("deviceID"));
			}
			
			List allfolders = (List) config.get("folders");
			List folders = new ArrayList(allfolders);
			for (Iterator iterator = folders.iterator(); iterator.hasNext();) {
				JSONObject folder = (JSONObject) iterator.next();
				String folderid = (String) folder.get("id");
				if( folderid.startsWith("EnterMediaDB/" + inCatalogId + "/") )
				{
					allfolders.remove(folder);
				}								
			}
			//TODO: Add all the folders and devices needed
			for (Iterator iterator = hotfolders.iterator(); iterator.hasNext();) {
				Data folder = (Data) iterator.next();
				String type = folder.get("hotfoldertype");
				if( !"syncthing".equals(type))
				{
					continue;
				}
				//Add self if not already in there
				String clientdeviceid = folder.get("deviceid");
				String toplevelfolder = folder.get("subfolder");
				if(clientdeviceid != null && !existingdevices.contains(clientdeviceid))
				{
					JSONObject newdevice = new JSONObject();
					newdevice.put("deviceID", clientdeviceid );
					newdevice.put("addresses", Arrays.asList("dynamic")  );
					newdevice.put("certName" , "");
					newdevice.put("compression" , "metadata");
					newdevice.put("introducer", false );
					newdevice.put("name", "EnterMediaDB/" + inCatalogId + "/" + clientdeviceid.substring(0,7) );
					devices.add(newdevice);
				}
				//TODO: add the folder
				JSONObject newfolder = new JSONObject();
				//dev json = new JsonBuilder()
				newfolder.put("autoNormalize", true);
				newfolder.put("copiers", 0);
	            newfolder.put("hashers", 0);
	            newfolder.put("id", "EnterMediaDB/" + inCatalogId + "/" + toplevelfolder);
	            newfolder.put("ignoreDelete", false);
	            newfolder.put("ignorePerms", false);
	            newfolder.put("invalid","");
	            newfolder.put("maxConflicts", -1);
	            newfolder.put("minDiskFreePct", 1);
	            newfolder.put("order", "random");
	            newfolder.put("path", folder.get("externalpath"));
	            newfolder.put("pullerPauseS", 0);
	            newfolder.put("pullerSleepS", 0);
	            newfolder.put("pullers", 0);
	            newfolder.put("readOnly", false);
	            newfolder.put("rescanIntervalS", 60);
	            newfolder.put("scanProgressIntervalS", 0);
	            
	            // TODO: check integrity of this messy thing
	            JSONArray outputdevices = new JSONArray();
	            JSONObject serverdevice = new JSONObject();
	            serverdevice.put("deviceID", serverdeviceid);
	            JSONObject clientdevice = new JSONObject();
	            clientdevice.put("deviceID", clientdeviceid);
	            outputdevices.add(serverdevice);
	            outputdevices.add(clientdevice);
	            JSONObject versioning = new JSONObject();
	            versioning.put("params", new JSONObject());
	            versioning.put("type", "");
	            newfolder.put("devices", outputdevices);
				newfolder.put("versioning",  versioning);
				allfolders.add(newfolder);
			}
	
			HttpPost post = new HttpPost(postUrl);
			post.setHeader("X-API-Key", serverapi);
			post.setHeader("Content-type", "application/json");
			String json = config.toJSONString();
			StringEntity  postingString = new StringEntity(json);//convert your pojo to   json
			post.setEntity(postingString);
			
			HttpResponse  response = httpclient.execute(post);
			if( response.getStatusLine().getStatusCode() != 200 )
			{
				log.info(json);
				throw new OpenEditException("SyncThing Server post error " + response.getStatusLine().getStatusCode());
			}
			
		}
		catch( Throwable ex)
		{
			log.error(ex);
			throw new OpenEditException(ex);
		}
	}



//https://docs.oracle.com/javase/tutorial/displayCode.html?code=https://docs.oracle.com/javase/tutorial/essential/io/examples/WatchDir.java
	@Override
	public void scanFolders(final MediaArchive inArchive, ScriptLogger inLog)
	{
		getWebServer().checkMounts();
		Collection hits = loadFolders( inArchive.getCatalogId() );
		for(Iterator iterator = hits.iterator(); iterator.hasNext();)
		{
			Data data = (Data)iterator.next();
			final Data folder = getFolderSearcher(inArchive.getCatalogId()).loadData(data);

			String base = "/WEB-INF/data/" + inArchive.getCatalogId() + "/originals";
			String name = folder.get("subfolder");
			String path = base + "/" + name ;
			Lock lock = inArchive.getLockManager().lockIfPossible(path, "HotFolderManager");
			if( lock == null)
			{
				inLog.info("folder is already in lock table" + path);
				continue;
			}
			try
			{
				Object enabled = folder.getValue("enabled");
				if( enabled != null && "false".equals( enabled.toString() ) )
				{
					inLog.info("Hot folder not enabled " + name);
					continue;
				}
				inLog.info("Starting hot folder import " + name);
	
				//look for git folders?
				try
				{
					//pullGit(path,1);
					Collection found = importHotFolder(inArchive,folder); 
					if( found != null)
					{
						inLog.info(name + " Imported " + found.size() + " assets");
					}
				}
				catch( Exception ex)
				{
					log.error("Could not process folder ${path}",ex);
				}
				
				String monitor = folder.get("monitortree");
				if(Boolean.valueOf(monitor) )
				{
					final ContentItem item = getPageManager().getRepository().getStub(path);
					if(item.exists() && !getFolderMonitor().hasFolderTree(item.getAbsolutePath()))
					{
						//will scan each folder once then monitor it from now on
						getFolderMonitor().addPathChangedListener(item.getAbsolutePath(), new PathChangedListener()
						{
							@Override
							public void pathChanged(String inType, String inAbsolutePath)
							{
								String ending = inAbsolutePath.substring( item.getAbsolutePath().length() );
								importHotFolder(inArchive, folder, ending);
							}
						});
					}
				}
			}
			finally
			{
				inArchive.releaseLock(lock);
			}
		}
	
	/*
	AssetImporter importer = (AssetImporter)moduleManager.getBean("assetImporter");
	importer.setExcludeFolders("Fonts,Links");
	//importer.setIncludeFiles("psd,tif,pdf,eps");
	importer.setUseFolders(false);
	
	String assetRoot = "/WEB-INF/data/" + archive.getCatalogId() + "/originals/";
		
	List created = importer.processOn(assetRoot, assetRoot, archive, context.getUser());
	log.info("created images " + created.size() );
	*/
	}

/*
void pullGit(String path, int deep)
{
	ContentItem page = getPageManager().getRepository().getStub(path + "/.git");
	if( page.exists() )
	{
		Exec exec = .getBean("exec");
		List commands = new ArrayList();
		ContentItem root = pageManager.getRepository().get(path);		
//		commands.add("--work-tree=" + root.getAbsolutePath());
//		commands.add("--git-dir=" + page.getAbsolutePath());

		File from = new File( root.getAbsolutePath() );
		commands.add("pull");
		ExecResult result = exec.runExec("git",commands, from);
		if( result.isRunOk() )
		{
			log.info("pulled from git "  + root.getAbsolutePath() );
		}
		else
		{
			log.error("Could not pull from "  + root.getAbsolutePath() );
		}
	}
	else if( deep < 4 )
	{
		List paths = pageManager.getChildrenPaths(path);
		if( paths != null )
		{
			deep++;
			for(String child: paths)
			{
				ContentItem childpage = pageManager.getRepository().getStub(child);
				if( childpage.isFolder() )
				{
					pullGit(child,deep);
				}
			}	
		}
	}
}
*/
}
