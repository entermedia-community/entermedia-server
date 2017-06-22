package org.entermediadb.asset.search;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.users.UserProfileManager;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.BaseSearcher;
import org.openedit.data.Searcher;
import org.openedit.hittracker.CompositeHitTracker;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.profile.UserProfile;

public abstract class CompositeSearcher extends BaseSearcher
{

	private static final Log log = LogFactory.getLog(CompositeSearcher.class);
	protected UserProfileManager fieldProfileManager;
	
	public UserProfileManager getUserProfileManager()
	{
		return fieldProfileManager;
	}

	public void setUserProfileManager(UserProfileManager inProfileManager)
	{
		fieldProfileManager = inProfileManager;
	}

	public List getSearchers()
	{
		Searcher catalogSearcher = getSearcherManager().getSearcher(getCatalogId(), "catalogs");
		HitTracker catalogList = catalogSearcher.getAllHits();

		List searchers = new ArrayList();
		for (Iterator iterator = catalogList.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			String id = data.getId();
			String type = data.get("searchtype");
			Searcher searcher = getSearcherManager().getSearcher(id, type);
			if (searcher != null)
			{
				searchers.add(searcher);
			}
		}
		return searchers;
	}

	private List getSearchersByCatalogIds(String[] inCatalogids)
	{
		List catids = new ArrayList();
		if (inCatalogids != null)
		{
			for (int i = 0; i < inCatalogids.length; i++)
			{
				catids.add(inCatalogids[i]);
			}
		}

		List searchers = getSearchers();
		List targetSearchers = new ArrayList();
		for (Iterator iterator = searchers.iterator(); iterator.hasNext();)
		{
			Searcher searcher = (Searcher) iterator.next();
			if (catids.contains(searcher.getCatalogId()))
			{
				targetSearchers.add(searcher);
			}
		}
		return targetSearchers;
	}

	
	public HitTracker search(SearchQuery inQuery){
		List catalogs = inQuery.getCatalogs();
		
		CompositeHitTracker allHits = new CompositeHitTracker();
		allHits.setSearchQuery(inQuery);
		List catalogids = inQuery.getCatalogs();
		String[] ids = (String[]) catalogids.toArray(new String[]{});
		
		List searchers = getSearchers(null,ids);
				
		for (Iterator iterator = searchers.iterator(); iterator.hasNext();)
		{
			Searcher searcher = (Searcher) iterator.next();
			String catalogid = searcher.getCatalogId();
			
			SearchQuery subQuery = inQuery.copy();
			subQuery.setCatalogId(catalogid);
			HitTracker hits = searcher.search(subQuery);
			if (hits != null)
			{
				allHits.addSubTracker(catalogid, hits);
			}
		}
		return allHits;
		
	}
	
	public HitTracker cachedSearch(WebPageRequest inReq, SearchQuery inQuery)
	{
		String applicationid = inReq.findValue("applicationid");
		String[] catalogids = inReq.getRequestParameters("catalogid");
		if( inQuery.getHitsName() == null)
		{
			String hitsname = inReq.getRequestParameter("hitsname");
			if(hitsname == null)
			{
				hitsname = inReq.findValue("hitsname");
			}
			if (hitsname == null )
			{
				hitsname = "hits";
			}
			inQuery.setHitsName(hitsname);
		}

		CompositeHitTracker allHits = new CompositeHitTracker();

		UserProfile pref = (UserProfile)inReq.getUserProfile();
		List searchers = getSearchers(pref, catalogids);
//		PageStreamer pages = inReq.getPageStreamer();
		
		for (Iterator iterator = searchers.iterator(); iterator.hasNext();)
		{
			Searcher searcher = (Searcher) iterator.next();
			String catid =  searcher.getCatalogId();
			inQuery.addCatalog(catid);
		}
		if( inReq.getUser() != null && inQuery.getCatalogs().size() == 0)
		{
			log.info(inReq.getUserName() + " has no permissions to view any catalogs");
		}
		//TODO: Move to standard term area?
		String sort = inReq.findValue("sortby");
		if (sort != null)
		{
			inQuery.setSortBy(sort);
		}

		
		for (Iterator iterator = searchers.iterator(); iterator.hasNext();)
		{
			Searcher searcher = (Searcher) iterator.next();
			String catalogid = searcher.getCatalogId();
			
			SearchQuery subQuery = inQuery.copy();
			subQuery.setCatalogId(catalogid);
			getUserProfileManager().loadUserProfile(inReq, catalogid, inReq.getUserName());
			HitTracker hits = searcher.cachedSearch(inReq, subQuery);
			if (hits != null)
			{
				allHits.addSubTracker(catalogid, hits);
			}
		}

		allHits.setSearchQuery(inQuery);
		inReq.putSessionValue(allHits.getSessionId(), allHits);
		inReq.putPageValue(allHits.getHitsName(), allHits);
		return allHits;
	}

	private List getSearchers(UserProfile pref, String[] catalogids)
	{
		if (catalogids != null && catalogids.length > 0)
		{
			return getSearchersByCatalogIds(catalogids);
		}
//		else if ( pref != null && pref.getCatalogs() != null)
//		{
//			catalogids = new String[pref.getCatalogs().size()];
//			for (int i = 0; i < pref.getCatalogs().size(); i++)
//			{
//				Data catalog = (Data)pref.getCatalogs().get(i);
//				catalogids[i] = catalog.getId();
//			}
//			return getSearchersByCatalogIds(catalogids);
//		}
		else
		{
			return getSearchers(); //Is this an OK default? Seems insecure
		}
		
	}

}
