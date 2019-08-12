package asset

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.google.GoogleManager
import org.openedit.Data
import org.openedit.data.QueryBuilder
import org.openedit.hittracker.HitTracker
import org.openedit.util.HttpSharedConnection

import com.google.gson.JsonArray
import com.google.gson.JsonObject

public void runit()
{
	
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");

	GoogleManager manager = mediaArchive.getBean("googleManager");

	HttpSharedConnection connection = new HttpSharedConnection();
		
	String googleapikey = mediaArchive.getCatalogSettingValue("googleapikey");
	if(googleapikey == null || googleapikey.isEmpty()) {
	//	log.info("Must specify google api key");
		return null;
	}
	
	//Search collections marked for processing
	QueryBuilder query = mediaArchive.getAssetSearcher().query().exact("googletagged", "false").exact("importstatus", "complete");

	String systemwidetagging = mediaArchive.getCatalogSettingValue("systemwidetagging");
	if( !Boolean.parseBoolean(systemwidetagging))
	{
		Collection cats = new HashSet();
		Collection found = mediaArchive.query("librarycollection").exact("automatictagging", true).search();
		for(Data col in found)
		{
			Category cat = mediaArchive.getCategory(col.get("rootcategory"));
			if( cat != null)
			{
				cats.add(cat);
			}
		}
		query.orgroup("category", cats);
	}	
	HitTracker hits = query.search();
	
	log.info(hits.size()+" assets to be tagged by Google. System wide:"  + query);
	Integer assetcount = 0;
	hits.each{
		Data hit = it;
		Asset asset = mediaArchive.getAsset(it.id);
		
		JsonObject object = manager.processImage(connection,asset);
		if(object == null) {
			return;
		}
		
		JsonObject responselist = object.getAsJsonArray("responses").get(0);
		JsonArray labels = responselist.getAsJsonArray("labelAnnotations");
		labels.each{
			String tag = it.description.getAsString();
			asset.addValue("googlekeywords", tag);
			asset.setValue("googletagged", "true");
		}
		JsonArray others = responselist.getAsJsonArray("localizedObjectAnnotations");
		others.each { 
			String tag = it.name.getAsString();
			asset.addValue("googlekeywords", tag);
			asset.setValue("googletagged", "true");
			
		}
		
		mediaArchive.saveAsset(asset);
		
		assetcount++;
		
	}
	log.info(assetcount+" assets tagged by Google.")
	
}

runit();

