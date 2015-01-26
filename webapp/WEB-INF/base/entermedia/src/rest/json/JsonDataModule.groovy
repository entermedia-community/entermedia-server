package rest.json

import groovy.json.JsonSlurper

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive

import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery


public class JsonDataModule extends BaseJsonModule 
{
	private static final Log log = LogFactory.getLog(JsonDataModule.class);

	public void handleSearch(WebPageRequest inReq)
	{
	
		//Could probably handle this generically, but I think they want tags, keywords etc.

		SearcherManager sm = inReq.getPageValue("searcherManager");

		String catalogid =  findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);
		
		JsonSlurper slurper = new JsonSlurper();
		def request = null;
		String content = inReq.getPageValue("jsondata");
		
		if(content != null)
		{
			//TODO: do we want to do something here? 
		}
		else
		{
			request = slurper.parse(inReq.getRequest().getReader()); //this is real, the other way is just for testing
		}
		
		String searchtype = request.searchtype;
		if(searchtype == null){
			searchtype = inReq.findValue("searchtype");
		}
		Searcher searcher = archive.getSearcher(searchtype);

		ArrayList <String> fields = new ArrayList();
		ArrayList <String> operations = new ArrayList();
		
		request.query.each
		{
			fields.add(it.field);
			operations.add(it.operator.toLowerCase());
			StringBuffer values = new StringBuffer();
			it.values.each{
				values.append(it);
				values.append(" ");
			}
			String finals = values.toString().trim();
			inReq.setRequestParameter(it.field + ".value", finals);
		}

		String[] fieldarray = fields.toArray(new String[fields.size()]) as String[];
		String[] opsarray = operations.toArray(new String[operations.size()]) as String[];

		inReq.setRequestParameter("field", fieldarray);
		inReq.setRequestParameter("operation", opsarray);

		SearchQuery query = searcher.addStandardSearchTerms(inReq);

		HitTracker hits = searcher.cachedSearch(inReq, query);
		String hitsperpage = request.hitsperpage;
		
		if (hitsperpage != null)
		{
			int pagesnum = Integer.parseInt(hitsperpage);
			hits.setHitsPerPage(pagesnum);
		}
		
		String page = request.page;
		
		if(page != null)
		{
			int pagenumb = Integer.parseInt(page);
			hits.setPage(pagenumb);
		}
		
		inReq.putPageValue("searcher", searcher);

	}
	
	
	
	public void createData(WebPageRequest inReq)
	{
	
		SearcherManager sm = inReq.getPageValue("searcherManager");
		Map request = inReq.getJsonRequest();
		String catalogid =  findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);
		String searchtype = resolveSearchType(inReq, request);
		Searcher searcher = archive.getSearcher(searchtype);
		
		String id = request.id;
		String sourcepath = request.sourcepath;
		Data newdata = searcher.createNewData();
		newdata.setId(id);
		newdata.setProperty("sourcepath", sourcepath);
		request.each
		{
			String key = it.key;
			String value = it.value;
			newdata.setProperty(key, value);
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
		//inReq.putPageValue("data", data);
		//inReq.putPageValue("searcher", searcher);

		

	}
	
	public String resolveSearchType(WebPageRequest inReq, Map inJson)
	{
		String	searchtype = inReq.findValue("searchtype");
		if(searchtype == null)
		{
			if( inJson != null)
			{
				searchtype = inJson.searchtype;
			}
			String path = inReq.getPath();
			if(searchtype == null && path.contains("modules/default/data/") )
			{
				int  start  = path.indexOf( "modules/default/data/") + "modules/default/data/".length();
				if(!path.endsWith("/"))
				{
					path = path + "/";
				}
				String id = path.substring(start, path.length())
				searchtype = id.substring(0, id.indexOf("/"));
			}
		}
		return searchtype;
	}
	
	public String resolveSearchType(WebPageRequest inReq)
	{
//		def request = null;
//		request = inReq.getJsonRequest();
//		String searchtype = null;
//		if(request){
//			searchtype = request.searchtype;
//		}
		//if(searchtype == null){
		String	searchtype = inReq.findValue("searchtype");
		//}
		
		if(searchtype == null){
			String root  = "/mediadb/services/modules/default/data/";
			String url = inReq.getPath();
			if(!url.endsWith("/"))
			{
				url = url + "/";
			}
			String id = url.substring(root.length(), url.length())
			id = id.substring(0, id.indexOf("/"));
			return id;
		}
		return searchtype;
	}
	
	public void updateData(WebPageRequest inReq)
	{
	
		SearcherManager sm = inReq.getPageValue("searcherManager");
		def request = inReq.getJsonRequest();
		String catalogid =  findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);
		String searchtype = resolveSearchType(inReq, request);
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