/*
 * Created on Apr 30, 2004
 */
package org.entermediadb.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.CompositeAsset;
import org.entermediadb.asset.RelatedAsset;
import org.entermediadb.asset.edit.CategoryEditor;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.modules.translations.LanguageMap;
import org.openedit.users.User;

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
		if(getMediaArchive().getAssetSearcher().getAllHits().size() == 0){
			getMediaArchive().getAssetSearcher().reIndexAll();
		}
		Category blank = getCategoryEditor().getCategory("GOODSTUFF"); 
		if( blank == null)
		{
			blank = getCategoryEditor().addNewCategory( "GOODSTUFF","Some Good Stuff");
			getCategoryEditor().saveCategory(blank);
		}
		assertNotNull( blank );
	}
	
	public void xtestAddAndEditAsset() throws Exception
	{
		Asset asset = createAsset();
		Category category = getCategoryEditor().getCategory( "GOODSTUFF" );
		asset.addCategory( category );
		User user = getFixture().createPageRequest().getUser();
		getMediaArchive().saveAsset(asset, user);
		
		 asset = findAssetById(category, asset.getId());
		
		//if this is null then the update index is not working
		assertNotNull(asset);
		String name = "New Name" + System.currentTimeMillis();
		asset.setName(name);
		getMediaArchive().saveAsset(asset, user);
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

		getAssetEditor().deleteAsset( asset, user );
		getCategoryEditor().clearCategories();

		asset = findAssetById(category, assetId);
		assertNull(asset);
	}
	
	public void testEditAssetProperties() throws Exception
	{
		String originaltext = "Some weird & whacky product attribute's to \"insert\"";
		Asset asset = createAsset();
		asset.setProperty("assettitle", originaltext);
		
		User user = getFixture().createPageRequest().getUser();
		getMediaArchive().saveAsset(asset, user);
		asset = getMediaArchive().getAsset(asset.getId());
		String returntext = asset.get("assettitle");
		assertEquals(originaltext, returntext);
	}

	public void testKeyordExportImport() throws Exception
	{
		Asset asset = createAsset();
		String input = "one | two | tree";
		String inputgood = "one|two|tree";
		asset.setProperty("keywords", input);
		assertEquals(3,asset.getKeywords().size());
		assertEquals(inputgood,asset.get("keywords") );
	}
	public void XXXOLDtestEditRelatedAssets() throws Exception
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
	

//	public void testMultipleEditsAdding() throws Exception
//	{
//		Asset product = getMediaArchive().getAsset("1");
//		if( product == null)
//		{
//			product = getMediaArchive().createAsset("1","multitest/1");
//		}
//		product.setProperty("libraries", "1");
//		User user = getFixture().createPageRequest().getUser();
//		getMediaArchive().saveAsset(product, user);
//
//		Asset product2 = getMediaArchive().getAsset("2");
//		if( product2 == null)
//		{
//			product2 = getMediaArchive().createAsset("2","multitest/2");
//		}
//		product2.setProperty("libraries", "1 | 2");
//		getMediaArchive().saveAsset(product2, user);
//
//		SearchQuery q = getMediaArchive().getAssetSearcher().createSearchQuery();
//		q.addOrsGroup("id", "1 2" );
//		HitTracker hits = getMediaArchive().getAssetSearcher().search(q);
//		hits.selectAll();
//		assertEquals( 2, hits.size() );
//		CompositeAsset composite = new CompositeAsset(getMediaArchive(),hits);
//		composite.addLibrary("3");
//		composite.saveChanges();
//		
//		product = getMediaArchive().getAsset("1");
//		Collection values = product.getValues("libraries");
//		assertEquals( 2 , values.size());
//		assertTrue(values.contains("1"));
//		assertTrue(values.contains("3"));
//
//		product = getMediaArchive().getAsset("2");
//		values = product.getValues("libraries");
//		assertEquals( 3 , values.size());
//		assertTrue(values.contains("1"));
//		assertTrue(values.contains("2"));
//		assertTrue(values.contains("3"));
//
//	}

	public void testMultipleEditsRemove() throws Exception
	{
		Asset product = getMediaArchive().getAsset("1");
		if( product == null)
		{
			product = getMediaArchive().createAsset("1","multitest/1");
		}
		product.setProperty("keywords", "1common");
		product.setProperty("categories", "index");
		WebPageRequest req = getFixture().createPageRequest();
		User user = req.getUser();
		getMediaArchive().saveAsset(product, user);

		Asset product2 = getMediaArchive().getAsset("2");
		if( product2 == null)
		{
			product2 = getMediaArchive().createAsset("2","multitest/2");
		}
		ArrayList libs = new ArrayList();
		libs.add("1common");
		libs.add("2onproduct2");
		product2.setValue("keywords", libs);
		product2.setProperty("categories", "index");
		getMediaArchive().saveAsset(product2, user);

		SearchQuery q = getMediaArchive().getAssetSearcher().createSearchQuery();
		//HitTracker hits = getMediaArchive().getAssetSearcher().getAllHits();
		//q.addMatches("id","*");
		q.addOrsGroup("id", "1 2 102" );
		q.addSortBy("categories");
	
		getMediaArchive().getSearcherManager().setShowSearchLogs(getMediaArchive().getCatalogId(),true);
	
		
		HitTracker hits = getMediaArchive().getAssetSearcher().search(q);
		hits.toggleSelected("1");
		hits.toggleSelected("2");
		assertEquals( 2, hits.getSelections().size() );
		CompositeAsset composite = new CompositeAsset(getMediaArchive(),hits);
		ArrayList fields = new ArrayList();
		fields.add("keywords");
		fields.add("headline");
	
		LanguageMap headline = new LanguageMap();
		headline.setText("en", "English");
		headline.setText("de", "German");
		composite.setValue("headline", headline);


		composite.setEditFields(fields);
		composite.setSearcher(getMediaArchive().getAssetSearcher());
		Collection existing = composite.getValues("keywords");
		assertEquals(1,existing.size() );  //So "1common" is common and 2 is not
		assertTrue(existing.contains("1common"));
		existing.add("3addedtoboth");  //Add 3 to both records
		composite.setValue("keywords",existing); //We removed ("1common") and added 3
		
		composite.saveChanges(req);
		Collection values = composite.getValues("keywords");
		assertEquals( 2 , values.size());
		assertTrue(values.contains("1common"));
		assertTrue(values.contains("3addedtoboth"));


		product = getMediaArchive().getAsset("1");
		values = product.getValues("keywords");
		assertEquals( 2 , values.size());
		assertTrue(values.contains("1common"));
		assertTrue(values.contains("3addedtoboth"));

		product = getMediaArchive().getAsset("2");
		values = product.getValues("keywords");
		assertTrue(values.contains("1common"));
		assertTrue(values.contains("3addedtoboth"));
		assertTrue(values.contains("2onproduct2"));

		//Now set it again and it will fail since results are not updated
		composite.setValue("keywords" , new ArrayList() );
		composite.saveChanges(req); //removed 1common and  3addedtoboth, should be empty
		values = composite.getValues("keywords");
		int size = values.size();
		assertEquals( 0 , size);

		//We remved all the common ones 1 and 3
		product = getMediaArchive().getAsset("2");
		values = product.getValues("keywords");
		assertEquals( 1 , values.size());
		assertTrue(values.contains("2onproduct2"));
		LanguageMap map = (LanguageMap) product.getValue("headline");
		assertNotNull(map);

	}

	public void testMultipleEditsLanguage() throws Exception
	{
		Asset product = getMediaArchive().getAsset("1multilang");
		if( product == null)
		{
			product = getMediaArchive().createAsset("1multilang","multitest/1multilang");
		}

		LanguageMap headline = new LanguageMap();
		headline.setText("en", "EnglishH1");
		headline.setText("de", "GermanH1");
		product.setValue("headline", headline);
		
		WebPageRequest req = getFixture().createPageRequest();
		User user = req.getUser();
		getMediaArchive().saveAsset(product, user);
		
		Asset product2 = getMediaArchive().getAsset("2multilang");
		if( product2 == null)
		{
			product2 = getMediaArchive().createAsset("2multilang","multitest/2multilang");
		}
		LanguageMap headline2 = new LanguageMap();
		headline2.setText("en", "EnglishH2");
		headline2.setText("de", "GermanH2");
		product2.setValue("headline", headline2);

		getMediaArchive().saveAsset(product2, user);

		SearchQuery q = getMediaArchive().getAssetSearcher().createSearchQuery();
		//HitTracker hits = getMediaArchive().getAssetSearcher().getAllHits();
		//q.addMatches("id","*");
		q.addOrsGroup("id", "1multilang 2multilang" );
		HitTracker hits = getMediaArchive().getAssetSearcher().search(q);
		hits.toggleSelected("1multilang");
		hits.toggleSelected("2multilang");
		assertEquals( 2, hits.getSelections().size() );
		CompositeAsset composite = new CompositeAsset(getMediaArchive(),hits);
		ArrayList fields = new ArrayList();
		fields.add("headline");
	
		composite.setEditFields(fields);
		composite.setSearcher(getMediaArchive().getAssetSearcher());

		LanguageMap map = (LanguageMap) composite.getValue("headline");
		//They dont match
		assertNull(map);
		
		//TODO: make them the same and check
		product2.setValue("headline", headline);
		getMediaArchive().saveAsset(product2, user);
		hits = getMediaArchive().getAssetSearcher().search(q);
		hits.toggleSelected("1multilang");
		hits.toggleSelected("2multilang");
		assertEquals( 2, hits.getSelections().size() );
		composite = new CompositeAsset(getMediaArchive(),hits);
		map = (LanguageMap) composite.getValue("headline");
		//They do match
		assertNotNull(map);
		
	}

	
	public void testMultiSaveExistingValue() throws Exception
	{
		Asset product = getMediaArchive().getAsset("1multilang");
		if( product == null)
		{
			product = getMediaArchive().createAsset("1multilang","multitest/1multilang");
		}

		LanguageMap headline = new LanguageMap();
		headline.setText("en", "EnglishH1");
		headline.setText("de", "GermanH1");
		product.setValue("headline", headline);
		
		WebPageRequest req = getFixture().createPageRequest();
		User user = req.getUser();
		getMediaArchive().saveAsset(product, user);
		
		Asset product2 = getMediaArchive().getAsset("2multilang");
		if( product2 == null)
		{
			product2 = getMediaArchive().createAsset("2multilang","multitest/2multilang");
		}
		LanguageMap headline2 = new LanguageMap();
		headline2.setText("en", "EnglishH2");
		headline2.setText("de", "GermanH2");
		product2.setValue("headline", headline2);

		getMediaArchive().saveAsset(product2, user);

		SearchQuery q = getMediaArchive().getAssetSearcher().createSearchQuery();
		//HitTracker hits = getMediaArchive().getAssetSearcher().getAllHits();
		//q.addMatches("id","*");
		q.addOrsGroup("id", "1multilang 2multilang" );
		HitTracker hits = getMediaArchive().getAssetSearcher().search(q);
		hits.toggleSelected("1multilang");
		hits.toggleSelected("2multilang");
		assertEquals( 2, hits.getSelections().size() );
		CompositeAsset composite = new CompositeAsset(getMediaArchive(),hits);
		ArrayList fields = new ArrayList();
		fields.add("headline");
	
		composite.setEditFields(fields);
		composite.setSearcher(getMediaArchive().getAssetSearcher());

		req.setRequestParameter("save", "true");
		req.setRequestParameter("field", "headline");
		req.setRequestParameter("headline.language", "en");
		req.setRequestParameter("headline.en.value", "EnglishH3");
		
		String[] array = (String[])fields.toArray(new String[fields.size()]);
		getMediaArchive().getAssetSearcher().updateData(req, array, composite);
		composite.saveChanges(req);
		Asset productreloaded = getMediaArchive().getAsset("1multilang");
		LanguageMap langnew = (LanguageMap)productreloaded.getValue("headline");
		assertEquals("EnglishH3",langnew.getText("en"));
		assertEquals("GermanH1",langnew.getText("de"));
		
		req.setRequestParameter("headline.en.value", "EnglishH4");
		getMediaArchive().getAssetSearcher().updateData(req, array, composite);
		composite.saveChanges(req);

		productreloaded = getMediaArchive().getAsset("1multilang");
		langnew = (LanguageMap)productreloaded.getValue("headline");
		assertEquals("EnglishH4",langnew.getText("en"));
		assertEquals("GermanH1",langnew.getText("de"));

		

	}
	
	public void XXXtestDeleteFromIndex() throws Exception
	{
		Asset asset = getMediaArchive().createAsset("666","multitest/666");
		
		asset.setProperty("keywords", "1");
		asset.setProperty("category", "index");
		getMediaArchive().saveAsset(asset, null);

		SearchQuery q = getMediaArchive().getAssetSearcher().createSearchQuery();
		q.addMatches("id","*");
		
		WebPageRequest req = getFixture().createPageRequest();

		HitTracker tracker = getMediaArchive().getAssetSearcher().search(q);
		int size = tracker.size();
		
		getMediaArchive().getAssetSearcher().deleteFromIndex(asset);

		tracker = getMediaArchive().getAssetSearcher().search(q);
		assertEquals( tracker.size() + 1, size);
	
	}
}
