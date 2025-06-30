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
	
	String moduleid = context.getPageValue("module");
	
	String playertype = context.getRequestParameter("playertype");
	
	String previewpath = "/"+ applicationid + "/services/module/"+module.getId()+"/players";
	previewpath = previewpath  + "/" + playertype + "/index.html";
	
	
	Page preview = pageManager.getPage(previewpath);
	WebPageRequest newcontext = context.copy(preview);
	
	StringWriter output = new StringWriter();
	preview.generate(newcontext, output);
	context.putPageValue("output", output.toString()); 
	
	//log.info(output.toString());
	log.info("Output generated");
}


init();


