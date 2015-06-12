package rest.json

import groovy.json.JsonSlurper

import java.awt.Dimension
import java.text.SimpleDateFormat

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.entermedia.upload.FileUpload
import org.entermedia.upload.UploadRequest
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.openedit.Data
import org.openedit.data.PropertyDetail
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.Asset
import org.openedit.entermedia.Category
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.creator.ConversionUtil
import org.openedit.entermedia.modules.BaseMediaModule
import org.openedit.entermedia.orders.Order
import org.openedit.entermedia.orders.OrderManager
import org.openedit.entermedia.orders.OrderSearcher
import org.openedit.entermedia.scanner.AssetImporter
import org.openedit.entermedia.search.AssetSearcher

import com.openedit.OpenEditException
import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.Page
import com.openedit.util.OutputFiller


public class JsonModule extends BaseMediaModule 
{
	private static final Log log = LogFactory.getLog(JsonModule.class);


	public void handleAssetRequest(WebPageRequest inReq)
	{
		inReq.getResponse().setHeader("Access-Control-Allow-Origin","*");
		
		JSONObject object = null;
		String method = inReq.getMethod();
		if(method == "POST"){
			object = handleAssetPost(inReq);
		}
		if(method == "PUT"){
			object = handleAssetPut(inReq);
		}
		if(method == "DELETE"){
			object = handleAssetDelete(inReq);
		}

		if(method == "GET"){
			object = handleAssetGet(inReq);
		}


		if(object != null){
			inReq.putPageValue("json", object.toString());
			try {
				OutputFiller filler = new OutputFiller();
				InputStream stream = new ByteArrayInputStream(object.toJSONString().getBytes("UTF-8"));
				if(inReq.getResponse()){
					inReq.getResponse().setContentType("application/json");
				}
				//filler.setBufferSize(40000);
				//InputStream input = object.
				filler.fill(stream, inReq.getOutputStream());
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


	public JSONObject handleAssetSearch(WebPageRequest inReq){
		//Could probably handle this generically, but I think they want tags, keywords etc.
		inReq.getResponse().setHeader("Access-Control-Allow-Origin","*");

		SearcherManager sm = inReq.getPageValue("searcherManager");

		String catalogid =  findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);
		AssetSearcher searcher = sm.getSearcher(catalogid,"asset" );
		JsonSlurper slurper = new JsonSlurper();
		def request = null;
		String content = inReq.getPageValue("jsondata");
		if(content != null){
			request = slurper.parseText(content); //NOTE:  This is for unit tests.
		} else{
			request = slurper.parse(inReq.getRequest().getReader()); //this is real, the other way is just for testing
		}


		ArrayList <String> fields = new ArrayList();
		ArrayList <String> operations = new ArrayList();

		request.query.each{
			println it;
			fields.add(it.field);
			operations.add(it.operator.toLowerCase());
			StringBuffer values = new StringBuffer();
			it.values.each{
				values.append(it);
				values.append(" ");
			}
			inReq.setRequestParameter(it.field + ".value", values.toString());
		}

		println "field" + fields;
		println "operations: " + operations;
		String[] fieldarray = fields.toArray(new String[fields.size()]) as String[];
		String[] opsarray = operations.toArray(new String[operations.size()]) as String[];

		inReq.setRequestParameter("field", fieldarray);
		inReq.setRequestParameter("operation", opsarray);

		SearchQuery query = searcher.addStandardSearchTerms(inReq);
		println "Query was: " + query;
		HitTracker hits = searcher.cachedSearch(inReq, query);
		println hits.size();
		JSONObject parent = new JSONObject();
		parent.put("size", hits.size());

		JSONArray array = new JSONArray();
		hits.getPageOfHits().each{
			JSONObject hit = getAssetJson(sm,searcher, it);

			array.add(hit);
		}


		parent.put("results", array);
		inReq.putPageValue("json", parent.toString());
//		try {
//			OutputFiller filler = new OutputFiller();
//			InputStream stream = new ByteArrayInputStream(parent.toJSONString().getBytes("UTF-8"));
//
//			//filler.setBufferSize(40000);
//			//InputStream input = object.
//			filler.fill(stream, inReq.getOutputStream());
//		} catch(Exception e){
//			throw new OpenEditException(e);
//		}
		return parent;
	}



	public JSONObject handleAssetPost(WebPageRequest inReq)
	{
		inReq.getResponse().setHeader("Access-Control-Allow-Origin","*");
	
		SearcherManager sm = inReq.getPageValue("searcherManager");

		String catalogid =  findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);
		AssetSearcher searcher = sm.getSearcher(catalogid,"asset" );
		//We will need to handle this differently depending on whether or not this asset has a real file attached to it.
		//if it does, we should move it and use the asset importer to create it so metadata gets read, etc.

		FileUpload command = archive.getSearcherManager().getModuleManager().getBean("fileUpload");
		UploadRequest properties = command.parseArguments(inReq);

		JsonSlurper slurper = new JsonSlurper();
		def request = null;
		String content = inReq.getPageValue("jsondata");
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
			SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
			String df = format.format(new Date());
			keys.put("formatteddate", df);


		}
		String id = request.id;
		if(id == null){
			id = searcher.nextAssetNumber()
		}
		keys.put("id", id);
		String sourcepath = keys.get("sourcepath");

		if(sourcepath == null){
			sourcepath = archive.getCatalogSettingValue("catalogassetupload");  //${division.uploadpath}/${user.userName}/${formateddate}
		}
		if(sourcepath == null || sourcepath.length() == 0){
			sourcepath = "receivedfiles/${id}";
		}
		sourcepath = sm.getValue(catalogid, sourcepath, keys);
		Asset asset = null;

		if(properties.getFirstItem() != null){
			String path = "/WEB-INF/data/" + archive.getCatalogId()	+ "/originals/" + sourcepath + "/${properties.getFirstItem().getName()}";
			properties.saveFileAs(properties.getFirstItem(), path, inReq.getUser());
			Page newfile = archive.getPageManager().getPage(path);
			asset = importer.createAssetFromPage(archive, inReq.getUser(), newfile);
		}


		if(asset == null && keys.get("fetchURL") != null){
			asset = importer.createAssetFromFetchUrl(archive, keys.get("fetchURL"), inReq.getUser(), sourcepath, keys.get("importfilename"));
		}

		if(asset == null && keys.get("localPath") != null)
		{
			//log.info("HERE!!!");
			File file = new File(keys.get("localPath"));
			if(file.exists())
			{
				String path = "/WEB-INF/data/" + archive.getCatalogId()	+ "/originals/" + sourcepath + "/${file.getName()}";
				Page newfile = archive.getPageManager().getPage(path);
				String realpath = newfile.getContentItem().getAbsolutePath();
				File target = new File(realpath);
				target.getParentFile().mkdirs();
				if(file.renameTo(realpath)){
					asset = importer.createAssetFromPage(archive, inReq.getUser(), newfile);
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


		asset.setProperty("sourcepath", sourcepath);
		searcher.saveData(asset, inReq.getUser());

		JSONObject result = getAssetJson(sm, searcher, asset);
		String jsondata = result.toString();

		inReq.putPageValue("json", jsondata);
		return result;

	}


	public JSONObject handleAssetPut(WebPageRequest inReq)
	{
		inReq.getResponse().setHeader("Access-Control-Allow-Origin","*");

		SearcherManager sm = inReq.getPageValue("searcherManager");
		//	slurper.parse(inReq.getRequest().getReader()); //this is real, the other way is just for testing
		String content = inReq.getPageValue("jsondata");
		JSONObject inputdata = null;
		JSONParser parser = new JSONParser();
		
		if(content != null){
			inputdata = parser.parse(content);
		} else{
			inputdata = parser.parse(inReq.getRequest().getReader()); //this is real, the other way is just for testing
		}


		String catalogid =  findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);


		AssetSearcher searcher = sm.getSearcher(catalogid,"asset" );
		//We will need to handle this differently depending on whether or not this asset has a real file attached to it.
		//if it does, we should move it and use the asset importer to create it so metadata gets read, etc.

		String id = getId(inReq);
		Asset asset = null;
		if(id == null)
		{
			id = searcher.createNewData()
		}
		else
		{
			 asset = archive.getAsset(id);
			if(asset == null){
				throw new OpenEditException("Asset was not found! (${catalogid}:${id})");
			}
		}
		

		inputdata.keySet().each {
			println it;
			
			String key = it;
			Object value = inputdata.get(key);
			if(value instanceof String){
				asset.setProperty(key, value);
			} 
			
			if(value instanceof List){
				ArrayList ids = new ArrayList();
				PropertyDetail detail = searcher.getDetail(key);
				
				
				value.each{
					JSONObject object = it;
					String val = it.get("id");
					ids.add(val);
					if(detail != null){
						Searcher rsearcher = archive.getSearcher(key);
						Data remote = rsearcher.searchById(val);
						if(remote == null){
							remote = rsearcher.createNewData();
							remote.setId(val);							
						}
						object.keySet().each{
							remote.setProperty(it, object.get(it));
						}
						rsearcher.saveData(remote, inReq.getUser());
					}
					
					
					
				} 
				asset.setValues(key, ids);				
			}
			
			
			if(value instanceof Map ){
					Map values = value;
					
					PropertyDetail detail = searcher.getDetail(key);
					Searcher rsearcher = archive.getSearcher(key);
					String targetid = value.id;
					Data remote = rsearcher.searchById(id);
					if(remote == null){
						remote = rsearcher.createNewData();
						remote.setId(targetid);
					}
					values.keySet().each{
						Object test = values.get(it);
						if(test instanceof String){
							remote.setProperty(it,test );
						}
					}
					rsearcher.saveData(remote, inReq.getUser());
					asset.setProperty(key, targetid);
			}
			
			else{
				//do osomething else
				
				println value;
			}

		}
		
				

//		request.each{
//			println it;
//			String key = it.key;
			
	//		String value = it.value;
//			
//			asset.setProperty(key, value);
//		}

		
		
		
		searcher.saveData(asset, inReq.getUser());
		JSONObject result = getAssetJson(sm,searcher, asset);


		JSONObject converisons = getConversions(archive, asset);

		result.put("conversions", converisons);

		String jsondata = result.toString();

		inReq.putPageValue("json", jsondata);
		return result;

	}


	public JSONObject handleAssetGet(WebPageRequest inReq)
	{
		inReq.getResponse().setHeader("Access-Control-Allow-Origin","*");

		SearcherManager sm = inReq.getPageValue("searcherManager");
		//	slurper.parse(inReq.getRequest().getReader()); //this is real, the other way is just for testing
		//				JsonSlurper slurper = new JsonSlurper();
		//				def request = null;
		//				String content = inReq.getPageValue("jsondata");
		//				if(properties != null){
		//
		//				}
		//				if(content != null){
		//					request = slurper.parseText(content); //NOTE:  This is for unit tests.
		//				} else{
		//					request = slurper.parse(inReq.getRequest().getReader()); //this is real, the other way is just for testing
		//				}
		//

		String catalogid =  findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);


		AssetSearcher searcher = sm.getSearcher(catalogid,"asset" );
		//We will need to handle this differently depending on whether or not this asset has a real file attached to it.
		//if it does, we should move it and use the asset importer to create it so metadata gets read, etc.
		String id = getId(inReq);

		log.info("JSON get with ${id} and ${catalogid}");
		

		Asset asset = archive.getAsset(id);

		if(asset == null){
			throw new OpenEditException("Asset was not found!");
		}

		JSONObject result = getAssetJson(sm,searcher, asset);
		JSONObject converisons = getConversions(archive, asset);


		result.put("conversions", converisons);

		String jsondata = result.toString();

		inReq.putPageValue("json", jsondata);
		return result;

	}



	public void handleAssetDelete(WebPageRequest inReq)
	{
	    inReq.getResponse().setHeader("Access-Control-Allow-Origin","*");
	
		JsonSlurper slurper = new JsonSlurper();

		SearcherManager sm = inReq.getPageValue("searcherManager");
		String catalogid = findCatalogId(inReq);

		MediaArchive archive = getMediaArchive(inReq, catalogid);
		AssetSearcher searcher = sm.getSearcher(catalogid,"asset" );
		//We will need to handle this differently depending on whether or not this asset has a real file attached to it.
		//if it does, we should move it and use the asset importer to create it so metadata gets read, etc.
		String id = getId(inReq);

		Asset asset = archive.getAsset(id);

		if(asset != null){
			searcher.delete(asset, null);
		}


	}

	//this is not needed see parent class
	public MediaArchive getMediaArchive(WebPageRequest inReq,  String inCatalogid)
	{
		SearcherManager sm = inReq.getPageValue("searcherManager");

		if (inCatalogid == null)
		{
			return null;
		}
		MediaArchive archive = (MediaArchive) sm.getModuleManager().getBean(inCatalogid, "mediaArchive");
		return archive;
	}

	public JSONObject getAssetJson(SearcherManager sm, Searcher inSearcher, Data inAsset){

		JSONObject asset = new JSONObject();
		inSearcher.getPropertyDetails().each{
			PropertyDetail detail = it;

			String key = it.id;
			String value=inAsset.get(it.id);
			if(key && value)
			{
				if(detail.isMultiValue() || key =="category")
				{
					List values = inAsset.getValues(key);
					JSONArray items = new JSONArray();
					values.each{
						JSONObject data = getDataJson(sm,detail,it);
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
			Data data = searcher.searchById(inId);
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
		inSearcher.getPropertyDetails().each{
			PropertyDetail detail = it;
			String key = it.id;
			String value=inData.get(it.id);
			if(key && value){
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
		String assettype = inAsset.get("assettype");
                 if (assettype)
                 {
		HitTracker conversions = util.getActivePresetList(inArchive.getCatalogId(),inAsset.get("assettype"));
		conversions.each{
			if(util.doesExist(inArchive.getCatalogId(), inAsset.getId(), inAsset.getSourcePath(), it.id)){
				Dimension dimension = util.getConvertPresetDimension(inArchive.getCatalogId(), it.id);
				JSONObject data = new JSONObject();
				//			<a class="thickbox btn" href="$home$apphome/views/modules/asset/downloads/generatedpreview/${asset.sourcepath}/${presetdata.outputfile}/$mediaarchive.asExportFileName($asset, $presetdata)">Preview</a>
				String exportfilename = inArchive.asExportFileName(inAsset, it);
				String url = "/views/modules/asset/downloads/preview/${inAsset.getSourcePath()}/${it.outputfile}";
				data.put("URL", url);
				data.put("height", dimension.getHeight());
				data.put("width", dimension.getWidth());
				asset.put(it.id, data);

			}
		}
              }
		return asset;
	}
	

	public void preprocess(WebPageRequest inReq)
	{
		inReq.getResponse().setHeader("Access-Control-Allow-Origin","*");

		JsonSlurper slurper = new JsonSlurper();
		def request = null;
		String content = inReq.getPageValue("jsondata");
		if(content != null){
			request = slurper.parseText(content); //NOTE:  This is for unit tests.
		} else{
			request = slurper.parse(inReq.getRequest().getReader()); //this is real, the other way is just for testing
		}

		request.keySet().each{
			println it;
			String key = it;
			Object val = request.get(key);
			if(val instanceof String){
			inReq.setRequestParameter(key, val);
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
		String id = url.substring(root.length(), url.length())
		id = id.substring(0, id.indexOf("/"));
		return id;
	}



	public String findCatalogId(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		if(catalogid == null){
			if(inReq.getRequest()){
				catalogid = inReq.getRequest().getHeader("catalogid");
			}
		}
		
		return catalogid;
	}

	public void handleOrderRequest(WebPageRequest inReq)
	{
		inReq.getResponse().setHeader("Access-Control-Allow-Origin","*");
		println "Order request detected";
		JSONObject object = null;
		String method = inReq.getMethod();
		if(method == "POST"){
			object = handleOrderPost(inReq);
			println object;

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
				if(inReq.getResponse()){
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
	
		println "starting to handle create order request";
		SearcherManager sm = inReq.getPageValue("searcherManager");

		String catalogid =  findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);
		AssetSearcher searcher = sm.getSearcher(catalogid,"asset" );
		OrderSearcher ordersearcher = sm.getSearcher(catalogid,"order" );
		Searcher itemsearcher = sm.getSearcher(catalogid,"orderitem" );
		OrderManager orderManager = ordersearcher.getOrderManager();


		JsonSlurper slurper = new JsonSlurper();
		def request = null;
		String content = inReq.getPageValue("jsondata");

		if(content != null){
			request = slurper.parseText(content); //NOTE:  This is for unit tests.
		} else{
			request = slurper.parse(inReq.getRequest().getReader()); //this is real, the other way is just for testing
		}



		String publishdestination = request.publishdestination;
		Order order = ordersearcher.createNewData();
		order.setProperty("publishdestination", publishdestination);
		order.setId(ordersearcher.nextId());

		ordersearcher.saveData(order, null);

		request.items.each{
			String assetid = it.assetid;
			String presetid = it.presetid;
			Data orderitem = itemsearcher.createNewData();
			orderitem.setProperty("assetid", assetid);
			orderitem.setProperty("presetid", presetid);
			orderitem.setProperty("orderid", order.getId());
			itemsearcher.saveData(orderitem, null);

		}


		orderManager.addConversionAndPublishRequest(inReq, order, archive, new HashMap(), inReq.getUser())



		JSONObject result = getOrderJson(sm, ordersearcher, order);
		String jsondata = result.toString();
		println jsondata;
		inReq.putPageValue("json", jsondata);
		return result;

	}
	public void populateJsonObject(Searcher inSearcher, JSONObject inObject, Data inData){
		inSearcher.getPropertyDetails().each{
			PropertyDetail detail = it;
			String key = it.id;
			String value=inData.get(it.id);
			if(key && value){
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
		items.each{

			JSONObject item = new JSONObject();
			populateJsonObject(itemsearcher, item,it);
			array.add(item);
		}
		asset.put("items", array);



		return asset;
	}





}