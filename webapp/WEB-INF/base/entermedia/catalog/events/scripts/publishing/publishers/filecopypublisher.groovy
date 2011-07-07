package publishing.publishers;

import java.io.File;

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.Publisher

import com.openedit.page.Page
import com.openedit.util.FileUtils

public class filecopypublisher extends basepublisher implements Publisher
{
	private static final Log log = LogFactory.getLog(filecopypublisher.class);
	
	private void publishFailure(MediaArchive mediaArchive, Data inOrderItem, String inError)
	{
		inOrderItem.setProperty("status", "publisherror");
		inOrderItem.setProperty("errordetails", inError);
		log.error(inError);
		Searcher itemsearcher = mediaArchive.getSearcherManager().getSearcher(mediaArchive.getCatalogId(), "orderitem");
		itemsearcher.saveData(inOrderItem, null);
	}
	
	public void publish(MediaArchive mediaArchive,Asset inAsset, Data inPublishRequest,  Data inDestination, Data inPreset)
	{
		Page inputpage = findInputPage(mediaArchive,inAsset,inPreset.getId());
		String destinationpath = inDestination.get("url");
		if(!destinationpath.endsWith("/")){
			destinationpath = destinationpath + "/";
		}
		
		FileUtils utils = new FileUtils();
		File destination = new File(destinationpath);
		File source = new File(inputpage.getContentItem().getAbsolutePath());
		File finalfile = new File(destination, source.getName());
		utils.copyFiles(source, finalfile)
		
		
		log.info("publishished  ${inAsset} to Local Folder ${destinationpath}");
	}
	
	public void publish(MediaArchive mediaArchive,Data inOrder, Data inOrderItem, Asset asset)
	{
		String publishdestination = inOrderItem.get("publishdestination");
		if( publishdestination == null)
		{
			publishdestination = inOrder.get("publishdestination");
		}
		String presetid = inOrderItem.get("presetid");
		if( presetid == null)
		{
			presetid = inOrder.get("presetid");
		}
		
		if(publishdestination != null)
		{
			Data destination = mediaArchive.getSearcherManager().getData( mediaArchive.getCatalogId(), "publishdestination", publishdestination);
					
			Page inputpage = findInputPage(mediaArchive,asset,presetid);
			String destinationpath = destination.get("url");
			if(!destinationpath.endsWith("/")){
				destinationpath = destinationpath + "/";
			}
			File file = new File(destinationpath);
			file.mkdirs();
			
			FileUtils utils = new FileUtils();
			utils.copyFiles(inputpage.getContentItem().getAbsolutePath(), destinationpath)
			
			
			log.info("publishished  ${asset} to Local Folder ${destinationpath}");
			inOrderItem.setProperty("status", "complete");
			Searcher itemsearcher = mediaArchive.getSearcherManager().getSearcher(mediaArchive.getCatalogId(), "orderitem");
			itemsearcher.saveData(inOrderItem, null);
		}
	}
}