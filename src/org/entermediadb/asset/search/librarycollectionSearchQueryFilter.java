package org.entermediadb.asset.search;

import java.util.ArrayList;
import java.util.Collection;
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

//		if(archive != null){
//			boolean publiclibs = Boolean.parseBoolean(archive.getCatalogSettingValue("publiccollections"));
//			if(publiclibs){
//				return inQuery;
//			}
//		}

		UserProfile profile = inPageRequest.getUserProfile();
		if (profile != null)
		{
			
			Collection<String> viewcategories = profile.getViewCategories();
			if (log.isDebugEnabled())
			{
				log.debug("added security filer for " + inPageRequest.getUserProfile());
			}
			if (viewcategories.size() == 0)
			{
				viewcategories = new ArrayList();
				viewcategories.add("-1");
			}
			Collection catshidden = archive.listHiddenCategories();
			Collection hiddencatids = new ArrayList();
			for (Iterator iterator = catshidden.iterator(); iterator.hasNext();)
			{
				Category cathidden = (Category)iterator.next();
				if( !cathidden.hasSelf(viewcategories))
				{
					hiddencatids.add(cathidden.getId());
				}
				
			}
			//.or().orgroup("rootcategory", viewcategories).not("visibility", "3")
			
//			Searcher librarysearcher = inSearcher.getSearcherManager().getSearcher(inSearcher.getCatalogId(),"library");
//			Collection<Data> libraries = librarysearcher.query().orgroup("categoryid", viewcategories).search();
//			for(Data library: libraries)
//			{
//				ids.add(library.getId());
//			}
			SearchQuery child = inSearcher.query().all().notgroup("rootcategory", hiddencatids).getQuery();
			//TODO: Clear old child queries
			inQuery.setChildren(null);
			inQuery.addChildQuery(child);
			inQuery.setSecurityAttached(true);
			//inTracker.getSearchQuery().setSecurityIds(libraryids);
		}

		return inQuery;
	}
}
