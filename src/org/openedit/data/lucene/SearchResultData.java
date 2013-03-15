package org.openedit.data.lucene;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.openedit.Data;
import org.openedit.MultiValued;

import com.openedit.OpenEditException;

public class SearchResultData implements Data, MultiValued
{
	protected final String[] fieldValues; //fixed size value array
	protected final Map<String,Integer> fieldFieldLocations; //shared object

//	public SearchResultData() performance blah...
//	{
//	}
	public SearchResultData(Map<String,Integer> inFieldLocations, String[] inValues)
	{
		fieldValues = inValues;
		fieldFieldLocations = inFieldLocations;
	}

	public Map<String,Integer> getFieldLocations()
	{
		return fieldFieldLocations;
	}
	public String[] getValues()
	{
		return fieldValues;
	}
	public String get(String inId)
	{
		final Integer index = fieldFieldLocations.get(inId);
		if( index == null )
		{
			return null;
		}
		if( fieldValues.length <= index )
		{
			return null;
		}
		return fieldValues[index];
	}

	public String getId()
	{
		return get("id");
	}

	public String getName()
	{
		return get("name");
	}
	public void setName(String inName)
	{
		setProperty("name", inName);
	}
	public void setId(String inNewid)
	{
		throw new OpenEditException("Search results are not editable");
	}

	public void setProperty(String inId, String inValue)
	{
		throw new OpenEditException("Search results are not editable");
	}
	public Iterator keys()
	{
		return getFieldLocations().keySet().iterator();
	}
	public String getSourcePath()
	{
		return get("sourcepath");
	}
	public void setSourcePath(String inSourcepath)
	{
		throw new OpenEditException("Search results are not editable");
	}
	public Map getProperties() 
	{
		Map fields = new HashMap(getFieldLocations().size());
		for (Iterator iterator = keys(); iterator.hasNext();)
		{
			String	key = (String)iterator.next();
			String value = get(key);
			fields.put(key,value);
		}
		return fields;
	}
	@Override
	public void setProperties(Map<String, String> inProperties)
	{
		// TODO Auto-generated method stub
		
	}
	public String toString()
	{
		if(getName() != null){
		return getName();
		} else{
			return getId();
		}
	}

	public Collection getValues(String inKey)
	{
		String val = get(inKey);
		
		if (val == null)
			return null;
		
		String[] vals = null;
		if( val.contains("|") )
		{
			vals = VALUEDELMITER.split(val);
		}
		else
		{
			vals = val.split("\\s+"); //legacy
		}

		Collection collection = Arrays.asList(vals);
		//if null check parent
		return collection;
	}

	@Override
	public void setValues(String inKey, Collection<String> inValues)
	{
		// TODO Auto-generated method stub
		
	}

	
}
