package org.entermediadb.asset.publish.publishers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.publishing.BasePublisher;
import org.entermediadb.asset.publishing.PublishResult;
import org.entermediadb.asset.publishing.Publisher;
import org.openedit.Data;

public class gallerypublisher extends BasePublisher implements Publisher
{
	private static final Log log = LogFactory.getLog(gallerypublisher.class);
	
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