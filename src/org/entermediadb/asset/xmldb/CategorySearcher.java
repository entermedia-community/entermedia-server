package org.entermediadb.asset.xmldb;

import java.util.List;
import java.util.Set;

import org.entermediadb.asset.Category;
import org.openedit.data.Searcher;

public interface CategorySearcher extends Searcher
{
	Category getRootCategory();

	Category getCategory(String inCatalog);

	void saveCategory(Category inCategory);
	//CategoryArchive getCategoryArchive(); //TODO: Remove this API, legacy support

	Category createCategoryPath(String inPath);

	void deleteCategoryTree(Category inChild);

	List findChildren(Category inElasticCategory);
	
	void clearCategories();
	
	public Set buildCategorySet(List inCategories);
	public Set buildCategorySet(Category inCategory);

	public Category loadCategoryByPath(String categorypath);

	public List listAllCategories(Category inTopCategory);
    public void saveCategoryTree(Category inRootCategory);

	
	
}
