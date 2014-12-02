import groovy.json.JsonSlurper

import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager

import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker

public void handleSearch(){


	WebPageRequest inReq = context;
	JsonSlurper slurper = new JsonSlurper();
	SearcherManager sm = inReq.getPageValue("searcherManager");
	//	slurper.parse(inReq.getRequest().getReader()); //this is real, the other way is just for testing
	String content = inReq.getPageValue("jsondata");
	def request = slurper.parseText(content);
	String catalogid = request.query.catalogid;
	String searchtype = request.query.searchtype;
	Searcher searcher = sm.getSearcher(catalogid,searchtype )
	HitTracker hits = searcher.getAllHits();

	JSONArray list1 = new JSONArray();
	
				  
	JSONObject obj = new JSONObject();
	obj.put("size", hits.size());
	hits.each{ Data data ->
		JSONObject asset = new JSONObject();
		searcher.getPropertyDetails().each{
			String key = it.id;
			String value=data.get(it.id);
			if(key && value){
				asset.put(key, value);
			}
		}
		list1.add(asset);
		
	}
	  
	obj.put("assets", list1);
	
				  
	jsondata = obj.toString();

	log.info(jsondata);
	context.putPageValue("json", jsondata);


}

handleSearch();
