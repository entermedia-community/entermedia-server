package org.entermediadb.ai.assistant;

import org.json.simple.JSONObject;

public class AiCreation
{
	
	String fieldCreationType;
	
	String fieldImagePrompt;
	
	JSONObject fieldEntityFields;
	
	public String getCreationType()
	{
		return fieldCreationType;
	}
	public void setCreationType(String inCreationType)
	{
		fieldCreationType = inCreationType;
	}
	
	public String getImagePrompt()
	{
		return fieldImagePrompt;
	}
	public void setImagePrompt(String inPrompt)
	{
		fieldImagePrompt = inPrompt;
	}
	
	public JSONObject getEntityFields()
	{
		return fieldEntityFields;
	}
	public void setEntityFields(JSONObject inEntityFields)
	{
		fieldEntityFields = inEntityFields;
	}
	
}
