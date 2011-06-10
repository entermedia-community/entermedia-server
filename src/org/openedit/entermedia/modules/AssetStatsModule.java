package org.openedit.entermedia.modules;

import org.openedit.data.Searcher;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.event.WebEvent;
import org.openedit.event.WebEventHandler;
import org.openedit.event.WebEventListener;

import com.openedit.WebPageRequest;

public class AssetStatsModule extends BaseMediaModule
{

	public void addPreViewEvent(WebPageRequest inReq) throws Exception
	{
		//save to logger
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = (Asset)inReq.getPageValue("asset");
		if(asset != null){
			archive.getAssetStatsManager().logAssetPreview(asset.getCatalogId(), asset.getSourcePath(),asset.getId(), inReq.getUser());
		}
	}
	public void updateAssetStats(WebPageRequest inReq) throws Exception
	{
		WebEvent inEvent = (WebEvent)inReq.getPageValue("webevent");
		if ( inEvent != null)
		{
			MediaArchive archive = getMediaArchive(inEvent.getCatalogId());
			Asset asset = archive.getAssetBySourcePath(inEvent.getSourcePath());
			//archive.getAssetStatsManager().getViewsForAsset(asset); //should already be done
			archive.getAssetSearcher().updateIndex(asset);
		}
	}	
}
