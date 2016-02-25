package org.entermediadb.asset.publish.publishers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.publishing.PublishResult;
import org.entermediadb.asset.publishing.Publisher;
import org.openedit.Data;

public class attachmentpublisher extends filecopypublisher implements Publisher
{
	private static final Log log = LogFactory.getLog(attachmentpublisher.class);
	
	public PublishResult publish(MediaArchive mediaArchive,Asset inAsset, Data inPublishRequest,  Data inDestination, Data inPreset)
	{
		//make the asset folder based
		mediaArchive.getAssetEditor().makeFolderAsset(inAsset, null);
		//modify the destination url
		inDestination.setProperty("url", "webapp/WEB-INF/data/" + mediaArchive.getCatalogId() + "/originals/" + inAsset.getSourcePath() + "/");
		return super.publish(mediaArchive, inAsset, inPublishRequest, inDestination, inPreset);
	}

}