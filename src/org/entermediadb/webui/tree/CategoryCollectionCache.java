package org.entermediadb.webui.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.entermediadb.asset.Category;
import org.entermediadb.projects.LibraryCollection;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.cache.CacheManager;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.HitTracker;

public class CategoryCollectionCache implements CatalogEnabled
{
	protected SearcherManager fieldSearcherManager;
	protected String fieldCatalogId;
	protected CacheManager fieldCacheManager;
	protected boolean init = false;
	
	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String inCategoryId)
	{
		fieldCatalogId = inCategoryId;
	}

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}

	protected CacheManager getCacheManager()
	{
		return fieldCacheManager;
	}
	
	public void setCacheManager(CacheManager inCacheManager)
	{
		fieldCacheManager = inCacheManager;
	}

	protected void loadRoots()
	{
		Searcher searcher = getSearcherManager().getSearcher(getCatalogId(), "librarycollection");
		HitTracker all = searcher.query().all().search();
		all.setHitsPerPage(2000);
		
		for (Iterator iterator = all.getPageOfHits().iterator(); iterator.hasNext();)
		{
			Data collection = (Data) iterator.next();
			String rootid = collection.get("rootcategory");
			if( rootid != null)
			{
				LibraryCollection librarycollection = (LibraryCollection)searcher.loadData(collection);
				fieldCacheManager.put("collectioncache", rootid, librarycollection);
			}
		}
//		CategorySearcher searcher = (CategorySearcher)getSearcherManager().getSearcher(getCatalogId(), "category");
//		
//		for (Iterator iterator = categoryids.iterator(); iterator.hasNext();)
//		{
//			String id = (String) iterator.next();
//			Category cat = searcher.getCategory(id);
//			if( cat != null)
//			{
//				fieldCategoryRoots.put( id, cat);
//			}
//		}
		
	}
	public String findCollectionId(Category inRoot)
	{
		if( !init )
		{
			loadRoots();
			init = true;
		}
		List parents  = inRoot.getParentCategories();
		if( parents != null)
		{
			parents = new ArrayList(parents);
			Collections.reverse(parents);
		}
		for (Iterator iterator = parents.iterator(); iterator.hasNext();)
		{
			Category parent = (Category) iterator.next();
			LibraryCollection exists = (LibraryCollection)getCacheManager().get("collectioncache", parent.getId());
			if( exists != null)
			{
				return exists.getId();
			}
		}
		
		//TODO: Do a DB lookup just to be sure?
		
		return null;
	}	
	public boolean isPartOfCollection(Category inRoot)
	{
		return findCollectionId(inRoot) != null;
	}

	public void addCollection(LibraryCollection inSaved)
	{
		getCacheManager().put("collectioncache", inSaved.getCategory().getId(),inSaved);
		
	}
	public void removedCollection(LibraryCollection inSaved)
	{
		getCacheManager().remove("collectioncache", inSaved.getCategory().getId());
		
	}
}
