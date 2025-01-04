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
import org.openedit.page.Page;
import org.openedit.repository.filesystem.StringItem;

public class asperapublisher extends BasePublisher implements Publisher
{
	private static final Log log = LogFactory.getLog(asperapublisher.class);

	public PublishResult publish(MediaArchive mediaArchive,Order inOrder, Data inPublishRequest,  Data inDestination, Data inPreset, Asset inAsset)
	{
		//log.info("Publish asset to aspera ${asset} for preset: ${presetid} on server: ${publishdestination}" );
		PublishResult result = new PublishResult();
		
		Page inputpage = findInputPage(mediaArchive,inAsset,inPreset);
		String exportname = inPublishRequest.get("itemexportname");
		
		if( !exportname.startsWith("/"))
		{
			exportname ="/" + exportname;
		}
		StringItem item = new StringItem();
		item.setPath(exportname); //Aspera repo is mounted in a sub folder already. So we just need to append the name
		item.setAbsolutePath(inputpage.getContentItem().getAbsolutePath());

//		AsperaManager manager = (AsperaManager)mediaArchive.getModuleManager().getBean("asperaManager");
//
//		AsperaRepository repo = manager.loadRepository(mediaArchive.getCatalogId(), inDestination );
//		repo.put(item); //copy the file
		log.info("publishished  ${inAsset.sourcepath} to aspera");

		result.setComplete(true);
		return result;
	}
	
	
}

