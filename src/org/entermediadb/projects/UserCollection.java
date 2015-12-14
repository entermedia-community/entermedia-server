package org.entermediadb.projects;

import org.openedit.Data;

public class UserCollection
{
	protected Data fieldCollection;
	protected Data fieldLibrary;
	
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
}