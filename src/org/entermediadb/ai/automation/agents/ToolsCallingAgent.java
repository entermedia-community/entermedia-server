package org.entermediadb.ai.automation.agents;

import java.util.Collection;
import java.util.Iterator;

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
		AgentContext subcontext = new AgentContext(inContext);
		subcontext.put("previousagent", inContext.getCurrentAgentEnable().getAgentConfig().getId());
		// subcontext.put("previousoutput", inContext.getCurrentAgentEnable())); TODO
		subcontext.put("previousoutput", "From<hi@shakil.info>\\nSubject: Hello!\\nMessage: What is your opening hours?\\n\\n-Regards,\\nShakil");


		Collection<AgentEnabled> enabledchildren = inContext.getCurrentAgentEnable().getChildren();
		if( enabledchildren.size() > 1)
		{
			JSONParser parser = new JSONParser();

			for (Iterator<AgentEnabled> it = enabledchildren.iterator(); it.hasNext();) {
					AgentEnabled agentenabledchild = it.next();

					String paramstructure = agentenabledchild.getAutomationEnabledData().get("parameterstructure");
					if( paramstructure != null)
					{
						Collection paramstructurejson = parser.parseCollection(paramstructure);
						agentenabledchild.setAgentParameterStructure(paramstructurejson);
					}
			}

			

			LlmConnection llmConnection =	getMediaArchive().getLlmConnection("agentdecision");

			subcontext.put("enabledchildren", enabledchildren);

			LlmResponse res = llmConnection.callToolsFunction(subcontext, "agentdecision");

			String selectedagent = (String) res.getFunctionName();
			JSONObject params = (JSONObject) res.getFunctionArguments();

			for (Iterator<AgentEnabled> it = enabledchildren.iterator(); it.hasNext();) {
				AgentEnabled agentenabledchild = it.next();
				if( agentenabledchild.getAgentConfig().getId().equals(selectedagent))
				{
					// selected agent
					for (String key : params.keySet()) {
						Object value = params.get(key);
						subcontext.put(key, value);
					}
					agentenabledchild.getAgent().process(subcontext);
				}
			}
		}
		else
		{
			super.process(inContext);
		}
	}
}
