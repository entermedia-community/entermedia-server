package model.importer;

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.http.HttpResponse
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.scanner.AssetImporter
import org.openedit.entermedia.scanner.HotFolderManager
import org.openedit.entermedia.util.TimeParser
import org.openedit.repository.Repository
import org.openedit.repository.filesystem.FileRepository
import org.openedit.repository.filesystem.XmlVersionRepository
import org.openedit.util.DateStorageUtil

import com.openedit.WebServer
import com.openedit.page.manage.PageManager
import com.openedit.util.EmStringUtils

public class BaseHotFolderManager implements HotFolderManager
{
	private static final Log log = LogFactory.getLog(BaseHotFolderManager.class);

	protected PageManager fieldPageManager;
	protected SearcherManager fieldSearcherManager;
	protected WebServer fieldWebServer;

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
			String external = folder.get("externalpath");;
			String categoryname =  folder.get("categoryname");
			String type = folder.get("hotfoldertype");
			
			if(type == null || type  == "mount" )
			{
				type = "mount";
				
			}
			
			//save subfolder with the value of the end of externalpath
//			if( external != null )
//			{
//				String epath = external.trim();
//				epath = epath.replace('\\', '/');
//				if( epath.endsWith("/"))
//				{
//					epath = epath.substring(0,epath.length() - 1);
//				}
//				folderpath = PathUtilities.extractDirectoryName(epath + "/junk.html");
//			}
			
			String fullpath = originalpath + "/" + categoryname;
			//String versioncontrol = folder.get("versioncontrol");
			Repository created = createRepo(type);
			created.setPath(fullpath);
			created.setExternalPath(external);
			created.setFilterIn(folder.get("includes"));
			created.setFilterOut(folder.get("excludes"));
			//add varliables
			for (String key:folder.getProperties().keySet()) 
			{
				created.setProperty(key,folder.getProperties().get(key))
			}
			
			newrepos.add(created);
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
		if( inType == "version" )
		{
			repo = new XmlVersionRepository();
			repo.setRepositoryType("versionRepository");
		}
		else if( inType == "s3" )
		{
			repo = new XmlVersionRepository();
			repo.setRepositoryType("versionRepository");
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

		return hfsearcher.getAllHits();
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
	 * @see org.openedit.entermedia.scanner.HotFolderManager2#deleteFolder(java.lang.String, org.openedit.Data)
	 */
	@Override
	public void deleteFolder(String inCatalogId, Data inExisting)
	{
		String type = inExisting.get("hotfoldertype");
		if( type == "syncthing")
		{
			updateSyncThingFolders(inCatalogId);
		}
		getFolderSearcher(inCatalogId).delete(inExisting, null);
	}


	/* (non-Javadoc)
	 * @see org.openedit.entermedia.scanner.HotFolderManager2#saveFolder(java.lang.String, org.openedit.Data)
	 */
	@Override
	public void saveFolder(String inCatalogId, Data inNewrow)
	{
		//String categoryfolder = inNewrow.get("categoryfolder");
		String type = inNewrow.get("hotfoldertype");
		if( type == "syncthing")
		{
			//getPageManager().getPage("/WEB-INF/data/" + inCatalogId + "/originals/" + categoryfolder );
			//inNewrow.setProperty("externalpath",
			updateSyncThingFolders(inCatalogId);
		}
		else if( type == "mount")
		{
			
		}
		
		getFolderSearcher(inCatalogId).saveData(inNewrow, null);		
	}	

	/* (non-Javadoc)
	 * @see org.openedit.entermedia.scanner.HotFolderManager2#importHotFolder(org.openedit.entermedia.MediaArchive, org.openedit.Data)
	 */
	@Override
	public List<String> importHotFolder(MediaArchive inArchive, Data inFolder)
	{
		inFolder = getFolderSearcher(inArchive.getCatalogId()).loadData(inFolder);
		String base = "/WEB-INF/data/" + inArchive.getCatalogId() + "/originals";
		String name = inFolder.get("subfolder");
		String path = base + "/" + name;

//		Page local = getPageManager().getPage(path + "/");
//		if( !local.exists() )
//		{
//			getPageManager().putPage(local);
//		}
		AssetImporter importer = (AssetImporter)getWebServer().getModuleManager().getBean("assetImporter");
		
		String excludes = inFolder.get("excludes");
		if( excludes != null )
		{
			List<String> list = EmStringUtils.split(excludes);
			for (int i = 0; i < list.size(); i++)
			{
				String row = list.get(i).trim();
				if( row.startsWith("/") &&  !row.startsWith(path))
				{
					row = path + row;
				}
				list.set(i, row);
			}
			importer.setExcludeMatches(list);
		}
		
		importer.setIncludeExtensions(inFolder.get("includes"));
		String attachments = inFolder.get("attachmenttrigger");
		if( attachments != null )
		{
			Collection attachmentslist = EmStringUtils.split(attachments);
			importer.setAttachmentFilters(attachmentslist);
		}
		
		Date started = new Date();
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
		log.info(path + " scan started. skip mod check = " + skipmodcheck );
		
		List<String> paths = importer.processOn(base, path, inArchive, skipmodcheck, null);
		if( !skipmodcheck )
		{
			inFolder.setProperty("lastscanstart", DateStorageUtil.getStorageUtil().formatForStorage(started));
			getFolderSearcher(inArchive.getCatalogId()).saveData(inFolder, null);
		}
		
		log.info(inFolder + " Imported " + paths.size() + " in " + ((new Date().getTime() - started.getTime())/6000L) );
		
		return paths;
	}
	
	public void updateSyncThingFolders(String inCatalogId)	
	{
		Collection hotfolders = loadFolders(inCatalogId);
		
		//TODO: get login/key information from system/systemsettings
		Data server = getSearcherManager().getData("system","systemsettings","syncthingserver");
		String postUrl = "http://" + server.get("value") + "/rest/sdfdsf";
		try
		{
			CloseableHttpClient httpclient = HttpClients.createDefault();
			HttpGet httpGet = new HttpGet(postUrl);
			CloseableHttpResponse response1 = httpclient.execute(httpGet);
			String returned = EntityUtils.toString(response1);
			JSONObject config = new JSONParser().parse(returned);

			//Save it
			//TODO: Make a map of existing device ids
			Set devices = new Set();
			config.get("devices");
			//TODO: Remove all existing folders with that that contains inCatalogId + "/originals"
			
			//TODO: Add all the folders and devices needed
			for(Data folder:hotfolders)
			{
				//TODO: add back in the device and the folder
				
			}
	
			HttpPost post = new HttpPost(postUrl);
			StringEntity  postingString =new StringEntity(config.toJSONString());//convert your pojo to   json
			post.setEntity(postingString);
			post.setHeader("Content-type", "application/json");
			HttpResponse  response = httpclient.execute(post);
		}
		catch( Throwable ex)
		{
			log.error(ex);
		}
		
	
	}			

}
