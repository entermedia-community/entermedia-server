/*
 * Created on Nov 16, 2004
 */
package org.entermediadb.asset.modules;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.BaseCategory;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.CompositeAsset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.edit.CategoryEditor;
import org.entermediadb.asset.xmldb.CategorySearcher;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.event.WebEventListener;
import org.openedit.hittracker.HitTracker;
import org.openedit.util.PathUtilities;

/**
 * @author cburkey
 * 
 */
public class CategoryEditModule extends BaseMediaModule {
	protected WebEventListener fieldWebEventListener;
	protected static final String CATEGORYID = "categoryid";
	private static final Log log = LogFactory.getLog(CategoryEditModule.class);

	public void addCategory(WebPageRequest inContext) throws OpenEditException {
		CategoryEditor categoryeditor = getCategoryEditor(inContext);
		String newname = inContext.getRequestParameter("newname");
		if (newname == null) {
			newname = "New Category";
		}
		Category newcategory = categoryeditor.addNewCategory(
				new Date().getTime() + "", newname);

		categoryeditor.sortCategory(categoryeditor.getCurrentCategory());

		categoryeditor.setCurrentCategory(newcategory);
		categoryeditor.saveCategory(newcategory);
		inContext.putPageValue("category", newcategory);
	}
	
	public void renameCategory(WebPageRequest inReq) {
		MediaArchive archive = getMediaArchive(inReq);
		String category1Id = inReq.getRequestParameter("categoryid");
		CategoryEditor categoryeditor = getCategoryEditor(inReq);

		CategorySearcher searcher = (CategorySearcher) archive.getSearcher(categoryeditor.getSearchType());
		Category cat = searcher.getCategory(category1Id);
		String newname = inReq.findValue("newname");
		cat.setName(newname);
		searcher.saveData(cat);
	}

	
	
	public void handlePaste(WebPageRequest inContext) throws OpenEditException {
		CategoryEditor editor = getCategoryEditor(inContext);
		MediaArchive archive = editor.getMediaArchive();
		CategorySearcher searcher = (CategorySearcher) archive.getSearcher(editor.getSearchType());

		String mode = inContext.getRequestParameter("mode"); // "copy" or "cut"
		String sourceId = inContext.getRequestParameter("fromid");
		String targetId = inContext.getRequestParameter("targetid");

		if (sourceId == null || targetId == null || mode == null) {
			log.warn("Missing required parameters for paste. mode=" + mode + " source=" + sourceId + " target=" + targetId);
			return;
		}

		Category source = searcher.getCategory(sourceId);
		Category target = searcher.getCategory(targetId);

		if (source == null || target == null) {
			log.warn("Source or target category not found. source=" + sourceId + ", target=" + targetId);
			return;
		}

		if ("cut".equals(mode)) {
			inContext.setRequestParameter("categoryid", sourceId);
			inContext.setRequestParameter("categoryid2", targetId);
			moveCategory(inContext);
			log.info("Moved category " + source.getId() + " under " + target.getId());

		} else if ("copy".equals(mode)) {
			List descendants = source.getChildren();
			String sourceroot = source.getCategoryPath();
			String destroot = target.getCategoryPath();
			for (Iterator iterator = descendants.iterator(); iterator.hasNext();)
			{
				Category copyitem = (Category) iterator.next();
				String currentPath = copyitem.getCategoryPath();
				if (currentPath.startsWith(sourceroot)) {
					String relative = currentPath.substring(sourceroot.length());
					if (relative.startsWith("/")) {
						relative = relative.substring(1); // trim leading slash
					}
					String targetPath = destroot + "/" + source.getName() + (relative.isEmpty() ? "" : "/" + relative);
					searcher.createCategoryPath(targetPath);
				}
			}

		} else {
			log.warn("Unknown paste mode: " + mode);
		}
	}


	
	
	public void moveCategory(WebPageRequest inContext) throws OpenEditException 
	{
		CategoryEditor categoryeditor = getCategoryEditor(inContext);

		String category1Id = inContext.getRequestParameter("categoryid");
		Category category1 = null;
		if( category1Id == null)
		{
			category1 = categoryeditor.getCurrentCategory();
		}
		else
		{
			category1 = categoryeditor.getCategory(category1Id);
		}
		String category2Id = inContext.getRequestParameter("categoryid2");
		Category category2 = categoryeditor.getCategory(category2Id);

		if (category1 != null && category2 != null) {
			// don't move if same catalog or catalog2 is already the parent
			if (category1 != category2
					&& category1.getParentCategory() != category2
					&& !category1.isAncestorOf(category2)) {
				category1.getParentCategory().removeChild(category1);

				category1.setParentCategory(category2);
				category2.addChild(category1);

				categoryeditor.saveCategory(category1, true);
				//categoryeditor.saveCategory(category2);
				
			}
		}
	}

	public void moveCategoryHere(WebPageRequest inContext)
			throws OpenEditException {
		CategoryEditor CategoryEditor = getCategoryEditor(inContext);
		String catalog2Id = inContext.getRequestParameter("categoryid2");
		Category catalog1 = CategoryEditor.getCurrentCategory();
		Category catalog2 = CategoryEditor.getCategory(catalog2Id);

		CategoryEditor.moveCategoryBefore(catalog1, catalog2);
	}

	public void sortCategory(WebPageRequest inContext) throws OpenEditException {
		getCategoryEditor(inContext).sortCategory(
				getCategoryEditor(inContext).getCurrentCategory());
	}

	public void moveCategoryUp(WebPageRequest inContext)
			throws OpenEditException {
		CategoryEditor CategoryEditor = getCategoryEditor(inContext);
		String catalogId = inContext.getRequestParameter(CATEGORYID);
		Category catalog = CategoryEditor.getCategory(catalogId);

		// don't move if doesn't have a parent
		if (catalog.getParentCategory() != null) {
			CategoryEditor.moveCategoryUp(catalog);
		}
	}

	public void moveCategoryDown(WebPageRequest inContext)
			throws OpenEditException {
		CategoryEditor CategoryEditor = getCategoryEditor(inContext);
		String catalogId = inContext.getRequestParameter(CATEGORYID);
		Category catalog = CategoryEditor.getCategory(catalogId);

		// don't move if doesn't have a parent
		if (catalog.getParentCategory() != null) {
			CategoryEditor.moveCategoryDown(catalog);
		}
	}

	public void deleteCategory(WebPageRequest inContext)
			throws OpenEditException {
		String catalogId = inContext.getRequestParameter(CATEGORYID);
		CategoryEditor CategoryEditor = getCategoryEditor(inContext);
		Category catalog = CategoryEditor.getCategory(catalogId);
		if (catalog != null) {
			Category parent = catalog.getParentCategory();
			CategoryEditor.deleteCategory(catalog);
			if (parent != null) {
				CategoryEditor.setCurrentCategory(parent);
			}
		}

		// check for a web tree?

	}
	
	public void toggleFeatured(WebPageRequest inReq) throws OpenEditException {
		String categoryid = inReq.getRequestParameter("categoryid");
		CategoryEditor categoryEditor = getCategoryEditor(inReq);
		Category cat = categoryEditor.getMediaArchive().getCategory(categoryid);
		if(cat == null) {
			return;
		}
		if (cat.getBoolean("isfeatured")) 
		{
			cat.setProperty("isfeatured", "false");
		}
		else
		{
			cat.setProperty("isfeatured", "true");
		}
		categoryEditor.saveCategory(cat);
	}
	
	
/*
	public void resizeAllImages(WebPageRequest inContext) throws Exception {
		CategoryEditor editor = getCategoryEditor(inContext);

		boolean createthumb = !(inContext.getRequestParameter("createthumb") == null);
		boolean createmedium = !(inContext.getRequestParameter("createmedium") == null);
		boolean replacethumb = !(inContext.getRequestParameter("replacethumb") == null);
		boolean replacemedium = !(inContext
				.getRequestParameter("replacemedium") == null);
		String cat = inContext.getRequestParameter("categoryid");
		Searcher targetsearcher = editor.getMediaArchive().getAssetSearcher();
		SearchQuery q = targetsearcher.createSearchQuery();
		q.addMatches("categoryid", cat);

		try {
			List failures = editor
					.getMediaArchive()
					.getTranscodeTools()
					.run(createthumb, createmedium, replacethumb,
							replacemedium, targetsearcher.search(q));
			inContext.putPageValue("failures", failures);
		} catch (Exception e) {
			inContext.putPageValue("error", e.getMessage());
			log.error(e);
		}
	}
*/
	public void saveCategory(WebPageRequest inContext) throws OpenEditException {
		String id = inContext.getRequestParameter("id");
		String name = inContext.getRequestParameter("name");

		CategoryEditor editor = getCategoryEditor(inContext);
		Category currentCatalog = editor.getCurrentCategory();

		String copy = inContext.getRequestParameter("saveasnew");
		if (currentCatalog != null && Boolean.parseBoolean(copy)) {
			currentCatalog = new BaseCategory(currentCatalog.getId() + "copy",
					currentCatalog.getName());
			editor.getCurrentCategory().getParentCategory()
					.addChild(currentCatalog);
		} else if (!id.equals(currentCatalog.getId())) {
			editor.changeCategoryId(currentCatalog, id);
		}

		currentCatalog.setShortDescription(inContext
				.getRequestParameter("shortdescription"));
		currentCatalog.setName(name);

		String sortfield = inContext.getRequestParameter("sortfield");
		if (sortfield == null || sortfield.length() < 1) {
			currentCatalog.setValue("sortfield",null);
		} else {
			currentCatalog.setProperty("sortfield", sortfield);
		}
		editor.saveCategory(currentCatalog);
		inContext.putSessionValue("reloadcategorytree","true");
	}

	public void saveCategoryProperties(WebPageRequest inReq)
			throws OpenEditException {
		CategoryEditor editor = getCategoryEditor(inReq);
		String[] fields = inReq.getRequestParameters("field");
		String catid = inReq.getRequestParameter("categoryid");
		if (fields == null || catid == null) {
			return;
		}
		Category cat = editor.getMediaArchive().getCategory(catid);
		for (int i = 0; i < fields.length; i++) {
			String field = fields[i];
			String value = inReq.getRequestParameter(field + ".value");
			cat.setProperty(field, value);
		}
		editor.getMediaArchive().getCategoryArchive().saveCategory(cat);
	}

	public void loadCategory(WebPageRequest inContext) throws OpenEditException {
		String catalogid = inContext.findValue(CATEGORYID);
		if (catalogid == null) {
			catalogid = PathUtilities.extractPageName(inContext.getPath());
		}
		CategoryEditor editor = getCategoryEditor(inContext);
		if (catalogid != null) {
			// load up catalog and assets
			Category catalog = editor.getCategory(catalogid);
			if (catalog != null) {
				editor.setCurrentCategory(catalog);
				inContext.putPageValue("category", catalog);
			}
		}
	}

	public CategoryEditor getCategoryEditor(WebPageRequest inContext)
			throws OpenEditException {
		MediaArchive mediaarchive = getMediaArchive(inContext);
		String searchtype = inContext.findValue("categorysearchtype");
		if(searchtype == null) {
			searchtype= "category";
		}
		CategoryEditor editor = (CategoryEditor) inContext
				.getSessionValue("CategoryEditor" +searchtype+ mediaarchive.getCatalogId());
		if (editor == null) {
			editor = (CategoryEditor) getModuleManager().getBean(
					"categoryEditor");
			editor.setMediaArchive(mediaarchive);
			editor.setSearchType(searchtype);
			inContext.putSessionValue(
					"CategoryEditor" +searchtype+ mediaarchive.getCatalogId(), editor);
		}
		inContext.putPageValue("CategoryEditor", editor);

		return editor;
	}

	public void removeCategoryFromAsset(WebPageRequest inPageRequest)
			throws Exception {
		String assetid = inPageRequest.getRequestParameter("assetid");
		String add = inPageRequest.getRequestParameter("categoryid");
		MediaArchive archive = getMediaArchive(inPageRequest);
		Category c = archive.getCategory(add);
		if (c == null) {
			return;
		}

		String message = "Removed from category \"" + c.getName() + "\"";
		/*
		Asset asset = (Asset)archive.getAssetSearcher().loadData(inPageRequest,assetid);
		asset.removeCategory(c);
		archive.saveAsset(asset, inPageRequest.getUser());
		archive.fireMediaEvent("saved", inPageRequest.getUser(), asset);
		inPageRequest.putPageValue("asset", asset);*/
		
		
		Asset asset;
		
		String[] assetIds = inPageRequest.getRequestParameters("assetid");
		for (int i = 0; i < assetIds.length; i++)
		{
		
			if (assetIds[i].startsWith("multiedit:"))
			{
				try
				{
					CompositeAsset assets = (CompositeAsset) inPageRequest.getSessionValue(assetIds[i]);
					List tosave = new ArrayList();
					Integer count = 0;
					for (Iterator iterator = assets.iterator(); iterator.hasNext();)
					{
						asset = (Asset) iterator.next();
						if (asset == null) {
							log.error("No asset id passed in");
							return;
						}
						asset.removeCategory(c);
						tosave.add(asset);
						if( tosave.size() > 100)
						{
							archive.getAssetSearcher().saveAllData(tosave, inPageRequest.getUser());
							tosave.clear();
						}
						count = count +1;
						//archive.saveAsset(asset, inPageRequest.getUser());
					}
					if( tosave.size() > 0)
					{
						archive.getAssetSearcher().saveAllData(tosave, inPageRequest.getUser());
						tosave.clear();
					}
					log.info("Removed category from assets: " + count);
				}
				catch (Exception e)
				{
					continue;
				}
			}
			else 
			{
				asset = getAsset(inPageRequest);
				if (asset == null) {
					log.error("No asset id passed in");
					return;
				}
				asset.removeCategory(c);
				archive.saveAsset(asset);
				inPageRequest.putPageValue("removed" , "1");
				inPageRequest.putPageValue("asset", asset);
			}
		}
		inPageRequest.setRequestParameter("assetid", assetid);
	}

	public void addCategoryToAsset(WebPageRequest inPageRequest) throws Exception 
	{
		
		String[] categories = inPageRequest.getRequestParameters("categoryid");
		MediaArchive archive = getMediaArchive(inPageRequest);
		if (categories == null) 
		{
			return;
		}
		String moduleid = inPageRequest.findPathValue("module");
		HitTracker tracker = loadHitTracker(inPageRequest, moduleid);
		boolean movecategory = Boolean.parseBoolean( inPageRequest.getRequestParameter("moveasset") );
		String rootcategoryid = inPageRequest.getRequestParameter("rootcategoryid");
		if( tracker != null )
		{
			tracker = tracker.getSelectedHitracker();
		}
		if( tracker != null && tracker.size() > 0 )
		{
			int added = 0;
			tracker.enableBulkOperations();
			for (Iterator iterator = tracker.iterator(); iterator.hasNext();)
			{
				Data data = (Data) iterator.next();
				Asset asset = archive.getAsset(data.getId());
				addCategoryToAsset(inPageRequest, archive ,categories, asset, movecategory, rootcategoryid);
				archive.saveAsset(asset, inPageRequest.getUser());
				archive.fireMediaEvent("saved", inPageRequest.getUser(), asset);
				added++;
			}
			
			inPageRequest.putPageValue("added" , String.valueOf( added ) );
			return;
		}
		
		Asset asset;
		
		String[] assetIds = inPageRequest.getRequestParameters("assetid");
		for (int i = 0; i < assetIds.length; i++)
		{
		
			if (assetIds[i].startsWith("multiedit:"))
			{
				try
				{
					CompositeAsset assets = (CompositeAsset) inPageRequest.getSessionValue(assetIds[i]);
					log.info("Saving assets: " + assets.size());
					List tosave = new ArrayList();
					Integer count = 0;
					for (Iterator iterator = assets.iterator(); iterator.hasNext();)
					{
						asset = (Asset) iterator.next();
						if (asset == null) {
							log.error("No asset id passed in");
							continue;
						}
						addCategoryToAsset(inPageRequest, archive ,categories, asset, movecategory, rootcategoryid);
						tosave.add(asset);
						if( tosave.size() > 100)
						{
							archive.getAssetSearcher().saveAllData(tosave, inPageRequest.getUser());
							tosave.clear();
						}
						count = count +1;
					}
					if( tosave.size() > 0)
					{
						archive.getAssetSearcher().saveAllData(tosave, inPageRequest.getUser());
						tosave.clear();
					}
					log.info("Saved: " + count);
				}
				catch (Exception e)
				{
					continue;
				}
			}
			else 
			{
				asset = getAsset(inPageRequest);
				if (asset == null) {
					log.error("No asset id passed in");
					return;
				}
				addCategoryToAsset(inPageRequest, archive ,categories, asset, movecategory, rootcategoryid);
				archive.saveAsset(asset, inPageRequest.getUser());
				archive.fireMediaEvent("saved", inPageRequest.getUser(), asset);
				inPageRequest.putPageValue("added" , "1");
			}
		}
	}

	protected void addCategoryToAsset(WebPageRequest inPageRequest, MediaArchive archive, String[] add, Asset asset, boolean moveasset, String rootcategoryid)
	{
		if( moveasset )
		{
			List<Category> cats = new ArrayList(asset.getCategories());
			for(Category cat: cats)
			{
				if( cat.hasParent(rootcategoryid))
				{
					asset.removeCategory(cat);
				}
			}
		}
		
		for (int i = 0; i < add.length; i++) {
			Category c = archive.getCategory(add[i]);
			if (c == null) {
				log.info("No category found. " + add[i]);
				return;
			}
			asset.addCategory(c);
		}
		
		//Save externally
		//archive.saveAsset(asset, inPageRequest.getUser());
		//archive.fireMediaEvent("saved", inPageRequest.getUser(), asset);
	}

	public void setAssetCategories(WebPageRequest inPageRequest)
			throws Exception {
		Asset asset = getAsset(inPageRequest);
		if (asset == null) {
			log.error("No asset id passed in");
			
			return;
		}
		String[] add = inPageRequest.getRequestParameters("categoryid");
		MediaArchive archive = getMediaArchive(inPageRequest);
		if (add == null) {
			log.info("No categoryid specified");
			return;
		}
		String message = "Added to category ";
		asset.clearCategories();

		for (int i = 0; i < add.length; i++) {
			Category c = archive.getCategory(add[i]);
			if (c == null) {
				log.info("No category found. " + add[i]);
				continue;
			}

			message = message + "\"" + c.getName() + "\"";
			asset.addCategory(c);
		}
		archive.saveAsset(asset, inPageRequest.getUser());
		archive.fireMediaEvent("saved", inPageRequest.getUser(), asset);
	}
	
	
	public void addEntityToCategory(WebPageRequest inPageRequest) throws Exception 
	{
		
		String categoryid = inPageRequest.getRequestParameter("categoryid");
		String moduleid = inPageRequest.getRequestParameter("moduleid");
		String entityid = inPageRequest.getRequestParameter("entityid");
		MediaArchive archive = getMediaArchive(inPageRequest);
		if (categoryid == null || moduleid == null || entityid == null) 
		{
			return;
		}
		
		Category category = archive.getCategory(categoryid);
		if(category == null) {
			return;
		}
		category.addValue(moduleid, entityid);
		archive.getCategorySearcher().saveData(category);
	}
	
	
	
	
	

	/**
	 * Removes generated images (medium, thumbs, etc) for a asset. TODO:
	 * Shouldn't this go into AssetEditModule ?
	 * 
	 * @param inRequest
	 *            The web request. Needs a <code>assetid</code> or
	 *            <code>sourcePath</code> request parameter.
	 */
	public void removeAssetImages(WebPageRequest inRequest) {
		Asset asset = getAsset(inRequest);
		MediaArchive mediaarchive = getMediaArchive(inRequest);

		String catalogId = mediaarchive.getCatalogId();

		String prefix = "/" + catalogId + "/assets/images/generated/"
				+ asset.getSourcePath();

		final File path = new File(getPageManager().getRepository()
				.getStub(prefix).getAbsolutePath());
		File[] todelete = null;
		if (path.exists() && path.isDirectory()) {
			todelete = path.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.endsWith(".jpg");
				}
			});
		} else {
			File parent = path.getParentFile();
			todelete = parent.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.startsWith(PathUtilities.extractFileName(path
							.getAbsolutePath()));
				}
			});
		}
		for (int i = 0; i < todelete.length; i++) {
			todelete[i].delete();
		}
	}

	

	public WebEventListener getWebEventListener() {
		return fieldWebEventListener;
	}

	public void setWebEventListener(WebEventListener inWebEventListener) {
		fieldWebEventListener = inWebEventListener;
	}

}