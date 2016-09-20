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
	public Category getParentCategory() 
	{
		String parentid = get("parentid");
		if( parentid != null && fieldParentCategory == null && !"index".equals(getId()) && !parentid.equals(getId()))
		{
			fieldParentCategory = (Category)getCategorySearcher().searchById(parentid);
		}
		return fieldParentCategory;
	}

	
	
}
