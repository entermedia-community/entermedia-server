package importing

import org.apache.commons.codec.digest.DigestUtils
import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.scanner.PresetCreator;
import org.entermediadb.asset.search.AssetSearcher
import org.openedit.Data
import org.openedit.OpenEditException
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker
import org.openedit.hittracker.SearchQuery
import org.openedit.page.Page
import org.openedit.page.manage.PageManager
import org.openedit.util.FileUtils

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
	assets.enableBulkOperations();
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
			   String currentmd5 = asset.get("md5hex");
			   if(currentmd5 != null){
				   String md5;
				   Page content = pageManager.getPage(pathToOriginal)
				   InputStream inputStream = content.getInputStream();
   
				   try
				   {
					   md5 = DigestUtils.md5Hex(inputStream);
				   }
				   catch (Exception e)
				   {
					   throw new OpenEditException(e);
				   }
				   finally
				   {
					   FileUtils.safeClose(inputStream);
				   }
				   if(!md5.equals(currentmd5)){
					   //This is a different file!
					   archive.removeGeneratedImages(asset, true);
					 	archive.getAssetSearcher().delete(asset, null);
						 archive.getAssetImporter().createAssetFromPage(archive, null, content);
					   continue;
				   }
			   }
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
