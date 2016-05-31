package org.entermediadb.asset.search;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.cache.CacheManager;
import org.openedit.data.SearchQueryFilter;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.profile.UserProfile;
import org.openedit.users.User;

public class librarySearchQueryFilter implements SearchQueryFilter
{
	private static final Log log = LogFactory.getLog(librarySearchQueryFilter.class);

	public SearchQuery  attachFilter(WebPageRequest inPageRequest, Searcher inSearcher, SearchQuery inQuery) 
	{
		User user = inPageRequest.getUser();
		//log.info( "found filer user  "  + user + " " + user.isInGroup("administrators"));
		if (user != null && user.isInGroup("administrators"))
		{
			//dont filter since its the admin
			return inQuery;
		}

		String enabled = inQuery.isEndUserSearch();
		//log.info( "security filer enabled "  + enabled );
		if (!Boolean.parseBoolean(enabled))
		{
			return inQuery;
		}

		//Run a search on another table, find a list of id's, add them to the query
		UserProfile profile = inPageRequest.getUserProfile();
		if (profile != null)
		{
			Collection<String> libraryids = profile.getCombinedLibraries();
			if (log.isDebugEnabled())
			{
				log.debug("added security filer for " + inPageRequest.getUserProfile());
			}
			if (libraryids.size() == 0)
			{
				libraryids = new ArrayList();
				libraryids.add("-1");
			}
			inQuery.setSecurityIds(libraryids);
			inQuery.setSecurityAttached(true);
		}
		return inQuery;
	}
}
