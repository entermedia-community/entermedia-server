package org.entermediadb.ai.automation.agents;

import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.events.PathEventManager;
import org.openedit.BaseWebPageRequest;
import org.openedit.WebPageRequest;

public class RunEventAgent extends BaseAgent {

	/**
	 * Calls render to html
	 * Attaches and asset version
	 * sends it to a blog?
	 */
	@Override
	public void process(AgentContext inContext) {
		String operation = inContext.getCurrentAgentEnable().getAgentData().get("runoperation");
		WebPageRequest request = (WebPageRequest) inContext.getContextValue("webpagerequest");
		runPathEvent(inContext, operation, request);
	}

	protected void runPathEvent(AgentContext inContext, String operation, WebPageRequest inRequest) {
		AgentContext child = new AgentContext(inContext);
		String runpath = "/" + getCatalogId() + "/events/" + operation + ".html";
		PathEventManager manager = (PathEventManager) getModuleManager().getBean(getCatalogId(), "pathEventManager");
		WebPageRequest request = new BaseWebPageRequest(inRequest);

		child.put("webpagerequest", request); // TODO: Make this one combined context
		request.putPageValue("currentagentcontext", child);

		request.setRequestParameter("catalogid", getCatalogId());
		manager.runPathEvent(runpath, request);
		super.process(child);

	}
}
