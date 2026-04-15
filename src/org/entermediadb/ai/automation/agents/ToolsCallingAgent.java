package org.entermediadb.ai.automation.agents;

import java.util.Collection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.AgentEnabled;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.json.simple.JSONObject;
import org.openedit.util.JSONParser;

public class ToolsCallingAgent extends BaseAgent
{
	private static final Log log = LogFactory.getLog(ToolsCallingAgent.class);

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
			String function;
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
				function = "agentdecision";
				inContext.put("enabledchildren", enabledChildren);
			}

			String context_agentid = (String) inContext.getParentContext().getContext().get("agentid");
			String context_agentoutput = (String) inContext.getParentContext().getContext().get("agentoutput");

			inContext.put("agentid", context_agentid);
			inContext.put("agentoutput", context_agentoutput);

			LlmConnection llmConnection = getMediaArchive().getLlmConnection(function);
			LlmResponse res = llmConnection.callToolsFunction(inContext, function);

			String selectedagentid = (String) res.getFunctionName();

			AgentEnabled selectedenabled = currentEnabled.getChildren(selectedagentid);
			if (selectedenabled != null)
			{
				JSONObject params = (JSONObject) res.getFunctionArguments();
				selectedenabled.setAgentParameterValues(params);
				inContext.setAgentEnableChildren(selectedenabled);
			}
			else
			{
				inContext.error("Couldn't decide next agent for " + currentEnabled.getAgentData().getId());
			}
		}

		super.process(inContext);
	}
}
