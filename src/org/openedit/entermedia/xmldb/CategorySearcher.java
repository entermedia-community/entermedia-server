package org.openedit.entermedia.xmldb;

import org.openedit.data.Searcher;
import org.openedit.entermedia.Category;

public interface CategorySearcher extends Searcher
{
	Category getRootCategory();

	Category getCategory(String inCatalog);

	void saveCategory(Category inCategory);
	//CategoryArchive getCategoryArchive(); //TODO: Remove this API, legacy support
}
