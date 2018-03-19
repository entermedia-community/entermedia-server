package org.entermediadb.sitemanager;

import java.util.List;

import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.WebPageRequest;

public class DiskModule extends BaseMediaModule
{
	public void checkDisks(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		DiskManager manager = (DiskManager)getModuleManager().getBean(catalogid,"diskManager");
		MediaArchive archive = getMediaArchive(inReq);

		List<DiskPartition> partitions = manager.getPartitionsStats(archive);
		inReq.putPageValue("partitions", partitions);
	}
}
