package org.entermediadb.ai.automation;

import org.entermediadb.ai.llm.AgentContext;
import org.openedit.WebPageRequest;

public class RunModuleAgent extends StartEventAgent
{
	
	/**
	 */
	@Override
	public void process(AgentContext inContext)
	{
		//Use path-event style exec
		String operation = inContext.getCurrentAgentEnable().getAgentConfig().getId();
		
		operation = operation.replaceFirst("_", ".");
		
		WebPageRequest request =  (WebPageRequest)inContext.getContextValue("webpagerequest");
		runPathEvent(inContext, operation, request);
		getModuleManager().execute(operation,request);
	}

	
}
