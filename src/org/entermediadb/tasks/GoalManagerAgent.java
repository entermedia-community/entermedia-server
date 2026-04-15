package org.entermediadb.tasks;

import org.entermediadb.ai.automation.agents.ToolsCallingAgent;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.AgentEnabled;

public class GoalManagerAgent extends ToolsCallingAgent
{
    @Override
    public void process(AgentContext inContext)
    {
        AgentEnabled currentEnabled = inContext.getCurrentAgentEnable();

        super.process(inContext);
    }

}
