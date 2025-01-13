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

public class OrderSearchSecurity extends BaseSearchSecurity
{
	private static final Log log = LogFactory.getLog(OrderSearchSecurity.class);

	public SearchQuery attachSecurity(WebPageRequest inPageRequest, Searcher inSearcher, SearchQuery inQuery)
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
		

		QueryBuilder builder = inSearcher.query().or().orgroup("viewgroups", groupids)
					.exact("viewroles", roleid)
					.exact("viewusers", userid)
					.exact("userid", userid);
		builder.exact("securityenabled", "false");
		SearchQuery securityfilter = builder.getQuery();

		inQuery.addChildQuery(securityfilter);

		inQuery.setSecurityAttached(true);
		return inQuery;
	}

}
