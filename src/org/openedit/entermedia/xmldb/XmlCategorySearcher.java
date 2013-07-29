package org.openedit.entermedia.xmldb;

import java.util.Collection;
import java.util.Iterator;

import org.openedit.Data;
import org.openedit.data.BaseSearcher;
import org.openedit.entermedia.Category;
import org.openedit.entermedia.CategoryArchive;

import com.openedit.OpenEditException;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.ListHitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.hittracker.Term;
import com.openedit.users.User;

public class XmlCategorySearcher extends BaseSearcher
{
	protected CategoryArchive fieldCategoryArchive;
	
	public CategoryArchive getCategoryArchive()
	{
		return fieldCategoryArchive;
	}

	public void setCategoryArchive(CategoryArchive inCategoryArchive)
	{
		fieldCategoryArchive = inCategoryArchive;
	}

	@Override
	public void reIndexAll() throws OpenEditException
	{
		
	}
	@Override
	public Data createNewData()
	{
		return new Category();
	}
	@Override
	public SearchQuery createSearchQuery()
	{
		return new SearchQuery();
	}

	@Override
	public HitTracker search(SearchQuery inQuery)
	{
		ListHitTracker hits = new ListHitTracker();
		Term id = inQuery.getTermByDetailId("id");
		if( id != null)
		{
			Category category = getCategoryArchive().getCategory(id.getValue());
			if( category != null)
			{
				hits.getList().add(category);
			}
		}
		
		Term name = inQuery.getTermByDetailId("name");
		if( name != null)
		{
			Category category = getCategoryArchive().getCategoryByName(name.getValue());
			if( category != null)
			{
				hits.getList().add(category);
			}
		}
		return hits;
	}

	@Override
	public String getIndexId()
	{
		return String.valueOf( getCategoryArchive().getRootCategory().hashCode() );
	}

	@Override
	public void clearIndex()
	{
		
	}

	@Override
	public void deleteAll(User inUser)
	{
		getCategoryArchive().deleteCategory(getCategoryArchive().getRootCategory());
	}

	@Override
	public void delete(Data inData, User inUser)
	{
		Category cat = getCategoryArchive().getCategory(inData.getId());
		getCategoryArchive().deleteCategory(cat);
	}

	@Override
	public void saveAllData(Collection<Data> inAll, User inUser)
	{
		for (Iterator iterator = inAll.iterator(); iterator.hasNext();)
		{
			Category cat = (Category) iterator.next();
			getCategoryArchive().saveCategory(cat);		
			
		}
	}

}
