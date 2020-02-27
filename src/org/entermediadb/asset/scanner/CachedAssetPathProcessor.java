package org.entermediadb.asset.scanner;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.hittracker.HitTracker;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;

public class CachedAssetPathProcessor extends AssetPathProcessor
{
	private static final Log log = LogFactory.getLog(CachedAssetPathProcessor.class);
	protected HashMap<String,Long> fieldSizeCache = new HashMap<String,Long>(100000); //So we dont have double in memory at any given time
	int cachesaves = 0;
	//Starts here
	public void processAssets(String inStartingPoint, User inUser)
	{
		ContentItem item = getPageManager().getRepository().getStub(inStartingPoint);
		if( !item.exists() )
		{		
			log.info(item.getAbsolutePath() + " Did not exist");
			return;
		}

		//Make sure no part of sourcepath is already an asset
		String sourcepath = getAssetUtilities().extractSourcePath(item, true, getMediaArchive());
		String[] folderlist = sourcepath.split("/");
		String pathtocheck = "";
		for (int i = 0; i < folderlist.length; i++)
		{
			String nextfolder = folderlist[i];
			if(i > 0){
				pathtocheck = pathtocheck + "/" + nextfolder;
			} else{
				pathtocheck =folderlist[0];
			}
			Asset asset = getMediaArchive().getAssetSearcher().getAssetBySourcePath(pathtocheck);
			if(asset != null)
			{
				if( isShowLogs() )
				{
					log.error("Found top level asset " + inStartingPoint + " " + "checked: " + pathtocheck);
					getAssetImporter().fireHotFolderEvent(getMediaArchive(), "init", "error", 
							"Found top level asset " + pathtocheck , null);
				}
				return;
			}
		}
		
		//Start processing
		if (item.isFolder())
		{
			List paths = getPageManager().getChildrenPaths(
					item.getPath());
			for (Iterator iterator = paths.iterator(); iterator.hasNext();)
			{
				String path = (String) iterator.next();
				ContentItem subitem = getPageManager().getRepository()
						.getStub(path);
				if( subitem.isFolder() )
				{
					String foldersourcepath = getAssetUtilities().extractSourcePath(subitem, true, getMediaArchive());
					//Is this an asset itself?
					Asset asset = getMediaArchive().getAssetSearcher().getAssetBySourcePath(foldersourcepath);
					if( asset != null)
					{
						//processFile(subitem, inUser);
						continue; 
					}
					//Ok if its a folder then do a search, cached asset results and start processing
					process(subitem,inUser);
				}
				else if (acceptFile(subitem))
				{
					processFile(subitem, inUser); //Old school way. This checks cache first. IF not found then do a search anyways to double check
				}
			}
		}
		else if (acceptFile(item))
		{
			processFile(item, inUser);
		}
		saveImportedAssets(inUser);
		System.gc(); 

	}
	
	@Override
	protected void processAssetFolder(ContentItem inInput, User inUser)
	{
		String foldersourcepath = getAssetUtilities().extractSourcePath(inInput, true, getMediaArchive());
		
		loadCache(foldersourcepath);
		cachesaves = 0;
		super.processAssetFolder(inInput, inUser);
		log.info("foldersourcepath complete. CacheSaves: " + cachesaves);
		fieldSizeCache.clear();

	}
	
	protected void loadCache(String inFoldersourcepath)
	{
		HitTracker allchildren = getMediaArchive().query("asset").exact("foldersourcepath",inFoldersourcepath).search(); 
		allchildren.setHitsPerPage(99999);
		log.info("Loading Cache: " + allchildren.size() + " on " + inFoldersourcepath );

		for (Iterator iterator = allchildren.getPageOfHits().iterator(); iterator.hasNext();)
		{
			MultiValued asset = (MultiValued) iterator.next();
			long longval = asset.getLong("filesize");
			
			String sp = asset.get("archivesourcepath");
			if( sp == null)
			{
				sp = asset.getSourcePath();
			}
			fieldSizeCache.put(sp,longval);
		}
	}

	@Override
	protected Asset createAssetIfNeeded(ContentItem inContent, MediaArchive inMediaArchive, User inUser)
	{
		String foldersourcepath = getAssetUtilities().extractSourcePath(inContent, true, getMediaArchive());
		Long sizeval = fieldSizeCache.get(foldersourcepath);
		if(sizeval != null && sizeval == inContent.getLength())
		{
			cachesaves++;
			return null; //Not needed
		}
		return super.createAssetIfNeeded(inContent, inMediaArchive, inUser);
	}
	
}
