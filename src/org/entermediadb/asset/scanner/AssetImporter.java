/*
 * Created on Oct 2, 2005
 */
package org.entermediadb.asset.scanner;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.AssetUtilities;
import org.entermediadb.asset.CompositeAsset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.fetch.UrlMetadataImporter;
import org.entermediadb.asset.search.AssetSearcher;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.ListHitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;
import org.openedit.users.User;
import org.openedit.util.PathUtilities;
import org.openedit.util.ZipUtil;

public class AssetImporter
{
	// protected List fieldInputTypes;
	protected PageManager fieldPageManager;
	private static final Log log = LogFactory.getLog(AssetImporter.class);
	protected Boolean fieldLimitSize;
	protected AssetUtilities fieldAssetUtilities;
    protected List<UrlMetadataImporter> fieldUrlMetadataImporters;
    protected boolean fieldUseFolders = false;
//    
//	protected List fieldIncludeExtensions;
//	protected List fieldExcludeExtensions;
//	protected List fieldExcludeFolderMatch;
    
    protected List<String> fieldExcludeMatches;
    protected String fieldIncludeMatches;
    protected Collection fieldAttachmentFilters;
    
	public Collection getAttachmentFilters()
	{
		return fieldAttachmentFilters;
	}

	public void setAttachmentFilters(Collection inAttachmentFilters)
	{
		fieldAttachmentFilters = inAttachmentFilters;
	}

	public List<String> getExcludeMatches()
	{
		return fieldExcludeMatches;
	}

	public void setExcludeMatches(List<String> inExcludeFolders)
	{
		fieldExcludeMatches = inExcludeFolders;
	}

	public String getIncludeMatches()
	{
		return fieldIncludeMatches;
	}

	public void setIncludeMatches(String inIncludeFiles)
	{
		fieldIncludeMatches = inIncludeFiles;
	}


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
	protected void assetsImported( MediaArchive inArchive, java.util.List<Asset> inAssetsSaved)
	{
		//this might be overriden to push
	}
	public List<String> processOn(String inRootPath, String inStartingPoint, final MediaArchive inArchive, User inUser)
	{
		AssetPathProcessor finder = new AssetPathProcessor()
		{
			protected void assetsImported(java.util.List<Asset> inAssetsSaved)
			{
				AssetImporter.this.assetsImported(inArchive, inAssetsSaved);
			};
		};
		//finder.setSkipModificationCheck(true);
		finder.setMediaArchive(inArchive);
		finder.setPageManager(getPageManager());
		finder.setRootPath(inRootPath);
		finder.setAssetUtilities(getAssetUtilities());
		finder.setExcludeMatches(getExcludeMatches()); //The rest should be filtered by the mount itself
		finder.setIncludeMatches(getIncludeMatches());
		finder.setAttachmentFilters(getAttachmentFilters());
		finder.processAssets(inStartingPoint, inUser);
		
		// Windows, for instance, has an absolute file system path limit of 256
		// characters
//		if( isOnWindows() )
//		{
//			checkPathLengths(inArchive, assets);
//		}
		return finder.assetsids;
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}


	public Asset createAssetFromExistingFile( MediaArchive inArchive, User inUser, boolean unzip,  String inSourcepath)
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
			List assets = new ArrayList();
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
						assets.add(asset);
					}
				}
				
				getPageManager().removePage(page);
				CompositeAsset results = new CompositeAsset(inArchive,new ListHitTracker(assets));

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
	public Asset createAssetFromPage(MediaArchive inArchive, boolean includefilename, User inUser, Page inAssetPage, String inAssetId)
	{
		Asset asset = getAssetUtilities().createAssetIfNeeded(inAssetPage.getContentItem(),includefilename, inArchive, inUser);

		if( asset == null)
		{
			//Should never call this
			String originals = "/WEB-INF/data" +  inArchive.getCatalogHome() + "/originals/";
			String sourcepath = inAssetPage.getPath().substring(originals.length());
			asset = inArchive.getAssetBySourcePath(sourcepath);
			return asset;
		}
		if( asset.getId() == null) 
		{
			asset.setId(inAssetId);
		}
		saveAsset(inArchive, inUser, asset);
		
		
		return asset;
	}

	public void saveAsset(MediaArchive inArchive, User inUser, Asset asset) {
		boolean existing = true;
		if( asset.get("recordmodificationdate") == null )
		{
			existing = false;
		}

		inArchive.saveAsset(asset, inUser);
		if( existing )
		{
			inArchive.fireMediaEvent("asset/originalmodified",inUser, asset);				
		}
		else
		{
			inArchive.fireMediaEvent("asset/assetcreated",inUser, asset);
		}

		inArchive.fireMediaEvent("importing/assetsimported", inUser, asset);
	}

	protected Asset createAssetFromPage(MediaArchive inArchive, User inUser, Page inAssetPage)
	{
		return createAssetFromPage(inArchive, true, inUser, inAssetPage, null);
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

	public Asset createAssetFromFetchUrl(MediaArchive inArchive, String inUrl, User inUser, String inSourcePath, String inFileName)
	{
		for(UrlMetadataImporter importer: getUrlMetadataImporters())
		{
			Asset asset = importer.importFromUrl(inArchive, inUrl, inUser,  inSourcePath,  inFileName);
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
	
	/**
	 * For this to work, inSourcePath needs to have an extention, i.e.
	 * newassets/admin/118/picture.jpg
	 * @param inStructions
	 * @param inSourcePath
	 * @return
	 */
	public Asset createAsset(MediaArchive inArchive, String inSourcePath)
	{
		String extension = PathUtilities.extractPageType(inSourcePath);
		if (extension != null)
		{
			Asset asset = new Asset(inArchive); //throw away
			asset.setCatalogId(inArchive.getCatalogId());
	//		asset.setId(inArchive.getAssetArchive().nextAssetNumber());
			asset.setSourcePath(inSourcePath);
			extension = extension.toLowerCase();
			asset.setProperty("fileformat", extension);
	//		inArchive.saveAsset(asset, null);
			return asset;
		}
		return null;
	}
	
}