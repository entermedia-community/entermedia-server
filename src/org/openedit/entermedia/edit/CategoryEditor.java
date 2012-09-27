package org.openedit.entermedia.edit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.Category;
import org.openedit.entermedia.MediaArchive;
import org.openedit.repository.RepositoryException;
import org.openedit.repository.filesystem.StringItem;

import com.openedit.OpenEditException;
import com.openedit.OpenEditRuntimeException;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;

public class CategoryEditor {
	protected MediaArchive fieldMediaArchive;
	protected PageManager fieldPageManager;
	
	protected Category fieldCurrentCategory;
	
	private static final Log log = LogFactory.getLog(CategoryEditor.class);

	public Category getCategory(String inCategoryId) throws OpenEditRuntimeException
	{
		return getMediaArchive().getCategoryArchive().getCategory(inCategoryId);
	}

	public void moveCategoryUp(Category inCategory) throws OpenEditRuntimeException
	{
		log.debug("Moving category up: " + inCategory.getName());
		Category parent = inCategory.getParentCategory();
		List children = (List)parent.getChildren();
		Category prev = null;
		for (Iterator iter = children.iterator(); iter.hasNext();) 
		{
			Category child = (Category) iter.next();
			if (child == inCategory)
			{
				if (prev != null)
				{
					int childIndex = children.indexOf(child);
					children.set(childIndex - 1, child);
					children.set(childIndex, prev);
					log.info("Category '" + inCategory.getId() + "' moved up");
				}
				break;
			}
			prev = child;
		}
		parent.setChildren(children);
		saveCategory(parent);
	}
	
	public void moveCategoryDown(Category ininCategory) throws OpenEditRuntimeException
	{
		Category parent = ininCategory.getParentCategory();
		List children = (List)parent.getChildren();
		for (Iterator iter = children.iterator(); iter.hasNext();) 
		{
			Category child = (Category) iter.next();
			if (child == ininCategory)
			{
				if (iter.hasNext())
				{
					int childIndex = children.indexOf(child);
					children.set(childIndex, (Category) iter.next());
					children.set(childIndex + 1, child);
				}
				break;
			}
		}
		parent.setChildren(children);
		saveCategory(parent);
	}
	
	public void moveCategoryBefore (Category inCategory, Category inBeforeCategory) throws OpenEditRuntimeException
	{
		Category parent = inCategory.getParentCategory();

		if (inBeforeCategory == null || inBeforeCategory.getParentCategory() != parent || inCategory == inBeforeCategory)
			return;

		List list = parent.getChildren();
		int toIndex = list.indexOf(inBeforeCategory);
		if (list.indexOf(inCategory) < toIndex)
		{
			while (list.indexOf(inCategory) < toIndex)
				moveCategoryDown(inCategory);
		} 
		else
		{
			while (list.indexOf(inCategory) > toIndex)
				moveCategoryUp(inCategory);
		}
		parent.setChildren(list);
	}

	public void sortCategory (Category inCategory)
	{
		if (inCategory == null)
			return;
		List children = inCategory.getChildren();

		Collections.sort(children, new Comparator() 
		{
			public int compare(Object o1, Object o2) {
				Category c1 = (Category)o1, c2 = (Category)o2;
				return c1.getName().compareTo(c2.getName());
			}
		});
		inCategory.setChildren(children);
	}

	/**
	 * Adds a new category to the catalog.
	 * @param inId the new category's ID.
	 * @param inName the new category's name.
	 * @return the new category object.
	 */
	 public Category addNewCategory(String inId, String inName) throws OpenEditRuntimeException
	 {
		 Category newCat = new Category();
		 newCat.setId(inId);
		 newCat.setName(inName);
		 if (getCurrentCategory() != null)
		 {
			 getCurrentCategory().addChild(newCat);
		 }
		 else if (getRootCategory() != null)
		 {
			 getRootCategory().addChild(newCat);
		 }
		 else
		 {
			 getMediaArchive().getCategoryArchive().setRootCategory(newCat);
		 }
		 getMediaArchive().getCategoryArchive().cacheCategory(newCat);
		 return newCat;
	 }

	 public void saveCategory(Category inCategory) throws OpenEditRuntimeException
	 {
		 if ( inCategory.getParentCategory() == null && getMediaArchive().getCategoryArchive().getRootCategory() != inCategory)
		 {
			 getMediaArchive().getCategoryArchive().getRootCategory().addChild(inCategory);
			 getMediaArchive().getCategoryArchive().cacheCategory(inCategory);
		 }
		 try
		 {
			 Page desc = getPageManager().getPage(getMediaArchive().getCatalogHome() + "/categories/" + inCategory.getId() + ".html");
			 if ( !desc.exists() )
			 {
				 StringItem item = new StringItem(desc.getPath(), " ",desc.getCharacterEncoding() );
				 desc.setContentItem(item);
				 getPageManager().putPage(desc);
			 }
		 }
		 catch ( Exception ex )
		 {
			 throw new OpenEditRuntimeException(ex);
		 }
		 getMediaArchive().getCategoryArchive().saveCategory(inCategory);		
	 }

	 /**
	  * Deletes a category from the catalog.
	  * @param inCategory the category to be deleted.
	  */
	 public void deleteCategory(Category inCategory) throws OpenEditException
	 {	
		 List assets = getMediaArchive().getAssetsInCategory(inCategory);
		 for (Iterator iter = assets.iterator(); iter.hasNext();) 
		 {
			 Asset element = (Asset) iter.next();
			 element.removeCategory(inCategory);
		 }
		 getMediaArchive().getCategoryArchive().deleteCategory(inCategory);
		 getMediaArchive().saveAssets(assets);
	 }

	 public Category getRootCategory() throws OpenEditRuntimeException
	 {
		 return getMediaArchive().getCategoryArchive().getRootCategory();
	 }

	 public void clearCategories() throws OpenEditRuntimeException
	 {
		 getMediaArchive().getCategoryArchive().clearCategories();
	 }

	 public void reloadCategories() throws OpenEditRuntimeException
	 {
		 if (getCurrentCategory() != null)
		 {
			 String id = getCurrentCategory().getId();
			 getMediaArchive().getCategoryArchive().reloadCategories();
			 Category catalog = getCategory(id);
			 if ( catalog == null)
			 {
				 catalog = getRootCategory();
			 }
			 setCurrentCategory(catalog);
		 }
		 else
		 {
			 getMediaArchive().getCategoryArchive().reloadCategories();
		 }
	 }

	 public PageManager getPageManager() 
	 {
		 return fieldPageManager;
	 }
	 public void setPageManager(PageManager inPageManager) 
	 {
		 fieldPageManager = inPageManager;
	 }

	 public void changeCategoryId(Category inCategory, String inId) throws OpenEditException 
	 {
		 inId = inId.replace('(', '-');
		 inId = inId.replace(')', '-');
		 inId = inId.replace(' ', '-');

		 List assets = getMediaArchive().getAssetsInCategory(inCategory);

		 PageManager pageManager = getPageManager();
		 //			reload = true;
		 Page oldPage = pageManager.getPage(getMediaArchive().getCatalogHome() +"/categories/" + inCategory.getId() + ".html");
		 Page newPage = pageManager.getPage(getMediaArchive().getCatalogHome() +"/categories/" + inId + ".html");
		 if (oldPage.exists() && !newPage.exists())
		 {
			 try
			 {
				 pageManager.movePage(oldPage, newPage);
			 }
			 catch ( RepositoryException re )
			 {
				 throw new OpenEditException( re );
			 }
		 }
		 if (assets != null)
		 {
			 for (Iterator iter = assets.iterator(); iter.hasNext();) 
			 {
				 Asset element = (Asset) iter.next(); //element is an existing asset
				 element.removeCategory(inCategory); //add the new asset (with the new id) to the new catalog
			 }
		 }

		 inCategory.setId( inId );
		 saveCategory( inCategory );

		 if (assets != null)
		 {
			 for (Iterator iter = assets.iterator(); iter.hasNext();) 
			 {
				 Asset element = (Asset) iter.next(); //element is an existing asset
				 element.addCategory(inCategory); //add the new asset (with the new id) to the new catalog
			 }
			 getMediaArchive().saveAssets(assets); //save all the assets that need to be
		 }

	 }

	 public void removeAssetFromCategory(Category inCategory, String[] inAssetIds) throws OpenEditException
	 {
		 if ( inCategory == null )
		 {
			 throw new OpenEditException("No category found ");
		 }
		 List assetsToSave = new ArrayList();

		 for (int i = 0; i < inAssetIds.length; i++) 
		 {
			 Asset asset = getMediaArchive().getAsset( inAssetIds[i] );
			 if ( asset != null )
			 {
				 asset.removeCategory(inCategory);
				 assetsToSave.add(asset);
			 }	
		 }
		 getMediaArchive().saveAssets(assetsToSave);

	 }

	 public void addAssetsToCategory(String[] inAssetIds, String inPrefix, String inSuffix, Category inCategory) throws OpenEditException
	 {
		 List assetsToSave = new ArrayList();

		 if (inCategory != null)
		 {
			 List assets = null;
			 if ( inAssetIds == null)
			 {
				 //copy all of them
				 assets = getMediaArchive().getAssetsInCategory(getCurrentCategory());
			 }
			 else
			 {
				 assets = new ArrayList();
				 for (int i = 0; i < inAssetIds.length; i++)
				 {
					 Asset element = getMediaArchive().getAsset(inAssetIds[i]);
					 assets.add(element);
				 }
			 }
			 for (Iterator iter = assets.iterator(); iter.hasNext();)
			 {
				 Asset element = (Asset) iter.next();
				 Asset asset = element;
				 if (inPrefix != null || inSuffix != null)
				 {
					 if ( inPrefix == null) inPrefix = "";
					 if ( inSuffix == null) inSuffix = "";

					 asset = getMediaArchive().getAssetEditor().copyAsset(element, inPrefix + element.getId() + inSuffix); 
				 }
				 if (asset != null)
				 {
					 asset.addCategory(inCategory);
					 assetsToSave.add(asset);
				 }
			 }
			 getMediaArchive().saveAssets(assetsToSave);
		 }
	 }
	 public void moveAssetsToCategory(String[] inAssetid, Category inCategory1, Category inCategory2) throws OpenEditRuntimeException
	 {
		 List assetsToSave = new ArrayList();

		 if (inCategory1 != null && inCategory2 != null && inCategory1 != inCategory2)
		 {
			 for (int i = 0; i < inAssetid.length; i++) 
			 {
				 Asset asset = getMediaArchive().getAsset(inAssetid[i]); 
				 if (asset != null)
				 {
					 asset.removeCategory(inCategory1);
					 asset.addCategory(inCategory2);
					 assetsToSave.add(asset);
				 }
			 }
			 getMediaArchive().saveAssets(assetsToSave);
		 }


	 }
	 
	 public MediaArchive getMediaArchive()
	 {
		 return fieldMediaArchive;
	 }
	 
	 public void setMediaArchive(MediaArchive inMediaArchive)
	 {
		 fieldMediaArchive = inMediaArchive;
	 }

	public Category getCurrentCategory() {
		return fieldCurrentCategory;
	}

	public void setCurrentCategory(Category currentCategory) {
		fieldCurrentCategory = currentCategory;
	}
}
