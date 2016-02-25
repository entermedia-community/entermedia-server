package org.entermediadb.asset.publish.publishers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.publishing.BasePublisher;
import org.entermediadb.asset.publishing.PublishResult;
import org.entermediadb.asset.publishing.Publisher;
import org.openedit.Data;
import org.openedit.page.Page;

/**
 * This is a poorly named browser download publisher. 
 * @author shanti
 *
 */

public class httppublisher extends BasePublisher implements Publisher
{
	private static final Log log = LogFactory.getLog(httppublisher.class);
	
	public PublishResult publish(MediaArchive mediaArchive,Asset inAsset, Data inPublishRequest,  Data inDestination, Data inPreset)
	{
		PublishResult result = checkOnConversion(mediaArchive,inPublishRequest,inAsset,inPreset); 
		if( result != null)
		{
			return result;
		}

		result = new PublishResult();
		
		Page inputpage = findInputPage(mediaArchive,inAsset,inPreset);
		if( inputpage.exists() )
		{
			result.setComplete(true);
		}
		else
		{
			result.setErrorMessage("Input file is missing");
		}
		return result;
	}
	
}