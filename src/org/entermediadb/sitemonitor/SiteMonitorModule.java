package org.entermediadb.sitemonitor;

import java.util.List;
import java.util.Random;

import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.WebPageRequest;

public class SiteMonitorModule extends BaseMediaModule
{
	public static final long  MEGABYTE = 1024L * 1024L;

	public void checkDisks(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		DiskManager diskManager = (DiskManager)getModuleManager().getBean(catalogid,"diskManager");
//		StatManager statManager = (StatManager)getModuleManager().getBean(catalogid,"statManager");
		MediaArchive archive = getMediaArchive(inReq);

		List<DiskPartition> partitions = diskManager.getPartitionsStats(archive);
		inReq.putPageValue("partitions", partitions);

/*		List<Stat> stats = statManager.getStats(archive);
		inReq.putPageValue("stats", stats);
*/		
		Random rnd = new Random();
		Long random = Math.abs(System.currentTimeMillis() - rnd.nextLong());
		inReq.putPageValue("random", random.toString());
	}
	
	public void checkStatus(WebPageRequest inReq)
	{
		
		MediaArchive archive = getMediaArchive(inReq);
		String health = archive.getNodeManager().getClusterHealth();
		if (health.equals("GREEN") || health.equals("YELLOW")) {
			inReq.putPageValue("status", "ok");
		}
		else {
			inReq.putPageValue("status", "error");
			
		}
	}
}
