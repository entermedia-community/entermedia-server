package org.entermediadb.ai.assistant;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.util.JSONParser;

public class AiSearch extends JSONObject
{
	String originalSearch;
	
	public String getOriginalSearchString()
	{
		return originalSearch;
	}
	
	public void setOriginalSearchString(String inOriginalSearch)
	{
		originalSearch = inOriginalSearch;
	}
	
	public Data getParentModule()
	{
		return fieldParentModule;
	}
	public void setParentModule(Data inParentModule)
	{
		fieldParentModule = inParentModule;
	}
	public Data getChildModule()
	{
		return fieldChildModule;
	}
	public void setChildModule(Data inChildModule)
	{
		fieldChildModule = inChildModule;
	}

	Data fieldParentModule;
	Data fieldChildModule;
	protected AiSearchStep fieldStep1;
	protected AiSearchStep fieldStep2;
	protected AiSearchStep fieldStep3;
	
	public AiSearchStep getStep1()
	{
		return fieldStep1;
	}
	public void setStep1(AiSearchStep inStep1)
	{
		fieldStep1 = inStep1;
	}
	public AiSearchStep getStep2()
	{
		return fieldStep2;
	}
	public void setStep2(AiSearchStep inStep2)
	{
		fieldStep2 = inStep2;
	}
	public AiSearchStep getStep3()
	{
		return fieldStep3;
	}
	public void setStep3(AiSearchStep inStep3)
	{
		fieldStep3 = inStep3;
	}


	
//	public String toSemanticQuery() {
//		return String.join(" ", fieldKeywords);
//	}
	public String toJson() 
	{
		JSONObject parent = new JSONObject();
		parent.put("search",this);
		
		JSONArray parts  = new JSONArray();
		if( getStep1() != null)
		{
			parts.add(getStep1());
		}
		if( getStep2() != null)
		{
			parts.add(getStep2());
		}
		if( getStep3() != null)
		{
			parts.add(getStep3());
		}
		return parent.toJSONString();
	}
	
	public void loadJsonParts(String inJson)
	{
		//Read the parts
		JSONObject parent = new JSONParser().parse(inJson);
		//TODO Finish loading from DB
		
	}
	
	
	
}
