package org.openedit.entermedia.scanner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.util.TimeParser;
import org.openedit.repository.Repository;
import org.openedit.repository.filesystem.FileRepository;
import org.openedit.repository.filesystem.XmlVersionRepository;
import org.openedit.util.DateStorageUtil;

import com.openedit.WebServer;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.util.EmStringUtils;

public class HotFolderManager
{
	private static final Log log = LogFactory.getLog(HotFolderManager.class);

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
		String path = "/WEB-INF/data/" + inCatalogId + "/";
		List extras = new ArrayList();
		
		for (Iterator iterator = configs.iterator(); iterator.hasNext();)
		{
			Repository config = (Repository) iterator.next();
			if( config.getPath().startsWith(path))
			{
				extras.add( config.getPath());
			}
		}

		//We should see if they are already configured?
		List<Repository> mounts = new ArrayList<Repository>();
		Collection folders = loadFolders(inCatalogId);
		for (Iterator iterator = folders.iterator(); iterator.hasNext();)
		{
			Data folder = (Data) iterator.next();
			String external = folder.get("externalpath");

			String folderpath = folder.get("subfolder");
			String originalpath = "/WEB-INF/data/" + inCatalogId + "/originals";
			String fullpath = originalpath + "/" + folderpath;
			String versioncontrol = folder.get("versioncontrol");
			if( addOrEdit(configs,mounts,fullpath,external,Boolean.parseBoolean(versioncontrol)) )
			{
				extras.remove(fullpath);
			}

			String generatedpath = "/WEB-INF/data/" + inCatalogId + "/generated";
			String fullgeneratedpath = generatedpath + "/" + folderpath;
			String genversioncontrol = folder.get("generatedversioncontrol");
			if( addOrEdit(configs,mounts,fullgeneratedpath,null,Boolean.parseBoolean(genversioncontrol)) )
			{
				extras.remove(fullgeneratedpath); //remove the old
			}

		}
		for (Iterator iterator = extras.iterator(); iterator.hasNext();)
		{
			String fullpath = (String) iterator.next();
			getPageManager().getRepositoryManager().removeRepository(fullpath);
		}

		configs = getPageManager().getRepositoryManager().getRepositories();
		configs.addAll(mounts);
		getWebServer().saveMounts(configs);
		//getPageManager().getRepositoryManager().setRepositories(configs);
		//save the file
	}

	/**
	 * return true if we should just keep the old one
	*/
	
	protected boolean addOrEdit(List configs, List<Repository> mounts, String inPath, String externalpath, boolean useversioncontrol)
	{
		if( externalpath == null && !useversioncontrol)
		{
			return false; //this will remove it			
		}

		Repository existing = findRepoByPath( configs, inPath );
		//Do we need to create one?
		boolean keepexisting = true;
		if( existing != null)
		{
			existing.setExternalPath(externalpath);
			if( useversioncontrol && !(existing instanceof XmlVersionRepository) )
			{
				configs.remove(existing);
				existing = null;
			}
			if( !useversioncontrol && (existing instanceof XmlVersionRepository) )
			{
				configs.remove(existing);
				existing = null;
			}
		}
		
		if( existing == null)
		{
			existing = createRepo(useversioncontrol);
			existing.setPath(inPath);
			existing.setExternalPath(externalpath);
			mounts.add(existing);
			keepexisting = false;
		}

		return keepexisting;
	}


	protected Repository createRepo(boolean versioncontrol)
	{
		Repository repo;
		if( versioncontrol )
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


	public void deleteFolder(String inCatalogId, Data inExisting)
	{
		getFolderSearcher(inCatalogId).delete(inExisting, null);
		saveMounts(inCatalogId);
	}


	public void saveFolder(String inCatalogId, Data inNewrow)
	{
		getFolderSearcher(inCatalogId).saveData(inNewrow, null);		
		saveMounts(inCatalogId);
	}	

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
		log.info(inFolder + " Imported " + paths.size() );
		
		return paths;
	}
	
}
