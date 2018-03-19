package utils

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.google.GoogleManager
import org.entermediadb.video.CloudTranscodeManager
import org.openedit.Data
import org.openedit.repository.ContentItem

public void runit()
{
	
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	Asset asset = mediaArchive.getAsset("AWIp-4xQYLXW0p7SkXIt");
	ContentItem item = mediaArchive.getOriginalContent(asset);
	GoogleManager manager = mediaArchive.getBean("googleManager");
	Data auth = mediaArchive.getData("oauthprovider","google");
	manager.uploadToBucket(auth, "testbench", item, "{\"name\": \"test\"}");
	CloudTranscodeManager trans = mediaArchive.getBean("cloudTranscodeManager");
	trans.tr
}

runit();

