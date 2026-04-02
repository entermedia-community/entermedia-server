package org.entermediadb.ai.automation.agents;

import java.util.Collection;
import java.util.Map;

import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.AgentEnabled;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.openedit.MultiValued;
import org.openedit.WebPageRequest;

public class ToolsCallingAgent extends BaseAgent
{
	@Override
	public void process(AgentContext inContext)
	{
		AgentContext subcontext = new AgentContext(inContext);

		LlmConnection llmConnection =	getMediaArchive().getLlmConnection("toolcallagent");
		
		Collection<AgentEnabled> children = inContext.getCurrentAgentEnable().getChildren();
		if( children.size() > 1)
		{
			subcontext.put("agentoptions",children);
			LlmResponse res = llmConnection.callToolsFunction(subcontext,"toolcallagent");

			String selectedagent = (String) res.getMessageStructured().get("selectedagent");
			for (AgentEnabled agentEnabled : children)
			{
				if( agentEnabled.getAgentConfig().getId().equals(selectedagent))
				{
					inContext.info("Selected agent: " + agentEnabled.getAgentConfig().getName());
					subcontext.setCurrentAgentEnable(agentEnabled);
					agentEnabled.getAgent().process(subcontext);
				}
			}
		}
		else
		{
			super.process(inContext);
		}
	}
}
