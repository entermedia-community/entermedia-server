package org.entermediadb.ai.automation;

import org.entermediadb.ai.llm.AgentContext;
import org.openedit.WebPageRequest;

public class RunEventAgent extends StartEventAgent
{
	
	/**
	 * Calls render to html
	 * Attaches and asset version
	 * sends it to a blog?
	 */
	@Override
	public void process(AgentContext inContext)
	{
		String operation = inContext.getCurrentAgentEnable().getAgentConfig().getId();
		WebPageRequest request =  (WebPageRequest)inContext.getContextValue("webpagerequest");
		runPathEvent(inContext, operation, request);
	}

	
}
