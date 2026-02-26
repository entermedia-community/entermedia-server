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

	protected String fieldTitleName;
	
	public String getTitleName()
	{
		return fieldTitleName;
	}

	public void setTitleName(String inNewTitleName)
	{
		fieldTitleName = inNewTitleName;
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

	public String getStepOutlineCreate()
	{
		return stepOutlineCreate;
	}

	public void setStepOutlineCreate(String inStep1create)
	{
		stepOutlineCreate = inStep1create;
	}

	public String getStepContentCreate()
	{
		return stepContentCreate;
	}

	public void setStepContentCreate(String inStep2create)
	{
		stepContentCreate = inStep2create;
	}
	
	public String getStepOutlineStyle()
	{
		return stepOutlineStyle;
	}
	
	public void setStepOutlineStyle(String inStepOutlineStyle)
	{
		stepOutlineStyle = inStepOutlineStyle;
	}
	
	
	public void setStepContentStyle(String inStepContentStyle)
	{
		stepContentStyle = inStepContentStyle;
	}

	protected Data fieldCreatedEntity;
	
	String stepOutlineCreate = null;
	String stepOutlineStyle = null;
	
	String stepContentCreate = null;
	String stepContentStyle = null;

	
	public void loadJsonParts(JSONObject inJson)
	{
//		{
//			"topic": "Employee Code of Conduct",
//			"outline_section": {
//				"instruction": "Create an outline for Employee Code of Conduct."
//			},
//			"section_content": {
//				"instruction": "Make short and concise 2 or 3 paragraph for each section.",
//				"style": "short and concise, 2 or 3 paragraphs"
//			}
//		}

	
		String title = (String) inJson.get("topic");
		setTitleName(title);
		
		JSONObject outline = (JSONObject) inJson.get("outline_section");
		if( outline != null)
		{
			String instruction = (String) outline.get("instruction");
			setStepOutlineCreate(instruction);
			
//			String style = (String) outline.get("style");
//			setStepOutlineStyle(style);
		}
		
		JSONObject content = (JSONObject) inJson.get("section_content");
		if( content != null)
		{
			String instruction = (String) content.get("instruction");
			setStepContentCreate(instruction);
		}
	}
	
	public String getOutlineCreatePrompt()
	{
		String prompt = getStepOutlineCreate();
//		if(getStepOutlineStyle() != null) 
//		{
//			prompt += " " + getStepOutlineStyle();
//		}
		return prompt;
	}
	
	public String getContentCreatePrompt()
	{
		return getStepContentCreate();
	}
	
}
