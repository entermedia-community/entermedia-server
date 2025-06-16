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
	
	String assetid = context.getRequestParameter("assetid");
	String presetid = context.getRequestParameter("presetid");	

	//by rendertype: image, video...
	
	Asset asset = archive.getAsset(assetid);
	context.putPageValue("asset", asset);
	
	String rendertype = archive.getMediaRenderType(asset);
	String previewpath = "/"+ applicationid + "/services/module/asset/players/webplayer/type";
	previewpath = previewpath + "/" + rendertype + ".html";
	
	Data preset = archive.getCachedData("convertpreset", presetid);
	context.putPageValue("preset", preset);
	
	if (asset == null || preset == null)
	{
		log.info("Missing asset or preset");
		context.putPageValue("status", "error");
		return;
	}
	
	context.putPageValue("searcher", archive.getAssetSearcher());
	
	Page preview = pageManager.getPage(previewpath);
	
	WebPageRequest newcontext = context.copy(preview);
	
	StringWriter output = new StringWriter();
	
	preview.generate(newcontext, output);
	
	context.putPageValue("renderedhtml", output.toString()); 
	
	//log.info(output.toString());
	
	log.info("Output generated");
	
	context.putPageValue("status", "ok");
}


init();


