package org.entermediadb.ai.automation.agents;

import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.events.PathEventManager;

public class RunSharedEventAgent extends RunEventAgent {

	/**
	 * Calls render to html
	 * Attaches and asset version
	 * sends it to a blog?
	 */
	@Override
	public void process(AgentContext inContext) {
		PathEventManager manager = (PathEventManager) getModuleManager().getBean(getCatalogId(), "pathEventManager");
		String operation = inContext.getCurrentAgentEnable().getAgentData().get("runoperation");

		manager.runSharedPathEvent(getMediaArchive().getCatalogHome() + "/events/" + operation + ".html");

		super.process(inContext);
	}

}
