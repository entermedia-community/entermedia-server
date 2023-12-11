package org.entermediadb.asset.mediadb;

import java.awt.Dimension;
import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.BaseAsset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.convert.ConversionUtil;
import org.entermediadb.asset.orders.Order;
import org.entermediadb.asset.orders.OrderSearcher;
import org.entermediadb.asset.scanner.AssetImporter;
import org.entermediadb.asset.search.AssetSearcher;
import org.entermediadb.asset.upload.FileUpload;
import org.entermediadb.asset.upload.UploadRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.HitTracker;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;
import org.openedit.repository.filesystem.FileItem;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.PathUtilities;

import groovy.json.JsonSlurper;

public class JsonAssetModule extends BaseJsonModule {
	private static final Log log = LogFactory.getLog(JsonAssetModule.class);

	/*
	 * public JSONObject handleAssetSearch(WebPageRequest inReq) { //Could
	 * probably handle this generically, but I think they want tags, keywords
	 * etc.
	 * 
	 * SearcherManager sm = inReq.getPageValue("searcherManager");
	 * 
	 * String catalogid = findCatalogId(inReq); MediaArchive archive =
	 * getMediaArchive(inReq, catalogid); AssetSearcher searcher =
	 * sm.getSearcher(catalogid,"asset" );
	 * 
	 * def request = inReq.getJsonRequest(); //this is real, the other way is
	 * just for testing
	 * 
	 * ArrayList <String> fields = new ArrayList(); ArrayList <String>
	 * operations = new ArrayList();
	 * 
	 * request.query.terms.each { fields.add(it.field);
	 * operations.add(it.operator.toLowerCase()); StringBuffer values = new
	 * StringBuffer(); it.values.each{ values.append(it); values.append(" "); }
	 * inReq.setRequestParameter(it.field + ".value", values.toString()); }
	 * 
	 * String[] fieldarray = fields.toArray(new String[fields.size()]) as
	 * String[]; String[] opsarray = operations.toArray(new
	 * String[operations.size()]) as String[];
	 * 
	 * inReq.setRequestParameter("field", fieldarray);
	 * inReq.setRequestParameter("operation", opsarray);
	 * inReq.setRequestParameter("hitsperpage",
	 * String.valueOf(request.hitsperpage));
	 * 
	 * SearchQuery query = searcher.addStandardSearchTerms(inReq); HitTracker
	 * hits = searcher.cachedSearch(inReq, query); // String hitsperpage =
	 * request.hitsperpage; // if (hitsperpage != null) // { // int pagesnum =
	 * Integer.parseInt(hitsperpage); // hits.setHitsPerPage(pagesnum); // }
	 * String page = request.page; if(page != null) { int pagenumb =
	 * Integer.parseInt(page); hits.setPage(pagenumb); } JSONObject parent = new
	 * JSONObject(); parent.put("size", hits.size());
	 * 
	 * JSONArray array = new JSONArray(); hits.getPageOfHits().each { JSONObject
	 * hit = getAssetJson(sm,searcher, it);
	 * 
	 * array.add(hit); }
	 * 
	 * 
	 * parent.put("results", array); inReq.putPageValue("json",
	 * parent.toString()); return parent; }
	 * 
	 */

	public void createAsset(WebPageRequest inReq) {
		SearcherManager sm = (SearcherManager) inReq.getPageValue("searcherManager");

		String catalogid = findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);
		AssetSearcher searcher = archive.getAssetSearcher();
		// We will need to handle this differently depending on whether or not
		// this asset has a real file attached to it.
		// if it does, we should move it and use the asset importer to create it
		// so metadata gets read, etc.

		FileUpload command = (FileUpload) archive.getSearcherManager().getModuleManager().getBean("fileUpload");
		UploadRequest properties = command.parseArguments(inReq);

		Map request = inReq.getJsonRequest();

		AssetImporter importer = archive.getAssetImporter();
		HashMap vals = new HashMap();
		vals.putAll(inReq.getParameterMap());
		if (properties.getFirstItem()  != null) {
			String fileName = properties.getFirstItem().getName();
			if (fileName != null) {
				vals.put("filename", fileName);
				String ext = PathUtilities.extractPageType(fileName);
				String render = archive.getMediaRenderType(ext);
				vals.put("extension", ext);
				vals.put("rendertype", render);
			}
			// vals.put("filename", item.getName());
			// vals.put("guid", item.getName());
		}
		String guid = UUID.randomUUID().toString();
		String sguid = guid.substring(0, Math.min(guid.length(), 13));
		vals.put("guid", sguid);
		vals.put("splitguid", sguid.substring(0, 2) + "/" + sguid.substring(3).replace("-", ""));

		String df = DateStorageUtil.getStorageUtil().formatDateObj(new Date(), "yyyyMM");// new
																							// SimpleDateFormat("yyyyMM");

		vals.put("formatteddate", df);

		df = DateStorageUtil.getStorageUtil().formatDateObj(new Date(), "yyyy/MM");
		vals.put("formattedmonth", df);

		String importpath = (String)vals.get("importpath");
		if( importpath != null)
		{
			String filename = PathUtilities.extractFileName(importpath);
			vals.put("filename", filename);
		}
		
		Asset asset = null;
		String sourcepath = null;
		String id = null;
		if( request == null)
		{
			//throw new OpenEditException("JSON not parsed ");
			
			
		}
		else {
			id = (String) request.get("id");
			if (id == null) {
				// id = searcher.nextAssetNumber();
				vals.put("id", id);
			}
			else {
				asset = archive.getAsset(id);
				if(asset != null ){
					sourcepath = asset.getSourcePath();
				}
			}
		}
		
		if( asset == null)
		{
			//sourcepath = (String) vals.get("sourcepath");
			sourcepath = inReq.getRequiredParameter("sourcepath");
	
			if (sourcepath == null) {
				sourcepath = archive.getCatalogSettingValue("catalogassetupload"); // ${division.uploadpath}/${user.userName}/${formateddate}
			}
			if (sourcepath == null || sourcepath.length() == 0) {
				sourcepath = "receivedfiles/${id}";
			}
			sourcepath = sm.getValue(catalogid, sourcepath, vals);
		}
		//String legacysourcepath = archive.getCatalogSettingValue("catalogassetuploadalwaysappend"); //PRN? ${division.uploadpath}/${user.userName}/${formateddate}
		if (properties.getFirstItem() != null) 
		{
			String path = "/WEB-INF/data/" + archive.getCatalogId() + "/originals/" + sourcepath ;
			
			//if( legacysourcepath != null && "true".equalsIgnoreCase(legacysourcepath))
			boolean foldrbased = false;
			if( path.endsWith("/"))
			{
				path = path + "/" + properties.getFirstItem().getName();
				foldrbased = true;
			}
			path = path.replace("//", "/");
			properties.saveFileAs(properties.getFirstItem(), path, inReq.getUser());
			Page newfile = archive.getPageManager().getPage(path);
			// THis will NOT append the filename to the source path
			asset = importer.createAssetFromPage(archive, foldrbased, inReq.getUser(), newfile, id);
		}
		else if (asset == null && vals.get("fetchURL") != null) {
			asset = importer.createAssetFromFetchUrl(archive, (String) vals.get("fetchURL"), inReq.getUser(),
					sourcepath, (String) vals.get("importfilename"), id);
			
		} 
		else if ( importpath != null)
		{
			//Create a page for this path
			File checkfile = new File(importpath);
			if( !checkfile.exists())
			{
				throw new OpenEditException("Could not find or did not have access to " + importpath);
			}
			Collection hotfolders = archive.getList("hotfolder");
			boolean foundmatch = false;
			for (Iterator iterator = hotfolders.iterator(); iterator.hasNext();)
			{
				Data hotfolder = (Data) iterator.next();
				String path = hotfolder.get("externalpath");
				if(path != null && importpath.startsWith(path))
				{
					String ending = importpath.substring(path.length());
					String subfolder = hotfolder.get("subfolder");
					sourcepath = subfolder +  ending;
					String contentpath = "/WEB-INF/data/" + archive.getCatalogId() + "/originals/" + sourcepath;
					Page item = archive.getPageManager().getPage(contentpath);
					asset = importer.createAssetFromPage(archive, false, inReq.getUser(), item, id);
					foundmatch = true;
					break;
				}
			}
			if( !foundmatch)
			{
				ContentItem item = new FileItem(new File(importpath));
				boolean folderbased = sourcepath.endsWith("/");
				String postfix = sourcepath;
				if( folderbased )
				{
					postfix = postfix + PathUtilities.extractFileName(importpath); //Add a filename
				}
				String destpath = "/WEB-INF/data/" + archive.getCatalogId() + "/originals/" + postfix;  
				destpath = destpath.replace("//", "/");

				Page destitem = archive.getPageManager().getPage(destpath);
				archive.getPageManager().getRepository().copy(item, destitem.getContentItem());
				
				asset = importer.createAssetFromPage(archive, folderbased, inReq.getUser(), destitem, id);
			}
		}

		if (asset == null && vals.get("localPath") != null) {
			// log.info("HERE!!!");
			File file = new File((String) vals.get("localPath"));
			if (file.exists()) {
				String path = "/WEB-INF/data/" + archive.getCatalogId() + "/originals/" + sourcepath + "/"
						+ file.getName();
				path = path.replace("//", "/");
				Page newfile = archive.getPageManager().getPage(path);
				String realpath = newfile.getContentItem().getAbsolutePath();
				File target = new File(realpath);
				target.getParentFile().mkdirs();
				if (file.renameTo(new File(realpath))) {
					asset = importer.createAssetFromPage(archive, false, inReq.getUser(), newfile, id);
					if(id != null){
						asset.setId(id);
					
					}
				} else {
					throw new OpenEditException("Error moving file: " + realpath);
				}
			}
		}
		if (asset == null) {
			asset = new BaseAsset(archive);// Empty Record
			asset.setId(id);
			asset.setProperty("sourcepath", sourcepath);
			asset.setProperty("assetaddeddate", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
		}
		if (request!=null) 
		{
			Map remaining = new HashMap(request);
			remaining.remove("categorypath");
			remaining.remove("category-exact");
			remaining.remove("category");
			remaining.remove("sourcepath");
			populateJsonData(remaining, searcher, asset);
			
			Map categories = (Map)request.get("category");
			if( categories != null)
			{
				log.info("Ignoring category ids, use categorypath " + categories );
			}
		}

		
		Object categorypaths = vals.get("categorypath");
		if(categorypaths instanceof Collection){
			Collection <String> paths = (Collection) categorypaths;
			for (String path : paths) {
				Category cat = archive.createCategoryPath(path);
				asset.addCategory(cat);
				
			}
		} 
		if(categorypaths instanceof String){
			String path = (String) categorypaths;
			
			Category cat = archive.createCategoryPath(path);
			asset.addCategory(cat);
		}
		
		
		
		
		importer.saveAsset(archive, inReq.getUser(), asset);

		// JSONObject result = getAssetJson(sm, searcher, asset);
		// String jsondata = result.toString();
		inReq.putPageValue("searcher", searcher);
		inReq.putPageValue("asset", asset);
		inReq.putPageValue("data", asset);
		// inReq.putPageValue("json", jsondata);
		// return result;

	}

	public void updateAsset(WebPageRequest inReq) {

		SearcherManager sm = (SearcherManager) inReq.getPageValue("searcherManager");

		Map inputdata = (Map) inReq.getJsonRequest();

		String catalogid = findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);

		AssetSearcher searcher = archive.getAssetSearcher();
		// We will need to handle this differently depending on whether or not
		// this asset has a real file attached to it.
		// if it does, we should move it and use the asset importer to create it
		// so metadata gets read, etc.

		String id = getId(inReq);
		if (id == null) {
			return;
		}

		Asset asset = archive.getAsset(id);

		if (asset == null) {
			return;
		}
		populateJsonData(inputdata, searcher, asset);

		searcher.saveData(asset, inReq.getUser());
		archive.fireMediaEvent("assetedited", inReq.getUser(), asset);

		inReq.putPageValue("asset", asset);
		inReq.putPageValue("data", asset);
		inReq.putPageValue("searcher", searcher);
		// return result;

	}
	
	public void cloneAsset(WebPageRequest inReq) {

		SearcherManager sm = (SearcherManager) inReq.getPageValue("searcherManager");

		Map inputdata = (Map) inReq.getJsonRequest();

		String catalogid = findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);

		AssetSearcher searcher = archive.getAssetSearcher();

		String assetid = inReq.getRequestParameter("assetid");
		if (assetid == null) {
			return;
		}

		Asset asset = archive.getAsset(assetid);
		
		if (asset == null) {
			return;
		}
		//populateJsonData(inputdata, searcher, asset);
		
		//asset.setId(inNewid);
		String originalSourcepath = asset.getSourcePath();
		String basepath = "/WEB-INF/data/" + catalogid + "/originals/";
		
		ContentItem dest = getPageManager().getContent(basepath + originalSourcepath);
		int i = 2;
		String newSourcepath = originalSourcepath;
		
		while (dest.exists())
		{
			String pagename = PathUtilities.extractPageName(originalSourcepath);
			String tmppath = originalSourcepath.replace(pagename, pagename + "_" + i);
			dest = getPageManager().getContent(basepath + tmppath);
			if (!dest.exists())
			{
				newSourcepath = tmppath;
				break;
			}
			i++;
		}
		//asset.setSourcePath(originalSourcepath);
		
		
		Asset newasset = archive.getAssetEditor().copyAsset(archive, asset, newSourcepath);
		
		//asset.setSourcePath(originalSourcepath);
		//asset.setId(null);
		
		searcher.saveData(newasset, inReq.getUser());
		archive.fireMediaEvent("assetedited", inReq.getUser(), newasset);

		inReq.putPageValue("newasset", newasset);
		inReq.putPageValue("newdata", newasset);
		inReq.putPageValue("searcher", searcher);
		// return result;

	}

	/*
	 * public void loadAsset(WebPageRequest inReq) {
	 * 
	 * SearcherManager sm = inReq.getPageValue("searcherManager");
	 * 
	 * String catalogid = findCatalogId(inReq); MediaArchive archive =
	 * getMediaArchive(inReq, catalogid);
	 * 
	 * 
	 * AssetSearcher searcher = sm.getSearcher(catalogid,"asset" ); //We will
	 * need to handle this differently depending on whether or not this asset
	 * has a real file attached to it. //if it does, we should move it and use
	 * the asset importer to create it so metadata gets read, etc. String id =
	 * getId(inReq);
	 * 
	 * log.info("JSON get with ${id} and ${catalogid}");
	 * 
	 * 
	 * Asset asset = archive.getAsset(id);
	 * 
	 * if(asset == null) { //throw new
	 * OpenEditException("Asset was not found!"); return; }
	 * 
	 * inReq.putPageValue("asset", asset); inReq.putPageValue("searcher",
	 * searcher);
	 * 
	 * 
	 * 
	 * }
	 */
	/*
	 * public void deleteAsset(WebPageRequest inReq) {
	 * 
	 * JsonSlurper slurper = new JsonSlurper();
	 * 
	 * SearcherManager sm = inReq.getPageValue("searcherManager"); String
	 * catalogid = findCatalogId(inReq);
	 * 
	 * MediaArchive archive = getMediaArchive(inReq, catalogid); AssetSearcher
	 * searcher = sm.getSearcher(catalogid,"asset" ); //We will need to handle
	 * this differently depending on whether or not this asset has a real file
	 * attached to it. //if it does, we should move it and use the asset
	 * importer to create it so metadata gets read, etc. String id =
	 * getId(inReq); //TODO: Handle multiple deletes?
	 * 
	 * Asset asset = archive.getAsset(id);
	 * 
	 * int counted = 0; if(asset != null) { searcher.delete(asset, null);
	 * counted++; } inReq.putPageValue("id", id); inReq.putPageValue("asset",
	 * asset); inReq.putPageValue("data", asset); inReq.putPageValue("searcher",
	 * searcher); inReq.putPageValue("deleted", counted);
	 * 
	 * 
	 * }
	 * 
	 * public void getAssetJson(SearcherManager sm, Searcher inSearcher, Data
	 * inAsset) {
	 * 
	 * JSONObject asset = new JSONObject(); inSearcher.getPropertyDetails().each
	 * { PropertyDetail detail = it;
	 * 
	 * String key = it.id; String value=inAsset.get(it.id); if(key && value) {
	 * if(detail.isMultiValue() || key =="category") { List values =
	 * inAsset.getValues(key); JSONArray items = new JSONArray(); values.each{
	 * JSONObject data = getDataJson(sm,detail,it); if( data != null ) {
	 * items.add(data); } } asset.put(key, items); } else if(detail.isList()) {
	 * JSONObject data = getDataJson(sm,detail,value); if( data != null) {
	 * asset.put(key,data); } } else if(detail.isBoolean()) { asset.put(key,
	 * Boolean.parseBoolean(value));
	 * 
	 * 
	 * } else { asset.put(key, value); } } //need to add tags and categories,
	 * etc } //return asset; }
	 */

	public JSONObject getAssetPublishLocations(MediaArchive inArchive, Data inAsset) {

		JSONObject asset = new JSONObject();
		Searcher publish = inArchive.getSearcher("publishqueue");
		return asset;
	}

	public JSONObject getConversions(MediaArchive inArchive, Asset inAsset) {

		JSONObject asset = new JSONObject();
		Searcher publish = inArchive.getSearcher("conversiontask");

		JSONArray array = new JSONArray();
		ConversionUtil util = new ConversionUtil();
		util.setSearcherManager(inArchive.getSearcherManager());

		String origurl = "/views/modules/asset/downloads/originals/${inAsset.getSourcePath()}/${inAsset.getMediaName()}";

		JSONObject original = new JSONObject();
		original.put("URL", origurl);

		asset.put("original", original);

		HitTracker conversions = util.getActivePresetList(inArchive.getCatalogId(), inAsset.get("assettype"));
		for (Iterator iterator = conversions.iterator(); iterator.hasNext();) {
			Data it = (Data) iterator.next();
			if (util.doesExist(inArchive.getCatalogId(), inAsset.getId(), inAsset.getSourcePath(), it.getId())) {
				Dimension dimension = (Dimension) util.getConvertPresetDimension(inArchive.getCatalogId(), it.getId());
				JSONObject data = new JSONObject();
				// <a class="thickbox btn"
				// href="$home$apphome/views/modules/asset/downloads/generatedpreview/${asset.sourcepath}/${presetdata.outputfile}/$mediaarchive.asExportFileName($asset,
				// $presetdata)">Preview</a>
				String exportfilename = inArchive.asExportFileName(inAsset, it);
				String url = "/views/modules/asset/downloads/preview/" + inAsset.getSourcePath() + "/" + exportfilename;
				data.put("URL", url);
				data.put("height", dimension.getHeight());
				data.put("width", dimension.getWidth());
				asset.put(it.getId(), data);
			}
		}
		return asset;
	}

	public JSONObject createOrder(WebPageRequest inReq) {

		log.info("starting to handle create order request");

		String catalogid = findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);
		AssetSearcher searcher = (AssetSearcher) archive.getSearcher("asset");
		OrderSearcher ordersearcher = (OrderSearcher) archive.getSearcher("order");
		Searcher itemsearcher = archive.getSearcher("orderitem");
		// OrderManager orderManager = archive.getOrderManager();

		JsonSlurper slurper = new JsonSlurper();
		Map request = null;
		String content = (String) inReq.getPageValue("jsondata");
		try {
			if (content != null) {
				request = (Map) slurper.parseText(content); // NOTE: This is for
															// unit tests.
			} else {
				request = (Map) slurper.parse(inReq.getRequest().getReader()); // this
																				// is
																				// real,
																				// the
																				// other
																				// way
																				// is
																				// just
																				// for
																				// testing
			}
		} catch (Throwable ex) {
			throw new OpenEditException(ex);
		}
		String publishdestination = (String) request.get("publishdestination");
		Order order = (Order) ordersearcher.createNewData();
		order.setProperty("publishdestination", publishdestination);
		order.setId(ordersearcher.nextId());

		ordersearcher.saveData(order, null);

		Collection items = (Collection) request.get("items");
		for (Iterator iterator = items.iterator(); iterator.hasNext();) {
			Data it = (Data) iterator.next();
			String assetid = it.get("assetid");
			String presetid = it.get("presetid");
			Data orderitem = itemsearcher.createNewData();
			orderitem.setProperty("assetid", assetid);
			orderitem.setProperty("presetid", presetid);
			orderitem.setProperty("orderid", order.getId());
			itemsearcher.saveData(orderitem, null);

		}
		archive.getOrderManager().addConversionAndPublishRequest(inReq, order, archive, new HashMap(), inReq.getUser());

		JSONObject result = getOrderJson(archive.getSearcherManager(), ordersearcher, order);
		String jsondata = result.toString();
		inReq.putPageValue("json", jsondata);
		return result;

	}
	
	/**
	 * @deprecated only works within app. Use createAsset
	 * @param inReq
	 */

	public void importAssetJson(WebPageRequest inReq) {
		SearcherManager sm = (SearcherManager) inReq.getPageValue("searcherManager");

		MediaArchive archive = (MediaArchive) inReq.getPageValue("mediaarchive");
		AssetSearcher searcher = archive.getAssetSearcher();
		// We will need to handle this differently depending on whether or not
		// this asset has a real file attached to it.
		// if it does, we should move it and use the asset importer to create it
		// so metadata gets read, etc.

		String importpath = inReq.findValue("importpath");
		Page upload = archive.getPageManager().getPage(importpath);
			
		

		AssetImporter importer = archive.getAssetImporter();

		
		JsonSlurper slurper = new JsonSlurper();
		Collection array = (Collection) slurper.parse(upload.getReader());
		
		
		for (Iterator iterator = array.iterator(); iterator.hasNext();)
		{
			Map asstdata = (Map) iterator.next();
						
			HashMap vals = new HashMap();
			vals.putAll(inReq.getParameterMap());
			
			String guid = UUID.randomUUID().toString();
			String sguid = guid.substring(0, Math.min(guid.length(), 13));
			vals.put("guid", sguid);
			vals.put("splitguid", sguid.substring(0, 2) + "/" + sguid.substring(3).replace("-", ""));
			String df = DateStorageUtil.getStorageUtil().formatDateObj(new Date(), "yyyyMM");
			vals.put("formatteddate", df);
			df = DateStorageUtil.getStorageUtil().formatDateObj(new Date(), "yyyy/MM");
			vals.put("formattedmonth", df);
			Asset asset = null;
			String id = (String) asstdata.get("id");
			String sourcepath = null;
			if (id == null) {
				// id = searcher.nextAssetNumber();
				//vals.put("id", id);
			}
			else
			{
				asset = archive.getAsset(id);
				if(asset != null ){
					sourcepath = asset.getSourcePath();
				}
			}
			if( asset == null)
			{
				sourcepath = (String) vals.get("sourcepath");
		
				if (sourcepath == null) {
					sourcepath = archive.getCatalogSettingValue("catalogassetupload"); // ${division.uploadpath}/${user.userName}/${formateddate}
				}
				if (sourcepath == null || sourcepath.length() == 0) {
					sourcepath = "receivedfiles/${id}";
				}
				sourcepath = sm.getValue(archive.getCatalogId(), sourcepath, vals);
			}
			

			if (asset == null && vals.get("fetchURL") != null) {
				asset = importer.createAssetFromFetchUrl(archive, (String) vals.get("fetchURL"), inReq.getUser(),
						sourcepath, (String) vals.get("importfilename"), id);
				
			}

		
			if (asset == null) {
				asset = new BaseAsset(archive);// Empty Record
				asset.setId(id);
				asset.setProperty("sourcepath", sourcepath);
				asset.setProperty("assetaddeddate", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
			}

			populateJsonData(asstdata, searcher, asset);

			importer.saveAsset(archive, inReq.getUser(), asset);

			// JSONObject result = getAssetJson(sm, searcher, asset);
			// String jsondata = result.toString();
			inReq.putPageValue("searcher", searcher);
			inReq.putPageValue("asset", asset);
			inReq.putPageValue("data", asset);
			
			
			
		}
		
		
		
		


	}
	

	
	public void createCategoryTree(WebPageRequest inReq) {
		JSONObject object = new JSONObject();
		
		
		MediaArchive archive = getMediaArchive(inReq);
		Map request = inReq.getJsonRequest();
		
		String categoryid = (String) request.get("categoryid");
		if(categoryid == null) {
			categoryid = "index";
		}
		Category root = archive.getCategory(categoryid);
		
		populateCategoryJson(object, root);
		String jsondata = object.toString();
		inReq.putPageValue("json", jsondata);
		
		
	}

	protected void populateCategoryJson(JSONObject inObject, Category inRoot) {
		if(inRoot.hasChildren()) {
			JSONArray children = new JSONArray();
			inObject.put("children", children);

			for (Iterator iterator = inRoot.getChildren().iterator(); iterator.hasNext();) {
				Category child = (Category) iterator.next();
				JSONObject nextchild = new JSONObject();
				children.add(nextchild);
				populateCategoryJson(nextchild, child);
			}
			
		}
		inObject.put("id", inRoot.getId());
		inObject.put("name", inRoot.getName());
		inObject.put("categorypath", inRoot.getCategoryPath());
		
		
		
	}
	
	

}