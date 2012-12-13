package importing;

import org.openedit.data.Searcher
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

		AssetSearcher searcher = mediaarchive.getAssetSearcher();
		Searcher divisionSearcher = mediaarchive.getSearcher("division")
		Searcher librarySearcher = mediaarchive.getSearcher("library")
		
		List tosave = new ArrayList();
		for (Data hit in inAssets)
		{	
			String split = hit.sourcepath.split("/");
			if( split.length > 1 )
			{
				Data division = divisionSearcher.find("folder",split[0]);
				if( division != null )
				{
					SearchQuery query = librarySearcher.createSearchQuery().append("division",division.getId()).append("folder",split[1]);
					Data library =	librarySearcher.searchByQuery(query);
					if( library == null )
					{
						//create
					}
					Asset asset = mediaarchive.
					tosave.add(
					//Add libraryasset table
					
				}
			}
			
			Asset real = mediaArchive.getAssetBySourcePath);
		
			Asset real = checkForEdits(typemap, hit);
			if( real == null )
			{
				real = checkLibrary(mediaarchive,hit);
			}
			else
			{
				checkLibrary(mediaarchive,real);
			}
			if(real != null)
			{
				tosave.add(real);
			}
			if(tosave.size() == 100)
			{
				saveAssets(searcher, tosave);
				tosave.clear();
			}
		}
		saveAssets(searcher, tosave);
	
	
}


setAssetTypes();
//sendEmail();
