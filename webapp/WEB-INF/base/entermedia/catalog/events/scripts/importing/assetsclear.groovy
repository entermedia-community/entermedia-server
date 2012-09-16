package importing;
import java.util.*;

import org.openedit.Data;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.search.AssetSearcher;
import org.openedit.repository.ContentItem;

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
		q = searcher.createSearchQuery().append("category", "index");
		q.addNot("editstatus","7");
	}
	else
	{
		q.addStartsWith("sourcepath", sourcepath);
	}
	assets = searcher.search(q);
	assets.setHitsPerPage(10000);
	int removed = 0;
	List tosave = new ArrayList();
	for(Object obj: assets)
	{
		Data hit = (Data)obj;
		int existed = 0;
		String path = hit.getSourcePath();
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
		ContentItem page = pageManager.getRepository().get(pathToOriginal);
		if(!page.exists())
		{
			removed++;
			//archive.removeGeneratedImages(asset);
			asset.setProperty("editstatus", "7");
			tosave.add(asset);
		}
		else
		{
			existed++;
		}
		if( tosave.size() == 100 )
		{
			log.info("removed " + removed + " found " + existed);
			archive.saveAssets(tosave);
			tosave.clear();
		}
	}
	archive.saveAssets(tosave);
	tosave.clear();
	log.info("removed " + removed + " found " + existed);
	
}


init();
