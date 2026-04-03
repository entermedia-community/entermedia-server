package org.entermediadb.ai;

import java.util.Collection;
import java.util.Iterator;

import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.AgentEnabled;
import org.openedit.CatalogEnabled;

public class BaseAgent extends BaseAiManager implements Agent, CatalogEnabled 
{
	public void processstart(AgentContext inContext)
	{
		//fire websocket event
	}
	
	public void processend(AgentContext inContext)
	{
	}
	
	@Override
	public void process(AgentContext inContext)
	{
		AgentEnabled enabled = inContext.getCurrentAgentEnable();
		if( enabled != null)
		{
			//set previous
			Collection<AgentEnabled> children = enabled.getChildren();
			for (Iterator iterator = children.iterator(); iterator.hasNext();)
			{
				AgentEnabled agentEnabled = (AgentEnabled) iterator.next();
				inContext.setCurrentAgentEnable(agentEnabled);
				agentEnabled.getAgent().processstart(inContext);
				agentEnabled.getAgent().process(inContext);
				agentEnabled.getAgent().processend(inContext);
			}
		}
	}
	

}
