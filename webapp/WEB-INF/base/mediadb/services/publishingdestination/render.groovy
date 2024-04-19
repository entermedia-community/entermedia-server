package modules;

import org.openedit.Data
import org.openedit.data.*
import org.openedit.page.Page
import org.openedit.WebPageRequest

import org.openedit.hittracker.HitTracker


public void init(){

	String applicationid = context.findPathValue("applicationid");
	
	Page preview = pageManager.getPage("/"+ applicationid + "/services/publishingdestination/preview.html");
	
	WebPageRequest newcontext = context.copy(preview);
	
	StringWriter output = new StringWriter();
	
	preview.generate(newcontext, output);
	
	context.putPageValue("output", output.toString()); 
	
	log.info(output.toString());
}


init();


