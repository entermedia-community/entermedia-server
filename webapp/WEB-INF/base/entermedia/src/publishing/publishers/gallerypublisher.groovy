package publishing.publishers;

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.Data
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.*

import com.openedit.page.Page
import com.openedit.util.FileUtils

public class gallerypublisher extends basepublisher implements Publisher
{
	private static final Log log = LogFactory.getLog(filecopypublisher.class);
	
	public PublishResult publish(MediaArchive mediaArchive,Asset inAsset, Data inPublishRequest, Data inDestination, Data inPreset)
	{
		
		
		//This is basically a "No Operation" for now
		PublishResult result = checkOnConversion(mediaArchive,inPublishRequest,inAsset,inPreset); 
		if( result != null)
		{
			return result;
		}

		result = new PublishResult();
		result.setComplete(true);
		
		log.info("published ${finalfile}");
		return result;
	}
	
}