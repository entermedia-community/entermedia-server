package org.entermediadb.asset.search;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.openedit.WebPageRequest;
import org.openedit.data.SearchQueryFilter;
import org.openedit.data.Searcher;
import org.openedit.hittracker.SearchQuery;
import org.openedit.profile.UserProfile;
import org.openedit.users.User;

public class librarycollectionSearchQueryFilter implements SearchQueryFilter
{
	private static final Log log = LogFactory.getLog(librarycollectionSearchQueryFilter.class);

	public SearchQuery attachFilter(WebPageRequest inPageRequest, Searcher inSearcher, SearchQuery inQuery)
	{
		boolean enabled = inQuery.isEndUserSearch();
		//log.info( "security filer enabled "  + enabled );
		if (!enabled)
		{
			return inQuery;
		}
		if( inQuery.isSecurityAttached() )
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
		MediaArchive archive = (MediaArchive) inPageRequest.getPageValue("mediaarchive");

		UserProfile profile = inPageRequest.getUserProfile();

		Collection<Category> catshidden = archive.listHiddenCategories(profile.getViewCategories());
//		HashSet toshow = new HashSet(profile.getCollectionIds());
//		for (Iterator iterator = catshidden.iterator(); iterator.hasNext();)
//		{
//			Category hidden = (Category) iterator.next();
//			toshow.remove(hidden.getId());
//		}
		SearchQuery child = inSearcher.query()
				.orgroup("parentcategories",profile.getViewCategories())
				.notgroup("parentcategories", catshidden)
				.getQuery();
		inQuery.addChildQuery(child);
		//Load all categories 1000
		//Compare to the profile categories and parents
		//run a securty fileter on collectionids
		//inQuery.setSecurityIds(toshow);
		inQuery.setSecurityAttached(true);
		
		//log.info(inQuery.toQuery());
		return inQuery;
	}
}
