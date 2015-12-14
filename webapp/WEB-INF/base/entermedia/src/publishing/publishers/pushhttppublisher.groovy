package publishing.publishers;

import java.io.File;

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.
import org.openedit.util.FileUtils;

import com.openedit.page.Page

/**
 * This is a poorly named browser download publisher. 
 * @author shanti
 *
 */

public class pushhttppublisher extends BasePublisher implements Publisher
{
	private static final Log log = LogFactory.getLog(pushhttppublisher.class);
	
	public PublishResult publish(MediaArchive mediaArchive,Asset inAsset, Data inPublishRequest,  Data inDestination, Data inPreset)
	{
		PublishResult result = new PublishResult();
		
		Page inputpage = findInputPage(mediaArchive,inAsset,inPreset);
		if( inputpage.exists() )
		{
			result.setComplete(true);
		}
		else
		{
			//Make sure have an entry?
			result.setPending(true);
		}
		return result;
	}
	
}