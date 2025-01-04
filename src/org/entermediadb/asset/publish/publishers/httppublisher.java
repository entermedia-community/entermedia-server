package org.entermediadb.asset.publish.publishers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.orders.Order;
import org.entermediadb.asset.publishing.BasePublisher;
import org.entermediadb.asset.publishing.PublishResult;
import org.entermediadb.asset.publishing.Publisher;
import org.openedit.Data;

/**
 * This is a poorly named browser download publisher. 
 * @author shanti
 *
 */

public class httppublisher extends BasePublisher implements Publisher
{
	private static final Log log = LogFactory.getLog(httppublisher.class);
	
	public PublishResult publish(MediaArchive mediaArchive,Order inOrder, Data inOrderItem, Data inDestination, Data inPreset, Asset inAsset)
	{
		PublishResult result = 	checkOnConversion(mediaArchive,inOrderItem,inAsset,inPreset); 

		if( result.isReadyToPublish() )
		{
			//The browser javascript will now mark is as complete
		}
		
		return result;
	}
	
}