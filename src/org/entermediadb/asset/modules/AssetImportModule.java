package org.entermediadb.asset.modules;

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.SearchQuery;

import model.assets.AssetTypeManager;
import model.assets.LibraryManager;

public class AssetImportModule  extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(AssetImportModule.class);

	
	public void assetsImported(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Collection hits = loadAssetHits(archive, inReq);
		if( hits == null)
		{
			log.error("No hits found");
		}
		//Set the asset type
		AssetTypeManager manager = new AssetTypeManager();
		manager.setContext(inReq);
		ScriptLogger logger = (ScriptLogger)inReq.getPageValue("log");
		manager.setLog(logger);
		manager.saveAssetTypes(hits, true);

		//TODO: Move this to AssetUtilities
//		boolean assigncategory = mediaArchive.isCatalogSettingTrue("assigncategoryonupload");
//		if(assigncategory) {
//			hits.each{
//				Asset current = it;
//				Category defaultcat = mediaArchive.getCategorySearcher().createCategoryPath(current.sourcePath);
//				
//				current.clearCategories();
//				current.addCategory(defaultcat);
//				mediaArchive.saveAsset(current, inReq.getUser());
//			}
//		}	
		
		//Look for collections and libraries
		LibraryManager librarymanager = new LibraryManager();
		librarymanager.setLog(logger);
		librarymanager.assignLibraries(archive, hits);
	}

	protected Collection loadAssetHits(MediaArchive archive, WebPageRequest inReq)
	{
		Collection hits = (Collection)inReq.getPageValue("hits");
		if( hits == null)
		{
			String ids = inReq.getRequestParameter("assetids");
			if( ids == null)
			{
			   log.info("AssetIDS required");
			   return null;
			}
			Searcher assetsearcher = archive.getAssetSearcher();
			SearchQuery q = assetsearcher.createSearchQuery();
			String assetids = ids.replace(","," ");
			q.addOrsGroup( "id", assetids );
		
			hits = assetsearcher.search(q);
		}
		return hits;
	}
	

	
}
