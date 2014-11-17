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
	
	
}