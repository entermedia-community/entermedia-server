package org.entermediadb.elasticsearch.categories;

import java.util.List;

import org.entermediadb.asset.Category;

public class ElasticCategory extends Category
{
	protected ElasticCategorySearcher fieldCategorySearcher;
	
	public ElasticCategorySearcher getCategorySearcher() {
		return fieldCategorySearcher;
	}

	public ElasticCategory() {
	}
	
	public ElasticCategory(ElasticCategorySearcher inElasticCategorySearcher) {
		fieldCategorySearcher =inElasticCategorySearcher;
	}

	public String getName() 
	{
		return get("name");
	}
	
	public void setName(String inName)
	{
		setProperty("name",inName);
	}
	public boolean hasChildren()
	{
		return getChildren().size() > 0;
	}
	public List getChildren()
	{
		boolean dirty = getCategorySearcher().getRootCategory().refresh();
		if( dirty)
		{
			fieldChildren = null;
		}
		if (fieldChildren == null)
		{
			fieldChildren = getCategorySearcher().findChildren(this);
		}
		return fieldChildren;
	}
//	public Category addChild(Category inNewChild)
//	{
//		super.addChild(inNewChild);
//		//add row to DB
//		getCategorySearcher().saveData(inNewChild, null);
//		
//		return inNewChild;
//	}
	public String get(String inKey)
	{
		String val = getProperty(inKey);
		return val;
	}
	
	public void setProperty(String inKey, String inValue)
	{
		if (inValue != null)
		{
			if( "id".equals(inKey) )
			{
				setId(inValue);
			}
			if("parentid".equals(inKey) && inValue.equals(getId())){
				return;
			}
			else
			{
				getProperties().put(inKey, inValue);
			}
		}
		else
		{
			getProperties().remove(inKey);
		}
	}
	
	public Category getParentCategory() 
	{
		if( fieldParentCategory == null && !"index".equals(getId()) )
		{
			String parentid = getProperty("parentid");
			fieldParentCategory = (Category)getCategorySearcher().searchById(parentid);
		}
		return fieldParentCategory;
	}
	
	
	public boolean refresh()
	{
		boolean dirty = isPropertyTrue("dirty");
		if( dirty )
		{
			fieldChildren = null;
			setProperty("dirty", false);
			return true;
		}
		return false;
	}
	
	
}
