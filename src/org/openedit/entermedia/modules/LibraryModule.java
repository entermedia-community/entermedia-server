package org.openedit.entermedia.modules;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;

import com.openedit.WebPageRequest;

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
		}
		archive.saveAsset(asset, inReq.getUser());
		
	}
}
