package org.openedit.entermedia.modules;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.entermedia.autocomplete.AutoCompleteSearcher;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.ListHitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.modules.BaseModule;
import com.openedit.users.User;

public class AutoCompleteModule extends BaseMediaModule
{

	private static final Log log = LogFactory.getLog(AutoCompleteModule.class);

	public AutoCompleteSearcher getAutoCompleteSearcher(WebPageRequest inReq, boolean inSave)
	{
		String catalogid = null;
		String type = inReq.findValue("searchtype");
		if (type == null || type.equals("compositeLucene") )
		{
			type = "compositeLucene";
			catalogid = inReq.findValue("applicationid");
		}
		else
		{
			catalogid = inReq.findValue("catalogid");
		}

		type = type + "AutoComplete";
		AutoCompleteSearcher searcher = (AutoCompleteSearcher) getSearcherManager().getSearcher(catalogid, type); // e.g.
		// "assetThesaurusSearcher"
		if (inSave)
		{
			inReq.putPageValue("searcher", searcher);
		}
		return searcher;

	}

	public AutoCompleteSearcher getAutoCompleteSearcher(WebPageRequest inReq)
	{
		return getAutoCompleteSearcher(inReq, true);
	}

	/**
	 * Look up the hits that we got and update our auto complete searcher
	 * 
	 * @param inReq
	 * @throws Exception
	 */
	public void updateHits(WebPageRequest inReq) throws Exception
	{
		String word = inReq.getRequestParameter("description.value");
		if (word == null)
		{
			return;
		}

		Searcher searcher = getAutoCompleteSearcher(inReq);
		if (searcher == null)
		{
			searcher = (Searcher) inReq.getPageValue("searcher");
			if (searcher == null)
			{
				return;
			}
		}
		HitTracker tracker = searcher.loadHits(inReq);
		if (tracker == null)
		{
			tracker = (HitTracker) inReq.getPageValue("hits");
		}
		if (tracker == null)
		{
			return;
		}
		getAutoCompleteSearcher(inReq, false).updateHits(tracker, word);
	}

	public HitTracker userSearchSuggestions(WebPageRequest inReq)
	{
		Searcher userSearcher = getSearcherManager().getSearcher("system", "user");
		SearchQuery query = userSearcher.createSearchQuery();
		query.setAndTogether(false);
		String searchString = inReq.getRequestParameter("q");
		query.addStartsWith("id", searchString);
		query.addStartsWith("email", searchString);
		query.addStartsWith("lastname", searchString);
		query.addStartsWith("firstname", searchString);
		
		HitTracker hits = userSearcher.cachedSearch(inReq, query);
		if (Boolean.parseBoolean(inReq.findValue("cancelactions")))
		{
			inReq.setCancelActions(true);
		}
		inReq.putPageValue("suggestions", hits);
		return hits;
	}

	public HitTracker searchUserEmails(WebPageRequest inReq) throws Exception
	{
		Searcher userSearcher = getSearcherManager().getSearcher("system", "user");
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

		SearchQuery query = userSearcher.createSearchQuery();
		query.addStartsWith("description", querystring);

		HitTracker hits = userSearcher.cachedSearch(inReq, query);
		inReq.putPageValue("suggestions", hits);
		
		return hits;
	}
	public HitTracker searchFriendEmails(WebPageRequest inReq) throws Exception
	{
		Searcher userSearcher = getSearcherManager().getSearcher("system", "user");
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

		SearchQuery query = userSearcher.createSearchQuery();
		query.addStartsWith("description", querystring);
		query.addMatches("friend.friendid", inReq.getUserName());
		HitTracker hits = userSearcher.cachedSearch(inReq, query);
		ListHitTracker users = new ListHitTracker();
		if( hits.size() > 0)
		{
			//only show friends
			for (Iterator iterator = hits.iterator(); iterator.hasNext();)
			{
				Data user = (Data) iterator.next();
				String email = user.get("email");
				if( email != null)
				{
					users.add(user);
				}
			}
		}
		inReq.putPageValue("suggestions", users);
		
		return users;
	}
	
	// this searches for suggestions that are already in the index. used for
	// autocomplete
	public HitTracker searchSuggestions(WebPageRequest inReq) throws Exception
	{
		AutoCompleteSearcher searcher = (AutoCompleteSearcher) getAutoCompleteSearcher(inReq);

		SearchQuery query = searcher.createSearchQuery();
		String field = inReq.getRequestParameter("field");

		String searchString = inReq.getRequestParameter(field + ".value");
		if (searchString == null)
		{
			searchString = inReq.getRequestParameter("q");
		}
		query.addStartsWith("synonymsenc", searchString);
		query.setSortBy("hitsDown");

		//	log.info("searching in : " + searcher.getCatalogId() +"/" + searcher.getSearchType() + "/" + searchString);

		HitTracker wordsHits = searcher.cachedSearch(inReq, query);
		if (Boolean.parseBoolean(inReq.findValue("cancelactions")))
		{
			inReq.setCancelActions(true);
		}
		inReq.putPageValue("searchstring", searchString);
		return wordsHits;
	}
}
