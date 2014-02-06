import groovy.json.JsonSlurper

import org.entermedia.upload.FileUpload
import org.entermedia.upload.UploadRequest
import org.json.simple.JSONObject
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.scanner.AssetImporter
import org.openedit.entermedia.search.AssetSearcher
import org.openedit.util.DateStorageUtil
import org.openedit.entermedia.Category

import com.openedit.OpenEditException
import com.openedit.WebPageRequest
import com.openedit.page.Page
import com.openedit.util.OutputFiller

public void handleAssetRequest(){


	WebPageRequest inReq = context;

	JSONObject object = null;
	String method = inReq.getMethod();

	if(method == "POST"){
		object = handlePost();
	}
	if(method == "PUT"){
		object = handlePut();
	}
	if(method == "DELETE"){
		object = handleDelete();
	}

	if(object != null){
		
		try
		{
			OutputFiller filler = new OutputFiller();
			InputStream stream = new ByteArrayInputStream(object.toJSONString().getBytes("UTF-8"));
			
			//filler.setBufferSize(40000);
			//InputStream input = object.
			filler.fill(stream, inReq.getOutputStream());
		}
		finally
		{
//			stream.close();
//			inOut.getStream().close();
//			log.info("Document sent");
//			//archive.logDownload(filename, "success", inReq.getUser());
		}
		
		
	}
	//	jsondata = obj.toString();
	//
	//	log.info(jsondata);
	//	context.putPageValue("json", jsondata);


}

public JSONObject handlePost(){
	WebPageRequest inReq = context;
	SearcherManager sm = inReq.getPageValue("searcherManager");

	String catalogid =  findCatalogId(inReq);
	MediaArchive archive = getMediaArchive(catalogid);
	AssetSearcher searcher = sm.getSearcher(catalogid,"asset" );
	//We will need to handle this differently depending on whether or not this asset has a real file attached to it.
	//if it does, we should move it and use the asset importer to create it so metadata gets read, etc.

	FileUpload command = archive.getSearcherManager().getModuleManager().getBean("fileUpload");
	UploadRequest properties = command.parseArguments(context);

	JsonSlurper slurper = new JsonSlurper();
	def request = null;
	String content = context.getPageValue("jsondata");
	if(properties != null){

	}
	if(content != null){
		request = slurper.parseText(content); //NOTE:  This is for unit tests.
	} else{
		request = slurper.parse(inReq.getRequest().getReader()); //this is real, the other way is just for testing
	}
		
	AssetImporter importer = archive.getAssetImporter();
	HashMap keys = new HashMap();

	request.each{
		println it;
		String key = it.key;
		String value = it.value;
		keys.put(key, value);
		keys.put("formatteddate", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
	}
	String id = request.id;
	if(id == null){
		id = searcher.nextAssetNumber()
	}

	String sourcepath = keys.get("sourcepath");

	if(sourcepath == null){
		sourcepath = archive.getCatalogSettingValue("catalogassetupload");  //${division.uploadpath}/${user.userName}/${formateddate}
	}
	if(sourcepath.length() == 0){
		sourcepath = "receivedfiles/${id}";
	}
	sourcepath = sm.getValue(catalogid, sourcepath, keys);
	Asset asset = null;

	if(properties.getFirstItem() != null){
		String path = "/WEB-INF/data/" + archive.getCatalogId()	+ "/originals/" + sourcepath + "/${properties.getFirstItem().getName()}";
		properties.saveFileAs(properties.getFirstItem(), path, context.getUser());
		Page newfile = archive.getPageManager().getPage(path);
		asset = importer.createAssetFromPage(archive, context.getUser(), newfile);
	}


	if(asset == null && keys.get("fetchURL") != null){
		asset = importer.createAssetFromFetchUrl(archive, keys.get("fetchURL"), context.getUser(), sourcepath);
	}

	if(asset == null && keys.get("localPath") != null)
	{
		log.info("HERE!!!");
		File file = new File(keys.get("localPath"));
		if(file.exists())
		{
			String path = "/WEB-INF/data/" + archive.getCatalogId()	+ "/originals/" + sourcepath + "/${file.getName()}";
			Page newfile = archive.getPageManager().getPage(path);
			String realpath = newfile.getContentItem().getAbsolutePath();
			File target = new File(realpath);
			target.getParentFile().mkdirs();
			if(file.renameTo(realpath)){
				asset = importer.createAssetFromPage(archive, context.getUser(), newfile);
			} else{
				throw new OpenEditException("Error moving file: " + realpath);
			}
		}
	}
	if(asset == null){
		asset = new Asset();//Empty Record
		asset.setId(id);
	}

	request.each{
		println it;
		String key = it.key;
		String value = it.value;
		asset.setProperty(key, value);
	}
	
	// Handle Tags
	def tagsList = request.tags;
	String tags = ""
	tags.each{
		tags += it.toString() + " ";
	}
	asset.addKeywords(tags);
	
	// Handle Categories
	if(request.category != null){
		// Get Category ID
		String id = request.category.id;
		
		if(id == null){
			// Create a unique ID
			id = "new"; // TODO: Needs to be unique!
		}
		
		// Check to see if category exists
		Category cat = archive.getCategory(id)
		if(cat == null){
			// New Category
			// TODO: Not sure how to add new category
		}
		else {
			// Modify existing Category
			cat.setName(request.category.name);
			cat.setSourcePath(request.category.tree);
			// ...
			// TODO: save category
		}
		
		asset.addCategory(id);
	}
	
	asset.setProperty("sourcepath", sourcepath);
	searcher.saveData(asset, context.getUser());


	JSONObject result = getAssetJson(searcher, asset);
	jsondata = result.toString();

	context.putPageValue("json", jsondata);
	return result;
}


public JSONObject handlePut(){
	WebPageRequest inReq = context;

	SearcherManager sm = inReq.getPageValue("searcherManager");
	//	slurper.parse(inReq.getRequest().getReader()); //this is real, the other way is just for testing
	JsonSlurper slurper = new JsonSlurper();
	def request = null;
	String content = context.getPageValue("jsondata");
	if(properties != null){

	}
	if(content != null){
		request = slurper.parseText(content); //NOTE:  This is for unit tests.
	} else{
		request = slurper.parse(inReq.getRequest().getReader()); //this is real, the other way is just for testing
	}
	
	
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
	return result;
	
}


public JSONObject handleDelete(){
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
		String value = "";
		
		if(key == "keywords"){
			// Handle Parsing for special keys
			value = "[".concat(inAsset.get(it.id).replace("|", ",")).concat("]");
		}
		else{
			value = inAsset.get(it.id);
		}
		
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
