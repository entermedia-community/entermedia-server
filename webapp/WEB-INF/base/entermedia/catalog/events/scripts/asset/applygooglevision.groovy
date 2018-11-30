package asset

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.google.GoogleManager
import org.openedit.Data
import org.openedit.hittracker.HitTracker

import com.google.gson.JsonArray
import com.google.gson.JsonObject

public void runit()
{
	
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");

	GoogleManager manager = mediaArchive.getBean("googleManager");
	
	String googleapikey = mediaArchive.getCatalogSettingValue("googleapikey");
	if(googleapikey == null || googleapikey.isEmpty()) {
		log.info("Must specify google api key");
		return null;
	}
	
	HitTracker hits = mediaArchive.getAssetSearcher().query().match("googletagged", "false").search();
	hits.each{
		Data hit = it;
		Asset asset = mediaArchive.getAsset(it.id);
		
		JsonObject object = manager.processImage(asset);
		if(object == null) {
			return;
		}
		
		JsonObject responselist = object.getAsJsonArray("responses").get(0);
		JsonArray labels = responselist.getAsJsonArray("labelAnnotations");
		labels.each{
			String tag = it.description.getAsString();
			asset.addValue("googlekeywords", tag);
		}
		JsonArray others = responselist.getAsJsonArray("localizedObjectAnnotations");
		others.each { 
			String tag = it.name.getAsString();
			asset.addValue("googlekeywords", tag);
			
		}
		
		mediaArchive.saveAsset(asset);
		
		
	}
	
}

runit();

