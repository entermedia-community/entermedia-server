package google;

import org.entermediadb.asset.MediaArchive
import org.entermediadb.google.GoogleManager

public void runit()
{
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	GoogleManager manager = (GoogleManager)mediaArchive.getBean("googleManager");
	manager.notifyTopic("9","Hello","Test message");
}

runit();