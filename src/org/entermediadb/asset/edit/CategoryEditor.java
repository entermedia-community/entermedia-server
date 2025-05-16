package org.entermediadb.asset.edit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.xmldb.CategorySearcher;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.OpenEditRuntimeException;
import org.openedit.hittracker.HitTracker;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;
import org.openedit.repository.RepositoryException;
import org.openedit.users.User;

public class CategoryEditor {
	
	
	protected MediaArchive fieldMediaArchive;
	protected PageManager fieldPageManager;
	protected String fieldSearchType = "category";
	
	
	
	public String getSearchType() {
		return fieldSearchType;
	}

	public void setSearchType(String inSearchType) {
		fieldSearchType = inSearchType;
	}

	protected Category fieldCurrentCategory;
	
	private static final Log log = LogFactory.getLog(CategoryEditor.class);

	
	public CategorySearcher getCategorySearcher() {
		CategorySearcher searcher = (CategorySearcher) getMediaArchive().getSearcher(getSearchType());
		return searcher;
	}
	
	public Category getCategory(String inCategoryId) throws OpenEditRuntimeException
	{
		return getCategorySearcher().getCategory(inCategoryId);
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
		inCategory.sortChildren(false);
	}

	/**
	 * Adds a new category to the catalog.
	 * @param inId the new category's ID.
	 * @param inName the new category's name.
	 * @return the new category object.
	 */
	 public Category addNewCategory(String inId, String inName) throws OpenEditRuntimeException
	 {
		 Category newCat =  (Category) getCategorySearcher().createNewData();
		 newCat.setId(inId);
		 newCat.setName(inName);
		 if (getCurrentCategory() != null)
		 {
			 getCurrentCategory().addChild(newCat);
		 }
		 else
		 {
			 getRootCategory().addChild(newCat);
		 }
//		 else
//		 {
//			 getMediaArchive().getCategoryArchive().setRootCategory(newCat);
//		 }
		 getCategorySearcher().saveCategory(newCat);
		 return newCat;
	 }
	 
	 
	 public void saveCategory(Category inCategory) throws OpenEditRuntimeException
	 {
		 saveCategory(inCategory, false);
	 }

	 public void saveCategory(Category inCategory, Boolean saveTree) throws OpenEditRuntimeException
	 {
		 if ( inCategory.getParentCategory() == null && getMediaArchive().getCategoryArchive().getRootCategory().getId() != inCategory.getId())
		 {
			 getCategorySearcher().getRootCategory().addChild(inCategory);
			// getMediaArchive().getCategoryArchive().cacheCategory(inCategory);
		 }
//		 try
//		 {
//			 Page desc = getPageManager().getPage(getMediaArchive().getCatalogHome() + "/categories/" + inCategory.getId() + ".html");
//			 if ( !desc.exists() )
//			 {
//				 StringItem item = new StringItem(desc.getPath(), " ",desc.getCharacterEncoding() );
//				 desc.setContentItem(item);
//				 getPageManager().putPage(desc);
//			 }
//		 }
//		 catch ( Exception ex )
//		 {
//			 throw new OpenEditRuntimeException(ex);
//		 }
		 if (saveTree) 
		 {
			 getCategorySearcher().saveCategoryTree(inCategory);
		 }
		 else
		 {
			 getCategorySearcher().saveCategory(inCategory);
		 }
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
		 getCategorySearcher().delete(inCategory,null);
		 getMediaArchive().saveAssets(assets);
	 }

	 public Category getRootCategory() throws OpenEditRuntimeException
	 {
		 return getCategorySearcher().getRootCategory();
	 }

	 public void clearCategories() throws OpenEditRuntimeException
	 {
		 getCategorySearcher().clearCategories();
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
				 asset.removeValue("category",inCategory);
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

	public void copyEverything(User inUser, String[] inSourcecategoryids, String inTargetcategoryid)
	{
		CategorySearcher searcher = getMediaArchive().getCategorySearcher();
		Category targetparent = getMediaArchive().getCategory(inTargetcategoryid);
		if( inSourcecategoryids != null)
		{
			for (int i = 0; i < inSourcecategoryids.length; i++)
			{
				Category from = getMediaArchive().getCategory(inSourcecategoryids[i]);
				copyEverything(inUser,from, targetparent);
			}
		}
	}
/*	  
	public void copyCategory(MediaArchive inArchive, Category inSource, Category inDestination)
	{
		String finalpath = inFolder + "/" + inRoot.getName();
		Searcher assets = inArchive.getAssetSearcher();
		Searcher cats = inArchive.getSearcher("category");

		HitTracker assetlist = assets.fieldSearch("category-exact", inRoot.getId());
		for (Iterator iterator = assetlist.iterator(); iterator.hasNext();)
		{
			Data hit = (Data) iterator.next();
			Asset asset = (Asset) assets.loadData(hit);
			Page fullpath = inArchive.getPageManager().getPage("/WEB-INF/data/" + inArchive.getCatalogId() + "/originals/" + asset.getPath());
			log.info(fullpath.isFolder());
			if (!fullpath.exists())
			{
				log.info("Fullpath " + fullpath + "Did not exist");

			}
			log.info(fullpath);
			Page target = inArchive.getPageManager().getPage(finalpath);
			inArchive.getPageManager().copyPage(fullpath, target);

		}

		for (Iterator iterator = inRoot.getChildren().iterator(); iterator.hasNext();)
		{
			Category child = (Category) iterator.next();
			exportCategoryTree(inArchive, child, finalpath);
		}

	}	 
*/
	public void copyEverything(User inUser, Category fromchild, Category target)
	{
		copyTree(fromchild, target);
		//Search for all ids

		HitTracker assetlist = getMediaArchive().getAssetSearcher().fieldSearch("category", target.getId());
		getMediaArchive().fireMediaEvent(inUser, "category","assetsaddded", assetlist);
	}
	public void copyTree(Category fromchild, Category target)
	{
		//Grab the contents of fromchild
		Collection tosave = new ArrayList();
		HitTracker assetlist = getMediaArchive().getAssetSearcher().fieldSearch("category-exact", fromchild.getId());
		for (Iterator iterator = assetlist.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			Asset asset = (Asset)getMediaArchive().getAssetSearcher().loadData(data);
			asset.addCategory(target);
			tosave.add(asset);
		}
		//<path-action name="PathEventModule.runEvent" runpath="/${catalogid}/events/categories/assetsadded.html"  allowduplicates="true"/>

		//copy assets to new category
		getMediaArchive().getAssetSearcher().saveAllData(tosave,null);
		

		//Now keep going
		for (Iterator iterator = fromchild.getChildren().iterator(); iterator.hasNext();)
		{
			Category subchild = (Category) iterator.next();
			Category newchild = target.getChildByName(subchild.getName()); //Aleady exists?
			if( newchild == null )
			{
				newchild = (Category)getMediaArchive().getCategorySearcher().createNewData();
				newchild.setName(subchild.getName());
				target.addChild(newchild);
				getMediaArchive().getCategorySearcher().saveCategory(newchild);
			}
			copyTree(subchild, newchild);
		}

	}
	
	
	

//	 public void reBuildCategories() throws OpenEditRuntimeException
//	 {
//		Page totrash = getPageManager().getPage("/WEB-INF/data/" + getMediaArchive().getCatalogId() + "/categories.xml" );
//		getPageManager().removePage(totrash);
//		
//		String	datadir = "/WEB-INF/data/" + getMediaArchive().getCatalogId() + "/originals/";
//		getMediaArchive().getCategoryArchive().clearCategories();
//		//Category root = getMediaArchive().getCategoryArchive().createNewCategory("Index");
//		//getMediaArchive().getCategoryArchive().setRootCategory(root);
//
//		reBuildCategories(getMediaArchive().getCategoryArchive(), root, datadir,datadir);
//		
//	 }
//
//	private void reBuildCategories(CategoryArchive inCategoryArchive, Category inParent,  String inStartingFrom, String inFolder )
//	{
//		List children = getPageManager().getChildrenPaths(inFolder);
//		List assets = new ArrayList();
//		for (Iterator iterator = children.iterator(); iterator.hasNext();)
//		{
//			String path = (String) iterator.next();
//			ContentItem item = getPageManager().getRepository().get(path);
//			String source = path.substring(inStartingFrom.length());
//			Asset existing = getMediaArchive().getAssetBySourcePath(source);
//
//			if( existing == null && item.isFolder() )
//			{
//				Category cat = inCategoryArchive.createCategoryTree(source);
//				reBuildCategories(inCategoryArchive,cat, inStartingFrom,path);
//			}
//			else if(existing != null )
//			{
//				if( existing.getCategories().size() == 1 )
//				{
//					Category found = (Category)existing.getCategories().get(0);
//					if( found.getId().equals(inParent.getId() ) )
//					{
//						continue;
//					}
//				}
//				existing.clearCategories();
//				existing.addCategory(inParent);
//				assets.add(existing);
//			}
//		}
//		getMediaArchive().saveAssets(assets);
//	}
//	
}
