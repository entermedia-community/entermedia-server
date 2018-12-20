package org.entermediadb.webui.tree;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.entermediadb.asset.Category;
import org.entermediadb.asset.xmldb.CategorySearcher;
import org.openedit.Data;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.HitTracker;

public class CategoryCollectionCache
{
	protected Map fieldCategoryRoots;
	protected SearcherManager fieldSearcherManager;
	protected String fieldCategoryId;
	
	public String getCategoryId()
	{
		return fieldCategoryId;
	}

	public void setCategoryId(String inCategoryId)
	{
		fieldCategoryId = inCategoryId;
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
		HitTracker all = getSearcherManager().query(getCategoryId(), "librarycollection").all().search();
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
		CategorySearcher searcher = (CategorySearcher)getSearcherManager().getSearcher(getCategoryId(), "category");
		
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
}
