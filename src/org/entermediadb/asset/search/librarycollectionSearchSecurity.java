package org.entermediadb.asset.search;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.openedit.WebPageRequest;
import org.openedit.data.SearchSecurity;
import org.openedit.data.Searcher;
import org.openedit.hittracker.SearchQuery;
import org.openedit.profile.UserProfile;

public class librarycollectionSearchSecurity implements SearchSecurity
{
	private static final Log log = LogFactory.getLog(librarycollectionSearchSecurity.class);

	public SearchQuery attachSecurity(WebPageRequest inPageRequest, Searcher inSearcher, SearchQuery inQuery)
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
			
		UserProfile profile = inPageRequest.getUserProfile();
		if (profile != null && profile.isInRole("administrator"))
		{
			SearchQuery child = inSearcher.query()
					.all()
					.notgroup("collectiontype", Arrays.asList("0","2","3"))
					.getQuery();
			inQuery.addChildQuery(child);
			inQuery.setSecurityAttached(true);
			return inQuery;
		}

		MediaArchive archive = (MediaArchive) inPageRequest.getPageValue("mediaarchive");

//		Collection<Category> catshidden = archive.listHiddenCategories(profile.getViewCategories()); //The ones I cant see
//		HashSet toshow = new HashSet(profile.getCollectionIds());
//		for (Iterator iterator = catshidden.iterator(); iterator.hasNext();)
//		{
//			Category hidden = (Category) iterator.next();
//			toshow.remove(hidden.getId());
//		}
		
		Set allowedcats = new HashSet(profile.getViewCategories());

		//@deprecate this code
		for (Iterator iterator = archive.listPublicCategories().iterator(); iterator.hasNext();)
		{
			Category publiccat = (Category) iterator.next();
			allowedcats.add(publiccat);
		}
		if( allowedcats.isEmpty() )
		{
			allowedcats.add("NONE");
		}
		SearchQuery child = inSearcher.query()
				.orgroup("parentcategories",allowedcats)
				//.notgroup("parentcategories", catshidden)
				.notgroup("collectiontype", Arrays.asList("0","2","3"))
				.getQuery();
		inQuery.addChildQuery(child);
		//Load all categories 1000
		//Compare to the profile categories and parents
		//run a securty fileter on collectionids
		//inQuery.setSecurityIds(toshow);
		inQuery.setSecurityAttached(true);
		
		//log.info("Collection search " + inQuery.toQuery());
		return inQuery;
	}
}
