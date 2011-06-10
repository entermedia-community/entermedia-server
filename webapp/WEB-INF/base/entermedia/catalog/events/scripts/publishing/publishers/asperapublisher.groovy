package publishing.publishers;

import com.openedit.OpenEditException;
import com.openedit.page.Page 

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermedia.aspera.AsperaManager 
import org.entermedia.aspera.AsperaRepository 
import org.openedit.Data 
import org.openedit.entermedia.Asset 
import org.openedit.entermedia.MediaArchive 
import org.openedit.entermedia.modules.OrderModule;
import org.openedit.entermedia.publishing.Publisher;
import org.openedit.repository.filesystem.StringItem;
import org.openedit.data.*;

public class asperapublisher extends basepublisher implements Publisher
{
	private static final Log log = LogFactory.getLog(asperapublisher.class);
	
	public void publish(MediaArchive mediaArchive,Data inOrder, Data inOrderItem, Asset asset)
	{
		AsperaManager manager = (AsperaManager)mediaArchive.getModuleManager().getBean("asperaManager");

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
		log.info("Publish asset to aspera ${asset} for preset: ${presetid} on server: ${publishdestination}" );
		Searcher presetsearcher = mediaArchive.getSearcherManager().getSearcher(mediaArchive.getCatalogId(), "convertpreset");

		if( publishdestination != null)
		{
			Data destination = mediaArchive.getSearcherManager().getData( mediaArchive.getCatalogId(), "publishdestination", publishdestination);
			Page inputpage = findInputPage(mediaArchive,asset,presetid);
			
			String exportName= inOrderItem.get("filename");
			if( !exportName.startsWith("/"))
			{
				exportName ="/" + exportName;
			}
			StringItem item = new StringItem();
			item.setPath(exportName); //the Aspera path
			item.setAbsolutePath(inputpage.getContentItem().getAbsolutePath());
			String server = destination.get("server");
			
			try
			{
				AsperaRepository repo = manager.loadRepository(mediaArchive.getCatalogId(), destination );
				repo.put(item); //copy the file
				log.info("publishished  ${asset.sourcepath} to aspera");
				inOrderItem.setProperty("status", "complete");
			}
			catch( Exception ex)
			{
				inOrderItem.setProperty("status", "publisherror");
				inOrderItem.setProperty("errordetails", "aspera publish failed to server ${server} " + ex);
				log.info("Could not publish ${ex} ${asset.sourcepath}");
				ex.printStackTrace();
			}
			Searcher itemsearcher = mediaArchive.getSearcherManager().getSearcher(mediaArchive.getCatalogId(), "orderitem");
			itemsearcher.saveData(inOrderItem, null);
		}
		
	}
	
	
	
	
	public void publish(MediaArchive mediaArchive,Asset inAsset, Data inPublishRequest,  Data inDestination, Data inPreset)
	{
		throw new OpenEditException("Not implemented");	
	}
	
	
	
}

