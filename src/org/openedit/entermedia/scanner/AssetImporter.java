/*
 * Created on Oct 2, 2005
 */
package org.openedit.entermedia.scanner;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.ListHitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.users.User;
import com.openedit.util.PathUtilities;
import com.openedit.util.Replacer;
import com.openedit.util.ZipUtil;

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
    protected String fieldIncludeExtensions;
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

	public String getIncludeExtensions()
	{
		return fieldIncludeExtensions;
	}

	public void setIncludeExtensions(String inIncludeFiles)
	{
		fieldIncludeExtensions = inIncludeFiles;
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
				processOn(inRootPath, path,inArchive, true, inUser);
			}
		}
	}
	protected void assetsImported( MediaArchive inArchive, java.util.List<Asset> inAssetsSaved)
	{
		//this might be overriden to push
	}
	public List<String> processOn(String inRootPath, String inStartingPoint, final MediaArchive inArchive, boolean inSkipModCheck, User inUser)
	{
		AssetPathProcessor finder = new AssetPathProcessor()
		{
			protected void assetsImported(java.util.List<Asset> inAssetsSaved)
			{
				AssetImporter.this.assetsImported(inArchive, inAssetsSaved);
			};
		};
		finder.setSkipModificationCheck(inSkipModCheck);
		finder.setMediaArchive(inArchive);
		finder.setPageManager(getPageManager());
		finder.setRootPath(inRootPath);
		finder.setAssetUtilities(getAssetUtilities());
		finder.setExcludeMatches(getExcludeMatches()); //The rest should be filtered by the mount itself
		finder.setIncludeExtensions(getIncludeExtensions());
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
	protected Asset createAssetFromPage(MediaArchive inArchive, User inUser, Page inAssetPage, String inAssetId)
	{
		Asset asset = getAssetUtilities().createAssetIfNeeded(inAssetPage.getContentItem(),inArchive, inUser);
		boolean existing = true;
		if( asset == null)
		{
			//Should never call this
			String originals = "/WEB-INF/data" +  inArchive.getCatalogHome() + "/originals/";
			String sourcepath = inAssetPage.getPath().substring(originals.length());
			asset = inArchive.getAssetBySourcePath(sourcepath);
			return asset;
		}
		if( asset.get("recordmodificationdate") == null )
		{
			existing = false;
		}
		if( asset.getId() == null) 
		{
			asset.setId(inAssetId);
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
		
		
		return asset;
	}

	protected Asset createAssetFromPage(MediaArchive inArchive, User inUser, Page inAssetPage)
	{
		return createAssetFromPage(inArchive, inUser, inAssetPage, null);
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
}