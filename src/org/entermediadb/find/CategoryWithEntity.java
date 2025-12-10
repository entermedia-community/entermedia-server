package org.entermediadb.find;

import org.entermediadb.asset.Category;
import org.openedit.Data;

public class CategoryWithEntity
{
	public Category getCategory()
	{
		return fieldCategory;
	}
	public void setCategory(Category inCategory)
	{
		fieldCategory = inCategory;
	}
	public Data getEntity()
	{
		return fieldEntity;
	}
	public void setEntity(Data inEntity)
	{
		fieldEntity = inEntity;
	}
	public Data getEntityModule()
	{
		return fieldEntityModule;
	}
	public void setEntityModule(Data inEntityModule)
	{
		fieldEntityModule = inEntityModule;
	}
	protected Category fieldCategory;
	protected Data fieldEntity;
	protected Data fieldEntityModule;
}

