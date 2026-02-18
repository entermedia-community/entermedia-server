package org.entermediadb.ai.creator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.openedit.Data;

public class SmartCreatorSession
{
	protected Collection<Data> fieldSections = null;
	
	public Collection<Data> getSections()
	{
		return fieldSections;
	}
	public void setSections(Collection<Data> inSections)
	{
		fieldSections = inSections;
	}
	public Map<String, Collection<Data>> getSessionComponents()
	{
		if( fieldSessionComponents == null)
		{
			fieldSessionComponents = new HashMap();
		}
		return fieldSessionComponents;
	}
	
	public void setSessionComponents(Map<String, Collection<Data>> inSessionComponents)
	{
		fieldSessionComponents = inSessionComponents;
	}
	
	
	protected Map<String, Collection<Data>> fieldSessionComponents;
	
	public void addSection(Data inSection)
	{
		if( fieldSections == null)
		{
			fieldSections = new ArrayList();
		}
		fieldSections.add(inSection);
	}
	
	public Collection<Data> getSectionComponents(String inSection)
	{
		Map<String, Collection<Data>> sessionComponents = getSessionComponents();
		return sessionComponents.get(inSection);
	}

	
	public Data getSectionComponent(String inSection, String inComponentid)
	{
		Map<String, Collection<Data>> sessionComponents = getSessionComponents();
		Collection<Data> components = sessionComponents.get(inSection);
		if( components != null)
		{
			for (Data component: components)
			{
				if( component.getId().equals(inComponentid))
				{
					return component;
				}
			}
		}
		return null;
	}
	
	public Data getComponent(String inComponentid)
	{
		Map<String, Collection<Data>> sessionComponents = getSessionComponents();
		for (String key: sessionComponents.keySet())
		{
			Collection<Data> components = sessionComponents.get(key);
			if( components != null)
			{
				for (Data component: components)
				{
					if( component.getId().equals(inComponentid))
					{
						return component;
					}
				}
			}
		}
		return null;
	}
	
	public void addComponent(String inSection, Data inComponent)
	{
		Map<String, Collection<Data>> sessionComponents = getSessionComponents();
		Collection<Data> components = sessionComponents.get(inSection);
		if( components == null)
		{
			components = new ArrayList();
			sessionComponents.put(inSection, components);
		}
		components.add(inComponent);
	}
	
	public Map<String, Data> getComponentsBySection(String inSection)
	{
		Map<String, Data> sectionComponents = new HashMap();
		Map<String, Collection<Data>> sessionComponents = getSessionComponents();
		for (String key: sessionComponents.keySet())
		{
			if( key.equals(inSection))
			{
				sectionComponents.put(key, getComponent(key));
			}
		}
		return sectionComponents;
	}
	
	
	
}
