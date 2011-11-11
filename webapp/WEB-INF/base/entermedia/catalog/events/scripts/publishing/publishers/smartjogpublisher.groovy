package publishing.publishers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data 
import org.openedit.data.Searcher 
import org.openedit.entermedia.Asset 
import org.openedit.entermedia.MediaArchive 
import org.openedit.entermedia.publishing.Publisher 
import org.openedit.entermedia.smartjog.SmartJog 

import com.openedit.page.Page;
import com.openedit.util.PathUtilities;
import com.smartjog.webservices.Delivery 
import com.smartjog.webservices.ServerFile
 

public class smartjogpublisher extends basepublisher implements Publisher
{
	private static final Log log = LogFactory.getLog(smartjogpublisher.class);
	
	public void publish(MediaArchive mediaArchive,Data inOrder, Data inOrderItem, Asset asset)
	{	
		String publishdestination = inOrderItem.get("publishdestination");
		if( publishdestination == null)
		{
			publishdestination = inOrder.get("publishdestination");
		}
		
		if( publishdestination != null)
		{
			Data destination = mediaArchive.getSearcherManager().getData( mediaArchive.getCatalogId(), "publishdestination", publishdestination);
			
			Searcher presetsearcher = mediaArchive.getSearcherManager().getSearcher (mediaArchive.getCatalogId(), "convertpreset");
			String presetid = inOrderItem.get("presetid");
			
			if(presetid != null)
			{	
				String exportname= inOrderItem.get("filename");
				
				String mountPath = "/WEB-INF/publish/smartjog";
				String fullPath = mountPath + exportname;
	
				Page publishPage = mediaArchive.getPageManager().getPage(fullPath);
				
				String serverId = destination.get("server");
							
				try
				{
					Page inputpage = findInputPage(mediaArchive,asset,presetid);
					mediaArchive.getPageManager().copyPage(inputpage, publishPage); //put the file on the ftp server for deliveryx
					doSmartJogDelivery(mediaArchive,exportname, new Integer(Integer.parseInt(serverId)));
				}
				catch (Exception e)
				{
					inOrderItem.setProperty("status", "publisherror");
					log.info "SMART JOG ERROR: Could not publish ${e} ${asset.sourcepath}";
				}
				
				inOrderItem.setProperty("status", "complete");
//				inOrderItem.setProperty("remotePath", remotePath);
				
				Searcher ordersearcher = mediaArchive.getSearcherManager().getSearcher(mediaArchive.getCatalogId(), "orderitem");
				ordersearcher.saveData(inOrderItem, null);
			}
		}
	}
		
	public void doSmartJogDelivery(MediaArchive mediaArchive, String inFilename, Integer serverId)
	{
		Page dir = mediaArchive.getPageManager().getPage("/WEB-INF/data/" + mediaArchive.getCatalogId() + "/smartjog/");
		SmartJog ssc = new SmartJog(dir.getContentItem().getAbsolutePath());
			
		if (serverId == null)
		{
			log.info ("SMART JOG ERROR: Must specify a server for delivery.");
			return;
		}
		
		//Get a file on the local server
		ServerFile serverFile = ssc.getServerFile(null, inFilename);
		
		Delivery delivery = ssc.deliverFileToServer(serverId.intValue(), serverFile.getServerFileId()); 
		if (delivery != null)
		{ 
			log.info ( "SMART JOG: Delivery has just been created. Displaying its outgoing tracking :"); 
			 
			log.info("< deliveryId="+delivery.getDeliveryId()
					+ " - trackingNumber="
					+ delivery.getTrackingNumber()+" - filename="
					+ delivery.getFilename() +" - md5="+delivery.getMd5()
					+ " - status="+delivery.getStatus()+" >");
		}
	}
	public void publish(MediaArchive mediaArchive,Asset inAsset, Data inPublishRequest,  Data inDestination, Data inPreset){}
}

