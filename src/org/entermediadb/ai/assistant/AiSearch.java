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
	protected AiSearchTable fieldStep1;
	protected AiSearchTable fieldStep2;
	protected AiSearchTable fieldStep3;
	
	public AiSearchTable getStep1()
	{
		return fieldStep1;
	}
	public void setStep1(AiSearchTable inStep1)
	{
		fieldStep1 = inStep1;
	}
	public AiSearchTable getStep2()
	{
		return fieldStep2;
	}
	public void setStep2(AiSearchTable inStep2)
	{
		fieldStep2 = inStep2;
	}
	public AiSearchTable getStep3()
	{
		return fieldStep3;
	}
	public void setStep3(AiSearchTable inStep3)
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
