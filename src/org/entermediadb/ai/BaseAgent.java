package org.entermediadb.ai;

import java.util.Collection;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.AgentEnabled;
import org.openedit.CatalogEnabled;

public class BaseAgent extends BaseAiManager implements Agent, CatalogEnabled
{
	public void processstart(AgentContext inContext)
	{
		// fire websocket event
	}

	public void processend(AgentContext inContext)
	{}

	public AgentContext createAgentContext(AgentContext inParentContext, AgentEnabled inEnabled)
	{
		String contextbeanname = inEnabled.getAgentData().get("contextbean");

		if (contextbeanname == null)
		{
			contextbeanname = "agentContext";
		}
		AgentContext childContext = (AgentContext) getMediaArchive().getBean(contextbeanname, false);
		childContext.setCurrentAgentEnable(inEnabled);
		childContext.setParentContext(inParentContext);
		return childContext;
	}

	@Override
	public void process(AgentContext inContext)
	{
		Collection<AgentEnabled> children = inContext.getAgentEnableChildren();
		for (AgentEnabled agentEnabled : children)
		{
			AgentContext childContext = createAgentContext(inContext, agentEnabled);

			agentEnabled.getAgent().processstart(childContext);
			agentEnabled.getAgent().process(childContext);
			agentEnabled.getAgent().processend(childContext);
		}
	}
}
