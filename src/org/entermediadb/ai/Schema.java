package org.entermediadb.ai;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.openedit.Data;

public class Schema
{
	Map<String,Collection<Data>> fieldChildren;
	
	public Collection<Data> getChildrenOf(String inModuleId)
	{
		Collection<Data> children = getChildren().get(inModuleId);
		if( children == null)
		{
			children = new HashSet();
			 getChildren().put( inModuleId,children);
		}
		return children;
	}
	
	public Map<String, Collection<Data>> getChildren()
	{
		if( fieldChildren == null)
		{
			fieldChildren = new HashMap();
		}
		return fieldChildren;
	}
	public void setChildren(Map<String, Collection<Data>> inChildren)
	{
		fieldChildren = inChildren;
	}
	public Collection<Data> getModules()
	{
		return fieldModules;
	}
	public void setModules(Collection<Data> inModules)
	{
		fieldModules = inModules;
	}
	public Collection<String> getModuleIds()
	{
		return fieldModuleIds;
	}
	public void setModuleIds(Collection<String> inModuleIds)
	{
		fieldModuleIds = inModuleIds;
	}
	Collection<Data> fieldModules;
	Collection<String> fieldModuleIds;

	public void addChildOf(String inId, Data inChildmodule)
	{
		getChildrenOf(inId).add(inChildmodule);
	}
	
	public boolean hasChildren(String inId)
	{
		return getChildrenOf(inId).size() > 0;
	}
	
	
}
