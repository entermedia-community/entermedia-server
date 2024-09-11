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
	MediaArchive archive = context.getPageValue("mediaarchive");
	
	
	
	Page preview = pageManager.getPage("/"+ applicationid + "/services/publishingdestination/caurosel/index.html");
	
	WebPageRequest newcontext = context.copy(preview);
	
	StringWriter output = new StringWriter();
	
	preview.generate(newcontext, output);
	
	context.putPageValue("output", output.toString()); 
	
	//log.info(output.toString());
	
	log.info("Output generated");
}


init();


