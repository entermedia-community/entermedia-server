package publishing.publishers;

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.Data
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.*

/**
 * This is a poorly named browser download publisher. 
 * @author shanti
 *
 */

public class externalpublisher extends BasePublisher implements Publisher
{
	private static final Log log = LogFactory.getLog(externalpublisher.class);
	
	public PublishResult publish(MediaArchive mediaArchive,Asset inAsset, Data inPublishRequest,  Data inDestination, Data inPreset)
	{
		PublishResult result = new PublishResult();
		result.setPending(true);
		return result;
	}
	
}