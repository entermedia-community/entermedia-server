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
	
	/*
	public void organizeHits(WebPageRequest inReq, HitTracker inModuleHits, Collection pageOfHits, HitTracker inAssets) 
	{
		//if( inReq.getPageValue("organizedHits") == null )
		{
			// String HitsName = inReq.findValue("hitsname");
			//  HitTracker hits = (HitTracker)inReq.getPageValue(HitsName);
			// Collection pageOfHits = hits.getPageOfHits();
			if( inModuleHits != null)
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
				
				Collection types = (Collection)inModuleHits.getSearchQuery().getValues("searchtypes");
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
				Map<String,Collection> bytypes = organizeHits(inReq, inModuleHits, pageOfHits.iterator(),targetsize, inAssets);
				
				ArrayList foundmodules = processResults(inModuleHits, archive, targetsize, bytypes);

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
	
	*/
/*
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
*/
	
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

	public OrganizedResults createOrganizedResults(HitTracker inEntities, HitTracker inAssets, int inPreferedSize)
	{
		OrganizedResults organizedResults = new OrganizedResults();
		organizedResults.setMediaArchive(getMediaArchive());
		organizedResults.setEntityResults(inEntities);
		organizedResults.setAssetResults(inAssets);
		organizedResults.setSizeOfResults(inPreferedSize);
		return organizedResults;
	}
	
	
	
	
	public OrganizedResults loadOrganizedResults(WebPageRequest inReq, HitTracker inUnsorted, HitTracker inAssetunsorted)
	{
		OrganizedResults organizedresults = loadOrganizedResults(inReq,inUnsorted,inAssetunsorted,0);
		return organizedresults; 
	}	
	public OrganizedResults loadOrganizedResults(WebPageRequest inReq, HitTracker inUnsortedEntities, HitTracker inAssetunsorted, int inSize)
	{
		OrganizedResults organizedresults = (OrganizedResults)inReq.getSessionValue("lastOrganizedResults");
		if( organizedresults != null)
		{
			boolean clearresults = organizedresults.hasChanged(inUnsortedEntities,inAssetunsorted);
			if(!clearresults)
			{
				inReq.putPageValue("organizedResults",organizedresults);
				return organizedresults;
			}
		}
		
		organizedresults =	createOrganizedResults(inUnsortedEntities, inAssetunsorted, inSize);  //Check old values?
		inReq.putSessionValue("lastOrganizedResults",organizedresults);
		inReq.putPageValue("organizedResults",organizedresults);
		return organizedresults;
	}

	public Collection<String> loadUserSearchTypes(WebPageRequest inReq)
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
	public Collection<String> loadUserSearchTypes(WebPageRequest inReq, Collection<String> moduleIds)
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
	public Collection<String> parseKeywords(Object keywords_object)
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
