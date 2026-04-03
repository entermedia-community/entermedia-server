package org.entermediadb.ai.llm;

import java.util.ArrayList;
import java.util.Collection;

import org.entermediadb.ai.Agent;
import org.json.simple.JSONObject;
import org.openedit.MultiValued;

public class AgentEnabled
{
	
	AgentEnabled fieldParentAgentEnabled;
	
	public AgentEnabled getParentAgentEnabled()
	{
		return fieldParentAgentEnabled;
	}
	public void setParentAgentEnabled(AgentEnabled inParentAgentEnabled)
	{
		fieldParentAgentEnabled = inParentAgentEnabled;
	}
	MultiValued fieldAgentConfig;
	
	public MultiValued getAgentConfig()
	{
		return fieldAgentConfig;
	}
	public void setAgentConfig(MultiValued inAgentConfig)
	{
		fieldAgentConfig = inAgentConfig;
	}
	MultiValued fieldAutomationEnabledData;
	public MultiValued getAutomationEnabledData()
	{
		return fieldAutomationEnabledData;
	}
	public void setAutomationEnabledData(MultiValued inAutomationEnabledData)
	{
		fieldAutomationEnabledData = inAutomationEnabledData;
	}
	public Agent getAgent()
	{
		return fieldAgent;
	}
	public void setAgent(Agent inAgent)
	{
		fieldAgent = inAgent;
	}
	Agent fieldAgent;
	
	public String getParentAgent()
	{
		String runafter = getAutomationEnabledData().get("runafter");
		return runafter;
	}
	
	Collection<AgentEnabled> fieldChildren;
	public Collection<AgentEnabled> getChildren()
	{
		if (fieldChildren == null)
		{
			fieldChildren = new ArrayList();
		}
		return fieldChildren;
	}
	public void setChildren(Collection<AgentEnabled> inChildren)
	{
		fieldChildren = inChildren;
	}
	public void addChild(AgentEnabled inChildAgent)
	{
		getChildren().add(inChildAgent);
		inChildAgent.setParentAgentEnabled(this);
	}
	@Override
	public String toString()
	{
		return String.valueOf(getAgentConfig());
	}

	Collection<JSONObject> fieldAgentParameterStructure;
	
	public Collection<JSONObject> getAgentParameterStructure()
	{		
		return fieldAgentParameterStructure;
	}

	public void setAgentParameterStructure(Collection<JSONObject> inAgentParameterStructure)
	{		
		fieldAgentParameterStructure = inAgentParameterStructure;
	}

	protected JSONObject fieldExtraContextValues;
	
	public JSONObject getExtraContextValues()
	{
		return fieldExtraContextValues;
	}
	
	public void setExtraContextValues(JSONObject inExtraContextValues)
	{
		fieldExtraContextValues = inExtraContextValues;		
	}
}
