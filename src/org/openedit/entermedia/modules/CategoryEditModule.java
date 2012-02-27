/*
 * Created on Nov 16, 2004
 */
package org.openedit.entermedia.modules;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.data.Searcher;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.Category;
import org.openedit.entermedia.CompositeAsset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.edit.CategoryEditor;
import org.openedit.event.WebEvent;
import org.openedit.event.WebEventListener;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.SearchQuery;
import com.openedit.users.User;
import com.openedit.util.PathUtilities;

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
		categoryeditor.setCurrentCategory(newcategory);
		categoryeditor.saveCategory(newcategory);
		inContext.putPageValue("category", newcategory);
	}

	public void moveCategory(WebPageRequest inContext) throws OpenEditException {
		CategoryEditor categoryeditor = getCategoryEditor(inContext);
		String category2Id = inContext.getRequestParameter("categoryid2");
		Category category1 = categoryeditor.getCurrentCategory();
		Category category2 = categoryeditor.getCategory(category2Id);

		if (category1 != null && category2 != null) {
			// don't move if same catalog or catalog2 is already the parent
			if (category1 != category2
					&& category1.getParentCategory() != category2
					&& !category1.isAncestorOf(category2)) {
				category1.getParentCategory().removeChild(category1);

				category1.setParentCategory(category2);
				category2.addChild(category1);

				categoryeditor.saveCategory(category1);
				categoryeditor.saveCategory(category2);
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
					.getCreatorManager()
					.run(createthumb, createmedium, replacethumb,
							replacemedium, targetsearcher.search(q));
			inContext.putPageValue("failures", failures);
		} catch (Exception e) {
			inContext.putPageValue("error", e.getMessage());
			log.error(e);
		}
	}

	public void saveCategory(WebPageRequest inContext) throws OpenEditException {
		String id = inContext.getRequestParameter("id");
		String name = inContext.getRequestParameter("name");

		CategoryEditor editor = getCategoryEditor(inContext);
		Category currentCatalog = editor.getCurrentCategory();

		String copy = inContext.getRequestParameter("saveasnew");
		if (currentCatalog != null && Boolean.parseBoolean(copy)) {
			currentCatalog = new Category(currentCatalog.getId() + "copy",
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
			currentCatalog.removeProperty("sortfield");
		} else {
			currentCatalog.setProperty("sortfield", sortfield);
		}
		editor.saveCategory(currentCatalog);
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
			if (value != null) {
				cat.setProperty(field, value);
			} else {
				cat.removeProperty(field);
			}
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
		CategoryEditor editor = (CategoryEditor) inContext
				.getSessionValue("CategoryEditor" + mediaarchive.getCatalogId());
		if (editor == null) {
			editor = (CategoryEditor) getModuleManager().getBean(
					"categoryEditor");
			editor.setMediaArchive(mediaarchive);

			inContext.putSessionValue(
					"CategoryEditor" + mediaarchive.getCatalogId(), editor);
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
		if (assetid.startsWith("multiedit:")) {
			CompositeAsset composite = (CompositeAsset) inPageRequest
					.getSessionValue(assetid);
			composite.removeCategory(c);
			archive.saveAssets(composite.getItems(), inPageRequest.getUser());
			for (Iterator i = composite.getItems().iterator(); i.hasNext();) {
				Asset asset = (Asset) i.next();
				fireAssetEditEvent(asset, inPageRequest.getUser(), message);
			}
		} else {
			Asset asset = archive.getAsset(assetid);
			asset.removeCategory(c);
			archive.saveAsset(asset, inPageRequest.getUser());
			fireAssetEditEvent(asset, inPageRequest.getUser(), message);
		}
	}

	public void addCategoryToAsset(WebPageRequest inPageRequest)
			throws Exception {
		Asset asset = getAsset(inPageRequest);
		if (asset == null) {
			log.error("No asset id passed in");
			return;
		}
		String[] add = inPageRequest.getRequestParameters("categoryid");
		MediaArchive archive = getMediaArchive(inPageRequest);
		if (add == null) {
			return;
		}
		String message = "Added to category ";
		for (int i = 0; i < add.length; i++) {
			Category c = archive.getCategory(add[i]);
			if (c == null) {
				log.info("No category found. " + add[i]);
				return;
			}

			message = message + "\"" + c.getName() + "\"";
			if (asset.getId().startsWith("multiedit:")) {
				CompositeAsset composite = (CompositeAsset) inPageRequest
						.getSessionValue(asset.getId());
				composite.addCategory(c);
				archive.saveAssets(composite.getItems());
				for (Iterator iter = composite.iterator(); iter.hasNext();) {
					Asset masset = (Asset) iter.next();
					fireAssetEditEvent(masset, inPageRequest.getUser(), message);
				}
			} else {
				asset.addCategory(c);
			}
		}
		archive.saveAsset(asset, inPageRequest.getUser());
		fireAssetEditEvent(asset, inPageRequest.getUser(), message);
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
			if (asset.getId().startsWith("multiedit:")) {
				CompositeAsset composite = (CompositeAsset) inPageRequest
						.getSessionValue(asset.getId());
				composite.addCategory(c);
			
				for (Iterator iter = composite.getItems().iterator(); iter.hasNext();) {
			
					Asset masset = (Asset) iter.next();
					archive.saveAsset(masset, inPageRequest.getUser());
					fireAssetEditEvent(masset, inPageRequest.getUser(), message);
				}
			} else {
				asset.addCategory(c);
			}
		}
		if (!asset.getId().startsWith("multiedit:")) {
			archive.saveAsset(asset, inPageRequest.getUser());
			fireAssetEditEvent(asset, inPageRequest.getUser(), message);
		}
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

	protected void fireAssetEditEvent(Asset inAsset, User inUser, String message) {
		WebEvent event = new WebEvent();
		event.setCatalogId(inAsset.getCatalogId());
		event.setSearchType("assetedit");
		event.setSource(this);
		event.addDetail("assetname", inAsset.getName());
		event.addDetail("assetid", inAsset.getId());
		event.addDetail("changes", message);
		event.setUser(inUser);
		getWebEventListener().eventFired(event);
	}

	public WebEventListener getWebEventListener() {
		return fieldWebEventListener;
	}

	public void setWebEventListener(WebEventListener inWebEventListener) {
		fieldWebEventListener = inWebEventListener;
	}

}