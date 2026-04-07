package org.entermediadb.ai.automation.agents;

import java.util.Collection;

import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.AgentEnabled;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.json.simple.JSONObject;
import org.openedit.util.JSONParser;

public class ToolsCallingAgent extends BaseAgent
{
	@Override
	public void process(AgentContext inContext)
	{

		JSONParser parser = new JSONParser();

		AgentEnabled currentEnabled = inContext.getCurrentAgentEnable();
		Collection<AgentEnabled> enabledChildren = currentEnabled.getChildren();

		for (AgentEnabled enabled : enabledChildren)
		{
			String paramstructure = enabled.getAutomationEnabledData().get("parameterstructure");
			if (paramstructure != null)
			{
				Collection paramstructurejson = parser.parseCollection(paramstructure);
				enabled.setAgentParameterStructure(paramstructurejson);
			}
		}

		if (enabledChildren.size() > 0)
		{
			String function = "agentdecision";
			if (enabledChildren.size() == 1)
			{
				// check if a param call is necessary
				AgentEnabled enabled = enabledChildren.iterator().next();
				Collection<JSONObject> paramstructure = enabled.getAgentParameterStructure();
				if (paramstructure == null || paramstructure.size() == 0)
				{
					super.process(inContext);
					return;
				}
				function = "agentparams";
				inContext.put("agentenabled", enabled);
			}
			else
			{
				inContext.info("Multiple child agents, invoking decision agent");
				inContext.put("enabledchildren", enabledChildren);
			}

			LlmConnection llmConnection = getMediaArchive().getLlmConnection(function);
			LlmResponse res = llmConnection.callToolsFunction(inContext, function);

			String selectedagent = (String) res.getFunctionName();
			JSONObject params = (JSONObject) res.getFunctionArguments();

			super.process(inContext);
		}

		// for (Iterator<AgentEnabled> it = enabledChildren.iterator(); it.hasNext();) {
		// AgentEnabled agentenabledchild = it.next();
		// if( agentenabledchild.getAgentConfig().getId().equals(selectedagent))
		// {
		// // selected agent
		// for (String key : params.keySet()) {
		// Object value = params.get(key);
		// subContext.put(key, value);
		// }
		// agentenabledchild.getAgent().process(subContext);
		// }
		// }
		super.process(inContext);
	}
}
