import org.entermedia.attachments.AttachmentManager
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive

import com.openedit.hittracker.HitTracker


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