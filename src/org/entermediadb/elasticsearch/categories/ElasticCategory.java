package org.entermediadb.elasticsearch.categories;

import java.util.List;

import org.entermediadb.asset.Category;
import org.entermediadb.asset.xmldb.CategorySearcher;
import org.openedit.data.ValuesMap;

public class ElasticCategory extends Category
{
	public ElasticCategory(CategorySearcher inSearcher)
	{
		super(inSearcher);
		setIndexId(inSearcher.getIndexId());
	}
	
	public boolean hasChildren()
	{
		return getChildren().size() > 0;
	}
	public List getChildren()
	{
		if( isDirty() )
		{
			fieldChildren = null;
		}
		if (fieldChildren == null)
		{
			fieldChildren = getCategorySearcher().findChildren(this);
			setIndexId(getCategorySearcher().getIndexId());
		}
		return fieldChildren;
	}
	public Category getParentCategory() 
	{
		String parentid = get("parentid");
		if( parentid != null && fieldParentCategory == null && !"index".equals(getId()) && !parentid.equals(getId()))
		{
			fieldParentCategory = (Category)getCategorySearcher().searchById(parentid);
		}
		return fieldParentCategory;
	}

	public ValuesMap getMap() 
	{
		return super.getMap();
	}
	
}
