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
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.HitTracker;

public class CategoryCollectionCache implements CatalogEnabled
{
	protected Map fieldCategoryRoots;
	protected SearcherManager fieldSearcherManager;
	protected String fieldCatalogId;
	
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

	protected Map getCategoryRoots()
	{
		if (fieldCategoryRoots == null)
		{
			fieldCategoryRoots = new HashMap(1000);
			loadRoots();
			
		}

		return fieldCategoryRoots;
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
				fieldCategoryRoots.put( rootid, librarycollection);
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
		List parents  = inRoot.getParentCategories();
		if( parents != null)
		{
			parents = new ArrayList(parents);
			Collections.reverse(parents);
		}
		for (Iterator iterator = parents.iterator(); iterator.hasNext();)
		{
			Category parent = (Category) iterator.next();
			LibraryCollection exists = (LibraryCollection)getCategoryRoots().get(parent.getId());
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
		getCategoryRoots().put(inSaved.getCategory().getId(),inSaved);
		
	}
	public void removedCollection(LibraryCollection inSaved)
	{
		getCategoryRoots().remove(inSaved.getCategory().getId());
		
	}
}
