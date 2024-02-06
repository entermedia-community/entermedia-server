package org.entermediadb.asset.publishing;

import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.WebPageRequest;

public class PublishModule extends BaseMediaModule
{
	public void checkQueue(WebPageRequest inRequest) 
	{
		MediaArchive archive = getMediaArchive(inRequest);
		PublishManager manager = (PublishManager)archive.getBean("publishManager");
		manager.checkQueue(inRequest);
	}
}
