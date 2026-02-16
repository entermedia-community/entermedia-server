package org.entermediadb.ai.assistant;

import org.json.simple.JSONObject;

public class AiCreation
{
	
	String fieldCreationFunction;
	
	String fieldCreationModule; 
	
	JSONObject fieldCreationFields;
	
	public String getCreationFunction()
	{
		return fieldCreationFunction;
	}
	public void setCreationFunction(String inCreationType)
	{
		fieldCreationFunction = inCreationType;
	}
	
	public String getCreationModule()
	{
		return fieldCreationModule;
	}
	public void setCreationModule(String inCreationModule)
	{
		fieldCreationModule = inCreationModule;
	}
	
	public JSONObject getCreationFields()
	{
		return fieldCreationFields;
	}
	public void setCreationFields(JSONObject inCreationFields)
	{
		fieldCreationFields = inCreationFields;
	}
	
	
}
