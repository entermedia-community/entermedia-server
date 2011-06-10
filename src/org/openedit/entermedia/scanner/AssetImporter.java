/*
 * Created on Oct 2, 2005
 */
package org.openedit.entermedia.scanner;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.AssetUtilities;
import org.openedit.entermedia.Category;
import org.openedit.entermedia.CompositeAsset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.fetch.UrlMetadataImporter;
import org.openedit.entermedia.search.AssetSearcher;
import org.openedit.repository.ContentItem;
import org.openedit.util.DateStorageUtil;

import com.openedit.OpenEditException;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.users.User;
import com.openedit.util.FileUtils;
import com.openedit.util.PathProcessor;
import com.openedit.util.PathUtilities;
import com.openedit.util.ZipUtil;

public class AssetImporter
{
	// protected List fieldInputTypes;
	protected PageManager fieldPageManager;
	private static final Log log = LogFactory.getLog(AssetImporter.class);
	protected Boolean fieldLimitSize;
	protected AssetUtilities fieldAssetUtilities;
    protected List<UrlMetadataImporter> fieldUrlMetadataImporters;
    
	public AssetUtilities getAssetUtilities()
	{
			return fieldAssetUtilities;
	}

	public void setAssetUtilities(AssetUtilities inAssetUtilities)
	{
		fieldAssetUtilities = inAssetUtilities;
	}

	public void processOnAll(String inRootPath, final MediaArchive inArchive, User inUser)
	{
		for (Iterator iterator = getPageManager().getChildrenPaths(inRootPath).iterator(); iterator.hasNext();)
		{
			String path = (String) iterator.next();
			Page topLevelPage = getPageManager().getPage(path);
			if (topLevelPage.isFolder() && !topLevelPage.getPath().endsWith("/CVS") && !topLevelPage.getPath().endsWith(".versions"))
			{
				processOn(inRootPath, path,inArchive, inUser);
			}
		}
	}
	
	public List<String> processOn(String inRootPath, String inStartingPoint, final MediaArchive inArchive, User inUser)
	{
		final List assets = new ArrayList();
		final List<String> allAssets = new ArrayList();
		PathProcessor finder = new PathProcessor()
		{
			public void process(ContentItem inInput, User inUser)
			{
				if (inInput.isFolder())
				{
					if (acceptDir(inInput))
					{
						String sourcepath = getAssetUtilities().extractSourcePath(inInput, inArchive);
						Asset asset = inArchive.getAssetArchive().getAssetBySourcePath(sourcepath);
						if( asset != null)
						{
							//check this one primary asset to see if it changed
							if( asset.getPrimaryFile() != null)
							{
								inInput = getPageManager().getRepository().getStub(inInput.getPath() + "/" + asset.getPrimaryFile());
								asset = getAssetUtilities().populateAsset(asset, inInput, inArchive, sourcepath, inUser);
								if( asset != null)
								{
									assets.add(asset);
									allAssets.add(asset.getId());
								}
							}
							//dont process sub-folders
						}
						else
						{
							//look deeper for assets
							List paths = getPageManager().getChildrenPaths(inInput.getPath());
							for (Iterator iterator = paths.iterator(); iterator.hasNext();)
							{
								String path = (String) iterator.next();
								ContentItem item = getPageManager().getRepository().getStub(path);
								if( isRecursive() )
								{
									process(item, inUser);
								}
							}
						}
					}
				}
				else
				{
					processFile(inInput, inUser);
				}
			}
			public void processFile(ContentItem inContent, User inUser)
			{
				Asset asset = getAssetUtilities().createAssetIfNeeded(inContent, inArchive, inUser);
				if( asset != null)
				{
					assets.add(asset);
					allAssets.add(asset.getId());
					if (assets.size() > 100)
					{
						saveAssets(assets, inArchive, inUser);
					}
				}
			}

		};
		finder.setPageManager(getPageManager());
		finder.setRootPath(inRootPath);
		finder.setExcludeFilter("xconf"); //The rest should be filtered by the mount itself

		finder.process(inStartingPoint, inUser);

		// Windows, for instance, has an absolute file system path limit of 256
		// characters
		checkPathLengths(inArchive, assets);
		Asset eventasset = null;
		if( assets.size() > 0)
		{
			eventasset = (Asset)assets.get(0);			
		}
		saveAssets(assets, inArchive,  inUser);
		if( eventasset != null)
		{
			inArchive.fireMediaEvent("assetsimported", inUser, eventasset, allAssets);
		}
		return allAssets;
	}

	/**
	 * This method removes any assets from the list that have absolute file
	 * paths which are too long. Should be used on windows servers that can't
	 * handle more than 256 characters in file names.
	 * 
	 * @param inArchive
	 * @param inAssets
	 */
	private void checkPathLengths(MediaArchive inArchive, List inAssets)
	{
		if (isSizeLimited().booleanValue())
		{
			int absolutepathlimit = 260;
			for (Iterator iterator = inAssets.iterator(); iterator.hasNext();)
			{
				Asset asset = (Asset) iterator.next();
				String path = inArchive.getAssetArchive().buildXmlPath(asset);
				ContentItem item = getPageManager().getPageSettingsManager().getRepository().get(path);
				if (item.getAbsolutePath().length() > absolutepathlimit)
				{
					log.info("Path too long. Couldn't save " + item.getPath());
					iterator.remove();
				}
			}
		}
	}

	private void saveAssets(List inAssets, MediaArchive inArchive, User inUser) throws OpenEditException
	{
		if (inAssets.size() == 0)
		{
			return;
		}
		for (Iterator iter = inAssets.iterator(); iter.hasNext();)
		{
			Asset asset = (Asset) iter.next();
			inArchive.getAssetArchive().saveAsset(asset);
			inArchive.fireMediaEvent("assetcreated",inUser, asset);
		}
		inArchive.getAssetSearcher().updateIndex(inAssets, false);
		inArchive.getAssetArchive().clearAssets();

		inAssets.clear();
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	public Boolean isSizeLimited()
	{
		if (fieldLimitSize == null)
		{
			if (System.getProperty("os.name").toLowerCase().contains("windows"))
			{
				fieldLimitSize = Boolean.TRUE;
			}
			else
			{
				fieldLimitSize = Boolean.FALSE;
			}
		}
		return fieldLimitSize;
	}

	//TODO: Reconcile this code with AssetUtilities.pupulateAsset


	public Data createAssetFromExistingFile( MediaArchive inArchive, User inUser, boolean unzip,  String inSourcepath)
	{
		String catalogid = inArchive.getCatalogId();
		
		String originalspath = "/WEB-INF/data/" + catalogid + "/originals/";
		Page page = getPageManager().getPage(originalspath + inSourcepath );
		if( !page.exists() )
		{
			return null;
		}

		String ext = PathUtilities.extractPageType(page.getName());
		if(unzip && "zip".equalsIgnoreCase(ext))
		{
			//unzip and create
			CompositeAsset results = new CompositeAsset();
			//the folder we are in
			Page parentfolder = getPageManager().getPage( page.getParentPath() );
			File dest = new File(parentfolder.getContentItem().getAbsolutePath());
			String destpath = dest.getAbsolutePath();
			ZipUtil zip = new ZipUtil();
			zip.setPageManager(getPageManager());
			try
			{
				List files = zip.unzip(page.getContentItem().getInputStream(), dest);
				for(Object o: files)
				{
					File f = (File) o;
					String path = f.getAbsolutePath().substring(destpath.length());
					path = path.replace('\\', '/');
					path =parentfolder.getPath() + path; //fix slashes
					Page p = getPageManager().getPage(path);
					Asset asset = createAssetFromPage(inArchive, inUser, p);
					if(asset != null)
					{
						results.addData(asset);
					}
				}
				
				getPageManager().removePage(page);
				return results;
			}
			catch (Exception e)
			{
				throw new OpenEditException(e);
			}
		}
		else
		{
			return createAssetFromPage(inArchive, inUser, page);
		}
	}
	
	protected Asset createAssetFromPage(MediaArchive inArchive, User inUser, Page inAssetPage)
	{
		String originals = "/WEB-INF/data" +  inArchive.getCatalogHome() + "/originals/";
		String sourcepath = inAssetPage.getPath().substring(originals.length());
		Asset asset = inArchive.getAssetBySourcePath(sourcepath);
		
		if(asset == null)
		{
			String id = inArchive.getAssetArchive().nextAssetNumber();
			asset = new Asset();
			asset.setId(id);
			asset.setSourcePath(sourcepath);
			//asset.setProperty("datatype", "original");
			asset.setFolder(inAssetPage.isFolder());
			String name = inAssetPage.getName();
			String ext = PathUtilities.extractPageType(name);
			if( ext != null)
			{
				ext = ext.toLowerCase();
			}
			asset.setProperty("fileformat", ext);
			asset.setName(name);
			asset.setCatalogId(inArchive.getCatalogId());
	
			String categorypath = PathUtilities.extractDirectoryPath(sourcepath);
			Category category = inArchive.getCategoryArchive().createCategoryTree(categorypath);
			asset.addCategory(category);
		}

		String absolutepath = inAssetPage.getContentItem().getAbsolutePath();
		File itemFile = new File(absolutepath);
		getAssetUtilities().getMetaDataReader().populateAsset(inArchive, itemFile, asset);
		inArchive.saveAsset(asset, inUser);
		
		return asset;
	}
	
	public List removeExpiredAssets(MediaArchive archive, String sourcepath, User inUser)
	{
		AssetSearcher searcher = archive.getAssetSearcher();
		SearchQuery q = searcher.createSearchQuery();
		HitTracker assets = null;
		if(sourcepath == null)
		{
			assets = searcher.getAllHits();
		}
		else
		{
			q.addStartsWith("sourcepath", sourcepath);
			assets = searcher.search(q);
		}
		List<String> removed = new ArrayList<String>();
		List<String> sourcepaths= new ArrayList<String>();
		
		for(Object obj: assets)
		{
			Data hit = (Data)obj;
			sourcepaths.add(hit.get("sourcepath")); //TODO: Move to using page of hits
			if( sourcepaths.size() > 250000)
			{
				log.error("Should not load up so many paths");
				break;
			}
		}
		for(String path: sourcepaths)
		{
			Asset asset = archive.getAssetBySourcePath(path);
			if( asset == null)
			{
				continue;
			}
			String assetsource = asset.getSourcePath();
			String pathToOriginal = "/WEB-INF/data" + archive.getCatalogHome() + "/originals/" + assetsource;
			if(asset.isFolder() && asset.getPrimaryFile() != null)
			{
				pathToOriginal = pathToOriginal + "/" + asset.getPrimaryFile();
			}
			Page page = getPageManager().getPage(pathToOriginal);
			if(!page.exists())
			{
				removed.add(asset.getSourcePath());
				archive.removeGeneratedImages(asset);
				archive.getAssetSearcher().delete(asset, inUser);
			}
		}
		return removed;
	}

	public Asset createAssetFromFetchUrl(MediaArchive inArchive, String inUrl, User inUser)
	{
		for(UrlMetadataImporter importer: getUrlMetadataImporters())
		{
			Asset asset = importer.importFromUrl(inArchive, inUrl, inUser);
			if( asset != null )
			{
				return asset;
			}
		}
		return null;
	}
		
	public List<UrlMetadataImporter> getUrlMetadataImporters()
	{
		if(fieldUrlMetadataImporters == null)
		{
			return new ArrayList<UrlMetadataImporter>();
		}
		return fieldUrlMetadataImporters;
	}

	public void setUrlMetadataImporters(List<UrlMetadataImporter> urlMetadataImporters)
	{
		fieldUrlMetadataImporters = urlMetadataImporters;
	}

	public void fetchMediaForAsset(MediaArchive inArchive, Asset inAsset, User inUser)
	{
			for(UrlMetadataImporter importer: getUrlMetadataImporters())
			{
				importer.fetchMediaForAsset(inArchive, inAsset,inUser);
			}
	}
	
	
}