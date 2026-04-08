package org.entermediadb.ai.automation.agents;

import org.entermediadb.ai.llm.AgentContext;
import org.openedit.WebPageRequest;

public class RunModuleAgent extends RunEventAgent
{

	/**
	 */
	@Override
	public void process(AgentContext inContext)
	{
		// Use path-event style exec
		String operation = inContext.getCurrentAgentEnable().getAgentData().get("runoperation");

		operation = operation.replaceFirst("_", ".");

		// inContext.info("Running: " + operation);

		WebPageRequest request = (WebPageRequest) inContext.getContextValue("webpagerequest");
		// runPathEvent(inContext, operation, request);
		getModuleManager().execute(operation, request);

		super.process(inContext);
	}

}
