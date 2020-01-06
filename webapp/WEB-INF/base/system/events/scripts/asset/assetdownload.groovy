package asset;

import org.elasticsearch.search.aggregations.AggregationBuilder
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms
import org.elasticsearch.search.aggregations.metrics.sum.SumBuilder
import org.entermediadb.asset.MediaArchive
import org.openedit.WebPageRequest
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.hittracker.SearchQuery
import java.util.List
import org.openedit.Data
import org.openedit.BaseWebPageRequest
import java.util.ArrayList
import org.entermediadb.elasticsearch.SearchHitData

public void init(){
	log.info("Running assetdownloadLog searchi ");
	WebPageRequest req = context;
        //log.info("*** MAP A ***" + req.getRequest().getParameterMap());
        def idx_lib = -1;
        String[] ar = req.getRequestParameters("field")
        def n_ar = new ArrayList();
        //log.info(n_ar);
        def idx = 0
        for(v in ar) {
            if(v.endsWith("library")) {
              idx_lib = idx
            } else {
              n_ar.add(v)
            }
            idx++
        }
        
        n_ar = n_ar.toArray() as String[]
        //log.info("*** LEN " + n_ar.length + " idx_lib " + idx_lib + " N_AR" + n_ar);
        req.setRequestParameter("field",n_ar) 
        if (idx_lib != -1) {
           ar = req.getRequestParameters("operation")
           n_ar = new ArrayList();
           idx = 0
           for(idx=0; idx < ar.length; idx++) {
              if(idx == idx_lib) {
             
              } else {
                n_ar.add(ar[idx])
              }
           }
           n_ar = n_ar.toArray() as String[]
           req.setRequestParameter("operation",n_ar)
        }
        //log.info("*** MAP B ***" + req.getRequest().getParameterMap());
        String catalogid = context.getPageValue("reportcatalogid");
        String libraryID = context.getParam("library.value");
        String libraryName = null;
        if (libraryID != null) {
            Searcher searcherLib = searcherManager.getSearcher(catalogid, "library"); 
            SearchQuery queryLibId = searcherLib.createSearchQuery();
	    queryLibId.setHitsPerPage(1);
	    queryLibId.addExact("id", libraryID);
	    HitTracker hits = searcherLib.search(queryLibId);
	    Data d = searcherLib.loadData((Data) hits.first());
            if (d != null) {
                libraryName = d.getName();
	    }
         }
        //Searcher searcherLib = searcherManager.getSearcher(catalogid, "library");         
	
	Searcher searcher = searcherManager.getSearcher(catalogid, "assetdownloadLog");//assetsearchLog");
	//searcher.putMappings();//just in case it's never been done.
	
	SearchQuery query = searcher.addStandardSearchTerms(req);
        //log.info("*** assetdownloadLog.groovy query : "+query);
        if (query != null) {
           if (libraryName != null) {
               query.addStartsWith("sourcepath", "Collections/"+libraryName);
           }

           //query.addContains("sourcepath", "Collections/Pixel")
        }
        //log.info("*** assetdownloadLog.groovy query : "+query);
	if(query == null){
		query = searcher.createSearchQuery();
		query.addMatches("id", "*");
		
	}
	AggregationBuilder b = AggregationBuilders.terms("keywords").field("query");//"query"
	query.setAggregation(b);
	HitTracker hits = searcher.search(query);
        
        //HitTracker hitsCopy = hits.copy()
        //log.info("REMOVE: " + hitsCopy.removeSelection("AWljpIhU5GkOnGRbdMAS"));
        //Iterator it = hitsCopy.iterator()
	//while(it.hasNext()) {
        //   SearchHitData x = it.next()
	   //log.info("IT "+x.getProperties());
	//}
        //log.info("**** hits.getSearchResponse " + hits.getSearchResponse(0))
	hits.enableBulkOperations();
	hits.getFilterOptions();
	StringTerms agginfo = hits.getAggregations().get("keywords");
        context.putPageValue("event_breakdown_day", hits)
	
}

init();

