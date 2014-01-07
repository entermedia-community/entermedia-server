package model.projects;

import org.openedit.Data;

public class UserCollection
{
	protected Data fieldCollection;
	
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