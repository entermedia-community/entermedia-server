import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.RelatedAsset
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.hittracker.SearchQuery

public void init()
{
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");//Search for all files looking for videos
	Searcher targetsearcher = mediaArchive.getAssetSearcher();
	Searcher relatedassetsearcher = mediaArchive.getSearcherManager().getSearcher(mediaArchive.getCatalogId(), "relatedasset");
	
	SearchQuery q = targetsearcher.createSearchQuery();
	String ids = context.getRequestParameter("assetids");
	if( ids != null)
	{
		q.setAndTogether(false);
		String[] assetids = ids.split(",");
		for (int i = 0; i < assetids.length; i++)
		{
			q.addMatches("id",assetids[i]);
		}
	}
	else
	{
		q.addMatches("category", "index");
	}

	HitTracker assets = targetsearcher.search(q);
	assets.setHitsName("hits");
	context.setRequestParameter("hitssessionid", assets.getSessionId());
	context.putSessionValue(assets.getSessionId(), assets);
	for(Data hit in assets){
		Asset target = mediaArchive.getAsset (hit.getId());
		if(target.getRelatedAssets() != null){
			for(RelatedAsset related in target.getRelatedAssets()){
				Data newline = relatedassetsearcher.createNewData();
				newline.setProperty "targetid", related.getId();
				newline.setProperty "targetcatalogid", related.getRelatedToCatalogId();
				newline.setProperty "assetid", related.getAssetId();
				newline.setProperty "type", related.getType();
				newline.setSourcePath target.getSourcePath();
				relatedassetsearcher.saveData newline, null;
			}
		} 
	}
	log.info("assets converted");
	
}

init();
