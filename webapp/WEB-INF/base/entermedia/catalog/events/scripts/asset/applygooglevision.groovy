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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

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
	
	//Search assets not tagged and importstatus complete
	QueryBuilder query = mediaArchive.getAssetSearcher().query().exact("googletagged", "false").exact("importstatus", "complete");

	//System Wide Enabled 
	String systemwidetagging = mediaArchive.getCatalogSettingValue("systemwidetagging");
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
			log.info("No collections are marked as automatic Google Tagging");
			return;
		}
		query.orgroup("category", cats);
	}
	
	//Google vision date filter
	String googlevisionstartdate = mediaArchive.getCatalogSettingValue("google_api_start_date");
	if (googlevisionstartdate == null)
	{
		googlevisionstartdate = "01/01/2020";
	}
	DateFormat format = new SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH);
	Date date = format.parse(googlevisionstartdate);
	query.after("assetaddeddate",date);
	
	HitTracker hits = query.search();
	if (hits.size() > 1) {
		log.info(hits.size()+" assets to be tagged by Google. System wide:"  + query);
		Integer assetcount = 0;
		
		List tosave = new ArrayList();
		hits.each
		{
			Data hit = it;
			Asset asset = mediaArchive.getAsset(it.id);
			
			//JsonElement jelement = new JsonParser(manager.processImage(connection,asset));
			JSONObject object = manager.processImage(connection,asset);
			//JsonArray object = (JsonArray) manager.processImage(connection,asset);
			if(object == null) {
				return;
			}
			//log.info(object);
			JSONObject responselist = (JSONObject) object.get("responses");
			JSONArray labels = (JSONArray) responselist.get("labelAnnotations");
			labels.each{
				String tag = it.description.toString();
				asset.addValue("googlekeywords", tag);
				asset.setValue("googletagged", "true");
			}
			JSONArray others = (JSONArray) responselist.get("localizedObjectAnnotations");
			others.each { 
				String tag = it.name.toString();
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
}

runit();

