/*
 * Created on Oct 3, 2004
 */
package org.entermediadb.asset;

import java.util.List;

import org.openedit.CatalogEnabled;
import org.openedit.OpenEditException;

/**
 * @author cburkey
 * 
 */
public interface CategoryArchive extends CatalogEnabled
{
	Category getCategory(String inCatalog);

	Category getCategoryByName(String inCatalogName);

	List listAllCategories();

//public Category cacheCategory(Category inCatalog);

//	public Category addChild(Category inCatalog);

	public void deleteCategory(Category inCatalog);

	//void setRootCategory(Category inRoot);

	Category getRootCategory();

	/**
	 * Blows away all children
	 * 
	 * @param inRoot
	 */
	public void clearCategories();

	void reloadCategories();

	// void saveCatalog( Category inCatalog) ;
	void saveAll();

	public void setCatalogId(String inCategoryId);

	void saveCategory(Category inCategory);

	public Category createNewCategory(String inLabel);
	
	public Category createCategoryTree(String inPath) throws OpenEditException;
}
