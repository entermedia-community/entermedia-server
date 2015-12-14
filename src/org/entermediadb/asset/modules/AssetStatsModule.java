package org.entermediadb.asset.modules;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.WebPageRequest;

public class AssetStatsModule extends BaseMediaModule
{

	public void addPreViewEvent(WebPageRequest inReq) throws Exception
	{
		//save to logger
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = (Asset)inReq.getPageValue("asset");
		if(asset != null){
			archive.getAssetStatsManager().logAssetPreview(archive,asset, inReq.getUser());
		}
	}
//	public void updateAssetStats(WebPageRequest inReq) throws Exception
//	{
//		WebEvent inEvent = (WebEvent)inReq.getPageValue("webevent");
//		if ( inEvent != null)
//		{
//			MediaArchive archive = getMediaArchive(inEvent.getCatalogId());
//			Asset asset = archive.getAssetBySourcePath(inEvent.getSourcePath());
//			//archive.getAssetStatsManager().getViewsForAsset(asset); //should already be done
//			archive.getAssetSearcher().updateIndex(asset);
//		}
//	}	
}
