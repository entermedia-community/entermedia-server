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
	
	protected Map<String,LibraryCollection> getCategoryLookup()
	{
		Map<String,LibraryCollection> lookup = (Map<String,LibraryCollection>)getTimedCacheManager().get(getCatalogId(),"collectioncache");
		if( lookup == null)
		{
			lookup = new HashMap();
			Searcher searcher = getSearcherManager().getSearcher(getCatalogId(), "librarycollection");
			HitTracker all = searcher.query().exists("rootcategory").search();
			all.setHitsPerPage(2000);
			
			for (Iterator iterator = all.getPageOfHits().iterator(); iterator.hasNext();)
			{
				Data collection = (Data) iterator.next();
				String rootid = collection.get("rootcategory");
				if( rootid != null)
				{
					LibraryCollection librarycollection = (LibraryCollection)searcher.loadData(collection);
					lookup.put(rootid, librarycollection);
				}
			}
			getTimedCacheManager().put(getCatalogId(),"collectioncache",lookup);
		}
		return lookup;
	}
	
	public String getCollectionId(Category inRoot)
	{
		if( inRoot == null)
		{
			return null;
		}
		LibraryCollection exists = getCategoryLookup().get(inRoot.getId());
		if( exists == null )
		{
			return null;
		}
		return exists.getId();
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
		String collectionid = getCollectionId(inRoot);
		if( collectionid == null)
		{
			return false;
		}
		return true;
					
	}

	
	public LibraryCollection findCollection(Category inRoot)
	{
		List parents  = inRoot.getParentCategories();
		if( parents != null)
		{
			parents = new ArrayList(parents);
			Collections.reverse(parents);
		}
		for (Iterator iterator = parents.iterator(); iterator.hasNext();)
		{
			Category parent = (Category) iterator.next();
			LibraryCollection exists = getCategoryLookup().get(parent.getId());
			if( exists != null)
			{
				return exists;  //Loaded on boot up once time and cached heaviliy
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
		String rootcat = inSaved.getRootCategoryId();
		if( rootcat!= null)
		{
			getCategoryLookup().put(rootcat,inSaved);
		}
	}
	
}
