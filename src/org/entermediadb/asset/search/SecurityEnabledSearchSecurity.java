package org.entermediadb.asset.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.QueryBuilder;
import org.openedit.data.SearchSecurity;
import org.openedit.data.Searcher;
import org.openedit.hittracker.SearchQuery;
import org.openedit.profile.UserProfile;
import org.openedit.users.Group;

public class SecurityEnabledSearchSecurity implements SearchSecurity
{
	private static final Log log = LogFactory.getLog(SecurityEnabledSearchSecurity.class);

	public SearchQuery  attachSecurity(WebPageRequest inPageRequest, Searcher inSearcher, SearchQuery inQuery) 
	{
		if( inQuery.isSecurityAttached())
		{
			return inQuery;
		}
		
		UserProfile profile = inPageRequest.getUserProfile();
		if (profile != null && profile.isInGroup("showalldata"))
		{
			return inQuery;					
		}

		if (profile != null && profile.isInRole("administrator"))
		{
			return inQuery;					
		}
		
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
		if( inUserprofile != null && inUserprofile.getSettingsGroup() != null)
		{
			roleid = inUserprofile.getSettingsGroup().getId();
		}
		else
		{
			roleid = "anonymous";
		}
		
		String userid = null;
		if( inUserprofile != null)
		{
			userid = inUserprofile.getUserId();
		}
		else
		{
			userid  = "null";
		}
		boolean lesssecure = true;

		Data module = inSearcher.getSearcherManager().getCachedData(inSearcher.getCatalogId(), "module", inSearcher.getSearchType());
		if( module != null)
		{
			String recordvisibility = module.get("recordvisibility");
			if( recordvisibility == null || recordvisibility.equals("showbydefault"))
			{
				lesssecure = true;
			}
			else if(  recordvisibility.equals("hidebydefault"))
			{
				lesssecure = false;
			}
		}
//		<property id="hidebydefault">Hidden</property>
//		<property id="showbydefault">Visible</property>
		
		QueryBuilder builder = inSearcher.query().or().
			orgroup("viewgroups", groupids).
			match("viewroles", roleid).
			match("owner", userid).
			match("viewusers", userid);
			
		if(lesssecure)
		{
			builder.match("securityenabled", "false");
		}
		SearchQuery securityfilter  = builder.getQuery();
		
		inQuery.addChildQuery(securityfilter);
		
		inQuery.setSecurityAttached(true);
		
		return inQuery;
	
	}
}
