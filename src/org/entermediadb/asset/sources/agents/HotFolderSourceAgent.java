package org.entermediadb.asset.sources.agents;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.AgentEnabled;
import org.entermediadb.asset.sources.AssetSource;
import org.entermediadb.asset.util.TimeParser;
import org.openedit.locks.Lock;
import org.openedit.util.DateStorageUtil;

// One per source. Saved to the DB
public class HotFolderSourceAgent extends BaseAgent
{
	private static final Log log = LogFactory.getLog(HotFolderSourceAgent.class);

	@Override
	public void process(AgentContext inContext)
	{
		scanSource(inContext);
	}

	public void scanSource(AgentContext inContext)
	{
		// Loop over the children SourceAgents and check the time config
		String base = "/WEB-INF/data/" + getCatalogId() + "/originals";

		String id = inContext.getCurrentAgentEnable().getAgentData().getId();
		AssetSource assetSource = getMediaArchive().getAssetManager().findAssetSourceById(id);

		String name = assetSource.getName();
		// String path = base + "/" + name ;
		if (!assetSource.isEnabled())
		{
			inContext.info("Hot folder not enabled " + name);
			super.process(inContext);
			return;
		}
		inContext.info("Checking: " + name);
		if (assetSource.getConfig() != null)
		{
			String periodString = assetSource.getConfig().get("runwithinperiod");
			if (periodString != null)
			{
				long period = new TimeParser().parse(periodString);
				Date laststarted = DateStorageUtil.getStorageUtil().parseFromObject(assetSource.getConfig().getValue("lastscanstart"));
				if (laststarted != null)
				{
					if (laststarted.getTime() + period > System.currentTimeMillis())
					{
						long remaining = System.currentTimeMillis() - laststarted.getTime() - period;
						inContext.info(name + ", will scan again within: " + remaining / 1000D + " seconds ");
						super.process(inContext);
						return;
					}
				}
			}
		}
		Lock lock = getMediaArchive().getLockManager().lockIfPossible("scan-" + assetSource.getId(), "HotFolderManager");
		if (lock != null)
		{
			inContext.info("Hot folder is already in lock table: " + name);
			super.process(inContext);
			return;
		}
		inContext.info(getMediaArchive().getCatalogId() + " - Hot folder import started: " + name);

		try
		{
			// pullGit(path,1);
			long starttime = System.currentTimeMillis();
			int found = assetSource.importAssets(null);
			long timetook = System.currentTimeMillis() - starttime;
			inContext.info("Hot folder: " + name + ", imported " + found + " assets within:" + timetook / 1000D + " seconds");
		}
		catch (Exception ex)
		{
			inContext.error("Could not process Hot folder " + name, ex);
			log.error("Could not process Hot folder " + name, ex);
		}
		finally
		{
			try
			{
				getMediaArchive().releaseLock(lock);
			}
			catch (Exception ex)
			{
				// We somehow got a version error. Someone save it from under us
				// TOOD: Delete them all?
			}
		}
		super.process(inContext);
	}

}
