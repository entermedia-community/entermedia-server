package org.entermediadb.asset.modules;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.modules.BaseModule;

public class LDAPModule extends BaseModule
{
	protected Log log = LogFactory.getLog(getClass());
	
	public HitTracker searchUserAlias(WebPageRequest inReq) throws Exception
	{
		Searcher searcher = getSearcherManager().getSearcher(inReq.findValue("applicationid"), "ldap");
		String querystring = inReq.getRequestParameter("value");
		if(querystring == null)
		{
			return null;
		}
		SearchQuery query = searcher.createSearchQuery();
		query.addMatches("uid", querystring);
		
		HitTracker results = searcher.search(query);
		inReq.putPageValue("results", results);
		
		return results;
	}

	public HitTracker searchEmails(WebPageRequest inReq) throws Exception
	{
		Searcher searcher = getSearcherManager().getSearcher(inReq.findValue("applicationid"), "ldap");
		
		String querystring = inReq.getRequestParameter("q");
		//get what comes after the last semicolon
		if(querystring == null)
		{
			return null;
		}
		
		int semicolon = querystring.lastIndexOf(";");
		if (semicolon > -1)
		{
			String existingmail = querystring.substring(0, semicolon);
			inReq.putPageValue("existingmail", existingmail + "; ");
			querystring = querystring.substring(semicolon + 1);
		}
		else
		{
			inReq.putPageValue("existingmail", "");
		}
		
		SearchQuery query = searcher.createSearchQuery();
		query.addMatches("mail", querystring);
		
		HitTracker results = searcher.search(query);
		inReq.putPageValue("results", results);
		
		return results;
	}
}
