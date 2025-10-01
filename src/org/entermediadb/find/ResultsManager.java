package org.entermediadb.find;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.assistant.AiSearch;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.elasticsearch.SearchHitData;
import org.entermediadb.manager.BaseManager;
import org.json.simple.JSONArray;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.WebPageRequest;
import org.openedit.data.PropertyDetail;
import org.openedit.data.QueryBuilder;
import org.openedit.data.Searcher;
import org.openedit.hittracker.FilterNode;
import org.openedit.hittracker.Highlighter;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.ListHitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.hittracker.Term;
import org.openedit.profile.UserProfile;
import org.openedit.users.User;

public class ResultsManager extends BaseManager {
	private static final Log log = LogFactory.getLog(ResultsManager.class);
	
	private static final int MEDIASAMPLE=7;

	public void organizeHits(WebPageRequest inReq, HitTracker hits, Collection pageOfHits) 
	{
		//if( inReq.getPageValue("organizedHits") == null )
		{
			// String HitsName = inReq.findValue("hitsname");
			//  HitTracker hits = (HitTracker)inReq.getPageValue(HitsName);
			// Collection pageOfHits = hits.getPageOfHits();
			if( hits != null)
			{
				MediaArchive archive = getMediaArchive();

//				String key = "modulesearch" + archive.getCatalogId() + "userFilters";
//				UserFilters filtervalues = (UserFilters)inReq.getSessionValue(key);
//				if( filtervalues != null)
//				{
//
//				}

				
				// log.info(hits.getHitsPerPage());
				//Find counts
				
				String smaxsize = inReq.findValue("maxhitsperpage");
				
				Collection types = (Collection)hits.getSearchQuery().getValues("searchtypes");
				if(types != null && types.size() > 1)
				{
					String maxhitsperpagemultiple = inReq.findValue("maxhitsperpagemultiple");
					if( maxhitsperpagemultiple != null)
					{
						smaxsize = maxhitsperpagemultiple;
					}
				}
				int targetsize = 10;
				
				if( smaxsize != null)
				{
					targetsize = Integer.parseInt(smaxsize);
				}
				Map<String,Collection> bytypes = organizeHits(inReq, hits, pageOfHits.iterator(),targetsize);
				
				ArrayList foundmodules = processResults(hits, archive, targetsize, bytypes);

				//Put asset into session
				//HitTracker assets = (HitTracker)bytypes.get("asset");
				//
				String moduleid = inReq.findValue("module");
				if(moduleid == null) {
					moduleid = (String) inReq.getPageValue("moduleid");
				}
				
				
				
//				if( moduleid == null || !moduleid.equals("asset"))
//				{
//					if( types == null || types.contains("asset"))
//					{
//						SearchQuery copy = hits.getSearchQuery().copy();
//						copy.setFacets(null);
//						copy.setProperty("ignoresearchttype", "true");
//						//Fix the terms so it has the right details
//	//					for (Iterator iterator = copy.getTerms().iterator(); iterator.hasNext();)
//	//					{
//	//						Term term = (Term) iterator.next();
//	//						term.copy();
//	//						
//	//					}
//						copy.addSortBy("assetaddeddateDown");
//						copy.setHitsName("entityhits"); //? assethits makes more sense
//						HitTracker assethits = archive.getAssetSearcher().cachedSearch(inReq,copy);
//	//					assets.setSearcher(archive.getAssetSearcher());
//	//					assets.setDataSource("asset");
//						assethits.setSessionId("hitsasset" + archive.getCatalogId() );
//	//					assets.setSearchQuery(hits.getSearchQuery());
//	//					assets.setIndexId(archive.getAssetSearcher().getIndexId());
//						//log.info(assets.getSessionId());
//						UserProfile profile = inReq.getUserProfile();
//						//Integer mediasample  = profile.getHitsPerPageForSearchType(hits.getSearchType());
//						assethits.setHitsPerPage( targetsize );
//						
//						inReq.putPageValue(assethits.getHitsName(), assethits);
//						inReq.putSessionValue(assethits.getSessionId(), assethits);
//						if( !assethits.isEmpty())
//						{
//							Data assetmodule = archive.getCachedData("module", "asset");
//							foundmodules.add(assetmodule);
//						}
//						bytypes.put("asset",assethits);
//					}
//				}
				
				sortModules(foundmodules);
				
				if( log.isDebugEnabled())
				{
					log.debug("Organized Modules: " + foundmodules);
				}
				
				if (foundmodules.size() == 0) {
					if( log.isDebugEnabled())
					{
						log.debug("Found no modules.");
					}
				}
				
				inReq.putPageValue("organizedModules",foundmodules);
				//inReq.putPageValue("organizedModules",foundmodules);
				
			}
		}
	}
	
	

	protected ArrayList processResults(HitTracker hits, MediaArchive archive, int targetsize, Map<String, Collection> bytypes)
	{
		ArrayList foundmodules = new ArrayList();
		boolean foundasset = false;
		//See if we have enough from one page. If not then run searches to get some results
		FilterNode node = hits.findFilterValue("entitysourcetype");
		if( node != null)
		{
			for (Iterator iterator = node.getChildren().iterator(); iterator.hasNext();)
			{
				FilterNode filter = (FilterNode) iterator.next();
				String sourcetype = filter.getId();
				int total  = filter.getCount();
				Collection sthits = bytypes.get(sourcetype);
				int max = targetsize;
				if( sourcetype.equals("asset"))
				{
					//max = Math.min(total,MEDIASAMPLE);
					foundasset = true;
					Data module = archive.getCachedData("module", sourcetype);
					foundmodules.add(module);
					continue; //Skip asset. Done below
				}
				int maxpossible = Math.min(total,max);

				if( sthits == null || sthits.size() < maxpossible)
				{
					if( !hits.getSearchQuery().isEmpty())
					{
						//Only makes sense when someone searched for text. Otherwise we get all values from *
						String input = hits.getSearchQuery().getMainInput();
						if( input != null)
						{
							Collection moredata = loadMoreResults(archive,hits.getSearchQuery(),sourcetype, maxpossible);
							if(moredata != null)
							{
								bytypes.put(sourcetype,moredata);
							}
						}
					}
					else
					{
						//Collection moredata = loadMoreResults(archive,hits.getSearchQuery(),sourcetype, maxpossible);
						Searcher searcher = archive.getSearcher(sourcetype);
						if( sourcetype.equals("category"))
						{
							sthits = searcher.query().hitsPerPage(maxpossible).exact("parentid","index").sort("name").search();
						}
						else
						{
							sthits = searcher.query().hitsPerPage(maxpossible).all().sort("name").search();
						}
						//TODO: Compine results, avoid dups
						bytypes.put(sourcetype,sthits);
						
					}
				}
				if( sthits != null && !sthits.isEmpty())
				{
					Data module = archive.getCachedData("module", sourcetype);
					foundmodules.add(module);
				}
			}
		}
		return foundmodules;
	}

	protected void sortModules(ArrayList foundmodules)
	{
		if (!foundmodules.isEmpty()) {
			Collections.sort(foundmodules,  new Comparator<Data>() 
			{ 
			    // Used for sorting in ascending order of 
			    // roll number 
			    public int compare(Data a, Data b) 
			    { 
			    	int a1 = Integer.parseInt(a.get("ordering"));
			    	int b1 = Integer.parseInt(b.get("ordering"));
			    	if( a1 == b1)
			    	{
			    		return 0;
			    	}
			        if ( a1 > b1 ) {
			        	return 1;
			        }
			        return -1;
			    } 
			    
			});
		}
		//log.info("Complete sort" + foundmodules);
	}
	private Collection loadMoreResults(MediaArchive archive, SearchQuery inSearchQuery, String inSourcetype, int maxsize)
	{
		//search for more
		Searcher searcher = archive.getSearcher(inSourcetype);
		SearchQuery q = inSearchQuery.copy();
		q.setHitsPerPage(maxsize);
		for (Iterator iterator = q.getTerms().iterator(); iterator.hasNext();)
		{
			Term term = (Term) iterator.next();
			PropertyDetail old = term.getDetail();
			PropertyDetail  newd = searcher.getDetail(old.getId());
			if( newd == null)
			{
				log.info("Term does not exist " + inSourcetype + " " + q);
				return null;
			}
			term.setDetail( newd);
		}
		HitTracker more = searcher.search(q);
		return more.getPageOfHits();
	}


	public Map organizeHits(WebPageRequest inReq, HitTracker allhits,Iterator hits, int maxsize) 
	{
		Map bytypes = new HashMap();
		MediaArchive archive = getMediaArchive();
		
		for (Iterator iterator = hits; iterator.hasNext();)
		{
			SearchHitData data = (SearchHitData) iterator.next();
			String type = data.getSearchHit().getType();
			
			Collection values = (Collection) bytypes.get(type);
			if( values == null)
			{
				Searcher searcher = archive.getSearcher(type);
				ListHitTracker newvalues = new ListHitTracker();
				newvalues.setActiveFilterValues(  allhits.getActiveFilterValues() );
				newvalues.setHitsPerPage(maxsize);
				newvalues.setSearcher(searcher);
				SearchQuery query = allhits.getSearchQuery().copy();
				query.setResultType(type);
				newvalues.setSearchQuery(query);
//				String v = newvalues.getInput("description");
				//System.out.print(v);
				values = newvalues;
				bytypes.put(type,values);
				//newvalues.setHitsName("idhits");
				newvalues.setHitsName(type +"idhits");
				newvalues.setSessionId(type + "idhits"+ archive.getCatalogId());
				inReq.putSessionValue(newvalues.getSessionId(), newvalues);
			}
			int max = maxsize;
			if( type.equals("asset"))
			{
				max = MEDIASAMPLE;
			}

			if(values.size()<max)
			{
				values.add(data);
			}
			
		}
//		log.info("put un page: " + bytypes);
//		log.info("size: " + bytypes.size());
		inReq.putPageValue("organizedHits",bytypes);
		return bytypes;
	}
	
	public void searchByKeywords(WebPageRequest inReq, AiSearch searchArgs)
	{
		
		log.info("Searching as:" + inReq.getUser().getName());
		MediaArchive archive = getMediaArchive();

		Collection<String> keywords = searchArgs.getKeywords();
		
		String plainquery = "";
		if(!searchArgs.isStrictSearch())
		{
			plainquery = String.join(" ", keywords);
		}
		else
		{
			plainquery = String.join(" OR ", keywords); // This does not work
		}
		
		QueryBuilder dq = archive.query("modulesearch").addFacet("entitysourcetype").freeform("description", plainquery).hitsPerPage(30);
		dq.getQuery().setIncludeDescription(true);
		
		Collection searchmodules = loadUserSearchTypes(inReq, searchArgs.getSelectedModuleIds());
		
		Collection searchmodulescopy = new ArrayList(searchmodules);
		searchmodulescopy.remove("asset");
		dq.getQuery().setValue("searchtypes", searchmodulescopy);
		
		
		HitTracker unsorted = dq.search(inReq);
		
		log.info(unsorted);

		Map<String,String> keywordsLower = new HashMap();
		
		collectMatches(keywordsLower, plainquery, unsorted);
		
		inReq.putPageValue("modulehits", unsorted);
		inReq.putPageValue("livesearchfor", plainquery);
		
		List finallist = new ArrayList();
		
		for (Iterator iterator = keywordsLower.keySet().iterator(); iterator.hasNext();)
		{
			String keyword = (String) iterator.next();
			String keywordcase = keywordsLower.get(keyword);
			finallist.add(keywordcase);
		}

		Collections.sort(finallist);
		
		
		inReq.putPageValue("livesuggestions", finallist);
		inReq.putPageValue("highlighter", new Highlighter());
		
		int assetmax = 15;
		if( unsorted.size() > 10)
		{
			assetmax = 5;
		}
		
		QueryBuilder assetdq = archive.query("asset")
				.freeform("description", plainquery)
				.hitsPerPage(assetmax);
				
		HitTracker assetunsorted = assetdq.search(inReq);
		collectMatches(keywordsLower, plainquery, assetunsorted);
		inReq.putPageValue("assethits", assetunsorted);
		
		Collection pageOfHits = unsorted.getPageOfHits();
		pageOfHits = new ArrayList(pageOfHits);
		
		String[] excludeentityids = new String[unsorted.size()];
		String[] excludeassetids = new String[assetunsorted.size()];
		int idx = 0;
		for (Object entity : unsorted.getPageOfHits()) {
			Data d = (Data) entity;
			excludeentityids[idx] = d.getId();
			idx++;
		}
		idx = 0;
		for (Object asset : assetunsorted.getPageOfHits()) {
			Data d = (Data) asset;
			excludeassetids[idx] = d.getId();
			idx++;
		}
		inReq.putPageValue("excludeentityids", excludeentityids);
		inReq.putPageValue("excludeassetids", excludeassetids);
		
		inReq.putPageValue("totalhits", unsorted.size() + assetunsorted.size());
		
		organizeHits(inReq, unsorted, pageOfHits);
		
	}
	
	public Collection loadUserSearchTypes(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive();

		Collection<Data> modules = archive.query("module").exact("showonsearch",true).search(inReq);
		Collection searchmodules = new ArrayList();
		
		/*
		String inModule = inReq.findValue("module");
		if( inModule == null)
		{
			String defaultmodule = (String)inReq.getUserProfile().getValue("defaultmodule");
			if( defaultmodule != null && !defaultmodule.isEmpty())
			{
				inModule = defaultmodule;
			}	
		}
		//pick last selected 
		
		
		Data selected = archive.getCachedData("module", inModule);
		if( selected == null)
		{
		*/
			for (Iterator iterator = modules.iterator(); iterator.hasNext();)
			{
				MultiValued amodule = (MultiValued) iterator.next();
				searchmodules.add(amodule.getId());
			}
//		}
//		else
//		{
			/*
			Collection children = selected.getValues("childentities");
			//String searchingfor = inReq.findActionValue("searchallchildren");
			for (Iterator iterator = modules.iterator(); iterator.hasNext();)
			{
				MultiValued amodule = (MultiValued) iterator.next();
				
				if( amodule.getId().equals(inModule))
				{
					continue; //Never search what we are in
				}
				if( children != null)
				{
					if( children.contains(amodule.getId() ) )
					{
						searchmodules.add(amodule.getId());
					}
				}
				else if(inModule == null)
				{
					searchmodules.add(amodule.getId());
				}
			}*/
		//}
		//searchmodules.remove("asset"); 
		return searchmodules;
	}
	protected Collection loadUserSearchTypes(WebPageRequest inReq, Collection<String> moduleIds)
	{
		MediaArchive archive = getMediaArchive();

		Collection<Data> modules = archive.query("module").exact("showonsearch",true).search(inReq);
		Collection searchmodules = new ArrayList();
	
		for (Iterator iterator = modules.iterator(); iterator.hasNext();)
		{
			MultiValued amodule = (MultiValued) iterator.next();
			String id = amodule.getId();
			if(moduleIds.isEmpty()) 
			{
				searchmodules.add(id);
			}
			else if(moduleIds.contains(id))
			{
				searchmodules.add(id);
			}
		}
		
		return searchmodules;
	}
	
	public void collectMatches(Map<String, String> keywordsLower, String query, HitTracker unsorted)
	{
		String lowerquery = query.toLowerCase();

		for (Iterator iterator = unsorted.getPageOfHits().iterator(); iterator.hasNext();)
		{
			Data hit = (Data) iterator.next();
			
			//Look in keywords and name
			String name = hit.getName();
			//Split with . or spaces
			addMatch(keywordsLower,query,lowerquery, name);
			String description = hit.get("description");
			if( description != null )
			{
				String[] keywords = MultiValued.VALUEDELMITER.split(description);//description.split("[\\s+\\,\\(\\)]");
				for (int i = 0; i < keywords.length; i++)
				{
					addMatch(keywordsLower,query, lowerquery, keywords[i]);
				}
			}
		}
	}
	
	protected void addMatch(Map<String,String> foundkeywords, String query, String lowerquery, String keyword)
	{
		if( keyword == null || keyword.isEmpty())
		{
			return;
		}
		
		String cleanedup = keyword.trim();

		if( cleanedup.length() > 50)
		{
			cleanedup = cleanedup.substring(0,50);
		}
		
		//cleanedup = cleanedup.replaceAll("[^a-zA-Z\\-\\._\\d^]", "").toLowerCase();
		
		//TODO: Remove any weird trailing things like enter or ascii
		
		if( cleanedup.endsWith("."))
		{
			cleanedup = cleanedup.substring(0,cleanedup.length()-1);
		}
		
		//if( !cleanedup.toLowerCase().startsWith(lowerquery) ) 
		if( !cleanedup.toLowerCase().contains(lowerquery) )
		{
			return;
		}
		
		//Add to the list or replace one
		String existing = foundkeywords.get(cleanedup.toLowerCase());
		if( existing == null ||  !existing.startsWith(query))
		{
			foundkeywords.put(cleanedup.toLowerCase(),cleanedup);
		}
		
	}
	public Collection<String> parseKeywords(Object keywords_object) throws Exception
	{
		Collection<String> keywords = new ArrayList();
		
		JSONArray keywords_json = new JSONArray();
		
				
		try {
			if(keywords_object instanceof JSONArray)
			{
				keywords_json = (JSONArray) keywords_object;
			}
			else if(keywords_object instanceof String)
			{
				keywords_json.add((String) keywords_object);
			}
			
			if(keywords_json == null || keywords_json.size() == 0)
			{
				log.error("No keywords provided");
				return keywords;
			}
			
			for (Iterator iterator = keywords_json.iterator(); iterator.hasNext();) {
				String k = (String) iterator.next();
				keywords.add(k);
			}
			
		} catch (Exception e) {
			throw e;
		}
		
		return keywords;
	}
	
	public void mdTestSearch(WebPageRequest inReq) throws Exception
	{
		Collection modules = new ArrayList();
		modules.add("Activity");
		modules.add("entityactivity");
		modules.add("asset");
		
		
		Collection keywords = new ArrayList();
		keywords.add("ac");
		
		User u = inReq.getUser();
		
//		searchByKeywords(inReq, modules, keywords, "exclusive");
	}

	public String joinWithAnd(Collection<String> items) {
		Iterator<String> iter = items.iterator();
		if (items == null || items.size() == 0) {
			return "";
		} else if (items.size() == 1) {
			return iter.next();
		} else if (items.size() == 2) {
			return iter.next() + " and " + iter.next();
		}

		StringBuilder result = new StringBuilder();
		for (int i = 0; i < items.size() - 1; i++) {
			result.append(iter.next());
		}

		result.append("and ").append(iter.next());
		return result.toString();
	}
}
