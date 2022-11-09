package asset

import org.entermediadb.asset.Asset
import org.entermediadb.asset.Category
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.util.TimeParser
import org.openedit.Data
import org.openedit.data.QueryBuilder
import org.openedit.hittracker.HitTracker
import org.openedit.page.Page
import org.openedit.page.manage.PageManager







public void init()
{
	MediaArchive mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");
	PageManager pageManager = mediaarchive.getPageManager();
	//Search assets not tagged and importstatus complete
	QueryBuilder query = mediaarchive.getAssetSearcher().query().all();

	Collection cats = new HashSet();
	Collection found = mediaarchive.query("librarycollection").exact("id","AYOPB7957ORpJkikdf1M").search();
	for(Data col in found)
	{
		Category cat = mediaarchive.getCategory(col.get("rootcategory"));
		if( cat != null)
		{
			cats.add(cat);
		}
	}
	query.orgroup("category", cats);
	
	HitTracker hits = query.search();
	if (hits.size() > 1) {
		log.info(hits.size()+" assets to be copied:"  + query);
		Integer assetcount = 0;
		
		List tosave = new ArrayList();
	//	hits.each
	}
	
}	

public void archiveAssets(Data retentionpolicy, Collection assets)
{
	assets.each
	{
		boolean complete = false;
		Asset asset = mediaarchive.getAssetSearcher().loadData(it);
		if( Boolean.valueOf( retentionpolicy.get("deletegenerated") ) )
		{
			asset.setValue("retentionstatus","deletegenerated");
			mediaarchive.removeGeneratedImages(asset,true);
			complete = true;
		}
		if( Boolean.valueOf( retentionpolicy.get("deleteoriginal") ) )
		{
			asset.setValue("retentionstatus","deleteoriginal");
			mediaarchive.removeOriginals(asset);
			complete = true;
		}
		if( Boolean.valueOf( retentionpolicy.get("deleteasset") ) )
		{
			mediaarchive.deleteAsset(asset,false);
			return;
		}
		 
		if( !complete && !it.archivesourcepath  ) //Archive it
		{
			complete = true;
			Page fullpath = pageManager.getPage("/WEB-INF/data/" + mediaarchive.getCatalogId() + "/originals/" + asset.getSourcePath() );
			if(fullpath.exists()){
				String mask = retentionpolicy.get("archivepath");
				String newsourcepath = mediaarchive.getAssetImporter().getAssetUtilities().createSourcePathFromMask( mediaarchive, null, asset.getName(), mask, asset.getProperties());
				
				Page newpage = pageManager.getPage("/WEB-INF/data/" + mediaarchive.getCatalogId() + "/originals/" + newsourcepath);
				pageManager.movePage(fullpath,newpage);
				log.info("Archived asset to ${newpage.getContentItem().getAbsolutePath()}");
				asset.setFolder(fullpath.isFolder());
				asset.setValue("retentionstatus","archived");
				asset.setValue("archivesourcepath",newsourcepath);
			}
			else
			{
				log.info("could not move to archive: ${asset.getSourcePath()}");
				asset.setValue("retentionstatus","error");
			}
		}
		if( complete )
		{
			log.info("Applied retention policy ${retentionpolicy} on ${asset}");
			mediaarchive.getAssetSearcher().saveData(asset);
		}
	}
	
}

public boolean isEmpty( Page inParentFolder)
{
	boolean hasstuff = false;
	Collection children = pageManager.getChildrenPaths(inParentFolder.getPath(),true);
	for (Iterator iterator = children.iterator(); iterator.hasNext();)
	{
		String childfolder =  iterator.next();
		
		Page node = pageManager.getPage(childfolder);
		if( !node.isFolder() )
		{
			hasstuff = true;
		}
		else if(!isEmpty(node))
		{
			hasstuff = true;
		}
	}
	if( !hasstuff )
	{
		log.info("trim " + inParentFolder);
		pageManager.removePage(inParentFolder);
		return true;
	}
	return false;
}




init();
