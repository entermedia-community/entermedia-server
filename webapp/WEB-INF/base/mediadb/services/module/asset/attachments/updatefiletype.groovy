import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.scanner.MetaDataReader

import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.Page


public void init(){
	System.out.println("#### updating file type");
	WebPageRequest req = context;
	MediaArchive archive = req.getPageValue("mediaarchive");
	Asset asset = getAsset(req);
	if (!asset){
		log.info("cannot load asset, aborting");
		return;
	}
	String primary = asset.getPrimaryFile();
	if (!primary || !primary.contains(".")){
		log.info("cannot find primary file for $asset, aborting");
		return;
	}
	//main things to update: fileformat, assettype, pages; each require defaults if not found
	String sourcepath = asset.getSourcePath();
	String path = "/WEB-INF/data/${archive.getCatalogId()}/originals/$sourcepath/$primary";
	MetaDataReader reader = archive.getModuleManager().getBean("metaDataReader");
	//problem is if pages not set, will not update page property
	Page content = archive.getPageManager().getPage(path);// read the primary file again and find # of pages
	Asset tempasset = new Asset();
	reader.populateAsset(archive, content.getContentItem(), tempasset);
	int pages = getPages(tempasset);
	asset.setProperty("pages", "$pages");
	String fileformat = tempasset.get("fileformat");
	if (fileformat){
		asset.setProperty("fileformat",fileformat);//default
	} else {
		asset.setProperty("fileformat","default");
	}
	Data type = archive.getDefaultAssetTypeForFile(primary);
	if(type){
		asset.setProperty("assettype", type.getId());//none
	} else {
		asset.setProperty("assettype", "none");
	}
	archive.saveAsset(asset, null);
}

public int getPages(Asset inAsset){
	if (inAsset.get("pages")){
		try{
			return Integer.parseInt(inAsset.get("pages"));
		}catch (Exception e){}
	}
	return 1;
}

public Asset getAsset(WebPageRequest inReq)
{
	Object found = inReq.getPageValue("asset");
	if( found instanceof Asset)
	{
		return (Asset)found;
	}
	String sourcePath = inReq.getRequestParameter("sourcepath");
	MediaArchive archive = inReq.getPageValue("mediaarchive");
	Asset asset = null;
	if (sourcePath != null)
	{
		asset = archive.getAssetSearcher().getAssetBySourcePath(sourcePath, true);
	}
	String assetid = null;
	if( asset == null)
	{
		assetid = inReq.getRequestParameter("assetid");
		if( assetid != null && assetid.startsWith("multiedit:") )
		{
			Asset data = archive.getAsset(assetid, inReq);
			inReq.putPageValue("asset", data);
			inReq.putPageValue("data", data);
			return (Asset) data;
		}

	}
	if (asset == null && archive != null)
	{
		asset = archive.getAssetBySourcePath(inReq.getContentPage());
		if (asset == null)
		{
			if (assetid != null)
			{
				asset = archive.getAsset(assetid);
			}
		}
	}
	return asset;
}

init();