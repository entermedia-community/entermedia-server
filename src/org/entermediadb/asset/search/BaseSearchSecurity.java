package org.entermediadb.asset.search;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.cache.CacheManager;
import org.openedit.data.SearchSecurity;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.SearchQuery;

public class BaseSearchSecurity implements SearchSecurity
{
	private static final Log log = LogFactory.getLog(BaseSearchSecurity.class);
	protected SearcherManager fieldSearcherManager;
	protected CacheManager fieldCacheManager;
	private static final SearchSecurity NULL = new SearchSecurity()
			{
				@Override
				public SearchQuery attachSecurity(WebPageRequest inPageRequest, Searcher inSearcher, SearchQuery inQuery) { return inQuery; };
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

	public SearchQuery attachSecurity(WebPageRequest inPageRequest, Searcher inSearcher, SearchQuery inQuery)
	{
		SearchSecurity filter = (SearchSecurity)getCacheManager().get("searchfilters", inSearcher.getCatalogId() +  inSearcher.getSearchType());
		if( filter == null)
		{
			//Look up in the database a cache for this filter type
			Data config = getSearcherManager().getData(inSearcher.getCatalogId(), "searchfilter",inSearcher.getSearchType());
			if( config != null)
			{
				String beanname = config.get("beanname");

				//Legacy fix
				beanname = beanname.replace("QueryFilter", "Security");
				filter = (SearchSecurity)getSearcherManager().getModuleManager().getBean(inSearcher.getCatalogId(),beanname);
			}
			else
			{
				if(getSearcherManager().getModuleManager().doesBeanExist(inSearcher.getSearchType() + "SearchSecurity")) 
				{
					filter = (SearchSecurity)getSearcherManager().getModuleManager().getBean(inSearcher.getCatalogId(),inSearcher.getSearchType() + "SearchSecurity" );
				} 
				else 
				{
					filter = NULL; 
				}
			}
			getCacheManager().put("searchfilters", inSearcher.getCatalogId() + inSearcher.getSearchType(),filter);
		}
		if( filter == NULL)
		{
			return inQuery;
		}
		return filter.attachSecurity(inPageRequest, inSearcher, inQuery);
	}
}
