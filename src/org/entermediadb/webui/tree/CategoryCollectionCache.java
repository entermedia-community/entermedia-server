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
	private static final LibraryCollection NULLCOLLECTION = new LibraryCollection();
	protected SearcherManager fieldSearcherManager;
	protected String fieldCatalogId;
	protected CacheManager fieldCacheManager;
	protected CacheManager fieldTimedCacheManager;
	public CacheManager getTimedCacheManager()
	{
		return fieldTimedCacheManager;
	}

	public void setTimedCacheManager(CacheManager inTimedCacheManager)
	{
		fieldTimedCacheManager = inTimedCacheManager;
	}
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
				getCacheManager().put(getCatalogId() + "collectioncache", rootid, librarycollection);
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
		if( inRoot == null)
		{
			return null;
		}
		LibraryCollection collection = findCollection(inRoot);
		if( collection != null)
		{
			return collection.getId();
		}
		return null;
	}

	public boolean isCollectionRoot(Category inRoot)
	{
		if( inRoot == null)
		{
			return false;
		}
		LibraryCollection exists = (LibraryCollection)getCacheManager().get(getCatalogId() + "collectioncache", inRoot.getId());
		if( exists == NULLCOLLECTION)
		{
			return false;
		}
		if( exists == null)
		{
			exists = findCollection(inRoot);
		}
		if( exists != null)
		{
			return inRoot.getId().equals(exists.getRootCategoryId());
		}
		
		return false;
	}

	
	public LibraryCollection findCollection(Category inRoot)
	{
		if( !init )
		{
			loadRoots();
			init = true;
		}
		if( "index".equals(inRoot.getId()) )
		{
			return null;
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
			LibraryCollection exists = (LibraryCollection)getCacheManager().get(getCatalogId() + "collectioncache", parent.getId());
			if( exists != null)
			{
				return exists;  //Loaded on boot up once time and cached heaviliy
			}
			
			exists = (LibraryCollection)getTimedCacheManager().get(getCatalogId() + "collectioncache", parent.getId());
			if( exists == NULLCOLLECTION)
			{
				return null;
			}
			if( exists != null)
			{
				return exists;
			}
			else
			{
				//It expired after 15 min. Do a DB lookup just to be sure
				Searcher searcher = getSearcherManager().getSearcher(getCatalogId(), "librarycollection");
				Data found = (Data)searcher.query().orgroup("rootcategory", parents).searchOne();
				if( found == null)
				{
					exists = NULLCOLLECTION;
				}
				else
				{
					exists = (LibraryCollection)searcher.loadData(found);
				}
				getTimedCacheManager().put(getCatalogId() + "collectioncache", inRoot.getId(), exists);
				if( exists != NULLCOLLECTION)
				{
					return exists;
				}
			}
		}
		return null;
		
	}	
	public boolean isPartOfCollection(Category inRoot)
	{
		return findCollectionId(inRoot) != null;
	}

	public void addCollection(LibraryCollection inSaved)
	{
		getCacheManager().put(getCatalogId() + "collectioncache", inSaved.getCategory().getId(),inSaved);
		
	}
	public void removedCollection(LibraryCollection inSaved)
	{
		getCacheManager().remove(getCatalogId() + "collectioncache", inSaved.getCategory().getId());
		
	}
}
