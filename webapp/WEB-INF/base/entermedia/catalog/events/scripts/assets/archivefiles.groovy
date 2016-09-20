package assets

import org.entermediadb.asset.MediaArchive
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.hittracker.SearchQuery

public void init()
{

	Collection policies = mediaarchive.getList("retentionrules");
	
	
	policies.each 
	{
		Map vals = new HashMap();
		Data retentionpolicy = it;
		Collection assets = mediaarchive.getAssetSearcher().query().exact("retentionpolicy",it.id).not("archivesourcepath","*").search();
		assets.each
		{
			Asset asset = mediaarchive.getAssetSearcher().loadData(it);
			Page fullpath = mediaarchive.getOriginalFileManager().getOriginalDocument(inAsset);
			
			String mask = retentionpolicy.get("archivepath");
			String newsourcepath = mediaarchive.getAssetImporter().getAssetUtilities().createSourcePathFromMask(context, mediaarchive, asset.getName(), mask, vals);
			
			Page newpage = pageManager.getPage("/WEB-INF/data/" + mediaarchive.getCatalogId() + "/originals/" + newsourcepath + "/");
			pageManager.movePage(fullpath,newpage);
			asset.setValue("archivesourcepath",newsourcepath);
			mediaarchive.getAssetSearcher().saveData(asset);
		}
	}
}	
	
init();