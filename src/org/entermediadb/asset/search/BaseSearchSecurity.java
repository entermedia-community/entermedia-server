package org.entermediadb.asset.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.cache.CacheManager;
import org.openedit.data.PropertyDetail;
import org.openedit.data.QueryBuilder;
import org.openedit.data.SearchSecurity;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.SearchQuery;
import org.openedit.profile.UserProfile;
import org.openedit.users.Group;

public class BaseSearchSecurity implements SearchSecurity
{
	private static final Log log = LogFactory.getLog(BaseSearchSecurity.class);
	protected SearcherManager fieldSearcherManager;
	protected CacheManager fieldCacheManager;
	private static final SearchSecurity NULL = new SearchSecurity()
	{
		@Override
		public SearchQuery attachSecurity(WebPageRequest inPageRequest, Searcher inSearcher, SearchQuery inQuery)
		{
			return inQuery;
		};
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
		SearchSecurity filter = (SearchSecurity) getCacheManager().get("searchfilters", inSearcher.getCatalogId() + inSearcher.getSearchType());
		if (filter == null)
		{
			//Look up in the database a cache for this filter type
			Data config = getSearcherManager().getData(inSearcher.getCatalogId(), "searchsecurity", inSearcher.getSearchType());
			if (config != null)
			{
				String beanname = config.get("beanname");

				//Legacy fix
				beanname = beanname.replace("QueryFilter", "Security");
				filter = (SearchSecurity) getSearcherManager().getModuleManager().getBean(inSearcher.getCatalogId(), beanname);
			}
			else
			{
				if (getSearcherManager().getModuleManager().doesBeanExist(inSearcher.getSearchType() + "SearchSecurity"))
				{
					filter = (SearchSecurity) getSearcherManager().getModuleManager().getBean(inSearcher.getCatalogId(), inSearcher.getSearchType() + "SearchSecurity");
				}
				else
				{
					PropertyDetail detail = inSearcher.getDetail("securityenabled");
					if (detail != null)
					{
						filter = (SearchSecurity) getSearcherManager().getModuleManager().getBean(inSearcher.getCatalogId(), "securityEnabledSearchSecurity");
					}
					else
					{
						filter = NULL;
					}
				}
			}
			getCacheManager().put("searchfilters", inSearcher.getCatalogId() + inSearcher.getSearchType(), filter);
		}
		if (filter == NULL)
		{
			return inQuery;
		}
		return filter.attachSecurity(inPageRequest, inSearcher, inQuery);
	}

	public SearchQuery attachStandardSecurity(WebPageRequest inPageRequest, Searcher inSearcher, SearchQuery inQuery)
	{

		if (inQuery.isSecurityAttached())
		{
			return inQuery;
		}

		UserProfile profile = inPageRequest.getUserProfile();
		

		if (profile != null && profile.isInRole("administrator"))
		{
			return inQuery;
		}

		Collection groupids = new ArrayList();
		UserProfile inUserprofile = inPageRequest.getUserProfile();

		if (inUserprofile == null || inUserprofile.getUser() == null)
		{
			groupids.add("anonymous");
		}
		else
		{
			for (Iterator iterator = inUserprofile.getUser().getGroups().iterator(); iterator.hasNext();)
			{
				Group group = (Group) iterator.next();
				groupids.add(group.getId());
			}
		}
		String roleid = null;
		if (inUserprofile != null && inUserprofile.getSettingsGroup() != null)
		{
			roleid = inUserprofile.getSettingsGroup().getId();
		}
		else
		{
			roleid = "anonymous";
		}

		String userid = inPageRequest.getUserName();
		
		
		if (userid == null)
		{
		
			userid = "null";
		}
		

		QueryBuilder builder = inSearcher.query().or().orgroup("viewgroups", groupids).exact("viewroles", roleid).exact("owner", userid).exact("viewusers", userid);
		builder.exact("securityenabled", "false");
		SearchQuery securityfilter = builder.getQuery();

		inQuery.addChildQuery(securityfilter);

		inQuery.setSecurityAttached(true);
		return inQuery;
	}

}
