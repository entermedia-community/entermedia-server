package org.entermediadb.asset.search;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.WebPageRequest;
import org.openedit.data.SearchQueryFilter;
import org.openedit.data.Searcher;
import org.openedit.hittracker.SearchQuery;
import org.openedit.profile.UserProfile;
import org.openedit.users.User;

public class librarycollectionSearchQueryFilter implements SearchQueryFilter {
	private static final Log log = LogFactory.getLog(librarycollectionSearchQueryFilter.class);

	public SearchQuery  attachFilter(WebPageRequest inPageRequest, Searcher inSearcher, SearchQuery inQuery) {
		boolean enabled = inQuery.isEndUserSearch();
		//log.info( "security filer enabled "  + enabled );
		if (!enabled)
		{
			return inQuery;
		}


		User user = inPageRequest.getUser();
		//log.info( "found filer user  "  + user + " " + user.isInGroup("administrators"));
		if (user != null && user.isInGroup("administrators"))
		{
			//dont filter since its the admin
			return inQuery;
		}

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
			SearchQuery child = inSearcher.query().orgroup("library", libraryids).getQuery();
			//TODO: Clear old child queries
			inQuery.setChildren(null);
			inQuery.addChildQuery(child);
			inQuery.setSecurityAttached(true);
			//inTracker.getSearchQuery().setSecurityIds(libraryids);
		}

		return inQuery;
	}
}
