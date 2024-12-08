package org.entermediadb.asset.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.WebPageRequest;
import org.openedit.data.QueryBuilder;
import org.openedit.data.SearchSecurity;
import org.openedit.data.Searcher;
import org.openedit.hittracker.SearchQuery;
import org.openedit.profile.UserProfile;
import org.openedit.users.Group;

public class SecurityEnabledSearchSecurity extends BaseSearchSecurity implements SearchSecurity
{
	private static final Log log = LogFactory.getLog(SecurityEnabledSearchSecurity.class);

	public SearchQuery attachSecurity(WebPageRequest inPageRequest, Searcher inSearcher, SearchQuery inQuery)
	{
		attachStandardSecurity(inPageRequest, inSearcher, inQuery);

		return inQuery;

	}
}
