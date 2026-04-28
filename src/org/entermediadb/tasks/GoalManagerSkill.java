package org.entermediadb.tasks;

import org.entermediadb.ai.automation.agents.ToolsCallingSkill;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.AgentEnabled;

public class GoalManagerSkill extends ToolsCallingSkill {
    @Override
    public void process(AgentContext inContext) {
        AgentEnabled currentEnabled = inContext.getCurrentAgentEnable();

        super.process(inContext);
    }

}
