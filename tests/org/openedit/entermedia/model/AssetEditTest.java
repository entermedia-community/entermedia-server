/*
 * Created on Apr 30, 2004
 */
package org.openedit.entermedia.model;

import java.util.Iterator;
import java.util.List;

import org.openedit.entermedia.Asset;
import org.openedit.entermedia.BaseEnterMediaTest;
import org.openedit.entermedia.Category;
import org.openedit.entermedia.RelatedAsset;
import org.openedit.entermedia.edit.CategoryEditor;

import com.openedit.users.User;

/**
 * @author cburkey
 *
 */
public class AssetEditTest extends BaseEnterMediaTest
{

	/**
	 * Constructor for ItemEditTest.
	 * @param arg0
	 */
	public AssetEditTest(String arg0)
	{
		super(arg0);
	}

	protected void setUp() throws Exception
	{
		Category blank = getMediaArchive().getCategoryEditor().addNewCategory( "GOODSTUFF","Some Good Stuff");
		assertNotNull( blank );
		getCategoryEditor().saveCategory( blank );
	}
	
	public void testAddAndEditAsset() throws Exception
	{
		Asset asset = createAsset();
		Category category = getCategoryEditor().getCategory( "GOODSTUFF" );
		asset.addCategory( category );
		User user = getFixture().createPageRequest().getUser();
		getMediaArchive().saveAsset(asset, user);
		
		Asset foundAsset = findAssetById(category, asset.getId());
		
		//if this is null then the update index is not working
		assertNotNull(foundAsset);
		String name = "New Name" + System.currentTimeMillis();
		foundAsset.setName(name);
		getMediaArchive().saveAsset(foundAsset, user);
		List products = getMediaArchive().getAssetsInCategory(category);
		int productsFound = 0;
		for (Iterator iter = products.iterator(); iter.hasNext();)
		{
			Asset element = (Asset) iter.next();
			if ( element.getName().equals(name))
			{
				productsFound++;
			}
		}
		assertEquals( "Number of products with the right name in the catalog",
			1, productsFound );
	}

	private Asset findAssetById(Category inCategory, String inId) throws Exception
	{
		List products = getMediaArchive().getAssetsInCategory(inCategory);
		assertNotNull(products);

		for (Iterator iter = products.iterator(); iter.hasNext();)
		{
			Asset element = (Asset) iter.next();
			if ( element.getId().equals(inId))
			{
				return element;
			}
		}
		return null;
	}
	
	public void testRemoveFromCategory() throws Exception
	{
		Asset asset = createAsset();
		Category good = getMediaArchive().getCategory("GOODSTUFF");
		asset.addCategory(good);
		User user = getFixture().createPageRequest().getUser();
		getMediaArchive().saveAsset(asset, user);

		asset = findAssetById(good, asset.getId());
		assertNotNull( asset ); //this passes on a local machine
		
		CategoryEditor editor = getCategoryEditor();
		editor.removeAssetFromCategory(good,new String[]{asset.getId()});
		assertNull( findAssetById(good, asset.getId()) );
	}

	public void testAddAndDeleteAsset() throws Exception
	{
		Asset asset = createAsset();
		Category category = getMediaArchive().getCategory( "GOODSTUFF" );
		asset.addCategory( category );
		User user = getFixture().createPageRequest().getUser();
		getMediaArchive().saveAsset(asset, user);
		getCategoryEditor().clearCategories();
		
		String assetId = asset.getId();
		asset = findAssetById(category, assetId);
		assertNotNull(asset);

		getAssetEditor().deleteAsset( asset );
		getCategoryEditor().clearCategories();

		asset = findAssetById(category, assetId);
		assertNull(asset);
	}
	
	public void testEditAssetProperties() throws Exception
	{
		String originaltext = "Some weird & whacky product attribute's to \"insert\"";
		Asset asset = createAsset();
		asset.setProperty("insertdata", originaltext);
		
		User user = getFixture().createPageRequest().getUser();
		getMediaArchive().saveAsset(asset, user);
		getMediaArchive().getAssetArchive().clearAssets();
		asset = getMediaArchive().getAsset(asset.getId());
		String returntext = asset.getProperty("insertdata");
		assertEquals(originaltext, returntext);
	}

	public void testKeyordExportImport() throws Exception
	{
		Asset asset = createAsset();
		String input = "one | two | tree";
		asset.setProperty("keywords", input);
		assertEquals(3,asset.getKeywords().size());
		assertEquals(input,asset.get("keywords") );
	}
	public void testEditRelatedAssets() throws Exception
	{
		Asset asset = createAsset();
		assertTrue(asset.getRelatedAssets().size() ==0);
		
		User user = getFixture().createPageRequest().getUser();
		getMediaArchive().saveAsset(asset, user);
		
		Asset asset2 = createAsset();
		getMediaArchive().saveAsset(asset2, user);
		
		RelatedAsset relationship = new RelatedAsset();
		relationship.setType("insert");		
		relationship.setRelatedToAssetId(asset2.getId());
		asset.addRelatedAsset(relationship);
		getMediaArchive().saveAsset(asset, user);
		
		try
		{
			getMediaArchive().getAssetArchive().clearAssets();
			asset = getMediaArchive().getAsset(asset.getId());
			assertEquals("Number of related products", 1, asset.getRelatedAssets().size());
			relationship = (RelatedAsset) asset.getRelatedAssets().iterator().next();
			assertEquals("insert", relationship.getType());
			assertEquals(asset2.getId(), relationship.getRelatedToAssetId());
		}
		finally
		{
			asset.clearRelatedAssets();
			getMediaArchive().saveAsset(asset, user);
		}
	}
	

//	public void testMultipleEdits() throws Exception
//	{
//		Asset product = getMediaArchive().getAsset("1");
//		product.setProperty("stuff", "before");
//		User user = getFixture().createPageRequest().getUser();
//		getMediaArchive().saveAsset(product, user);
//		MediaSearchModule module = (MediaSearchModule)getFixture().getModuleManager().getModule("MediaSearchModule");
//		WebPageRequest req = getFixture().createPageRequest();
//		req.setRequestParameter("query","id:1");
//		//assertNotNull(module.getSearcherManager());
//		HitTracker hits  = module.searchStore(req);
//		String id = hits.getIndexId();
//		req.setRequestParameter("multihitsname", "hitsstore");
//	
//		CategoryEditModule mod = (CategoryEditModule) getFixture().getModuleManager().getModule("CategoryEditModule");
//	
//		mod.createMultiEditData(req);
//		CompositeData data = (CompositeData) req.getSessionValue("multiedit:hitsstore");
//		assertNotNull(data);
//		assertEquals("before", data.get("stuff"));
//		req.setRequestParameter("id" , data.getId());
//		DataEditModule dmodule = (DataEditModule)getFixture().getModuleManager().getModule("DataEditModule");
//		data.setProperty("stuff", "after");
//		dmodule.saveData(req);
//		product = getMediaArchive().getAsset("1");
//		assertEquals("after", product.getProperty("stuff"));
//	}

	
}
