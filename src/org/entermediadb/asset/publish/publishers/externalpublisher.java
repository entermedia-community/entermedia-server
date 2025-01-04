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
 * @author shanti
 *
 */

public class externalpublisher extends BasePublisher implements Publisher
{
	private static final Log log = LogFactory.getLog(externalpublisher.class);
	
	public PublishResult publish(MediaArchive mediaArchive,Order inOrder, Data inOrderItem, Data inDestination, Data inPreset, Asset inAsset)
	{
		PublishResult result = new PublishResult();
		result.setPending(true);
		return result;
	}
	
}