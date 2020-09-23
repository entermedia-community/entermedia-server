package org.entermediadb.elasticsearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.modules.DataEditModule;
import org.openedit.WebPageRequest;
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
	private static final Log log = LogFactory.getLog(ElasticUserFilters.class);

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
	public Map getFilterValues(HitTracker inHits, WebPageRequest inReq)
	{
		if(inHits == null) {
			return new HashMap();
		}
		Map filterValues = getFilterValues(inHits.getSearcher(), inHits.getSearchQuery(), inReq);
		return filterValues;
	}
	public Map getFilterValues(Searcher inSearcher, SearchQuery inQuery, WebPageRequest inReq)
	{
		List <FilterNode> nodes = getFilterOptions(inSearcher, inQuery, inReq);
		Map options = new HashMap();
		if( nodes != null)
		{
			for (Iterator iterator = nodes.iterator(); iterator.hasNext();)
			{
				FilterNode filterNode = (FilterNode) iterator.next();
				PropertyDetail detail = filterNode.getPropertyDetail();
				if (detail == null) {
					log.error("Filter got Null property:" + filterNode.getName());
				} else {
					options.put(detail.getId(), filterNode);
				}
			}
		}
		return options;
		
	}
	public List<FilterNode> getFilterOptions(HitTracker inHits, WebPageRequest inReq)
	{
		return getFilterOptions(inHits.getSearcher(), inHits.getSearchQuery(), inReq);
	}
	public List<FilterNode> getFilterOptions(Searcher inSearcher, SearchQuery inQuery, WebPageRequest inReq)
	{
		
		//Return everything most of the time. 
		
		String input = inQuery.getMainInput();
		if( input == null)
		{
			input = "*";
		}
		
		String key = inSearcher.getSearchType() + inQuery.getMainInput();
		IndexValues values = (IndexValues)getValues().get(key);
		log.info("Loadedd from cache " + key + " = " + values);
		if( values == null || 
				!values.fieldIndexId.equals( inSearcher.getIndexId() ) || 
				values.isExpired()  
				 )
		 //|| true)
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
				/**
				 * We want to keep a broad selection of filter so that people can choose more than one
				 */
				HitTracker all = (HitTracker)inSearcher.query().facets(facets)
						.attachSecurity(inReq).freeform("description", input).hitsPerPage(2).search();
				if( all.isEmpty() )
				{
					log.debug(" has no data to filter. DO you have a description field?");
				}
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
	public void clearOptions(String inSearchType, String inDescription)
	{
		getValues().remove(inSearchType + inDescription);
	}

	@Override
	public void flagUserFilters(HitTracker inHits)
	{
		Collection terms = getFilteredTerms(inHits);
		for (Iterator iterator = terms.iterator(); iterator.hasNext();)
		{
			Term term = (Term) iterator.next();
			term.setUserFilter(true);
		}
	}

	@Override
	public List<FilterNode> getFilterOptions(HitTracker inHits)
	{
	return getFilterOptions(inHits, null);
	}

	@Override
	public Map<String, FilterNode> getFilterValues(HitTracker inHits)
	{
	return getFilterValues(inHits, null);
	}

}
