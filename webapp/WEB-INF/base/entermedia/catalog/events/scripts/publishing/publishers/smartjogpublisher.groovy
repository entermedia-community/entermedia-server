package publishing.publishers;

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.Data
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.PublishResult
import org.openedit.entermedia.publishing.Publisher
import org.openedit.entermedia.smartjog.SmartJog
import org.openedit.entermedia.smartjog.Status

import com.openedit.OpenEditException
import com.openedit.page.Page
import com.smartjog.webservices.Delivery
import com.smartjog.webservices.ServerFile
 

public class smartjogpublisher extends basepublisher implements Publisher
{
	private static final Log log = LogFactory.getLog(smartjogpublisher.class);
	
	public PublishResult publish(MediaArchive mediaArchive,Asset asset, Data inPublishRequest,  Data destination, Data inPreset)
	{
		PublishResult result = new PublishResult();

		Page inputpage = findInputPage(mediaArchive,asset,inPreset);
		String exportname = inPublishRequest.get("exportname");
		String mountPath = "/WEB-INF/publish/smartjog";
		if( !exportname.startsWith("/"))
		{
			mountPath = mountPath + "/";
		}
		String fullPath = mountPath + exportname;
	
		Page publishPage = mediaArchive.getPageManager().getPage(fullPath);

		String serverId = destination.get("server");
		if (serverId == null)
		{
			result.setErrorMessage("SMART JOG ERROR: Must specify a server for delivery.");
			return result;
		}
		if( inPublishRequest.get("status") == "pending")
		{
			updateStatus(mediaArchive,exportname, new Integer(Integer.parseInt(serverId)), inPublishRequest,result );			
		}
		else
		{
			mediaArchive.getPageManager().copyPage(inputpage, publishPage); //put the file on the ftp server for deliveryx
			
			startSmartJogDelivery(mediaArchive,exportname,inPublishRequest, new Integer(Integer.parseInt(serverId)), result);
		}

		return result;
	}
	public void updateStatus(MediaArchive mediaArchive, String inFilename, Integer serverId, Data publishtask, PublishResult inResult)
	{
		Page dir = mediaArchive.getPageManager().getPage("/WEB-INF/data/" + mediaArchive.getCatalogId() + "/smartjog/");
		SmartJog ssc = new SmartJog(dir.getContentItem().getAbsolutePath());
		
		String tracking = publishtask.get("trackingnumber");
		String[] numbers = tracking.split(",");
		int deliveryid = Integer.parseInt(numbers[1]);
		//Get a file on the local server
		Status status = ssc.updateStatus(numbers[0],serverId,deliveryid);
		publishtask.setProperty("completionpercent", status.getPercent() );
		if( status.getStatus().equals("Complete"))
		{
			inResult.setComplete(true);
		}
		else if( status.getStatus().equals("Error"))
		{
			inResult.setErrorMessage("SmartJob reported an status of Error");
		}
		else
		{
			inResult.setPending(true);
		}
		log.info("Smartjog status is updated to: " + status.getPercent());
		//ssc.
	}
	public void startSmartJogDelivery(MediaArchive mediaArchive, String inFilename, Data publishtask, Integer serverId, PublishResult inResult)
	{
		Page dir = mediaArchive.getPageManager().getPage("/WEB-INF/data/" + mediaArchive.getCatalogId() + "/smartjog/");
		SmartJog ssc = new SmartJog(dir.getContentItem().getAbsolutePath());
			
		//Get a file on the local server
		log.info("Filename was: " + inFilename);
		ServerFile serverFile = ssc.getServerFile(null, inFilename);
		int attempts = 0;
		while(serverFile == null){
			attempts++;
			try{
			Thread.sleep(1000);
			} catch (Exception e){}
			
			serverFile = ssc.getServerFile(null, inFilename);
			if(attempts == 20){
				throw new OpenEditException("SmartJog File could not be found after 20 seconds: ${inFilename}");
			}
		//	log.info("Attempt ${attempts}");
		}
		Delivery delivery = ssc.deliverFileToServer(serverId.intValue(), serverFile.getServerFileId()); 
		if (delivery != null)
		{ 
			log.info ( "SMART JOG: Delivery has just been created. Displaying its outgoing tracking :"); 
			 
			log.info("< deliveryId="+delivery.getDeliveryId()
					+ " - trackingNumber="
					+ delivery.getTrackingNumber()+" - filename="
					+ delivery.getFilename() +" - md5="+delivery.getMd5()
					+ " - status="+delivery.getStatus()+" >");
				
			publishtask.setProperty("trackingnumber","${delivery.getTrackingNumber()},${delivery.getDeliveryId()}" );
			inResult.setPending(true);
		}
		else
		{
			log.error("Delivery should no be null");
		}
	}
}

