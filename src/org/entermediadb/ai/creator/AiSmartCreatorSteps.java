package org.entermediadb.ai.creator;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.data.BaseData;

public class AiSmartCreatorSteps extends BaseData
{
	protected Data fieldTargetModule;
	protected Data fieldTargetEntity;
	
	public Data getTargetEntity()
	{
		return fieldTargetEntity;
	}

	public void setTargetEntity(Data inPlayback)
	{
		fieldTargetEntity = inPlayback;
	}
	
	public void setNewTitleName(String inNewTitleName)
	{
		fieldNewTitleName = inNewTitleName;
	}

	protected Collection<String> fieldProposedSections;
	public Collection<String> getProposedSections()
	{
		return fieldProposedSections;
	}

	public void setProposedSections(Collection<String> inProposedSections)
	{
		fieldProposedSections = inProposedSections;
	}
	
	protected Collection<Data> fieldConfirmedSections;
	public Collection<Data> getConfirmedSections()
	{
		return fieldConfirmedSections;
	}

	public void setConfirmedSections(Collection<Data> inConfirmedSections)
	{
		fieldConfirmedSections = inConfirmedSections;
	}

	public Map<Data, Collection<Data>> getSectionComponents()
	{
		return fieldSectionComponents;
	}

	public void setSectionComponents(Map<Data, Collection<Data>> inSectionComponents)
	{
		fieldSectionComponents = inSectionComponents;
	}

	protected Map<Data,Collection<Data>> fieldSectionComponents;
	
	public Data getTargetModule()
	{
		return fieldTargetModule;
	}

	public void setTargetModule(Data inTargetModule)
	{
		fieldTargetModule = inTargetModule;
	}

	public Data getCreatedEntity()
	{
		return fieldCreatedEntity;
	}

	public void setCreatedEntity(Data inCreatedEntity)
	{
		fieldCreatedEntity = inCreatedEntity;
	}

	public String getStep1create()
	{
		return step1create;
	}

	public void setStep1create(String inStep1create)
	{
		step1create = inStep1create;
	}

	public String getStep2create()
	{
		return step2create;
	}

	public void setStep2create(String inStep2create)
	{
		step2create = inStep2create;
	}

	protected Data fieldCreatedEntity;
	
	//Step 1 Create a simple list of index/outline for Theory of change instruction then in step 2 Create a detailed description of  each section that is relevant to the topic
	
	//Creating Web Site Content: Step 1 Create a home page hero section then popuplate a title and content

	//Create a blog with a title of "My trip to Guatemala" then Step 2 describe the best foods I ate
	String step1create = "Create a blog with a title of \"My trip to Guatemala\"";
	String step2create = "write 3 paraphaphs that describe the best foods I ate";   //One component-section with 1 title and 3 paragraphs
	
//	public String toSemanticQuery() {
//		return String.join(" ", fieldKeywords);
//	}
	public String toJson() 
	{
		JSONObject parent = new JSONObject();
		parent.put("creationdetails",this);
		
		JSONArray parts  = new JSONArray();
	
		return parent.toJSONString();
	}
	
	public void loadJsonParts(JSONObject inJson)
	{
		//Read the parts
		JSONArray paragraphs = (JSONArray)inJson.get("instructions");
		
		Iterator iter = paragraphs.iterator();
		JSONObject first = (JSONObject)iter.next();

		//TOOD: Make this more general, Like one sentance with both instructions in it
		
		step1create = (String)first.get("text");//"Create a blog with a title of \"My trip to Guatemala\"";
		
		fieldNewTitleName = (String)first.get("topic");
		JSONObject second = (JSONObject) iter.next();
		if( second != null)
		{
			step2create = (String) second.get("text"); //"write 3 paraphaphs that describe the best foods I ate";   //One component-section with 1 title and 3 paragraphs
		}
	}

	protected String fieldNewTitleName;
	
	public String getNewTitleName()
	{
		return fieldNewTitleName;
	}
	
}
