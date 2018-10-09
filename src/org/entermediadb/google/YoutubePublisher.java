package org.entermediadb.google;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.publishing.BasePublisher;
import org.entermediadb.asset.publishing.PublishResult;
import org.entermediadb.asset.publishing.Publisher;
import org.openedit.Data;
import org.openedit.page.Page;

public class YoutubePublisher extends BasePublisher implements Publisher
{
	private static final Log log = LogFactory.getLog(YoutubePublisher.class);
	
	public PublishResult publish(MediaArchive mediaArchive, Asset inAsset, Data inPublishRequest, Data inDestination, Data inPreset)
	{
		
		//https://github.com/youtube/api-samples/blob/master/java/src/main/java/com/google/api/services/samples/youtube/cmdline/data/UploadVideo.java
		//Could be a better way - see above, would support resuming?
		
		//setup result object
		PublishResult result = new PublishResult();
		Page input = findInputPage(mediaArchive, inAsset, inPreset);
		//get publishing service
		GoogleManager manager = (GoogleManager) mediaArchive.getBean("googleManager");
		//manager.publishToYoutube(inAsset, input.getContentItem());
		return result;
	}
}