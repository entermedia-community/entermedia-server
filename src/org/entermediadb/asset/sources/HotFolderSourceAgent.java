package org.entermediadb.asset.sources;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.asset.util.TimeParser;
import org.openedit.locks.Lock;
import org.openedit.util.DateStorageUtil;

public class HotFolderSourceAgent extends BaseAgent
{
	private static final Log log = LogFactory.getLog(HotFolderSourceAgent.class);
	protected AssetSource fieldAssetSource;
	
	public AssetSource getAssetSource()
	{
		return fieldAssetSource;
	}

	public void setAssetSource(AssetSource inAssetSource)
	{
		fieldAssetSource = inAssetSource;
	}

	@Override
	public void process(AgentContext inContext)
	{
		scanSource(inContext);
	}

	public void scanSource(AgentContext inContext)
	{
		//Loop over the children SourceAgents and check the time config
		String base = "/WEB-INF/data/" + getCatalogId() + "/originals";
			String name = getAssetSource().getName();
//			String path = base + "/" + name ;
			if( !getAssetSource().isEnabled() )
			{
				//inLog.info("Hot folder not enabled " + name);
				super.process(inContext);
				return;
			}
			if( getAssetSource().getConfig() != null)
			{
				String periodString = getAssetSource().getConfig().get("runwithinperiod");
				if( periodString != null)
				{
					long period = new TimeParser().parse(periodString);
					Date laststarted = DateStorageUtil.getStorageUtil().parseFromObject(getAssetSource().getConfig().getValue("lastscanstart"));
					if( laststarted  != null)
					{
						if( laststarted.getTime() + period > System.currentTimeMillis())
						{
							long remaining = System.currentTimeMillis() - laststarted.getTime() - period;
							inContext.info(name+ ", will scan again within: " + remaining/1000D + " seconds ");
							super.process(inContext);
							return;
						}
					}
				}
			}
			Lock lock = getMediaArchive().getLockManager().lockIfPossible("scan-" + getAssetSource().getId(), "HotFolderManager");
			if( lock == null)
			{
				inContext.info("Hot folder is already in lock table: " + name);
				super.process(inContext);
				return;
			}
			inContext.info(getMediaArchive().getCatalogId() +" - Hot folder import started: " + name);

			try
			{
				//pullGit(path,1);
				long starttime = System.currentTimeMillis();
				int found = getAssetSource().importAssets(null); 
				long timetook = System.currentTimeMillis() - starttime;
				inContext.info("Hot folder: " + name + ", imported " + found + " assets within:" + timetook/1000D + " seconds");
			}
			catch( Exception ex)
			{
				inContext.error("Could not process Hot folder " + name ,ex);
				log.error("Could not process Hot folder " + name,ex);
			}
			finally
			{
				try
				{
					getMediaArchive().releaseLock(lock);
				}
				catch ( Exception ex)
				{
					//We somehow got a version error. Someone save it from under us
					//TOOD: Delete them all?
				}
		}
		super.process(inContext);
	}
	
	
}
