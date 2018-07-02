package importing;

import org.entermediadb.asset.Asset
import org.entermediadb.asset.Category
import org.entermediadb.asset.MediaArchive
import org.openedit.data.Searcher
import org.openedit.hittracker.SearchQuery

import asset.model.AssetTypeManager
import model.assets.LibraryManager




public void readProjectData()
{
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	Collection hits = context.getPageValue("hits");
	if( hits == null)
	{
		String ids = context.getRequestParameter("assetids");
		if( ids == null)
		{
		   log.info("AssetIDS required");
		   return;
		}
		Searcher assetsearcher = mediaArchive.getAssetSearcher();
		SearchQuery q = assetsearcher.createSearchQuery();
		String assetids = ids.replace(","," ");
		q.addOrsGroup( "id", assetids );
	
		hits = assetsearcher.search(q);
	}
	//Set the asset type
	AssetTypeManager manager = new AssetTypeManager();
	manager.context = context;
	manager.log = log;
	manager.saveAssetTypes(hits, true);
	
	//Look for collections and libraries
	LibraryManager librarymanager = new LibraryManager();
	librarymanager.log = log;
	librarymanager.assignLibraries(mediaArchive, hits);

	boolean assigncategory = mediaArchive.isCatalogSettingTrue("assigncategoryonupload");
	
			
			
			if(assigncategory) {
				hits.each{
					Asset current = it;
					Category defaultcat = mediaArchive.getCategorySearcher().createCategoryPath(current.sourcePath);
					
					current.clearCategories();
					current.addCategory(defaultcat);
					mediaArchive.saveAsset(current, context.getUser());
					
				}
			
				
			}	
	
}

readProjectData();


