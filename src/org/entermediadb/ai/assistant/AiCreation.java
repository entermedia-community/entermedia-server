package org.entermediadb.ai.assistant;

import org.json.simple.JSONObject;

public class AiCreation
{
	
	String fieldCreationType;
	
	JSONObject fieldImageFields;
	
	JSONObject fieldEntityFields;
	
	public String getCreationType()
	{
		return fieldCreationType;
	}
	public void setCreationType(String inCreationType)
	{
		fieldCreationType = inCreationType;
	}
	
	public JSONObject getImageFields()
	{
		return fieldImageFields;
	}
	public void setImageFields(JSONObject inImageFields)
	{
		fieldImageFields = inImageFields;
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
