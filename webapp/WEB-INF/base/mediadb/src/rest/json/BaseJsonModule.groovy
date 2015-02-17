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


public class BaseJsonModule extends BaseMediaModule 
{
	private static final Log log = LogFactory.getLog(BaseJsonModule.class);
	
	public void allowHeaders(WebPageRequest inReq)
	{
		inReq.getResponse().setHeader("Access-Control-Allow-Origin","*");
	}
	 
	public void preprocess(WebPageRequest inReq)
	{
		
		inReq.getJsonRequest();
	}


	public String getId(WebPageRequest inReq)
	{
		String id = inReq.getRequestParameter("id"); 
		if( id == null)
		{
			id = inReq.getPage().getName();
			if (id.endsWith(".json"))
			{
				id = id.substring(0, id.length()-5);	
			}
		}
		return id;
	}

	public String findCatalogId(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		if(catalogid == null)
		{
			if(inReq.getRequest())
			{
				catalogid = inReq.getRequest().getHeader("catalogid");
			}
		}
		
		return catalogid;
	}
	
	public void populateJsonObject(Searcher inSearcher, JSONObject inObject, Data inData)
	{
		inSearcher.getPropertyDetails().each
		{
			PropertyDetail detail = it;
			String key = it.id;
			String value=inData.get(it.id);
			if(key && value)
			{
				if(detail.isList())
				{

					inObject.put(key, value);

				}
				else if(detail.isBoolean())
				{
					inObject.put(key, Boolean.parseBoolean(value));


				} else {
					inObject.put(key, value);
				}


			}

		}
	}

	public JSONObject getOrderJson(SearcherManager sm, Searcher inSearcher, Data inOrder)
	{

		JSONObject asset = new JSONObject();

		populateJsonObject(inSearcher, asset,inOrder);
		//need to add tags and categories, etc
		//String tags = inAsset.get("keywords");
		Searcher itemsearcher = sm.getSearcher(inSearcher.getCatalogId(),"orderitem" );
		HitTracker items = itemsearcher.query().match("orderid", inOrder.getId()).search();

		JSONArray array = new JSONArray();
		items.each
		{

			JSONObject item = new JSONObject();
			populateJsonObject(itemsearcher, item,it);
			array.add(item);
		}
		asset.put("items", array);



		return asset;
	}

	public MediaArchive getMediaArchive(WebPageRequest inReq,  String inCatalogid)
	{
		allowHeaders(inReq);
		
		SearcherManager sm = inReq.getPageValue("searcherManager");

		if (inCatalogid == null)
		{
			return null;
		}
		MediaArchive archive = (MediaArchive) sm.getModuleManager().getBean(inCatalogid, "mediaArchive");
		return archive;
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
	public JSONObject getDataJson(SearcherManager sm, Searcher inSearcher, Data inData)
	{
		JSONObject asset = new JSONObject();
		asset.put("id", inData.getId());
		asset.put("name", inData.getName());
		inSearcher.getPropertyDetails().each
		{
			PropertyDetail detail = it;
			String key = it.id;
			String value=inData.get(it.id);
			if(key && value)
			{
				if(detail.isList())
				{
					//friendly?
					asset.put(key, value);
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
	
	
	public void saveJsonData(Map inputdata, Searcher searcher, Data inData)
	{
		inputdata.keySet().each
		{
			String key = it;
			Object value = inputdata.get(key);
			if(value instanceof String)
			{
				inData.setProperty(key, value);
			} 
			
			if(value instanceof List)
			{
				Collection ids = new ArrayList();
				PropertyDetail detail = searcher.getDetail(key);
				
				//We have a list full of maps or strings
				value.each
				{
					if( it instanceof String)
					{
						ids.add(it);
					}
					else
					{
						JSONObject object = it;
						String val = it.get("id");
						//log.info("In VALUE: ${val}");
						ids.add(val);
						if(detail != null)
						{
							Searcher rsearcher = searcher.getSearcherManager().getSearcher(searcher.getCatalogId(),key);
							Data remote = rsearcher.searchById(val);
							if(remote == null)
							{
								remote = rsearcher.createNewData();
								remote.setId(val);							
							}
							object.keySet().each
							{
								remote.setProperty(it, object.get(it));
							}
							rsearcher.saveData(remote, inReq.getUser());
						}
					}
				} 
				inData.setValues(key, ids);				
			}
			if(value instanceof Map )
			{
					Map values = value;
					
					PropertyDetail detail = searcher.getDetail(key);
					Searcher rsearcher = searcher.getSearcherManager().getListSearcher(detail);
					String targetid = value.id;
					Data remote = rsearcher.searchById(targetid);
					if(remote == null)
					{
						remote = rsearcher.createNewData();
						remote.setId(targetid);
					}
					values.keySet().each
					{
						Object test = values.get(it);
						if(test instanceof String)
						{
							remote.setProperty(it,test );
						}
					}
					rsearcher.saveData(remote, null);
					inData.setProperty(key, targetid);
			}
			else
			{
				//do something else?
				
			}

		}
	
	}
	
	
}