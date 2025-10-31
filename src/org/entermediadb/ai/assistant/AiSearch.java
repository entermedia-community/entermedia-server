package org.entermediadb.ai.assistant;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.util.JSONParser;

public class AiSearch extends JSONObject
{
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
	AiSearchPart fieldPart1;
	
	public AiSearchPart getPart1()
	{
		return fieldPart1;
	}
	public void setPart1(AiSearchPart inPart1)
	{
		fieldPart1 = inPart1;
	}
	public AiSearchPart getPart2()
	{
		return fieldPart2;
	}
	public void setPart2(AiSearchPart inPart2)
	{
		fieldPart2 = inPart2;
	}
	public AiSearchPart getPart3()
	{
		return fieldPart3;
	}
	public void setPart3(AiSearchPart inPart3)
	{
		fieldPart3 = inPart3;
	}

	protected AiSearchPart fieldPart2;
	protected AiSearchPart fieldPart3;
	
//	public String toSemanticQuery() {
//		return String.join(" ", fieldKeywords);
//	}
	public String toJson() 
	{
		JSONObject parent = new JSONObject();
		parent.put("search",this);
		
		JSONArray parts  = new JSONArray();
		if( getPart1() != null)
		{
			parts.add(getPart1());
		}
		if( getPart2() != null)
		{
			parts.add(getPart2());
		}
		if( getPart3() != null)
		{
			parts.add(getPart3());
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
