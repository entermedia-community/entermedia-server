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
import org.openedit.hittracker.SharedFilters;
import org.openedit.profile.UserProfile;

public class ElasticUserFilters implements SharedFilters
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
		Map<String,FilterNode> fieldSharedValues;
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


	public Map<String,FilterNode> getSharedValues(HitTracker inHits, WebPageRequest inReq)
	{
		if(inHits == null) {
			return new HashMap();
		}
		
		String input = inHits.getSearchQuery().getMainInput();
		if( input == null)
		{
			input = "*";
		}
		
		if( inHits.isEmpty() )
		{
			input = "*";
		}
		
		return getSharedValues(inHits.getSearcher(), inHits.getSearchQuery(), input, inReq);
	}
	public Map<String,FilterNode> getSharedValues(Searcher inSearcher, SearchQuery inQuery, String input, WebPageRequest inReq)
	{
		//Return everything most of the time. 
		
		String key = inSearcher.getSearchType() + input;
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
			List facets = getFilterViewProperties(inSearcher,"advancedfilter");
			if( !facets.isEmpty())
			{
				/**
				 * We want to keep a broad selection of filter so that people can choose more than one
				 */
				HitTracker all = (HitTracker)inSearcher.query().facets(facets)
						.attachSecurity(inReq).freeform("description", input).hitsPerPage(2).search();
				if( all.isEmpty() )
				{
					log.debug(" has no data to filter. DO you have a description field?");
				}
				values.fieldSharedValues = all.getActualFilterValues();
			}	
			else
			{
				values.fieldSharedValues = java.util.Collections.EMPTY_MAP;
			}	
			getValues().put(key, values);
		}
		return values.fieldSharedValues;
	}

	private List getFilterViewProperties(Searcher inSearcher, String inName)
	{
		List<PropertyDetail> view = getFilterView(inSearcher,inName);
		List facets = new ArrayList<PropertyDetail>();
		if( view != null && !view.isEmpty() )
		{
			for (Iterator iterator = view.iterator(); iterator.hasNext();)
			{
				PropertyDetail	detail = (PropertyDetail) iterator.next();
				if( detail.isFilter())
				{
					facets.add(detail);
				}
			}
		}
		return facets;
	}

	protected List<PropertyDetail> getFilterView(Searcher inSearcher, String inName)
	{
		List<PropertyDetail> view = getPropertyDetailsArchive().getView(  //assetadvancedfilter
				inSearcher.getSearchType(),inSearcher.getSearchType() + "/" + inSearcher.getSearchType() + inName, getUserProfile());
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
		List<PropertyDetail> view = getFilterView(inHits.getSearcher(),"advancedfilter");
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

	public Map<String,FilterNode>  loadValuesFromResults(List<FilterNode> nodes)
	{
		Map<String,FilterNode> options = new HashMap();
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

}
