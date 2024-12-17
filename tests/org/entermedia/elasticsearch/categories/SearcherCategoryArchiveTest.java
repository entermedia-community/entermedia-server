package org.entermedia.elasticsearch.categories;


import java.util.List;

import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.CategoryArchive;
import org.entermediadb.elasticsearch.categories.ElasticCategoryArchive;

public class SearcherCategoryArchiveTest  extends BaseEnterMediaTest
{
	public void testVerifyConfiguration()
	{
		CategoryArchive archive = getMediaArchive().getCategoryArchive();
		assertNotNull("asset searcher is NULL!", archive);
		assertTrue( archive instanceof ElasticCategoryArchive );
	}

	public void testLoadTree()
	{
		CategoryArchive archive = getMediaArchive().getCategoryArchive();
		archive.reloadCategories();
		Category root = archive.getRootCategory();
		assertNotNull("root is null",root);
		List children  = root.getChildren();
		//assertTrue("Empty list", children.size() > 0);
	}

	
}
