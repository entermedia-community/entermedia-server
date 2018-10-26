package org.entermediadb.asset.search;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.cache.CacheManager;
import org.openedit.data.SearchQueryFilter;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.SearchQuery;

public class BaseSearchQueryFilter implements SearchQueryFilter
{
	private static final Log log = LogFactory.getLog(BaseSearchQueryFilter.class);
	protected SearcherManager fieldSearcherManager;
	protected CacheManager fieldCacheManager;
	private static final SearchQueryFilter NULL = new SearchQueryFilter()
			{
				@Override
				public SearchQuery attachFilter(WebPageRequest inPageRequest, Searcher inSearcher, SearchQuery inQuery) { return inQuery; };
			};
	public CacheManager getCacheManager()
	{
		return fieldCacheManager;
	}

	public void setCacheManager(CacheManager inCacheManager)
	{
		fieldCacheManager = inCacheManager;
	}

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}

	public SearchQuery attachFilter(WebPageRequest inPageRequest, Searcher inSearcher, SearchQuery inQuery)
	{
		SearchQueryFilter filter = (SearchQueryFilter)getCacheManager().get("searchfilters", inSearcher.getSearchType());
		if( filter == null)
		{
			Data config = getSearcherManager().getData(inSearcher.getCatalogId(), "searchfilter",inSearcher.getSearchType());
			if( config != null)
			{
				filter = (SearchQueryFilter)getSearcherManager().getModuleManager().getBean(inSearcher.getCatalogId(),config.get("beanname"));
			}
			else
			{
				if(getSearcherManager().getModuleManager().doesBeanExist(inSearcher.getSearchType() + "SearchQueryFilter")) {
					
				
				filter = (SearchQueryFilter)getSearcherManager().getModuleManager().getBean(inSearcher.getCatalogId(),inSearcher.getSearchType() + "SearchQueryFilter" );
				} else {
			
				
					filter = NULL; 
				}
			}
			getCacheManager().put("searchfilters",inSearcher.getSearchType(),filter);
		}
		if( filter == NULL)
		{
			return inQuery;
		}
		return filter.attachFilter(inPageRequest, inSearcher, inQuery);
	}
}
