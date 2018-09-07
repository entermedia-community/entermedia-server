package org.entermediadb.asset.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.WebPageRequest;
import org.openedit.data.SearchQueryFilter;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.profile.UserProfile;
import org.openedit.users.Group;

public class librarySearchQueryFilter implements SearchQueryFilter
{
	private static final Log log = LogFactory.getLog(librarySearchQueryFilter.class);

	public SearchQuery  attachFilter(WebPageRequest inPageRequest, Searcher inSearcher, SearchQuery inQuery) 
	{
		if( inQuery.isSecurityAttached())
		{
			return inQuery;
		}
		
		UserProfile profile = inPageRequest.getUserProfile();
		if (profile != null && profile.isInRole("administrator"))
		{
			return inQuery;					
		}
		
		//TODO: Add Hidden Libraries
		Collection groupids = new ArrayList();
		UserProfile inUserprofile = inPageRequest.getUserProfile();
		
		if( inUserprofile == null || inUserprofile.getUser() == null)
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
		if( inUserprofile.getSettingsGroup() != null)
		{
			roleid = inUserprofile.getSettingsGroup().getId();
		}
		else
		{
			roleid = "anonymous";
		}
		
		SearchQuery securityfilter = inSearcher.query().or().
			match("privatelibrary", "false").
			orgroup("viewgroups", groupids).
			match("viewroles", roleid).
			match("owner", inUserprofile.getUserId()).
			match("viewusers", inUserprofile.getUserId()).getQuery();
		
		inQuery.addChildQuery(securityfilter);
		
		inQuery.setSecurityAttached(true);
		
		
		return inQuery;
		
		/*
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
//			boolean publiclibs = Boolean.parseBoolean(archive.getCatalogSettingValue("publiclibraries"));
//			if(publiclibs){
//				return inQuery;
//			}
//		}
//		
		
		//Run a search on another table, find a list of id's, add them to the query
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
			//inQuery.setSecurityIds(libraryids);
			inQuery.addOrsGroup("categoryid", viewcategories);
			inQuery.setSecurityAttached(true);
		}
		return inQuery;
		*/
	}
}
