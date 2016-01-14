package org.entermediadb.asset.mediadb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
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

	public void handleSearch(WebPageRequest inReq)
	{
	
		//Could probably handle this generically, but I think they want tags, keywords etc.

		SearcherManager sm = (SearcherManager)inReq.getPageValue("searcherManager");

		String catalogid =  findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);
		Map request = inReq.getJsonRequest();

		String searchtype = resolveSearchType(inReq);
		Searcher searcher = archive.getSearcher(searchtype);

		ArrayList <String> fields = new ArrayList();
		ArrayList <String> operations = new ArrayList();
		
		Map query = (Map)request.get("query");
		Collection terms = (Collection)query.get("terms");
		
		for (Iterator iterator = terms.iterator(); iterator.hasNext();)
		{
			Map it = (Map)iterator.next();
			fields.add((String)it.get("field"));
			String opr = (String)it.get("operator");
			operations.add(opr.toLowerCase());
			Collection values = (Collection)it.get("values");
			if( values != null)
			{
				String[] svalues = (String[])values.toArray(new String[values.size()]);
				inReq.setRequestParameter(it.get("field")+ ".values", svalues);
			}
			else if( it.get("value") != null)
			{
				inReq.setRequestParameter(it.get("field") + ".value", (String)it.get("value"));
			}
		}

		String[] fieldarray = fields.toArray(new String[fields.size()]);
		String[] opsarray = operations.toArray(new String[operations.size()]);

		inReq.setRequestParameter("field", fieldarray);
		inReq.setRequestParameter("operation", opsarray);

		SearchQuery squery = searcher.addStandardSearchTerms(inReq);

		HitTracker hits = searcher.cachedSearch(inReq, squery);
		String hitsperpage = (String)request.get("hitsperpage");
		
		if (hitsperpage != null)
		{
			int pagesnum = Integer.parseInt(hitsperpage);
			hits.setHitsPerPage(pagesnum);
		}
		
		String page = (String)request.get("page");
		
		if(page != null)
		{
			int pagenumb = Integer.parseInt(page);
			hits.setPage(pagenumb);
		}
		
		if( "true".equals( request.get("showfilters") ) )
		{
			List nodes = hits.getFilterOptions();
			inReq.putPageValue("filteroptions", nodes);
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
			String sourcepath = request.sourcepath;
			newdata.setId(id);
			newdata.setProperty("sourcepath", sourcepath);
			request.each
			{
				String key = it.key;
				String value = it.value;
				newdata.setProperty(key, value);
			}
		}


		searcher.saveData(newdata, inReq.getUser());
		
	
		inReq.putPageValue("searcher", searcher);
		inReq.putPageValue("data", newdata);
		

	}
	
	public Data loadData(WebPageRequest inReq)
	{

		SearcherManager sm = inReq.getPageValue("searcherManager");

		String catalogid =  findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);

		String searchtype = resolveSearchType(inReq);
		Searcher searcher = archive.getSearcher(searchtype);
		String id = getId(inReq);

		log.info("JSON get with ${id} and ${catalogid}");
		

		Data data = searcher.searchById(id);

		if(data == null)
		{
			//throw new OpenEditException("Asset was not found!");
			return;
		}
		
		inReq.putPageValue("data", data);
		inReq.putPageValue("searcher", searcher);

		return data;

	}
	
	
	public void deleteData(WebPageRequest inReq)
	{

		SearcherManager sm = inReq.getPageValue("searcherManager");

		String catalogid =  findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);

		String searchtype = resolveSearchType(inReq);
		Searcher searcher = archive.getSearcher(searchtype);
		String id = getId(inReq);

		log.info("JSON get with ${id} and ${catalogid}");
		

		Data data = searcher.searchById(id);

		if(data == null)
		{
			//throw new OpenEditException("Asset was not found!");
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
			String url = inReq.getPath();
			if(!url.endsWith("/"))
			{
				url = url + "/";
			}
			String ending = url.substring(root.length(), url.length());
			searchtype = ending.substring(1, ending.indexOf("/",1));
			searchtype = PathUtilities.extractPageName(searchtype);
		}
		return searchtype;
	}
	
	public void updateData(WebPageRequest inReq)
	{
	
		SearcherManager sm = inReq.getPageValue("searcherManager");
		def request = inReq.getJsonRequest();
		String catalogid =  findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);
		String searchtype = resolveSearchType(inReq);
		Searcher searcher = archive.getSearcher(searchtype);
		
		Data newdata = loadData(inReq);
		if(newdata)
		{
			saveJsonData(request,searcher,newdata);
			searcher.saveData(newdata, inReq.getUser());
			inReq.putPageValue("searcher", searcher);
			inReq.putPageValue("data", newdata);
		}

	}
	

	
	
}