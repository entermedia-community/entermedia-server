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

	MultiValued fieldAgentData;

	public MultiValued getAgentData()
	{
		return fieldAgentData;
	}

	public Object getValue(String inField)
	{
		Object value = getAutomationEnabledData().getValue(inField);
		if (value == null)
		{
			value = getAgentData().getValue(inField);
		}
		return value;
	}

	public String get(String inField)
	{
		String value = getAutomationEnabledData().get(inField);
		if (value == null)
		{
			value = getAgentData().get(inField);
		}
		return value;
	}

	public void setAgentData(MultiValued inAgentData)
	{
		fieldAgentData = inAgentData;
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

	public AgentEnabled getChildren(String inId)
	{
		AgentEnabled selected = null;
		for (AgentEnabled child : getChildren())
		{
			String id = child.get("id");
			if (id.equals(inId))
			{
				selected = child;
				break;
			}
		}
		return selected;
	}

	public void addChild(AgentEnabled inChildAgent)
	{
		getChildren().add(inChildAgent);
		inChildAgent.setParentAgentEnabled(this);
	}

	@Override
	public String toString()
	{
		return String.valueOf(getAgentData());
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

	JSONObject fieldAgentParameterValues;

	public JSONObject getAgentParameterValues()
	{
		return fieldAgentParameterValues;
	}

	public void setAgentParameterValues(JSONObject inAgentParameterValues)
	{
		fieldAgentParameterValues = inAgentParameterValues;
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
