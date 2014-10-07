package importing;
import org.openedit.Data
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.search.AssetSearcher

import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.manage.*

public void init()
{
	MediaArchive archive = context.getPageValue("mediaarchive");//Search for all files looking for videos
	PageManager pageManager = archive.getPageManager();
	List children = pageManager.getChildrenPaths("/WEB-INF/data/" + archive.getCatalogId() + "/originals");
	if(children.size() == 0 )
	{
		log.info("No originals found. Skipping clear");
		return;	
	}

	AssetSearcher searcher = archive.getAssetSearcher();
	SearchQuery q = searcher.createSearchQuery();
	HitTracker assets = null;
	q = searcher.createSearchQuery().append("editstatus", "7");
	q.addSortBy("sourcepath");
	assets = searcher.search(q);
	assets.setHitsPerPage(1000);
	int removed = 0;
	List tosave = new ArrayList();
	int existed = 0;	
	for(Object obj: assets)
	{
		Data hit = (Data)obj;
	
		String assetsource = hit.getSourcePath();
		String pathToOriginal = "/WEB-INF/data" + archive.getCatalogHome() + "/originals/" + assetsource;
		
		if(pageManager.getRepository().doesExist(pathToOriginal) )
		{
			Asset asset = archive.getAssetBySourcePath(assetsource);
			if( asset == null)
			{
				log.info("invalid asset " + pathToOriginal);
				continue;
			}

			if(asset.isFolder() && asset.getPrimaryFile() != null)
			{
				pathToOriginal = pathToOriginal + "/" + asset.getPrimaryFile();
				if( !pageManager.getRepository().doesExist(pathToOriginal) )
				{
					removed++;
					continue; //never mind, it is deleted
				}
			}
			existed++;

           if( asset.get("editstatus") != "1" )
           {
			   asset.setProperty("editstatus", "1");
			   tosave.add(asset);
           }
		}
		else
		{
			removed++;
		}
		if( tosave.size() == 100 )
		{
			log.info("removed " + removed + " existed " + existed);
			archive.saveAssets(tosave);
			tosave.clear();
		}
	}
	archive.saveAssets(tosave);
	tosave.clear();
	log.info("removed " + removed + " existed " + existed);
	
}


init();
