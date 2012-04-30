package org.openedit.entermedia.scanner;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.entermedia.MediaArchive;
import org.openedit.repository.Repository;
import org.openedit.repository.filesystem.FileRepository;
import org.openedit.util.DateStorageUtil;

import com.openedit.WebServer;
import com.openedit.page.manage.PageManager;

public class HotFolderManager
{
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
		Map<String,Repository> mounts = new HashMap<String,Repository>();
		List configs = getPageManager().getRepositoryManager().getRepositories();
		String path = "/WEB-INF/data/" + inCatalogId + "/originals";
		
		for (Iterator iterator = configs.iterator(); iterator.hasNext();)
		{
			Repository config = (Repository) iterator.next();
			if (config.getPath().startsWith(path)) 
			{
				mounts.put(config.getPath(),config);
			}
		}
		
		Collection folders = loadFolders(inCatalogId);
		for (Iterator iterator = folders.iterator(); iterator.hasNext();)
		{
			Data folder = (Data) iterator.next();
			String folderpath = folder.get("subfolder");
			String fullpath = path + "/" + folderpath;
			Repository repo = mounts.get(fullpath);
			
			if( repo == null)
			{
				repo = new FileRepository();
				configs.add(repo);
			}
			else
			{
				mounts.remove(fullpath);
			}
			//save data to repo
			repo.setPath(fullpath);
			repo.setExternalPath(folder.get("externalpath"));
			repo.setFilterIn(folder.get("includes"));
			repo.setFilterOut(folder.get("excludes"));
		}
		
		//TODO: Clean out any old mounts
		if( mounts.size() > 0)
		{
			for (Iterator iterator = mounts.keySet().iterator(); iterator.hasNext();)
			{
				String key = (String) iterator.next();
				Repository repo = mounts.get(key);
				configs.remove(repo);
			}
		}
		//getPageManager().getRepositoryManager().setRepositories(configs);
		//save the file
		getWebServer().saveMounts(configs);
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
		String base = "/WEB-INF/data/" + inArchive.getCatalogId() + "/originals";
		String name = inFolder.get("subfolder");
		String path = base + "/" + name;
		
		AssetImporter importer = (AssetImporter)getWebServer().getModuleManager().getBean("assetImporter");
		importer.setExcludeFolders(inFolder.get("excludes"));
		importer.setIncludeFiles(inFolder.get("includes"));
		//importer.
		
		inFolder.setProperty("lastscanstart", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
		getFolderSearcher(inArchive.getCatalogId()).saveData(inFolder, null);
		List<String> paths = importer.processOn(base, path, inArchive, null);
		return paths;
	}
	
}
