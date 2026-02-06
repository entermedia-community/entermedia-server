package org.entermediadb.find;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.entermediadb.asset.Category;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.data.ValuesMap;

public class LightBox implements Data
{
	protected Data fieldData;
	
	public String getId()
	{
		return getData().getId();
	}
	
	public String getName()
	{
		return getData().getName();
	}
	public Object getValue(String inName)
	{
		return getData().getValue(inName);
	}
	public Data getData()
	{
		return fieldData;
	}
	public void setData(Data inData)
	{
		fieldData = inData;
	}
	public int getAssetCount()
	{
		return fieldAssetCount;
	}
	public void setAssetCount(int inAssetCount)
	{
		fieldAssetCount = inAssetCount;
	}
	public Category getRootCategory()
	{
		return fieldRootCategory;
	}
	public void setRootCategory(Category inRootCategory)
	{
		fieldRootCategory = inRootCategory;
	}
	protected int fieldAssetCount;
	protected Category fieldRootCategory;
	
	@Override
	public void setId(String inNewid)
	{
		
		
	}

	@Override
	public String getName(String inLocale)
	{
		
		return getData().getName(inLocale);
	}

	@Override
	public void setName(String inName)
	{
		
		
	}

	@Override
	public void setSourcePath(String inSourcepath)
	{
		
		
	}

	@Override
	public String getSourcePath()
	{
		
		return null;
	}

	@Override
	public void setProperty(String inId, String inValue)
	{
		
		
	}

	@Override
	public String get(String inId)
	{
		
		return null;
	}

	@Override
	public void setValue(String inKey, Object inValue)
	{
		
		
	}

	@Override
	public ValuesMap getProperties()
	{
		
		return null;
	}

	@Override
	public void setProperties(Map inObjects)
	{
		
		
	}

	@Override
	public Set keySet()
	{
		
		return null;
	}

	@Override
	public Collection getValues(String inField)
	{
		
		return null;
	}
	
	public String toJsonString()
	{		
		throw new OpenEditException();
	}
	
}
