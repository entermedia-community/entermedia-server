package org.entermediadb.elasticsearch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetailsArchive;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.FilterNode;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.hittracker.Term;
import org.openedit.hittracker.UserFilters;
import org.openedit.profile.UserProfile;

public class ElasticUserFilters implements UserFilters
{

	protected UserProfile fieldUserProfile;
	protected Map fieldValues;

	public Map getValues()
	{
		if (fieldValues == null)
		{
			fieldValues = new HashMap(2);
		}

		return fieldValues;
	}

	class IndexValues
	{
		String fieldIndexId;
		List<FilterNode> fieldValues;
		long fieldCreatedOn = System.currentTimeMillis();
		public boolean isExpired()
		{
			if( System.currentTimeMillis() > fieldCreatedOn + (1000l * 60l * 5l) ) //5 minutes
			{
				return true;
			}
			return false;
		}
		
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

//	public void addFilterOptions(Searcher inSearcher, SearchQuery inQuery, List<FilterNode> inFilters)
//	{
//		if (inQuery.getMainInput() != null)
//		{
//			getCacheManager().put(inQuery.getMainInput(), inFilters);
//		}
//	}
	public Map getFilterValues(HitTracker inHits)
	{
		return getFilterValues(inHits.getSearcher(), inHits.getSearchQuery());
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
	public List<FilterNode> getFilterOptions(HitTracker inHits)
	{
		return getFilterOptions(inHits.getSearcher(), inHits.getSearchQuery());
	}
	public List<FilterNode> getFilterOptions(Searcher inSearcher, SearchQuery inQuery)
	{
		if (inQuery.getMainInput() != null)
		{
			String key = inSearcher.getSearchType() + inQuery.getMainInput();
			IndexValues values = (IndexValues)getValues().get(key);
			if( values == null || values.fieldIndexId != inSearcher.getIndexId() || values.isExpired() ) //|| true)
			{
				values = new IndexValues();
				values.fieldIndexId = inSearcher.getIndexId();
				List<PropertyDetail> view = getFilterView(inSearcher);
				if( view != null && !view.isEmpty() )
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
					values.fieldValues = all.getFilterOptions();
				}	
				else
				{
					values.fieldValues = java.util.Collections.EMPTY_LIST;
				}
				getValues().put(key, values);
			}
			return values.fieldValues;
		}
		else
		{
			return null;
		}
	}

	protected List<PropertyDetail> getFilterView(Searcher inSearcher)
	{
		List<PropertyDetail> view = getPropertyDetailsArchive().getView(  //assetadvancedfilter
				inSearcher.getSearchType(),inSearcher.getSearchType() + "/" + inSearcher.getSearchType() + "advancedfilter", getUserProfile());
		return view;
	}
	public List getFilteredTerms(HitTracker inHits)
	{
		if( inHits == null)
		{
			return Collections.EMPTY_LIST;
		}
		List terms = new ArrayList();
		SearchQuery inQuery = inHits.getSearchQuery();
		List<PropertyDetail> view = getFilterView(inHits.getSearcher());
		if( view == null)
		{
			return Collections.EMPTY_LIST;
		}
		for (Iterator iterator = view.iterator(); iterator.hasNext();)
		{
			PropertyDetail propertyDetail = (PropertyDetail) iterator.next();
			Term term = inQuery.getTermByDetailId(propertyDetail.getId());
			if( term != null)
			{
				terms.add(term);
			}
		}
		return terms;
	}
	
	public void clear(String inSearchType)
	{
		getValues().clear();
	}

}
