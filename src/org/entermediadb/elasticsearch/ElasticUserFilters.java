package org.entermediadb.elasticsearch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openedit.cache.CacheManager;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetailsArchive;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.FilterNode;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.hittracker.UserFilters;
import org.openedit.profile.UserProfile;

public class ElasticUserFilters implements UserFilters
{

	protected UserProfile fieldUserProfile;
	protected CacheManager fieldCacheManager;

	public CacheManager getCacheManager()
	{
		return fieldCacheManager;
	}

	public void setCacheManager(CacheManager inCacheManager)
	{
		fieldCacheManager = inCacheManager;
	}

	protected PropertyDetailsArchive fieldPropertyDetailsArchive;
	protected SearcherManager fieldSearcherManager;

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}

	public PropertyDetailsArchive getPropertyDetailsArchive()
	{
		if (fieldPropertyDetailsArchive == null)
		{
			fieldPropertyDetailsArchive = getSearcherManager().getPropertyDetailsArchive(getUserProfile().getCatalogId());

		}

		return fieldPropertyDetailsArchive;

	}

	public UserProfile getUserProfile()
	{
		return fieldUserProfile;
	}

	public void setUserProfile(UserProfile inUserProfile)
	{
		fieldUserProfile = inUserProfile;
	}

	public void addFilterOptions(Searcher inSearcher, SearchQuery inQuery, List<FilterNode> inFilters)
	{
		if (inQuery.getMainInput() != null)
		{
			getCacheManager().put("facethits" + inSearcher.getSearchType(), inQuery.getMainInput(), inFilters);
		}
	}
	public Map getFilterValues(Searcher inSearcher, SearchQuery inQuery)
	{
		List <FilterNode> nodes = getFilterOptions(inSearcher, inQuery);
		Map options = new HashMap();
		if( nodes != null)
		{
			for (Iterator iterator = nodes.iterator(); iterator.hasNext();)
			{
				FilterNode filterNode = (FilterNode) iterator.next();
				options.put(filterNode.getPropertyDetail().getId(), filterNode);
			}
		}
		return options;
		
	}

	public List<FilterNode> getFilterOptions(Searcher inSearcher, SearchQuery inQuery)
	{
		if (inQuery.getMainInput() != null)
		{
			List<FilterNode> list = (List<FilterNode>)getCacheManager().get("facethits" + inSearcher.getSearchType(), inQuery.getMainInput());
			if( list == null)
			{
				List<PropertyDetail> view = getPropertyDetailsArchive().getView(inSearcher.getSearchType(),inSearcher.getSearchType() + "/" + inSearcher.getSearchType() + "facets", getUserProfile());
				if( view == null || view.isEmpty() )
				{
					List facets = new ArrayList<PropertyDetail>();
					for (Iterator iterator = view.iterator(); iterator.hasNext();)
					{
						PropertyDetail	detail = (PropertyDetail) iterator.next();
						if( detail.isFilter())
						{
							facets.add(detail);
						}
					}
					
					HitTracker all = (HitTracker)inSearcher.query().facets(facets).freeform("description", inQuery.getMainInput()).search();
					list = all.getFilterOptions();
				}				
				getCacheManager().put("facethits" + inSearcher.getSearchType(), inQuery.getMainInput(), list);
			}
			return list;
		}
		else
		{
			return null;
		}

	}

	
	public void clear(String inSearchType)
	{
		getCacheManager().clear("facethits" + inSearchType);
	}

}
