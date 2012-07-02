package importing;
import java.util.*;

import org.openedit.Data;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.search.AssetSearcher;

import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.page.Page;
import com.openedit.page.manage.*;

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
	String sourcepath = context.getRequestParameter("sourcepath");
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
	int existed = 0;
	for(String path: sourcepaths)
	{
		Asset asset = archive.getAssetBySourcePath(path);
		if( asset == null)
		{
			log.info("invalid asset " + path);
			continue;
		}
		String assetsource = asset.getSourcePath();
		String pathToOriginal = "/WEB-INF/data" + archive.getCatalogHome() + "/originals/" + assetsource;
		if(asset.isFolder() && asset.getPrimaryFile() != null)
		{
			pathToOriginal = pathToOriginal + "/" + asset.getPrimaryFile();
		}
		Page page = pageManager.getPage(pathToOriginal);
		if(!page.exists())
		{
			removed.add(asset.getSourcePath());
			archive.removeGeneratedImages(asset);
			archive.getAssetSearcher().delete(asset, user);
		}
		else
		{
			existed++;
		}
	}
	log.info("removed " + removed.size() + " found " + existed);
	
}


init();
