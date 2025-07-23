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
	
	//String previewpath = "/"+ applicationid + "/services/publishingdestination";
	String previewpath = "/"+ applicationid + "/services/module/default/players";  //Moved to Module Players
	
	String distributiontype = context.getPageValue("distributiontype");
	if(distributiontype.equals("gallery")) {
		previewpath = previewpath  + "/gallery/index.html";
	}
	else 
	{
		previewpath = previewpath  + "/carousel/index.html";
	}
	
	Page preview = pageManager.getPage(previewpath);
	
	WebPageRequest newcontext = context.copy(preview);
	
	StringWriter output = new StringWriter();
	
	preview.generate(newcontext, output);
	
	context.putPageValue("output", output.toString()); 
	
	//log.info(output.toString());
	
	log.info("Output generated");
}


init();


