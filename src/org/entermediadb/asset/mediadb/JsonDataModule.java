package org.entermediadb.asset.mediadb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.util.JsonUtil;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.util.PathUtilities;


public class JsonDataModule extends BaseJsonModule 
{
	private static final Log log = LogFactory.getLog(JsonDataModule.class);

	protected JsonUtil fieldJsonUtil;
	
	
	public JsonUtil getJsonUtil()
	{
		if (fieldJsonUtil == null)
		{
			fieldJsonUtil = (JsonUtil)getModuleManager().getBean("jsonUtil");
		}
		return fieldJsonUtil;
	}


	public void setJsonUtil(JsonUtil inJsonUtil)
	{
		fieldJsonUtil = inJsonUtil;
	}


	public void handleSearch(WebPageRequest inReq)
	{
		//Could probably handle this generically, but I think they want tags, keywords etc.

		SearcherManager sm = (SearcherManager)inReq.getPageValue("searcherManager");

		String catalogid =  findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);

		String searchtype = resolveSearchType(inReq);
		Searcher searcher = archive.getSearcher(searchtype);
		HitTracker hits = null;
		if( inReq.getJsonRequest() == null)
		{
			hits = searcher.getAllHits(inReq);
		}
		else
		{
			hits = getJsonUtil().searchByJson(searcher ,inReq);
		}
		
		inReq.putPageValue("searcher", searcher);

	}
	

	public void createData(WebPageRequest inReq)
	{
	
		SearcherManager sm = (SearcherManager)inReq.getPageValue("searcherManager");
		Map request = inReq.getJsonRequest();
		String catalogid =  findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);
		String searchtype = resolveSearchType(inReq);
		Searcher searcher = archive.getSearcher(searchtype);
		Data newdata = searcher.createNewData();
		
		if( request != null)
		{
			String id = (String)request.get("id");
			if(id == null) {
				id = (String)inReq.getPageValue("id");
			}
			String sourcepath = (String) request.get("sourcepath");
			newdata.setProperties(request);
			newdata.setId(id);
			newdata.setProperty("sourcepath", sourcepath);
		}

		searcher.saveData(newdata, inReq.getUser());
	
		inReq.putPageValue("searcher", searcher);
		inReq.putPageValue("data", newdata);

	}
	
	public Data loadData(WebPageRequest inReq)
	{

		String catalogid =  findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);

		String searchtype = resolveSearchType(inReq);
		Searcher searcher = archive.getSearcher(searchtype);
		String id = getId(inReq);

		Data data = (Data) searcher.searchById(id);

		if(data == null)
		{
			//throw new OpenEditException("Asset was not found!");
			return null;
		}
		
		inReq.putPageValue("data", data);
		inReq.putPageValue("searcher", searcher);

		return data;

	}
	
	
	public void deleteData(WebPageRequest inReq)
	{

		String catalogid =  findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);

		String searchtype = resolveSearchType(inReq);
		Searcher searcher = archive.getSearcher(searchtype);
		String id = getId(inReq);

		Data data = (Data) searcher.searchById(id);

		if(data == null)
		{
			//throw new OpenEditException("Asset was not found!");
//			if( inReq.getResponse() != null)
//			{
//				inReq.getResponse().setStatus(404);
//			}
			inReq.setCancelActions(true);
			return;
		}
		searcher.delete(data, inReq.getUser());
		inReq.putPageValue("data", data);
		//inReq.putPageValue("searcher", searcher);

		

	}
	
	public String resolveSearchType(WebPageRequest inReq)
	{
		String	searchtype = inReq.getContentProperty("searchtype");
		if(searchtype == null)
		{
			String root  = inReq.getContentProperty("searchtyperoot");
			if( root != null)
			{
				String url = inReq.getPath();
				if(!url.endsWith("/"))
				{
					url = url + "/";
				}
				String ending = url.substring(root.length(), url.length());
				searchtype = ending.substring(1, ending.indexOf("/",1));
				searchtype = PathUtilities.extractPageName(searchtype);
			}
		}
		if(searchtype == null)
		{
			searchtype = inReq.findValue("searchtype");
		}
		return searchtype;
	}
	
	public void updateData(WebPageRequest inReq)
	{
	
		Map request = inReq.getJsonRequest();
		String catalogid =  findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);
		String searchtype = resolveSearchType(inReq);
		Searcher searcher = archive.getSearcher(searchtype);
		
		Data newdata = loadData(inReq);
		if(newdata != null)
		{
			populateJsonData(request,searcher,newdata);
			searcher.saveData(newdata, inReq.getUser());
			inReq.putPageValue("searcher", searcher);
			inReq.putPageValue("data", newdata);
		}

	}
	
	public  void getUUID(WebPageRequest inReq) {
		Map request = inReq.getJsonRequest();
		String id = (String)request.get("id");
		if(id == null) {
			 id = UUID.randomUUID().toString();
			 inReq.putPageValue("id", id);
		}
		
		
	}
	
	
}