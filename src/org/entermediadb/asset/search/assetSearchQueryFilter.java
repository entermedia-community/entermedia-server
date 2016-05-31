package org.entermediadb.asset.search;

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.WebPageRequest;
import org.openedit.data.SearchQueryFilter;
import org.openedit.data.Searcher;
import org.openedit.hittracker.SearchQuery;
import org.openedit.profile.UserProfile;
import org.openedit.users.Group;
import org.openedit.users.User;

public class assetSearchQueryFilter implements SearchQueryFilter {
	private static final Log log = LogFactory.getLog(assetSearchQueryFilter.class);

	public SearchQuery  attachFilter(WebPageRequest inPageRequest, Searcher inSearcher, SearchQuery inQuery) 
	{
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
		if(!inQuery.isSecurityAttached())
		{
			//TODO: This should be in a child query with 	child.setFilter(true);
			
			//viewasset = "admin adminstrators guest designers"
			//goal: current query && (viewasset.contains(username) || viewasset.contains(group0) || ... || viewasset.contains(groupN))
			User currentUser = inPageRequest.getUser();
			StringBuffer buffer = new StringBuffer("true "); //true is for wide open searches

			UserProfile profile = inPageRequest.getUserProfile();
			if( profile != null)
			{
				//Get the libraries
				Collection libraries = profile.getCombinedLibraries();
				if( libraries != null)
				{
					for (Iterator iterator = libraries.iterator(); iterator	.hasNext();)
					{
						String library = (String) iterator.next();
						buffer.append( " library_" + library);
					}
				}
			}

			if (currentUser != null)
			{
				for (Iterator iterator = currentUser.getGroups().iterator(); iterator.hasNext();)
				{
					String allow = ((Group)iterator.next()).getId();
					buffer.append(" group_" + allow);
				}
				buffer.append(" user_" + currentUser.getUserName());
			}
			inQuery.addOrsGroup("viewasset", buffer.toString().toLowerCase());
			inQuery.setSecurityAttached(true);
		}

		return inQuery;
	}
}
