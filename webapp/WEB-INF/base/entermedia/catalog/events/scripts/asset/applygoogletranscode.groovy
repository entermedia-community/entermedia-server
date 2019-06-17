package asset

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.google.GoogleManager
import org.entermediadb.video.CloudTranscodeManager
import org.openedit.Data
import org.openedit.hittracker.HitTracker

import com.google.gson.JsonArray
import com.google.gson.JsonObject

public void runit()
{
	
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");

	
	
	HitTracker hits = mediaArchive.getAssetSearcher().query().match("googletranscoded", "false").orgroup("assettype", "video audio").match("importstatus", "complete").search();
	CloudTranscodeManager manager = mediaArchive.getBean( "cloudTranscodeManager");
	
	hits.each{
	
		String selectedlang = it.locale;
		if(selectedlang) {
			Asset asset = mediaArchive.getAsset(it.id);
			manager.addAutoTranscode(mediaArchive, selectedlang, asset, null);
			asset.setValue("googletranscoded", true);
			mediaArchive.saveAsset(asset);
		}
		
		
		
		
		
		
		
	}
	
	
	mediaArchive.fireSharedMediaEvent("asset/autotranscribe");
	
	
	
}

runit();

