package asset

import java.util.Iterator

import org.entermediadb.asset.Asset
import org.entermediadb.asset.Category
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.util.TimeParser
import org.openedit.Data
import org.openedit.page.Page
import org.openedit.page.manage.PageManager

import com.google.common.primitives.Booleans.BooleanArrayAsList


MediaArchive mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");
PageManager pageManager = mediaarchive.getPageManager();



public void checkRules()
{
	Collection policies = mediaarchive.getList("retentionrules");
	log.info("Checking assets rules " + policies.size() );
	policies.each 
	{
		Data retentionpolicy = it;
		TimeParser parser = new TimeParser();
		long daystokeep = parser.parse(it.expirationperiod);
		Date target = new Date(System.currentTimeMillis() -  daystokeep);
		def nots = ["deletegenerated","deleteoriginal"];
		
		Collection assets = mediaarchive.getAssetSearcher().query().match("importstatus","complete").exact("retentionpolicy",it.id).notgroup("retentionstatus",nots).before("assetaddeddate", target).search();
		log.info("Found ${assets.size()} for retention policy ${it} ${assets.query}");
		archiveAssets(retentionpolicy, assets);
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
			complete = true;
		}
		else if( !it.archivesourcepath  )
		{
			Page fullpath = pageManager.getPage("/WEB-INF/data/" + mediaarchive.getCatalogId() + "/originals/" + asset.getSourcePath() );
			if(fullpath.exists()){
				String mask = retentionpolicy.get("archivepath");
				String newsourcepath = mediaarchive.getAssetImporter().getAssetUtilities().createSourcePathFromMask( mediaarchive, null, asset.getName(), mask, asset.getProperties());
				
				Page newpage = pageManager.getPage("/WEB-INF/data/" + mediaarchive.getCatalogId() + "/originals/" + newsourcepath);
				pageManager.movePage(fullpath,newpage);
				log.info("Archived asset to ${newpage.getContentItem().getAbsolutePath()}");
				asset.setFolder(fullpath.isFolder());
				asset.setValue("archivesourcepath",newsourcepath);
				asset.setValue("retentionstatus","archived");
				mediaarchive.getAssetSearcher().saveData(asset);
				complete = true;
			}
			else
			{
				log.info("Original did not exist to archive: ${asset.getSourcePath()}");
			}
		}
		if( complete )
		{
			log.info("Applied retention policy ${retentionpolicy} on ${asset}");
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



public init()
{
	String assetids = context.getRequestParameter("assetids");
	if( assetids == null)
	{
		checkRules();
	}
	else
	{		
		//These files are being MOVED so the conversions will still work
		def nots = ["deletegenerated","deleteoriginal"];
		
		Collection hits = mediaarchive.getAssetSearcher().query().orgroup("id",assetids).notgroup("retentionstatus",nots).search();
		List assets = new ArrayList();
		hits.each
		{
			Asset asset = mediaarchive.getAssetSearcher().loadData(it);
			String policy = asset.get("retentionpolicy");
			if( policy != null)
			{
				Data retentionpolicy = mediaarchive.getData("retentionrules",policy);
				TimeParser parser = new TimeParser();
				long daystokeep = parser.parse(retentionpolicy.expirationperiod);
				Date cutoff = new Date(System.currentTimeMillis() -  daystokeep );
				Date uploaddate = asset.getDate("assetaddeddate");
				if( daystokeep == 0 || uploaddate.before(cutoff) )
				{
					assets.add(asset);	
					archiveAssets(retentionpolicy, assets);
					assets.clear();
				}
			}
		}
	}
	
	//look auto archive folders and clean them up
	Collection defaultcats = mediaarchive.getSearcher("categorydefaultdata").query().match("fieldname","retentionpolicy").search();
	for( Data row : defaultcats)
	{
		Category root = mediaarchive.getCategory(row.get("categoryid"));
		Page mountedpath = pageManager.getPage("/WEB-INF/data/" + mediaarchive.getCatalogId() + "/originals/" + root.getCategoryPath());
		log.info("checking for empty folders: " + mountedpath.getPath());
		
		isEmpty(mountedpath);
	}
}

init();
