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
	Downloader dl = new Downloader();
	assets.each
	{
		try
		{	
			Asset current = archive.getAssetBySourcePath(it.sourcepath);
			String fetchurl = current.fetchurl;
			if( fetchurl != null )
			{
				String filename = current.name;
				if(filename == null){
					filename = PathUtilities.extractFileName(fetchurl);
					filename = filename.replaceAll("\\?.*", "");
				}
				String path = "/WEB-INF/data/"	+ archive.getCatalogId() + "/originals/" + current.getSourcePath()			+ "/" + filename;
				Page finalfile = archive.getPageManager().getPage(path);
				
				File image = new File(finalfile.getContentItem().getAbsolutePath());
				
				dl.download(fetchurl, image);
		
				current.setPrimaryFile(image.getName());
				MetaDataReader reader = archive.getModuleManager().getBean("metaDataReader");
				reader.populateAsset(archive, finalfile.getContentItem(), current)
			}	
			String fetchthumbnailurl = current.fetchthumbnailurl;
			if(fetchthumbnailurl != null )
			{
				String path = "/WEB-INF/data/"	+ archive.getCatalogId() + "/generated/" + current.getSourcePath()	+ "/customthumb.jpg";
				Page finalfile = archive.getPageManager().getPage(path);
				File image = new File(finalfile.getContentItem().getAbsolutePath());
				archive.removeGeneratedImages(current, false);	
				dl.download(fetchthumbnailurl, image);
			}		
			def tasksearcher = archive.getSearcher("conversiontask");
			def existing = tasksearcher.query().match("assetid", current.getId() ).search(); 
			existing.each
			{
				tasksearcher.delete(it,user);		
			}			
			archive.saveAsset(current,user);
			archive.fireMediaEvent( "importing/queueconversions", user, current); //this will save the asset as imported
		}
		catch( Exception ex )
		{
			log.error("could not process asset: " + it.sourcepath,ex);
		}	
	}

}

init();