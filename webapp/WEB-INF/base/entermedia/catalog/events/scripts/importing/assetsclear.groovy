package importing

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.scanner.HotFolderManager
import org.entermediadb.asset.search.AssetSearcher
import org.openedit.Data
import org.openedit.data.QueryBuilder
import org.openedit.hittracker.HitTracker
import org.openedit.page.manage.PageManager
import org.openedit.repository.ContentItem

public void init()
{
	MediaArchive archive = context.getPageValue("mediaarchive");//Search for all files looking for videos
	
	
	//Make sure all the enabled hot folders are connected to some assets
	HotFolderManager manager = (HotFolderManager)archive.getModuleManager().getBean("hotFolderManager");
	PageManager pageManager = archive.getPageManager();
	
	Collection hits = manager.loadFolders( archive.getCatalogId() );
	for(Iterator iterator = hits.iterator(); iterator.hasNext();)
	{
		Data folder = (Data)iterator.next();
		
		Object enabled = folder.getValue("enabled");
		if( enabled != null && "false".equals( enabled.toString() ) )
		{
			continue;
		}
		
		String base = "/WEB-INF/data/" + archive.getCatalogId() + "/originals";
		String name = folder.get("subfolder");
		String path = base + "/" + name ;
		List paths = pageManager.getChildrenPaths(path);
		if( paths.size() == 0)
		{
			log.error("Found hot folder with no files, canceled delete request " + path);
			return;
		}
	}
	
	List children = pageManager.getChildrenPaths("/WEB-INF/data/" + archive.getCatalogId() + "/originals");
	if(children.size() == 0 )
	{
		log.info("No originals found. Skipping clear");
		return;	
	}

	AssetSearcher searcher = archive.getAssetSearcher();
	QueryBuilder q = searcher.query(); 
	HitTracker assets = null;
	String sourcepath = context.getRequestParameter("sourcepath");
	if(sourcepath == null)
	{
		q.all();
	}
	else
	{
		q.startsWith("sourcepath", sourcepath);
	}
	q.not("editstatus","7").sort("sourcepathUp");
	assets = q.search();
	assets.enableBulkOperations();
	int removed = 0;
	int existed = 0;	
	int modified = 0;
	List tosave = new ArrayList();
	for(Object obj: assets)
	{
		Data hit = (Data)obj;
	
		Asset asset = searcher.loadData(hit);
		ContentItem item = archive.getOriginalContent(asset);
		boolean saveit = false;
		//log.info(item.getPath());
		if(!item.exists() )
		{
			removed++;
		    asset.setProperty("editstatus", "7"); //mark as deleted
		    saveit = true;
		}
		else
		{
			existed++;
            if("7".equals(asset.get("editstatus")))
            {
			   asset.setProperty("editstatus", "6"); //restore files
			   saveit = true;
            }
			//TODO: Should we have locked the asset?
			if( !asset.isEquals(item.getLastModified()))
			{
				archive.getAssetImporter().reImportAsset(archive,asset); //this saves it
				modified++;
			}
		}
		if( saveit )
		{
			tosave.add(asset);
			if( tosave.size() == 100 )
			{
				log.info("found modified: " + modified + " found deleted: " + removed + " found unmodified:" + existed );
				archive.saveAssets(tosave);
				tosave.clear();
			}
		}	
	}
	archive.saveAssets(tosave);
	tosave.clear();
	log.info("found modified: " + modified + " found deleted: " + removed + " found unmodified:" + existed );
	if( modified > 0 )
	{
		archive.fireSharedMediaEvent("conversions/runconversions");
	}
}


init();
