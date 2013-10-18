package org.openedit.entermedia.xmldb;

import org.openedit.entermedia.Category;
import org.openedit.entermedia.CategoryArchive;

public interface CategorySearcher
{
	Category getRootCategory();

	CategoryArchive getCategoryArchive(); //TODO: Remove this API, legacy support
}
