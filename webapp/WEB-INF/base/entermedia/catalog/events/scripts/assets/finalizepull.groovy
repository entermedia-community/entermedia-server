
import org.openedit.Data
import org.openedit.data.*
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.*
import org.openedit.event.*

import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.Page

import org.openedit.entermedia.Category;


public init(){
	log.info("Starting Finalize Pull Event");
	MediaArchive mediaArchive = (MediaArchive)context.getPageValue("mediaarchive");
	WebEvent webevent = context.getPageValue("webevent");
	Asset asset = null;
	if( webevent != null)
	{
		String sourcepath = webevent.getSourcePath();
		if( sourcepath != null )
		{
			asset = mediaArchive.getAssetBySourcePath(sourcepath);
		}
	}
	if (asset != null){
		boolean haschanged = false;
		String category = mediaArchive.getCatalogSettingValue("push_download_category");
		if (category != null && !category.isEmpty()){
			log.info("found push_download_category $category, updating asset $asset (${asset.id})");
			Category target = mediaArchive.getCategoryArchive().createCategoryTree(category);
			asset.addCategory(target);
			mediaArchive.getCategoryArchive().saveAll();
			haschanged = true;
		}
		//check push_download_library then default_library
		Data library = null;
		String libraryid = mediaArchive.getCatalogSettingValue("push_download_library");
		if (libraryid == null || libraryid.isEmpty()){
			libraryid = mediaArchive.getCatalogSettingValue("default_library");
		}
		if (libraryid != null && !libraryid.isEmpty()){
			Searcher libsearcher = mediaArchive.getSearcher("library");
			library = libsearcher.searchById(libraryid);
		}
		if (library != null){
			log.info("found push_download_library (or default_library) $libraryid, updating asset $asset (${asset.id})");
			asset.addLibrary(library.getId());
			haschanged = true;
		}
		if (haschanged){
			mediaArchive.getAssetSearcher().saveData(asset, null);
		}
	} else {
		log.info("unable to find asset from webevent $webevent, aborting");
	}
	log.info("finished finalizing pull event");
}

init();