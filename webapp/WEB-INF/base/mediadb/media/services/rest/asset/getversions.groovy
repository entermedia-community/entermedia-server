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
	
	String folder = asset.getSourcePath();
	String root= "/WEB-INF/data/" + mediaArchive.getCatalogId() + "/originals/" + folder;
	
	Collection tracker = pageManager.getChildrenPaths(root);
	
	List allrevisions = new ArrayList();
	for( String path : tracker )
	{
		List revisions = pageManager.getRepository().getVersions( path );
		allrevisions.addAll(revisions);
	}
	context.putPageValue("allrevisions",allrevisions);
	log.info("done");
}

init();