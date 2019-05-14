package org.entermediadb.webui.tree;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.entermediadb.asset.Category;
import org.entermediadb.asset.xmldb.CategorySearcher;
import org.entermediadb.projects.LibraryCollection;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
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

	public Map getCategoryRoots()
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
		HitTracker all = getSearcherManager().query(getCatalogId(), "librarycollection").all().search();
		all.setHitsPerPage(1000);
		Set categoryids = new HashSet();
		for (Iterator iterator = all.getPageOfHits().iterator(); iterator.hasNext();)
		{
			Data collection = (Data) iterator.next();
			String rootid = collection.get("rootcategory");
			if( rootid != null)
			{
				categoryids.add(rootid);
			}
		}
		CategorySearcher searcher = (CategorySearcher)getSearcherManager().getSearcher(getCatalogId(), "category");
		
		for (Iterator iterator = categoryids.iterator(); iterator.hasNext();)
		{
			String id = (String) iterator.next();
			Category cat = searcher.getCategory(id);
			if( cat != null)
			{
				fieldCategoryRoots.put( id, cat);
			}
		}
		
	}

	public void setCategoryRoots(Map inCategoryRoots)
	{
		fieldCategoryRoots = inCategoryRoots;
	}
	
	public boolean isPartOfCollection(Category inRoot)
	{
		List parents  = inRoot.getParentCategories();
		for (Iterator iterator = parents.iterator(); iterator.hasNext();)
		{
			Category parent = (Category) iterator.next();
			Category exists = (Category)getCategoryRoots().get(parent.getId());
			if( exists != null)
			{
				return true;
			}
		}
		return false;
	}

	public void addCollection(LibraryCollection inSaved)
	{
		getCategoryRoots().put(inSaved.getCategory().getId(),inSaved.getCategory());
		
	}
	public void removedCollection(LibraryCollection inSaved)
	{
		getCategoryRoots().remove(inSaved.getCategory().getId());
		
	}
}
