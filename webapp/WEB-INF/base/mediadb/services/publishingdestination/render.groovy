package modules;

import org.openedit.Data
import org.openedit.data.*
import org.entermediadb.asset.*
import org.openedit.hittracker.SearchQuery
import org.openedit.hittracker.HitTracker
import org.openedit.page.Page
import org.openedit.WebPageRequest




public void init(){

	String applicationid = context.findPathValue("applicationid");
	MediaArchive archive = context.getPageValue("mediaarchive")
	
	Searcher assetsearcher = archive.getSearcher("asset");
	SearchQuery search = assetsearcher.addStandardSearchTerms(context);
	
	Category category = archive.getBean("entityManager").createDefaultFolder(entity, context.getUser());
	
	if(search == null) {
		search = assetsearcher.createSearchQuery();
	}
	
	search.addExact("category", category.getId());
	//TODO: Add approved only to query
	
	String hitsname = "publishingentityassethits";
	search.setHitsName(hitsname);
		
	
	HitTracker tracker = assetsearcher.search(search);
	tracker.setHitsPerPage(25);
	context.putPageValue(hitsname,tracker);
	
	Page preview = pageManager.getPage("/"+ applicationid + "/services/publishingdestination/preview.html");
	
	WebPageRequest newcontext = context.copy(preview);
	
	StringWriter output = new StringWriter();
	
	preview.generate(newcontext, output);
	
	context.putPageValue("output", output.toString()); 
	
	log.info(output.toString());
}


init();


