package org.entermediadb.projects;

import java.util.ArrayList;
import java.util.Collection;

import org.openedit.Data;

public class UserCollection
{
	protected Data fieldCollection;
	protected Data fieldLibrary;
	protected Collection fieldCategories;
	
	public Data getLibrary()
	{
		return fieldLibrary;
	}
	public void setLibrary(Data inLibrary)
	{
		fieldLibrary = inLibrary;
	}
	public Data getCollection()
	{
		return fieldCollection;
	}
	public void setCollection(Data inCollection)
	{
		fieldCollection = inCollection;
	}
	
	protected int fieldAssetCount;

	public int getAssetCount()
	{
		return fieldAssetCount;
	}
	public void setAssetCount(int inCount)
	{
		fieldAssetCount = inCount;
	}
	
	public String getName()
	{
		return getCollection().getName();
	}
	public String getId()
	{
		return getCollection().getId();
	}
	public void clearCategories()
	{
		fieldCategories = null;
	}
	
	public Collection<String> getCategories()
	{
		if (fieldCategories == null)
		{
			fieldCategories = new ArrayList();
		}

		return fieldCategories;
	}
	public void addCategory(String inString)
	{
		getCategories().add(inString);
	}
	
	public boolean hasCategories()
	{
		return fieldCategories != null && !fieldCategories.isEmpty();
	}
	public Integer getCategoryCount()
	{
		if( fieldCategories == null || fieldCategories.isEmpty() )
		{
			return null;
		}
		return getCategories().size();
	}
}