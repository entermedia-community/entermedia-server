package org.entermediadb.asset.search;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.QueryBuilder;
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
		MediaArchive archive = (MediaArchive) inPageRequest.getPageValue("mediaarchive");

		UserProfile profile = inPageRequest.getUserProfile();
	
		Collection catshidden = archive.listHiddenCategories(profile.getViewCategories());
			
		Searcher librarysearcher = inSearcher.getSearcherManager().getSearcher(inSearcher.getCatalogId(),"library");
		Collection<Data> allowedlibraries = librarysearcher.query().or().match("privatelibrary", "false")
				.permissions(profile)
				.search();
			
		Collection ids = new ArrayList();
		for(Data library: allowedlibraries)
		{
			ids.add(library.getId());
		}
		if( ids.isEmpty())
		{
			ids.add("-1");
		}
		QueryBuilder child = inSearcher.query().orgroup("library", ids);
		if( !catshidden.isEmpty() )
		{
			child.notgroup("rootcategory",catshidden);
		}	
		inQuery.setChildren(null);
		inQuery.addChildQuery(child.getQuery());
		inQuery.setSecurityAttached(true);

		return inQuery;
	}
}
