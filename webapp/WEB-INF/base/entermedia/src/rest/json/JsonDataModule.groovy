package rest.json

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive

import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.util.PathUtilities


public class JsonDataModule extends BaseJsonModule 
{
	private static final Log log = LogFactory.getLog(JsonDataModule.class);

	public void handleSearch(WebPageRequest inReq)
	{
	
		//Could probably handle this generically, but I think they want tags, keywords etc.

		SearcherManager sm = inReq.getPageValue("searcherManager");

		String catalogid =  findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);
		def request = inReq.getJsonRequest();

		String searchtype = resolveSearchType(inReq);
		Searcher searcher = archive.getSearcher(searchtype);

		ArrayList <String> fields = new ArrayList();
		ArrayList <String> operations = new ArrayList();
		
		request.query.terms.each
		{
			fields.add(it.field);
			operations.add(it.operator.toLowerCase());
			if( it.values != null)
			{
				String[] values = it.values.toArray(new String[it.values.size()]);
				inReq.setRequestParameter(it.field + ".values", values);
			}
			else
			{
				inReq.setRequestParameter(it.field + ".value", it.value);
			}
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
		
		if( "true" == request.showfilters)
		{
			List nodes = hits.getFilterOptions();
			inReq.putPageValue("filteroptions", nodes);
		}
		
		inReq.putPageValue("searcher", searcher);

	}
	
	public void createData(WebPageRequest inReq)
	{
	
		SearcherManager sm = inReq.getPageValue("searcherManager");
		Map request = inReq.getJsonRequest();
		String catalogid =  findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);
		String searchtype = resolveSearchType(inReq);
		Searcher searcher = archive.getSearcher(searchtype);
		Data newdata = searcher.createNewData();
		
		if( request )
		{
			String id = request.id;
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