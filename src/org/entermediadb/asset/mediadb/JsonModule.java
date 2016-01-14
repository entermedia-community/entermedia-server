package org.entermediadb.asset.mediadb;


import groovy.json.JsonSlurper;

import java.awt.Dimension;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.convert.ConversionUtil;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.asset.orders.Order;
import org.entermediadb.asset.orders.OrderManager;
import org.entermediadb.asset.orders.OrderSearcher;
import org.entermediadb.asset.scanner.AssetImporter;
import org.entermediadb.asset.search.AssetSearcher;
import org.entermediadb.asset.upload.FileUpload;
import org.entermediadb.asset.upload.UploadRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.PropertyDetail;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.page.Page;
import org.openedit.util.OutputFiller;




public class JsonModule extends BaseMediaModule 
{
	private static final Log log = LogFactory.getLog(JsonModule.class);


	public void handleAssetRequest(WebPageRequest inReq)
	{
		inReq.getResponse().setHeader("Access-Control-Allow-Origin","*");
		
		JSONObject object = null;
		String method = inReq.getMethod();
		if(method.equals("POST")){
			object = handleAssetPost(inReq);
		}
		if(method == "PUT"){
			object = handleAssetPut(inReq);
		}
		if(method.equals("DELETE")){
			handleAssetDelete(inReq);
	}

		if(method == "GET"){
			object = handleAssetGet(inReq);
		}


		if(object != null){
			inReq.putPageValue("json", object.toString());
			try {
				OutputFiller filler = new OutputFiller();
				InputStream stream = new ByteArrayInputStream(object.toJSONString().getBytes("UTF-8"));
				if(inReq.getResponse() != null){
					inReq.getResponse().setContentType("application/json");
				}
				//filler.setBufferSize(40000);
				//InputStream input = object.
				filler.fill(stream, inReq.getOutputStream());
			} catch (Throwable ex) {
				
				throw new OpenEditException(ex);
				
			}
			finally {
				//			stream.close();
				//			inOut.getStream().close();
				//			log.info("Document sent");
				//			//archive.logDownload(filename, "success", inReq.getUser());
			}
		}
		//	jsondata = obj.toString();
		//
		//	log.info(jsondata);
		//	inReq.putPageValue("json", jsondata);

	}


//	public JSONObject handleAssetSearch(WebPageRequest inReq){
//		//Could probably handle this generically, but I think they want tags, keywords etc.
//		inReq.getResponse().setHeader("Access-Control-Allow-Origin","*");
//
//		String catalogid =  findCatalogId(inReq);
//		MediaArchive archive = getMediaArchive(inReq, catalogid);
//		AssetSearcher searcher = (AssetSearcher) archive.getSearcherManager().getSearcher(catalogid, "asset");
//		JsonSlurper slurper = new JsonSlurper();
//		JSONObject request = null;
//		String content = (String) inReq.getPageValue("jsondata");
//		if(content != null){
//			request = (JSONObject) slurper.parseText(content); //NOTE:  This is for unit tests.
//		} else{
//			request = (JSONObject) slurper.parse(inReq.getRequest().getReader()); //this is real, the other way is just for testing
//		}
//
//
//		ArrayList <String> fields = new ArrayList();
//		ArrayList <String> operations = new ArrayList();
//
//		for (Iterator iterator = request.iterator(); iterator.hasNext();) {
//			String resultstring = (String) iterator.next();
//			fields.add(resultstring.get("field"));
//			operations.add(resultstring.get("operator").toLowerCase());
//			StringBuffer values = new StringBuffer();
//
//			for (int i = 0; i < it.length; i++) {
//				it.values.each{
//					values.append(it);
//					values.append(" ");
//				}
//				inReq.setRequestParameter(it.field + ".value", values.toString());
//			}
//		}
//
//		// log.info("field" + fields);
//		// log.info("operations: " + operations);
//		String[] fieldarray = fields.toArray(new String[fields.size()]);
//		String[] opsarray = operations.toArray(new String[operations.size()]);
//
//		inReq.setRequestParameter("field", fieldarray);
//		inReq.setRequestParameter("operation", opsarray);
//
//		SearchQuery query = searcher.addStandardSearchTerms(inReq);
//		//log.info("Query was: " + query);
//		HitTracker hits = searcher.cachedSearch(inReq, query);
//		//log.info(hits.size());
//		JSONObject parent = new JSONObject();
//		parent.put("size", hits.size());
//
//		JSONArray array = new JSONArray();
//		
//		for (Iterator iterator = hits.getPageOfHits().iterator(); iterator.hasNext();) {
//		
//			JSONObject hit = getAssetJson(archive.getSearcherManager(),searcher, (Asset) iterator.next());
//
//			array.add(hit);
//		}
//
//
//		parent.put("results", array);
//		inReq.putPageValue("json", parent.toString());
////		try {
////			OutputFiller filler = new OutputFiller();
////			InputStream stream = new ByteArrayInputStream(parent.toJSONString().getBytes("UTF-8"));
////
////			//filler.setBufferSize(40000);
////			//InputStream input = object.
////			filler.fill(stream, inReq.getOutputStream());
////		} catch(Exception e){
////			throw new OpenEditException(e);
////		}
//		return parent;
//	}


	//Not used anymore?
//	public JSONObject handleAssetPost(WebPageRequest inReq)
//	{
//		inReq.getResponse().setHeader("Access-Control-Allow-Origin","*");
//	
//		String catalogid =  findCatalogId(inReq);
//		MediaArchive archive = getMediaArchive(inReq, catalogid);
//		AssetSearcher searcher = (AssetSearcher) archive.getSearcherManager().getSearcher(catalogid,"asset" );
//		//We will need to handle this differently depending on whether or not this asset has a real file attached to it.
//		//if it does, we should move it and use the asset importer to create it so metadata gets read, etc.
//
//		FileUpload command = (FileUpload) archive.getSearcherManager().getModuleManager().getBean("fileUpload");
//		UploadRequest properties = command.parseArguments(inReq);
//
//		JsonSlurper slurper = new JsonSlurper();
//		Data request = null;
//		String content = (String) inReq.getPageValue("jsondata");
//		if(properties != null){
//
//		}
//		if(content != null){
//			request = (Data) slurper.parseText(content); //NOTE:  This is for unit tests.
//		} else{
//			request = (Data) slurper.parse(inReq.getRequest().getReader()); //this is real, the other way is just for testing
//		}
//
//		AssetImporter importer = archive.getAssetImporter();
//		HashMap keys = new HashMap();
//
//		for (Iterator iterator4 = request.iterator(); iterator4
//				.hasNext();) {
//			Data content2 = (Data) iterator4.next();
//			String key = content2.get("key");
//			String value = content2.get("value");
//			keys.put(key,  value);
//			SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
//			String df = format.format(new Date());
//			keys.put("formatteddate", df);
//		}
//		
//		String id = request.get("id");
//		if(id == null){
//			id = searcher.nextAssetNumber();
//		}
//		keys.put("id", id);
//		String sourcepath = (String) keys.get("sourcepath");
//
//		if(sourcepath == null){
//			sourcepath = archive.getCatalogSettingValue("catalogassetupload");  //${division.uploadpath}/${user.userName}/${formateddate}
//		}
//		if(sourcepath == null || sourcepath.length() == 0){
//			sourcepath = "receivedfiles/${id}";
//		}
//		sourcepath = archive.getSearcherManager().getValue(catalogid, sourcepath, keys);
//		Asset asset = null;
//
//		if(properties.getFirstItem() != null){
//			String path = "/WEB-INF/data/" + archive.getCatalogId()	+ "/originals/" + sourcepath + "/${properties.getFirstItem().getName()}";
//			properties.saveFileAs(properties.getFirstItem(), path, inReq.getUser());
//			Page newfile = archive.getPageManager().getPage(path);
//			asset = (Asset) importer.createAssetFromPage(archive, inReq.getUser(), newfile);
//		}


//		if(asset == null && keys.get("fetchURL") != null){
//			asset = importer.createAssetFromFetchUrl(archive, (String) keys.get("fetchURL"), inReq.getUser(), sourcepath, (String) keys.get("importfilename"));
//		}
//
//		if(asset == null && keys.get("localPath") != null)
//		{
//			//log.info("HERE!!!");
//			File file = new File((String) keys.get("localPath"));
//			if(file.exists())
//			{
//				String path = "/WEB-INF/data/" + archive.getCatalogId()	+ "/originals/" + sourcepath + "/${file.getName()}";
//				Page newfile = archive.getPageManager().getPage(path);
//				String realpath = newfile.getContentItem().getAbsolutePath();
//				File target = new File(realpath);
//				target.getParentFile().mkdirs();
//				// TODO: check logic
//				if(file.renameTo(target)){
//					asset = importer.createAssetFromPage(archive, inReq.getUser(), newfile);
//				} else{
//					throw new OpenEditException("Error moving file: " + realpath);
//				}
//			}
//		}
//		if(asset == null){
//			asset = new Asset();//Empty Record
//			asset.setId(id);
//		}
//
//
//
//		for (Iterator iterator = request.iterator(); iterator
//				.hasNext();) {
//			Data result = (Data) iterator.next();
//			String key = result.get("key");
//			String value = result.get("value");
//			
//			
//			
//			asset.setProperty(key, value);
//		}
//
//
//		asset.setProperty("sourcepath", sourcepath);
//		searcher.saveData(asset, inReq.getUser());
//
//		JSONObject result = getAssetJson(archive.getSearcherManager(), searcher, asset);
//		String jsondata = result.toString();
//
//		inReq.putPageValue("json", jsondata);
//		return result;
//
//	}


	public JSONObject handleAssetPut(WebPageRequest inReq)
	{
		inReq.getResponse().setHeader("Access-Control-Allow-Origin","*");

		//	slurper.parse(inReq.getRequest().getReader()); //this is real, the other way is just for testing
		String content = (String) inReq.getPageValue("jsondata");
		JSONObject inputdata = null;
		JSONParser parser = new JSONParser();
		
		if(content != null){
			inputdata = (JSONObject) parser.parse(content);
		} else{
			inputdata = (JSONObject) parser.parse(inReq.getRequest().getReader()); //this is real, the other way is just for testing
		}


		String catalogid =  findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);


		AssetSearcher searcher = (AssetSearcher) archive.getSearcherManager().getSearcher(catalogid,"asset" );
		//We will need to handle this differently depending on whether or not this asset has a real file attached to it.
		//if it does, we should move it and use the asset importer to create it so metadata gets read, etc.

		String id = getId(inReq);
		Asset asset = null;
		if(id == null)
		{
			id = searcher.createNewData().getId(); // TODO: check logic
		}
		else
		{
			 asset = archive.getAsset(id);
			if(asset == null){
				throw new OpenEditException("Asset was not found! (${catalogid}:${id})");
			}
		}
		

		for (Iterator iterator = inputdata.keySet().iterator(); iterator
				.hasNext();) {
			String key = (String) iterator.next();
			Object value = (String) inputdata.get(key); // TODO: check logic
			if(value instanceof String){
				asset.setProperty(key, (String) value);
			} 
			
			if(value instanceof List){
				ArrayList ids = new ArrayList();
				PropertyDetail detail = searcher.getDetail(key);
				
				
				for (int i = 0; i < ((List) value).size(); i++) {
					
					JSONObject object = (JSONObject) ((List) value).get(i); // TODO: check logic
					String val = (String) object.get("id");
					ids.add(val);
					if(detail != null){
						Searcher rsearcher = archive.getSearcher(key);
						Data remote = (Data) rsearcher.searchById(val);
						if(remote == null){
							remote = rsearcher.createNewData();
							remote.setId(val);							
						}
						for (Iterator iterator2 = object.keySet().iterator(); iterator2
								.hasNext();) {
							String result = (String) iterator2.next();
							remote.setProperty(result, (String) object.get(result));
						}
						
						rsearcher.saveData(remote, inReq.getUser());
					}
					
					
					
				} 
				asset.setValues(key, ids);				
			}
			
			
			if(value instanceof Map ){
					Map values = (Map) value;
					
					PropertyDetail detail = searcher.getDetail(key);
					Searcher rsearcher = archive.getSearcher(key);
					String targetid = (String) values.get("id");
					Data remote = (Data) rsearcher.searchById(targetid); // TODO: check logic
					if(remote == null){
						remote = rsearcher.createNewData();
						remote.setId(targetid);
					}
					for (Iterator iterator2 = values.keySet().iterator(); iterator2
							.hasNext();) {
						String result = (String) iterator2.next();
						Object test = values.get(result);
						if (test instanceof String) {
							remote.setProperty(result, (String) test);
						}
					}

					rsearcher.saveData(remote, inReq.getUser());
					asset.setProperty(key, targetid);
			}
			
			else{
				//do osomething else
				
				log.info(value);
			}

		}

		
		
		
		searcher.saveData(asset, inReq.getUser());
		JSONObject result = getAssetJson(archive.getSearcherManager(),searcher, asset);


		JSONObject converisons = getConversions(archive, asset);

		result.put("conversions", converisons);

		String jsondata = result.toString();

		inReq.putPageValue("json", jsondata);
		return result;

	}


	public JSONObject handleAssetGet(WebPageRequest inReq)
	{
		inReq.getResponse().setHeader("Access-Control-Allow-Origin","*");

		String catalogid =  findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);


		AssetSearcher searcher = (AssetSearcher) archive.getSearcherManager().getSearcher(catalogid,"asset" );
		//We will need to handle this differently depending on whether or not this asset has a real file attached to it.
		//if it does, we should move it and use the asset importer to create it so metadata gets read, etc.
		String id = getId(inReq);

		log.info("JSON get with ${id} and ${catalogid}");
		

		Asset asset = archive.getAsset(id);

		if(asset == null){
			throw new OpenEditException("Asset was not found!");
		}

		JSONObject result = (JSONObject) getAssetJson(archive.getSearcherManager(),searcher, asset);
		JSONObject converisons = getConversions(archive, asset);


		result.put("conversions", converisons);

		String jsondata = result.toString();

		inReq.putPageValue("json", jsondata);
		return result;

	}



	public JSONObject handleAssetDelete(WebPageRequest inReq)
	{
	    inReq.getResponse().setHeader("Access-Control-Allow-Origin","*");
	
		JsonSlurper slurper = new JsonSlurper();

		String catalogid = findCatalogId(inReq);

		MediaArchive archive = getMediaArchive(inReq, catalogid);
		//We will need to handle this differently depending on whether or not this asset has a real file attached to it.
		//if it does, we should move it and use the asset importer to create it so metadata gets read, etc.
		String id = getId(inReq);

		Asset asset = archive.getAsset(id);

		if(asset != null){
			archive.getAssetSearcher().delete(asset, null);
		}
		return getAssetJson(archive.getSearcherManager(), archive.getAssetSearcher(), (Asset) asset);

	}

	//this is not needed see parent class
	public MediaArchive getMediaArchive(WebPageRequest inReq,  String inCatalogid)
	{
		SearcherManager sm = (SearcherManager)inReq.getPageValue("searcherManager");

		if (inCatalogid == null)
		{
			return null;
		}
		MediaArchive archive = (MediaArchive) sm.getModuleManager().getBean(inCatalogid, "mediaArchive");
		return archive;
	}

	public JSONObject getAssetJson(SearcherManager sm, Searcher inSearcher, Asset inAsset){

		JSONObject asset = new JSONObject();
		for (Iterator iterator = inSearcher.getPropertyDetails().iterator(); iterator
				.hasNext();) {
			PropertyDetail detail = (PropertyDetail) iterator.next();
			String key = detail.get("id");
			String value = inAsset.get(key);
			if (key != null && value != null) {
				if (detail.isMultiValue() || key == "category") {
					Collection<String> values = inAsset.getValues(key);
					JSONArray items = new JSONArray();
					for (Iterator iterator2 = values.iterator(); iterator2
							.hasNext();) {
						JSONObject data = getDataJson(sm,detail,(String) iterator2.next());						
						if( data != null )
							{
								items.add(data);
							}
					}
					asset.put(key, items);
				}
				else if(detail.isList())
				{
					JSONObject data = getDataJson(sm,detail,value);
					if( data != null)
					{
						asset.put(key,data);
					}
				}
				else if(detail.isBoolean())
				{
					asset.put(key, Boolean.parseBoolean(value));


				} else{
					asset.put(key, value);
				}
			}
			//need to add tags and categories, etc
		}
		//String tags = inAsset.get("keywords");
//		List tags = inAsset.getValues("keywords");
//		JSONArray array = new JSONArray();
//		tags.each{
//			array.add(it);
//		}
//		asset.put("tags", array);
		return asset;
	}
	public JSONObject getDataJson(SearcherManager sm, PropertyDetail inDetail, String inId)
		{
			Searcher searcher = sm.getSearcher(inDetail.getListCatalogId(), inDetail.getListId());
			Data data = (Data) searcher.searchById(inId);
			if( data == null)
			{
				return null;
			}
			return getDataJson(sm,searcher,data);
		}
	
	public JSONObject getDataJson(SearcherManager sm, Searcher inSearcher, Data inData){
		JSONObject asset = new JSONObject();
		asset.put("id", inData.getId());
		asset.put("name", inData.getName());
		
		for (Iterator iterator = inSearcher.getPropertyDetails().iterator(); iterator
				.hasNext();) {
			PropertyDetail detail = (PropertyDetail) iterator.next();

			String key = detail.get("id");
			String value = inData.get(key);
			if(key != null && value != null){
				if(detail.isList()){
					//friendly?
					asset.put(key, value);
				}
				else if(detail.isBoolean()){
					asset.put(key, Boolean.parseBoolean(value));
				} else{
					asset.put(key, value);
				}
			}
		}
		if( inSearcher.getSearchType() == "category")
		{
			MediaArchive archive = getMediaArchive(inSearcher.getCatalogId());
			Category cat = archive.getCategory(inData.getId());
			if( cat != null)
			{
				StringBuffer out = new StringBuffer();
				for (Iterator iterator = cat.getParentCategories().iterator(); iterator.hasNext();)
				{
					Category parent = (Category) iterator.next();
					out.append(parent.getName());
					if( iterator.hasNext())
					{
						out.append("/");
					}
				}
				asset.put("path",out.toString());
			}
		}

		return asset;
	}



	public JSONObject getAssetPublishLocations(MediaArchive inArchive, Data inAsset){

		JSONObject asset = new JSONObject();
		Searcher publish = inArchive.getSearcher("publishqueue");



		return asset;
	}

	public JSONObject getConversions(MediaArchive inArchive, Asset inAsset){

		JSONObject asset = new JSONObject();
		Searcher publish = inArchive.getSearcher("conversiontask");

		JSONArray array = new JSONArray();
		ConversionUtil util = new ConversionUtil();
		util.setSearcherManager(inArchive.getSearcherManager());

		String origurl = "/views/modules/asset/downloads/originals/${inAsset.getSourcePath()}/${inAsset.getMediaName()}";
		
		JSONObject original = new JSONObject();
		original.put("URL", origurl);
		
		asset.put("original", original);
		String assettype = (String) inAsset.get("assettype");
        if (assettype != "")
        {
			HitTracker conversions = util.getActivePresetList(inArchive.getCatalogId(),inAsset.get("assettype"));
			for (Iterator iterator = conversions.iterator(); iterator.hasNext();) {
				Data conversion = (Data) iterator.next();
				
				if(util.doesExist(inArchive.getCatalogId(), inAsset.getId(), inAsset.getSourcePath(), conversion.get("id"))){
					Dimension dimension = util.getConvertPresetDimension(inArchive.getCatalogId(), conversion.get("id"));
					JSONObject data = new JSONObject();
					//			<a class="thickbox btn" href="$home$apphome/views/modules/asset/downloads/generatedpreview/${asset.sourcepath}/${presetdata.outputfile}/$mediaarchive.asExportFileName($asset, $presetdata)">Preview</a>
					String exportfilename = inArchive.asExportFileName(inAsset, conversion);
					String url = "/views/modules/asset/downloads/preview/${inAsset.getSourcePath()}/${it.outputfile}";
					data.put("URL", url);
					data.put("height", dimension.getHeight());
					data.put("width", dimension.getWidth());
					asset.put(conversion.get("id"), data);
	
				}
			}
        }
		return asset;
	}
	

	public void preprocess(WebPageRequest inReq)
	{
		inReq.getResponse().setHeader("Access-Control-Allow-Origin","*");

		JsonSlurper slurper = new JsonSlurper();
		JSONObject request = null;
		String content = (String) inReq.getPageValue("jsondata");
		if(content != null){
			request = (JSONObject) slurper.parseText(content); //NOTE:  This is for unit tests.
		} else {
			request = (JSONObject) slurper.parse(inReq.getRequest().getReader()); //this is real, the other way is just for testing
		}

		for (Iterator iterator = request.keySet().iterator(); iterator
				.hasNext();) {
			String key = (String) iterator.next();
			Object val = request.get(key);
			if(val instanceof String){
			inReq.setRequestParameter(key, (String) val);
			}
		}


	}


	public String getId(WebPageRequest inReq)
	{
		inReq.getResponse().setHeader("Access-Control-Allow-Origin","*");
	
		String id = inReq.getPage().getName();
		
//		String root  = "/entermedia/services/json/asset/";
//		String url = inReq.getPath();
//		if(!url.endsWith("/")){
//			url = url + "/";
//		}
//		String id = url.substring(root.length(), url.length())
//		id = id.substring(0, id.indexOf("/"));
		return id;
	}



	public String findSearchType(WebPageRequest inReq)
	{
		String root  = "/mediadb/json/search/data/";
		String url = inReq.getPath();
		if(!url.endsWith("/")){
			url = url + "/";
		}
		String id = url.substring(root.length(), url.length());
		id = id.substring(0, id.indexOf("/"));
		return id;
	}



	public String findCatalogId(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		if(catalogid == null){
			if(inReq.getRequest() != null){
				catalogid = inReq.getRequest().getHeader("catalogid");
			}
		}
		
		return catalogid;
	}

	public void handleOrderRequest(WebPageRequest inReq)
	{
		inReq.getResponse().setHeader("Access-Control-Allow-Origin","*");
		log.info("Order request detected");
		JSONObject object = null;
		String method = inReq.getMethod();
		if(method == "POST"){
			object = handleOrderPost(inReq);
			log.info(object);

		}
		if(method == "PUT"){
			//	object = handleAssetPut(inReq);
		}
		if(method == "DELETE"){
			//	object = handleAssetDelete(inReq);
		}

		if(method == "GET"){
			//		object = handleAssetGet(inReq);
		}


		if(object != null){
			inReq.putPageValue("json", object.toString());
			try {
				OutputFiller filler = new OutputFiller();
				InputStream stream = new ByteArrayInputStream(object.toJSONString().getBytes("UTF-8"));
				if(inReq.getResponse() != null){
					inReq.getResponse().setContentType("application/json");
				}

				filler.fill(stream, inReq.getOutputStream());
			}
			finally {
			}
		}


	}



	public JSONObject handleOrderPost(WebPageRequest inReq)
	{
		inReq.getResponse().setHeader("Access-Control-Allow-Origin","*");
	
		log.info("starting to handle create order request");

		String catalogid =  findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);
		AssetSearcher searcher = (AssetSearcher) archive.getSearcherManager().getSearcher(catalogid,"asset" );
		OrderSearcher ordersearcher = (OrderSearcher) archive.getSearcherManager().getSearcher(catalogid,"order" );
		Searcher itemsearcher = archive.getSearcherManager().getSearcher(catalogid,"orderitem" );
		OrderManager orderManager = (OrderManager) ordersearcher.getOrderManager();


		JsonSlurper slurper = new JsonSlurper();
		JSONObject request = null;
		String content = (String) inReq.getPageValue("jsondata");

		if(content != null){
			request = (JSONObject) slurper.parseText(content); //NOTE:  This is for unit tests.
		} else{
			request = (JSONObject) slurper.parse(inReq.getRequest().getReader()); //this is real, the other way is just for testing
		}



		String publishdestination = (String) request.get("publishdestination");
		Order order = (Order) ordersearcher.createNewData();
		order.setProperty("publishdestination", publishdestination);
		order.setId(ordersearcher.nextId());

		ordersearcher.saveData(order, null);

		for (Iterator iterator = request.get("items").iterator(); iterator
				.hasNext();) {
			Data object = (Data) iterator.next();
			String assetid = object.get("assetid");
			String presetid = object.get("presetid");
			Data orderitem = itemsearcher.createNewData();
			orderitem.setProperty("assetid", assetid);
			orderitem.setProperty("presetid", presetid);
			orderitem.setProperty("orderid", order.getId());
			itemsearcher.saveData(orderitem, null);

		}


		orderManager.addConversionAndPublishRequest(inReq, order, archive, new HashMap(), inReq.getUser());



		JSONObject result = getOrderJson(archive.getSearcherManager(), ordersearcher, order);
		String jsondata = result.toString();
		log.info(jsondata);
		inReq.putPageValue("json", jsondata);
		return result;

	}
	public void populateJsonObject(Searcher inSearcher, JSONObject inObject, Data inData){
		for (Iterator iterator = inSearcher.getPropertyDetails().iterator(); iterator
				.hasNext();) {
			PropertyDetail detail = (PropertyDetail) iterator.next();
			String key = detail.getId();
			String value = inData.get(key);
			if(key != null && value != null){
				if(detail.isList()){

					inObject.put(key, value);

				}
				else if(detail.isBoolean()){
					inObject.put(key, Boolean.parseBoolean(value));


				} else{
					inObject.put(key, value);
				}


			}

		}
	}

		public JSONObject getOrderJson(SearcherManager sm, Searcher inSearcher, Data inOrder){

			JSONObject asset = new JSONObject();

			populateJsonObject(inSearcher, asset,inOrder);
			//need to add tags and categories, etc
		//String tags = inAsset.get("keywords");
		Searcher itemsearcher = sm.getSearcher(inSearcher.getCatalogId(),"orderitem" );
		HitTracker items = itemsearcher.query().match("orderid", inOrder.getId()).search();

		JSONArray array = new JSONArray();
		for (Iterator iterator = items.iterator(); iterator.hasNext();) {
			JSONObject item = new JSONObject();
			populateJsonObject(itemsearcher, item, (Data) iterator.next());
			array.add(item);
		}
		asset.put("items", array);



		return asset;
	}





}