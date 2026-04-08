package org.entermediadb.ai.automation.agents;

import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.asset.convert.QueueManager;
import org.entermediadb.scripts.ScriptLogger;

public class ProcessConversions extends BaseAgent
{
	@Override
	public void process(AgentContext inContext)
	{

		QueueManager queueManager = (QueueManager) getModuleManager()
				.getBean(getMediaArchive().getCatalogId(), "queueManager");
		queueManager.checkQueue();

		if (queueManager.getTotalPending() > 0)
		{
			inContext.info("Total Pending Tasks: " + queueManager.getTotalPending()
					+ " Threads running: " + queueManager.runningProcesses() + " of "
					+ queueManager.getMaxProcessors());
		}

		if (queueManager.getRunningAssetIds().size() > 1)
		{
			// logger.info("Processing asset ids: " + queueManager.getRunningAssetIds());
		}
		super.process(inContext);
	}
}
