package importing;

import org.openedit.*
import org.openedit.data.Searcher
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.scanner.MetaDataReader

import com.openedit.hittracker.*
import com.openedit.modules.update.Downloader
import com.openedit.page.Page
import com.openedit.util.PathUtilities

public void init() {
	MediaArchive archive = context.getPageValue("mediaarchive");
	String assetid = context.findValue("assetid");
	log.info("Reading metadata for asset $assetid");


	Searcher searcher = archive.getAssetSearcher();
	HitTracker assets = searcher.fieldSearch("importstatus", "needsdownload");


	String ids = context.getRequestParameter("assetids");
	if( ids != null ) {
		String[] assetids = ids.split(",");
		assets.setSelections(Arrays.asList( assetids) );
		assets.setShowOnlySelected(true);
	}

	List tosave = new ArrayList();
	assets.each{


		Asset current = archive.getAssetBySourcePath(it.sourcepath);

		String fetchurl = current.fetchurl;
		String filename = current.name;




		if(filename == null){


			filename = PathUtilities.extractFileName(fetchurl);
			filename = filename.replaceAll("\\?.*", "");
		}



		String path = "/WEB-INF/data/"	+ archive.getCatalogId() + "/originals/" + current.getSourcePath()			+ "/" + filename;
		Page finalfile = archive.getPageManager().getPage(path);
		
				File image = new File(finalfile.getContentItem().getAbsolutePath());
		Downloader dl = new Downloader();
		//imagename = URIUtil.encodeQuery(imagename);
		//String imageurl = "http://rogersfido.area.ca/productimages/"	+ imagename;
		log.info("URL : " + fetchurl);
		dl.download(fetchurl, image);

		current.setPrimaryFile(image.getName());
		current.setProperty("importstatus", "imported");
		MetaDataReader reader = archive.getModuleManager().getBean("metaDataReader");
		reader.populateAsset(archive, finalfile.getContentItem(), current)
		tosave.add(current);
		if(tosave.size() > 1000){
			archive.saveAssets(tosave);
			tosave.clear();

		}
	}


	archive.saveAssets(tosave);

	archive.fireSharedMediaEvent("importing/queueconversions");


}




init();