package importing

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.scanner.MetaDataReader
import org.entermediadb.modules.update.Downloader
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.page.Page
import org.openedit.util.PathUtilities

public void init() {
	MediaArchive archive = context.getPageValue("mediaarchive");
	String assetid = context.findValue("assetid");

	Searcher searcher = archive.getAssetSearcher();
	HitTracker assets = searcher.fieldSearch("importstatus", "needsdownload");
	assets.enableBulkOperations();
	String ids = context.getRequestParameter("assetids");
	if( ids != null ) 
	{
		log.info("Reading metadata for asset $ids");
		String[] assetids = ids.split(",");
		assets.setSelections(Arrays.asList( assetids) );
		assets.setShowOnlySelected(true);
	}
	else
	{
		log.info("Found ${assets.size()} assets ");
	}
	Downloader dl = new Downloader();
	assets.each
	{
		Asset current = null;
		try
		{	
			current = archive.getAssetSearcher().loadData(it);
			String fetchurl = current.fetchurl;
			
			boolean regenerate = false;
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
				log.info("Downloaded ${fetchurl}" );
				current.setPrimaryFile(image.getName());
				current.setFolder(true);
				
				//String assettype = current.assettype;
				
				MetaDataReader reader = archive.getModuleManager().getBean("metaDataReader");
				reader.populateAsset(archive, finalfile.getContentItem(), current);
				//if( assettype != null && assettype.equals("embedded") )
				//{
					current.setValue("assettype","video");
				//}
				
				
				regenerate = true;
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
			if( regenerate )
			{
				current.setValue("importstatus","imported");				
				def tasksearcher = archive.getSearcher("conversiontask");
				def existing = tasksearcher.query().match("assetid", current.getId() ).search(); 
				existing.each
				{
					tasksearcher.delete(it,user);		
				}			
				archive.saveAsset(current,user);
				archive.fireMediaEvent( "assetimported", user, current); //TODO: Fire more than one at a time
			}
		}
		catch( Exception ex )
		{
			log.error("could not process asset: " + it.sourcepath,ex);
			current.setProperty("importstatus","error");
			archive.saveAsset(current,user);
			
		}	
	}

}

init();