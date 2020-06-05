package asset

import java.text.DateFormat
import java.text.SimpleDateFormat

import org.entermediadb.asset.Asset
import org.entermediadb.asset.Category
import org.entermediadb.asset.MediaArchive
import org.entermediadb.google.GoogleManager
import org.entermediadb.net.HttpSharedConnection
import org.openedit.Data
import org.openedit.data.QueryBuilder
import org.openedit.hittracker.HitTracker

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
	/*Google vision date filter*/
	String googlevisionstartdate = mediaArchive.getCatalogSettingValue("google_api_start_date");
	if (googlevisionstartdate == null)
	{
		googlevisionstartdate = "01/01/2000";
	}
	DateFormat format = new SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH);
	Date date = format.parse(googlevisionstartdate);
	
	if( !Boolean.parseBoolean(systemwidetagging))
	{
		Collection cats = new HashSet();
		Collection found = mediaArchive.query("librarycollection").exact("automatictagging", "true").search();
		for(Data col in found)
		{
			Category cat = mediaArchive.getCategory(col.get("rootcategory"));
			if( cat != null)
			{
				cats.add(cat);
			}
		}
		if(cats.isEmpty())
		{
			log.info("No collections are marked as automatic");
			return;
		}
		query.orgroup("category", cats).after("assetaddeddate",date);
	}	
	HitTracker hits = query.search();
	
	log.info(hits.size()+" assets to be tagged by Google. System wide:"  + query);
	Integer assetcount = 0;
	
	List tosave = new ArrayList();
	hits.each
	{
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
		/*
		others = responselist.getAsJsonArray("landmarkAnnotations");
		"locations": [
			{
			  "latLng": {
				"latitude": 55.752912,
				"longitude": 37.622315883636475
			  }
			}
		  ]
		others.each {
			String tag = it.name.getAsString();
			asset.addValue("googlekeywords", tag);
			asset.setValue("googletagged", "true");
		}
		*/
		tosave.add(asset);
		
		assetcount++;
		if( tosave.size() == 200)
		{
			mediaArchive.saveAssets(tosave);
			tosave.clear();
			log.info(assetcount+" assets tagged by Google.");
		}
	}
	mediaArchive.saveAssets(tosave);
	log.info(assetcount+" assets tagged by Google.")
	
}

runit();

