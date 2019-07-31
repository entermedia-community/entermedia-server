package org.entermediadb.asset.search;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.WebPageRequest;
import org.openedit.data.SearchSecurity;
import org.openedit.data.Searcher;
import org.openedit.hittracker.SearchQuery;
import org.openedit.profile.UserProfile;

public class categorySearchSecurity implements SearchSecurity
{
	private static final Log log = LogFactory.getLog(categorySearchSecurity.class);

	public SearchQuery  attachSecurity(WebPageRequest inPageRequest, Searcher inSearcher, SearchQuery inQuery) 
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
		
//		Collection groupids = new ArrayList();
		UserProfile inUserprofile = inPageRequest.getUserProfile();
//		
//		if( inUserprofile == null || inUserprofile.getUser() == null)
//		{
//			groupids.add("anonymous");
//		}
//		else
//		{
//			for (Iterator iterator = inUserprofile.getUser().getGroups().iterator(); iterator.hasNext();)
//			{
//				Group group = (Group) iterator.next();
//				groupids.add(group.getId());
//			}
//		}
//		String roleid = null;
//		if( inUserprofile.getSettingsGroup() != null)
//		{
//			roleid = inUserprofile.getSettingsGroup().getId();
//		}
//		else
//		{
//			roleid = "anonymous";
//		}
		
//		SearchQuery securityfilter = inSearcher.query().or().
//			orgroup("viewgroups", groupids).
//			match("viewroles", roleid).
//			match("viewusers", inUserprofile.getUserId()).getQuery();
		
		//get the list of parents. Add parent filter
		Collection view = inUserprofile.getViewCategories();
		
		SearchQuery securityfilter = inSearcher.query().orgroup("parents", view ).getQuery();
		
		inQuery.addChildQuery(securityfilter);
		
		inQuery.setSecurityAttached(true);
		
		
		return inQuery;
		
	
	}
}
