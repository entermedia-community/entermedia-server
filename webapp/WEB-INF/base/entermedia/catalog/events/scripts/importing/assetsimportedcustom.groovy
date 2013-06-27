package importing;

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive

import assets.model.AssetTypeManager
import assets.model.EmailNotifier;
import assets.model.LibraryAddingAssetTypeManager;

import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.manage.*;

public void setAssetTypes()
{
	String ids = context.getRequestParameter("assetids");
	if( ids == null)
	{
	   log.info("AssetIDS required");
	   return;
	}
	String assetids = ids.replace(","," ");

	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	Searcher assetsearcher = mediaArchive.getAssetSearcher();
	SearchQuery q = assetsearcher.createSearchQuery();
	q.addOrsGroup( "id", assetids );

	HitTracker assets = assetsearcher.search(q);
	AssetTypeManager manager = new LibraryAddingAssetTypeManager();
	manager.context = context;
	manager.saveAssetTypes(assets);
	
	setupProjects(assets);
	
}
public void sendEmail()
{
	EmailNotifier emailer = new EmailNotifier();
	emailer.context = context;
	emailer.emailOnImport();
}

public void setupProjects(HitTracker assets)
{
		//Look at source path for each asset?
		MediaArchive mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");//Search for all files looking for videos

		Searcher searcher = mediaarchive.getAssetSearcher();
		Searcher librarySearcher = mediaarchive.getSearcher("library")
		
		List tosave = new ArrayList();
		for (Data hit in assets)
		{
			def sourcepath = hit.getSourcePath();
			String[] split = sourcepath.split("/");
			if( split.length > 2 )
			{
				SearchQuery query = librarySearcher.createSearchQuery();
				query.setAndTogether(false);
				String sofar = "";
				for( int i=0;i<split.length - 1;i++)
				{
					sofar = "${sofar}${split[i]}";
					if( i > 0 )
					{
						query.addExact( "folder", sofar );
					}
					if( i > 10 )
					{
						break;
					}
					sofar = "${sofar}/";
				}
				query.addSortBy("folderDown");
				
				Data library =	librarySearcher.searchByQuery(query);
				if( library != null )
				{
					Asset asset = mediaarchive.getAssetBySourcePath(sourcepath);
					asset.addLibrary(library.getId());
					tosave.add(asset);
					log.info("auto added library by folder " + library.get("folder") ); 
				}
				
			}
			if(tosave.size() == 100)
			{
				searcher.saveAllData(tosave, null);
				tosave.clear();
			}
		}
		searcher.saveAllData(tosave, null);
	
	
}

public void verifyRules()
{
	String ids = context.getRequestParameter("assetids");
	if( ids == null)
	{
	   log.info("AssetIDS required");
	   return;
	}
	String assetids = ids.replace(","," ");

	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	Searcher assetsearcher = mediaArchive.getAssetSearcher();
	SearchQuery q = assetsearcher.createSearchQuery();
	q.addOrsGroup( "id", assetids );

	HitTracker assets = assetsearcher.search(q);
	assets.each{
		 Asset asset = mediaArchive.getAsset("${it.id}");
		 if(asset.width != null){
			 int width = Integer.parseInt(asset.width);
			 if(width < 1024){
				 asset.setProperty("editstatus", "rejected");
				 asset.setProperty("notes", "Asset did not meet minimum width criteria.  Width was ${asset.width}");
				 
			 }
			 assetsearcher.saveData(asset, null);
		 }
	}
	
	
	
}


setAssetTypes();
//verifyRules();

//sendEmail();
