package org.entermediadb.ai.automation;

import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.events.PathEventManager;
import org.openedit.WebPageRequest;

public class RunSharedEventAgent extends StartEventAgent
{
	
	/**
	 * Calls render to html
	 * Attaches and asset version
	 * sends it to a blog?
	 */
	@Override
	public void process(AgentContext inContext)
	{
		PathEventManager manager = (PathEventManager) getModuleManager().getBean(getCatalogId(), "pathEventManager");
		String operation = inContext.getCurrentAgentEnable().getAgentConfig().getId();
		manager.runSharedPathEvent(operation);
		super.process(inContext);
	}

	
}
