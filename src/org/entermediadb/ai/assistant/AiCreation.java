package org.entermediadb.ai.assistant;

import org.json.simple.JSONObject;

public class AiCreation
{
	
	String fieldCreationType;
	String fieldPrompt;
	
	String fieldEntityName;
	String fieldModuleId;
	
	public String getCreationType()
	{
		return fieldCreationType;
	}
	public void setCreationType(String inCreationType)
	{
		fieldCreationType = inCreationType;
	}
	
	public String getPrompt()
	{
		return fieldPrompt;
	}
	public void setPrompt(String inPrompt)
	{
		fieldPrompt = inPrompt;
	}
	
	public String getEntityName()
	{
		return fieldEntityName;
	}
	public void setEntityName(String inEntityName)
	{
		fieldEntityName = inEntityName;
	}
	
	public String getModuleId()
	{
		return fieldModuleId;
	}
	public void setModuleId(String inModuleId)
	{
		fieldModuleId = inModuleId;
	}
	
}
