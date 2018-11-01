package org.entermediadb.asset.mediadb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.PropertyDetail;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.HitTracker;
import org.openedit.modules.translations.LanguageMap;




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
			if(inReq.getRequest() != null)
			{
				catalogid = inReq.getRequest().getHeader("catalogid");
			}
		}
		
		return catalogid;
	}
	
	public void populateJsonObject(Searcher inSearcher, JSONObject inObject, Data inData)
	{
		for (Iterator iterator = inSearcher.getPropertyDetails().iterator(); iterator.hasNext();)
		{
			PropertyDetail detail = (PropertyDetail) iterator.next();
			String key = detail.getId();
			String value = inData.get(detail.getId());
			if(key !=null && value != null)
			{
				if(detail.isList())
				{
					inObject.put(key, value);
				}
				else if(detail.isBoolean())
				{
					inObject.put(key, Boolean.parseBoolean(value));
				}
				else 
				{
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
		for (Iterator iterator = items.iterator(); iterator.hasNext();)
		{
			Data it = (Data)iterator.next();
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
		
		SearcherManager sm = (SearcherManager)inReq.getPageValue("searcherManager");

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
		Data data = (Data)searcher.searchById(inId);
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
		for (Iterator iterator = inSearcher.getPropertyDetails().iterator(); iterator.hasNext();)
		{
			PropertyDetail detail = (PropertyDetail) iterator.next();
			String key = detail.getId();
			String value = inData.get(key);
			if(key != null && value != null)
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
	
	
	public void populateJsonData(Map inputdata, Searcher searcher, Data inData)
	{
		for (Iterator iterator = inputdata.keySet().iterator(); iterator.hasNext();)
		{
			String key = (String) iterator.next();
			if("categorypath".equalsIgnoreCase(key)) {
				continue;
			}
			Object value = inputdata.get(key);
			log.info("Got " + inputdata + " from JSON");
			
			PropertyDetail detail = searcher.getDetail(key);
			if( detail != null && detail.isMultiLanguage())
			{
				LanguageMap map = null;
				Object oldval = inData.getValue(detail.getId());
				if(oldval != null){
					if(oldval instanceof LanguageMap){
						map = (LanguageMap) oldval;										
					} else{
						map = new LanguageMap();
						map.setText("en",(String) oldval);
					}
				}
				if (map == null)
				{
					map = new LanguageMap();
				}
				if( value != null && key.contains("."))
				{
					String lang = key.substring(key.indexOf( ".") + 1);
					map.setText(lang,String.valueOf( value) );
				}
				inData.setValue(detail.getId(), map);
			}
			else if(value instanceof String)
			{
				inData.setProperty(key, (String)value);
			} 
			else if(value instanceof Collection)
			{
				Collection ids = new ArrayList();
				Collection values = (Collection)value;
				
				//We have a list full of maps or strings
				for (Iterator iterator2 = values.iterator(); iterator2.hasNext();)
				{
					Object it = (Object) iterator2.next();
					if( it instanceof String)
					{
						ids.add(it);
					}
					else
					{
						Map object = (Map)it;
						String val = (String)object.get("id");
						//log.info("In VALUE: ${val}");
						ids.add(val);
						if(detail != null)
						{
							Searcher rsearcher = searcher.getSearcherManager().getSearcher(searcher.getCatalogId(),key);
							Data remote = (Data)rsearcher.searchById(val);
							if(remote == null)
							{
								remote = rsearcher.createNewData();
								remote.setId(val);							
							}
							for (Iterator iterator3 = object.keySet().iterator(); iterator3.hasNext();)
							{
								String it2 = (String) iterator3.next();
								remote.setProperty(it2, (String)object.get(it2));
							}
							rsearcher.saveData(remote, null);
						}
					}
				} 
				inData.setValue(key, ids);				
			}
			else if(value instanceof Map )
			{
					Map values = (Map)value;
					
					Searcher rsearcher = searcher.getSearcherManager().getListSearcher(detail);
					String targetid = (String)values.get("id");
					Data remote = (Data)rsearcher.searchById(targetid);
					if(remote == null)
					{
						remote = rsearcher.createNewData();
						remote.setId(targetid);
					}
					for (Iterator iterator2 = values.keySet().iterator(); iterator2.hasNext();)
					{
						String it = (String) iterator2.next();
						Object test = values.get(it);
						if(test instanceof String)
						{
							remote.setProperty(it,(String)test );
						}
					}
					rsearcher.saveData(remote, null);
					inData.setProperty(key, targetid);
			}
			
			else
			{
				inData.setValue(key, value);
			}

		}
	
	}
	
	
}