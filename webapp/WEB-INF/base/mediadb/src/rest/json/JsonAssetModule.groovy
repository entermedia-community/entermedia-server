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
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.creator.ConversionUtil
import org.openedit.entermedia.orders.Order
import org.openedit.entermedia.orders.OrderManager
import org.openedit.entermedia.orders.OrderSearcher
import org.openedit.entermedia.scanner.AssetImporter
import org.openedit.entermedia.search.AssetSearcher

import com.openedit.OpenEditException
import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.page.Page


public class JsonAssetModule extends BaseJsonModule 
{
	private static final Log log = LogFactory.getLog(JsonAssetModule.class);

	/*
	public JSONObject handleAssetSearch(WebPageRequest inReq)
	{
		//Could probably handle this generically, but I think they want tags, keywords etc.

		SearcherManager sm = inReq.getPageValue("searcherManager");

		String catalogid =  findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);
		AssetSearcher searcher = sm.getSearcher(catalogid,"asset" );

		def	request = inReq.getJsonRequest(); //this is real, the other way is just for testing

		ArrayList <String> fields = new ArrayList();
		ArrayList <String> operations = new ArrayList();

		request.query.terms.each
		{
			fields.add(it.field);
			operations.add(it.operator.toLowerCase());
			StringBuffer values = new StringBuffer();
			it.values.each{
				values.append(it);
				values.append(" ");
			}
			inReq.setRequestParameter(it.field + ".value", values.toString());
		}

		String[] fieldarray = fields.toArray(new String[fields.size()]) as String[];
		String[] opsarray = operations.toArray(new String[operations.size()]) as String[];

		inReq.setRequestParameter("field", fieldarray);
		inReq.setRequestParameter("operation", opsarray);
		inReq.setRequestParameter("hitsperpage", String.valueOf(request.hitsperpage));

		SearchQuery query = searcher.addStandardSearchTerms(inReq);
		HitTracker hits = searcher.cachedSearch(inReq, query);
//		String hitsperpage = request.hitsperpage;
//		if (hitsperpage != null)
//		{
//			int pagesnum = Integer.parseInt(hitsperpage);
//			hits.setHitsPerPage(pagesnum);
//		}
		String page = request.page;
		if(page != null)
		{
			int pagenumb = Integer.parseInt(page);
			hits.setPage(pagenumb);
		}
		JSONObject parent = new JSONObject();
		parent.put("size", hits.size());

		JSONArray array = new JSONArray();
		hits.getPageOfHits().each
		{
			JSONObject hit = getAssetJson(sm,searcher, it);

			array.add(hit);
		}


		parent.put("results", array);
		inReq.putPageValue("json", parent.toString());
		return parent;
	}

*/

	public void createAsset(WebPageRequest inReq)
	{
		SearcherManager sm = inReq.getPageValue("searcherManager");

		String catalogid =  findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);
		AssetSearcher searcher = sm.getSearcher(catalogid,"asset" );
		//We will need to handle this differently depending on whether or not this asset has a real file attached to it.
		//if it does, we should move it and use the asset importer to create it so metadata gets read, etc.

		FileUpload command = archive.getSearcherManager().getModuleManager().getBean("fileUpload");
		UploadRequest properties = command.parseArguments(inReq);

		def request = inReq.getJsonRequest(); 

		AssetImporter importer = archive.getAssetImporter();
		HashMap keys = new HashMap();
		SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");
		String df = format.format(new Date());
		keys.put("formatteddate", df);

		format = new SimpleDateFormat("yyyy/MM");
		df = format.format(new Date());
		keys.put("formattedmonth", df);

		String id = request.id;
		if(id == null)
		{
			id = searcher.nextAssetNumber()
		}
		keys.put("id",id);		
		keys.put("guid",UUID.randomUUID().toString() );

		
		String sourcepath = keys.get("sourcepath");

		if(sourcepath == null)
		{
			sourcepath = archive.getCatalogSettingValue("catalogassetupload");  //${division.uploadpath}/${user.userName}/${formateddate}
		}
		if(sourcepath == null || sourcepath.length() == 0)
		{
			sourcepath = "receivedfiles/${id}";
		}
		sourcepath = sm.getValue(catalogid, sourcepath, keys);
		Asset asset = null;

		if(properties.getFirstItem() != null)
		{
			String path = "/WEB-INF/data/" + archive.getCatalogId()	+ "/originals/" + sourcepath + "/${properties.getFirstItem().getName()}";
			properties.saveFileAs(properties.getFirstItem(), path, inReq.getUser());
			Page newfile = archive.getPageManager().getPage(path);
			asset = importer.createAssetFromPage(archive, inReq.getUser(), newfile);
		}


		if(asset == null && keys.get("fetchURL") != null)
		{
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
				if(file.renameTo(realpath))
				{
					asset = importer.createAssetFromPage(archive, inReq.getUser(), newfile);
				}
				else
				{
					throw new OpenEditException("Error moving file: " + realpath);
				}
			}
		}
		if(asset == null)
		{
			asset = new Asset();//Empty Record
			asset.setId(id);
			asset.setProperty("sourcepath", sourcepath);
		}
		
		saveJsonData(request,searcher,asset);
		
		searcher.saveData(asset, inReq.getUser());
		
		//JSONObject result = getAssetJson(sm, searcher, asset);
		//String jsondata = result.toString();
		inReq.putPageValue("searcher", searcher);
		inReq.putPageValue("asset", asset);
		inReq.putPageValue("data", asset);
		//inReq.putPageValue("json", jsondata);
		//return result;

	}

	public void updateAsset(WebPageRequest inReq)
	{

		SearcherManager sm = inReq.getPageValue("searcherManager");

		JSONObject inputdata  = inReq.getJsonRequest(); 


		String catalogid =  findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);


		AssetSearcher searcher = sm.getSearcher(catalogid,"asset" );
		//We will need to handle this differently depending on whether or not this asset has a real file attached to it.
		//if it does, we should move it and use the asset importer to create it so metadata gets read, etc.

		String id = getId(inReq);
		if(id == null)
		{
			return;
		}
		
		Asset asset = archive.getAsset(id);
		
		if(asset == null)
		{
				return;
		}
		saveJsonData(inputdata,searcher,asset);
						
		searcher.saveData(asset, inReq.getUser());
		archive.fireMediaEvent("asset/assetedited", inReq.getUser(), asset)
		
		inReq.putPageValue("asset", asset);
		inReq.putPageValue("data", asset);
		inReq.putPageValue("searcher", searcher);
		//return result;

	}

/*
	public void loadAsset(WebPageRequest inReq)
	{

		SearcherManager sm = inReq.getPageValue("searcherManager");

		String catalogid =  findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);


		AssetSearcher searcher = sm.getSearcher(catalogid,"asset" );
		//We will need to handle this differently depending on whether or not this asset has a real file attached to it.
		//if it does, we should move it and use the asset importer to create it so metadata gets read, etc.
		String id = getId(inReq);

		log.info("JSON get with ${id} and ${catalogid}");
		

		Asset asset = archive.getAsset(id);

		if(asset == null)
		{
			//throw new OpenEditException("Asset was not found!");
			return;
		}
		
		inReq.putPageValue("asset", asset);
		inReq.putPageValue("searcher", searcher);

		

	}
	*/
/*
	public void deleteAsset(WebPageRequest inReq)
	{
	
		JsonSlurper slurper = new JsonSlurper();

		SearcherManager sm = inReq.getPageValue("searcherManager");
		String catalogid = findCatalogId(inReq);

		MediaArchive archive = getMediaArchive(inReq, catalogid);
		AssetSearcher searcher = sm.getSearcher(catalogid,"asset" );
		//We will need to handle this differently depending on whether or not this asset has a real file attached to it.
		//if it does, we should move it and use the asset importer to create it so metadata gets read, etc.
		String id = getId(inReq); //TODO: Handle multiple deletes?

		Asset asset = archive.getAsset(id);

		int counted = 0;
		if(asset != null)
		{
			searcher.delete(asset, null);
			counted++;
		}
		inReq.putPageValue("id", id);
		inReq.putPageValue("asset", asset);
		inReq.putPageValue("data", asset);
		inReq.putPageValue("searcher", searcher);
		inReq.putPageValue("deleted", counted);
		

	}

	public void getAssetJson(SearcherManager sm, Searcher inSearcher, Data inAsset)
	{

		JSONObject asset = new JSONObject();
		inSearcher.getPropertyDetails().each
		{
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


				}
				else
				{
					asset.put(key, value);
				}
			}
			//need to add tags and categories, etc
		}
		//return asset;
	}
*/

	public JSONObject getAssetPublishLocations(MediaArchive inArchive, Data inAsset)
	{

		JSONObject asset = new JSONObject();
		Searcher publish = inArchive.getSearcher("publishqueue");
		return asset;
	}

	public JSONObject getConversions(MediaArchive inArchive, Asset inAsset)
	{

		JSONObject asset = new JSONObject();
		Searcher publish = inArchive.getSearcher("conversiontask");

		JSONArray array = new JSONArray();
		ConversionUtil util = new ConversionUtil();
		util.setSearcherManager(inArchive.getSearcherManager());

		String origurl = "/views/modules/asset/downloads/originals/${inAsset.getSourcePath()}/${inAsset.getMediaName()}";
		
		JSONObject original = new JSONObject();
		original.put("URL", origurl);
		
		asset.put("original", original);
		
		HitTracker conversions = util.getActivePresetList(inArchive.getCatalogId(),inAsset.get("assettype"));
		conversions.each
		{
			if(util.doesExist(inArchive.getCatalogId(), inAsset.getId(), inAsset.getSourcePath(), it.id))
			{
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
		return asset;
	}
	
	public JSONObject createOrder(WebPageRequest inReq)
	{
	
		log.info("starting to handle create order request");
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

		if(content != null)
		{
			request = slurper.parseText(content); //NOTE:  This is for unit tests.
		}
		else
		{
			request = slurper.parse(inReq.getRequest().getReader()); //this is real, the other way is just for testing
		}



		String publishdestination = request.publishdestination;
		Order order = ordersearcher.createNewData();
		order.setProperty("publishdestination", publishdestination);
		order.setId(ordersearcher.nextId());

		ordersearcher.saveData(order, null);

		request.items.each
		{
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
		inReq.putPageValue("json", jsondata);
		return result;

	}

}