package org.entermediadb.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class PropertyContainer 
{
	protected Map fieldProperties;
	protected List fieldPropertyChildren;
	
	public List getPropertyChildren() 
	{
		if (fieldPropertyChildren == null) 
		{
			fieldPropertyChildren = new ArrayList();
		}
		return fieldPropertyChildren;
	}

	public void addPropertyChild(PropertyContainer inProps) 
	{
		inProps.putAllProperties(getProperties());
		getPropertyChildren().add(inProps);
	}
	
	public Map getProperties()
	{
		if (fieldProperties == null)
		{
			fieldProperties = new HashMap();
		}

		return fieldProperties;
	}
	
	public String getProperty(String key)
	{
		return (String)getProperties().get(key);
	}
	
	public void putProperty(String inName, String inValue)
	{
		getProperties().put(inName, inValue);
		for (Iterator iterator = getPropertyChildren().iterator(); iterator.hasNext();) 
		{
			PropertyContainer child = (PropertyContainer) iterator.next();
			child.putProperty(inName, inValue);
		}
	}
	
	public void putAllProperties(Map inProperties)
	{
		getProperties().putAll(inProperties);
		for (Iterator iterator = getPropertyChildren().iterator(); iterator.hasNext();) {
			PropertyContainer child = (PropertyContainer) iterator.next();
			child.putAllProperties(inProperties);
		}
	}
}
