package org.entermediadb.asset.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.autocomplete.AutoCompleteSearcher;
import org.entermediadb.asset.util.JsonUtil;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.ListHitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.users.Group;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;

public class AutoCompleteModule extends DataEditModule
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
	public void updateHits(WebPageRequest inReq)
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
		String searchString = inReq.getRequestParameter("term");
		query.addStartsWith("description", searchString);
		
		HitTracker hits = userSearcher.cachedSearch(inReq, query);
		if (Boolean.parseBoolean(inReq.findValue("cancelactions")))
		{
			inReq.setCancelActions(true);
		}
		inReq.putPageValue("suggestions", hits);
		return hits;
	}
	
	public HitTracker myGroupUsersSuggestions(WebPageRequest inReq)
	{
		User currentUser = inReq.getUser();
		Collection groups = currentUser.getGroups();
		HashSet<String> ids = new HashSet<String>();
		for (Iterator iterator = groups.iterator(); iterator.hasNext();) {
			Group group = (Group) iterator.next();
			Collection users = getUserManager(inReq).getUsersInGroup(group);
			for (Iterator iterator2 = users.iterator(); iterator2.hasNext();) {
				User user = (User) iterator2.next();
				ids.add(user.getId());
			}
		}
		
		//make sure we exclude users that are already in there
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		Collection<String> userNames = archive.getAssetSecurityArchive().getAccessList(archive, asset);
		ids.removeAll(userNames);
		
		HitTracker hits = null;
		if(ids.size() > 0)
		{
			StringBuffer groupuserids = new StringBuffer();
			for (Iterator iterator = ids.iterator(); iterator.hasNext();) {
				String id = (String) iterator.next();
				if(iterator.hasNext())
				{
					groupuserids.append(id + " ");
				}
				else
				{
					groupuserids.append(id);
				}
			}
				
			Searcher userSearcher = getSearcherManager().getSearcher("system", "user");
			SearchQuery innerquery = userSearcher.createSearchQuery();
			innerquery.setAndTogether(false);
			String searchString = inReq.getRequestParameter("term");
			innerquery.addStartsWith("id", searchString);
			innerquery.addStartsWith("email", searchString);
			innerquery.addStartsWith("lastName", searchString);
			innerquery.addStartsWith("firstName", searchString);
			
			SearchQuery query = userSearcher.createSearchQuery();
			query.setAndTogether(true);
			query.addChildQuery(innerquery);
			query.addOrsGroup("id", groupuserids.toString());
			query.addSortBy("lastName");

			hits = userSearcher.cachedSearch(inReq, query);
			if (Boolean.parseBoolean(inReq.findValue("cancelactions")))
			{
				inReq.setCancelActions(true);
			}
		}
		
		inReq.putPageValue("suggestions", hits);
		return hits;
	}
	/**
	 * @deprecated groupSuggestions is nicer to use
	 * @param inReq
	 * @return
	 */
	public HitTracker myGroupSuggestions(WebPageRequest inReq)
	{
		User currentUser = inReq.getUser();
		Collection<Group> groups = currentUser.getGroups();
		Collection<String> groupidscol = new ArrayList<String>();
		StringBuffer groupids = new StringBuffer();
		for (Iterator iterator = groups.iterator(); iterator.hasNext();) 
		{
			Group group = (Group) iterator.next();
			groupidscol.add(group.getId());
		}
		
		//make sure we exclude groups that are already in there
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		Collection<String> userNames = archive.getAssetSecurityArchive().getAccessList(archive, asset);
		groupidscol.removeAll(userNames);
		
		HitTracker hits = null;
		if(groupidscol.size() > 0)
		{
			//put them in something safe for a query
			for (Iterator iterator = groupidscol.iterator(); iterator.hasNext();) {
				String group = (String) iterator.next();
				if(iterator.hasNext())
				{
					groupids.append(group + " ");
				}
				else
				{
					groupids.append(group);
				}
			}
			Searcher groupSearcher = getSearcherManager().getSearcher("system", "group");
			
			
			SearchQuery innerquery = groupSearcher.createSearchQuery();
			String searchString = inReq.getRequestParameter("term");
			innerquery.addStartsWith("description", searchString);
			innerquery.setAndTogether(false);
			
			SearchQuery query = groupSearcher.createSearchQuery();
			query.addOrsGroup("id", groupids.toString());
			query.setAndTogether(true);
			
			query.addChildQuery(innerquery);
			
			
			hits = groupSearcher.cachedSearch(inReq, query);
			if (Boolean.parseBoolean(inReq.findValue("cancelactions")))
			{
				inReq.setCancelActions(true);
			}
		}
		inReq.putPageValue("suggestions", hits);
		return hits;
	}
	public HitTracker groupSuggestions(WebPageRequest inReq)
	{
		//make sure we exclude groups that are already in there
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		
		HitTracker hits = null;

		Searcher groupSearcher = getSearcherManager().getSearcher("system", "group");
		
		SearchQuery query = groupSearcher.createSearchQuery();
		String searchString = inReq.getRequestParameter("term");
		query.addStartsWith("description", searchString);
		query.addSortBy("namesorted");
		
		Collection<String> alreadyhave = archive.getAssetSecurityArchive().getAccessList(archive, asset);
		for (Iterator iterator = alreadyhave.iterator(); iterator.hasNext();)
		{
			String existinggroup = (String) iterator.next();
			query.addNot("id", existinggroup);
		}
		query.addNot("enabled","false");
		
		hits = groupSearcher.cachedSearch(inReq, query);
		if (Boolean.parseBoolean(inReq.findValue("cancelactions")))
		{
			inReq.setCancelActions(true);
		}
		inReq.putPageValue("suggestions", hits);

//		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
//		{
//			Data hit = (Data) iterator.next();
//			log.info(hit.getName());
//		}

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
		JsonUtil util = (JsonUtil) searcher.getSearcherManager().getModuleManager().getBean("jsonUtil");
		inReq.putPageValue("jsonUtil", util);
		
		SearchQuery query = searcher.createSearchQuery();
		String field = inReq.getRequestParameter("field");

		String searchString = inReq.getRequestParameter(field + ".value");
		if (searchString == null)
		{
			searchString = inReq.getRequestParameter("q");
		}
		if (searchString == null)
		{
			searchString = inReq.getRequestParameter("term");
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
	
	
	public void autocomplete(WebPageRequest inReq) throws Exception
	{
		Searcher searcher = loadSearcher(inReq);
		String field = inReq.getRequestParameter("field");
		if (searcher != null)
		{
			SearchQuery query = searcher.createSearchQuery();
			String term = inReq.getRequestParameter("term");
			query.addStartsWith(field, term);
			HitTracker hits = searcher.cachedSearch(inReq, query);
			

			if (hits != null)
			{
				String name = inReq.findValue("hitsname");
				inReq.putPageValue(name, hits);
				inReq.putSessionValue(hits.getSessionId(), hits);
			}
		}
		inReq.putPageValue("searcher", searcher);
	}
	
	public void expireSuggestions(WebPageRequest inReq) throws Exception
	{
		//Pass in search type
		String searchtype = inReq.findValue("searchtype");
		AutoCompleteSearcher searcher = (AutoCompleteSearcher) getAutoCompleteSearcher(inReq);
		//Look for any that now have 0 and remove them
		HitTracker all = searcher.getAllHits();
		all.enableBulkOperations();
		
		String catalogid = inReq.findValue("catalogid");
		Searcher finder = getSearcherManager().getSearcher(catalogid, searchtype);
		List todelete = new ArrayList();
		List tosave = new ArrayList();
		for (Iterator iterator = all.iterator(); iterator.hasNext();)
		{
			Data term = (Data) iterator.next();
			String searchby = term.getId();
			String hitcount = term.get("hitcount");
			int size = finder.query().freeform("description", searchby).hitsPerPage(1).search().size();
			if( size == 0)
			{
				todelete.add(term);
			}
			else if( size != Integer.parseInt(hitcount))
			{
				term.setValue("hitcount", size);
				term.setValue("timestamp", DateStorageUtil.getStorageUtil().formatForStorage(new Date()) );

				tosave.add(term);
			}
		}
		searcher.deleteAll(todelete, null);
		searcher.saveAllData(tosave, null);
		ScriptLogger logger = (ScriptLogger)inReq.getPageValue("log");
		if( logger != null)
		{
			logger.info("Deleted " + todelete.size() );
			logger.info("Updated " + tosave.size() );
		}
	}
	
}
