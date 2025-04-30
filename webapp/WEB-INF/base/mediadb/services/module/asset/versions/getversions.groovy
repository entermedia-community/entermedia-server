import org.entermediadb.asset.attachments.AttachmentManager
import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive

import org.openedit.hittracker.HitTracker


public void init()
{
	//pass in the asset id
	
	Asset asset = context.getPageValue("asset");
	AttachmentManager attachmentManager =  context.getPageValue("attachmentManager");
	MediaArchive mediaArchive =  context.getPageValue("mediaarchive");
	if (!asset){
		String assetid = context.getRequestParameter("assetid") ? context.getRequestParameter("assetid") : context.getRequestParameter("id");
		asset = mediaArchive.getAsset(assetid);
	}
	if (asset == null) {
		log.info("Asset not found");
		return;
	}
	String folder = asset.getSourcePath();
	String catalogid = context.findValue("catalogid");
	if (!catalogid){
		catalogid = mediaArchive.getCatalogId();
	}
	String root= "/WEB-INF/data/" + catalogid + "/originals/" + folder;
	
	Collection tracker = pageManager.getChildrenPaths(root);
	
	List allrevisions = new ArrayList();
	for( String path : tracker )
	{
		List revisions = pageManager.getRepository().getVersions( path );
		allrevisions.addAll(revisions);
	}
	context.putPageValue("allrevisions",allrevisions);
	context.putPageValue("hits",allrevisions);
	
	log.info("done");
}

init();