/*
 * Created on Apr 30, 2004
 */
package org.entermediadb.model;

import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.Category;

/**
 * @author cburkey
 *
 */
public class CategoryEditTest extends BaseEnterMediaTest
{

	/**
	 * Constructor for ItemEditTest.
	 * @param arg0
	 */
	public CategoryEditTest(String arg0)
	{
		super(arg0);
	}

	protected void setUp() throws Exception
	{
		Category blank = getCategoryEditor().getCategory("GOODSTUFF"); 
		if( blank == null)
		{
			blank = getCategoryEditor().addNewCategory( "GOODSTUFF","Some Good Stuff");
			getCategoryEditor().saveCategory(blank);
		}
		assertNotNull( blank );
	}
	
	public void testDeleteCategory() throws Exception
	{
		Category category = getCategoryEditor().getCategory("GOODSTUFF");
		assertNotNull( category );
		getCategoryEditor().deleteCategory(category);
		getMediaArchive().clearAll();

		Category deletedCategory = getCategoryEditor().getCategory("GOODSTUFF");
		assertNull( deletedCategory );
		getCategoryEditor().saveCategory(category);
	}
	
	public void testListCatalogs() throws Exception
	{
		Category category = getMediaArchive().getCategoryArchive().getRootCategory();
		assertNotNull(category);
		assertTrue(category.getChildren().size() > 0);
	}

	
}
