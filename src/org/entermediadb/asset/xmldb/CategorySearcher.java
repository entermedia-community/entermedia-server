package org.entermediadb.asset.xmldb;

import java.util.List;

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
}
