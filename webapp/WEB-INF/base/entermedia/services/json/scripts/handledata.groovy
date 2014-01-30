import groovy.json.JsonSlurper

import org.json.simple.JSONObject
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.search.AssetSearcher

import com.openedit.OpenEditException
import com.openedit.WebPageRequest

public void handleAssetRequest(){


	WebPageRequest inReq = context;


	String method = inReq.getMethod();

	if(method == "POST"){
		handlePost();
	}
	if(method == "PUT"){
		handlePut();
	}
	if(method == "DELETE"){
		handleDelete();
	}

	//	jsondata = obj.toString();
	//
	//	log.info(jsondata);
	//	context.putPageValue("json", jsondata);


}

public void handlePost(){
	JsonSlurper slurper = new JsonSlurper();
	WebPageRequest inReq = context;

	SearcherManager sm = inReq.getPageValue("searcherManager");
	def request = null;
	String content = inReq.getPageValue("jsondata");
	if(content != null){
		request = slurper.parseText(content); //NOTE:  This is for unit tests.
	} else{
		request = slurper.parse(inReq.getRequest().getReader()); //this is real, the other way is just for testing
	}
	String catalogid =  findCatalogId(inReq);
	MediaArchive archive = getMediaArchive(catalogid);
	String searchtype = findSearchType(inReq);
	Searcher searcher = sm.getSearcher(catalogid,searchtype);
	//We will need to handle this differently depending on whether or not this asset has a real file attached to it.
	//if it does, we should move it and use the asset importer to create it so metadata gets read, etc.

	Data data = searcher.createNewData();
	String id = request.id;
	if(id != null){
		data.setId(id);

	}else{

		data.setId(searcher.nextId());
	}
	request.each{
		println it;
		String key = it.key;
		String value = it.value;
		data.setProperty(key, value);
	}
	
	searcher.saveData(data, context.getUser());
	JSONObject result = getAssetJson(searcher, data);
	jsondata = result.toString();

	context.putPageValue("json", jsondata);

}


public void handlePut(){
	JsonSlurper slurper = new JsonSlurper();
	WebPageRequest inReq = context;

	SearcherManager sm = inReq.getPageValue("searcherManager");
	//	slurper.parse(inReq.getRequest().getReader()); //this is real, the other way is just for testing
	String content = inReq.getPageValue("jsondata");
	def request = slurper.parseText(content);
	String catalogid = request.catalogid;
	MediaArchive archive = getMediaArchive(catalogid);
	AssetSearcher searcher = sm.getSearcher(catalogid,"asset" );
	//We will need to handle this differently depending on whether or not this asset has a real file attached to it.
	//if it does, we should move it and use the asset importer to create it so metadata gets read, etc.
	String id = getId(inReq);




	Asset asset = archive.getAsset(id);

	if(asset == null){
		throw new OpenEditException("Asset was not found!");
	}

	request.each{
		println it;
		String key = it.key;
		String value = it.value;
		asset.setProperty(key, value);
	}

	searcher.saveData(asset, context.getUser());
	JSONObject result = getAssetJson(searcher, asset);
	jsondata = result.toString();

	context.putPageValue("json", jsondata);

}


public void handleDelete(){
	JsonSlurper slurper = new JsonSlurper();
	WebPageRequest inReq = context;

	SearcherManager sm = inReq.getPageValue("searcherManager");
	String catalogid = findCatalogId();

	MediaArchive archive = getMediaArchive(catalogid);
	AssetSearcher searcher = sm.getSearcher(catalogid,"asset" );
	//We will need to handle this differently depending on whether or not this asset has a real file attached to it.
	//if it does, we should move it and use the asset importer to create it so metadata gets read, etc.
	String id = getId(inReq);




	Asset asset = archive.getAsset(id);

	if(asset != null){
		searcher.delete(asset, null);
	}


}


handleAssetRequest();


public MediaArchive getMediaArchive(String inCatalogid)
{
	SearcherManager sm = context.getPageValue("searcherManager");

	if (inCatalogid == null)
	{
		return null;
	}
	MediaArchive archive = (MediaArchive) sm.getModuleManager().getBean(inCatalogid, "mediaArchive");
	return archive;
}

public JSONObject getAssetJson(Searcher inSearcher, Data inAsset){

	JSONObject asset = new JSONObject();
	inSearcher.getPropertyDetails().each{
		String key = it.id;
		String value=inAsset.get(it.id);
		if(key && value){
			asset.put(key, value);
		}
	}
	//need to add tags and categories, etc



	return asset;
}


public String getId(WebPageRequest inReq){
	String root  = "/entermedia/services/json/asset/";
	String url = inReq.getPath();
	if(!url.endsWith("/")){
		url = url + "/";
	}
	id = url.substring(root.length(), url.length())
	id = id.substring(0, id.indexOf("/"));
	return id;
}



public String findSearchType(WebPageRequest inReq){
	String root  = "/entermedia/services/json/data/";
	String url = inReq.getPath();
	if(!url.endsWith("/")){
		url = url + "/";
	}
	String id = url.substring(root.length(), url.length())
	id = id.substring(0, id.indexOf("/"));
	return id;
}



public String findCatalogId(WebPageRequest inReq){
	String catalogid = context.findValue("catalogid");
	if(catalogid == null){
		if(context.getRequest()){
			catalogid = context.getRequest().getHeader("catalogid");
		}
	}
	return catalogid;
}