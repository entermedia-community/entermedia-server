package org.openedit.entermedia.modules;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;

public class LibraryModule extends BaseMediaModule 
{
	
	private static final Log log = LogFactory.getLog(LibraryModule.class);

	public void addAssetToLibrary(WebPageRequest inReq){
		MediaArchive archive = getMediaArchive(inReq);
		String assetid = inReq.getRequestParameter("assetid");
		String libraryid = inReq.getRequestParameter("libraryid");
		
		Asset asset = archive.getAsset(assetid);
		
		if(asset != null && !asset.getLibraries().contains(libraryid)){
			asset.addLibrary(libraryid);
			archive.saveAsset(asset, inReq.getUser());


		}
		Searcher libraryassets = archive.getSearcher("libraryassets");
		SearchQuery query = libraryassets.createSearchQuery().append("libraryid", libraryid).append("assetid", assetid);
		HitTracker hits = libraryassets.search(query);
		if(hits.size() == 0){
			Data newrow = libraryassets.createNewData();
			newrow.setSourcePath(libraryid);
			newrow.setProperty("libraryid", libraryid);
			newrow.setProperty("assetid", assetid);
			newrow.setId(libraryassets.nextId());
			libraryassets.saveData(newrow, inReq.getUser());
			
		}
		
	}
}
