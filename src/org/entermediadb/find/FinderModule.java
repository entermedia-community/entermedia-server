package org.entermediadb.find;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.autocomplete.LiveSuggestion;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.elasticsearch.SearchHitData;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.PropertyDetail;
import org.openedit.data.QueryBuilder;
import org.openedit.data.Searcher;
import org.openedit.hittracker.FilterNode;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.ListHitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.hittracker.Term;
import org.openedit.profile.UserProfile;

public class FinderModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(FinderModule.class);

	private static final int MEDIASAMPLE=22;
	
	public void searchByQuery(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String query = inReq.getRequestParameter("description.value");
		if (query == null) {
			return;
		}
		QueryBuilder dq = archive.query("modulesearch").freeform("description",query);
		HitTracker unsorted = dq.search(inReq);
		//log.info(unsorted.size());
		
		inReq.setRequestParameter("clearfilters","true");
		unsorted.getSearchQuery().setValue("description",query); //Not needed?

	}
	
	public void organizeHits(WebPageRequest inReq) 
	{
		Collection foundmodules = (Collection)inReq.getPageValue("organizedModules");
		if( foundmodules != null)
		{
			log.info("Modules aready loaded" + inReq.getPage().getPath());
			return;
		}
		String HitsName = inReq.findValue("hitsname");
		HitTracker hits = (HitTracker)inReq.getPageValue(HitsName);
		if( hits != null)
		{
			Collection pageOfHits = hits.getPageOfHits();
			pageOfHits = new ArrayList(pageOfHits); 
			organizeHits(inReq, hits, pageOfHits);
		}
	}
	

	public void organizeHits(WebPageRequest inReq,HitTracker hits, Collection pageOfHits) 
	{
		//if( inReq.getPageValue("organizedHits") == null )
		{
			// String HitsName = inReq.findValue("hitsname");
			//  HitTracker hits = (HitTracker)inReq.getPageValue(HitsName);
			// Collection pageOfHits = hits.getPageOfHits();
			if( hits != null)
			{
				MediaArchive archive = getMediaArchive(inReq);

//				String key = "modulesearch" + archive.getCatalogId() + "userFilters";
//				UserFilters filtervalues = (UserFilters)inReq.getSessionValue(key);
//				if( filtervalues != null)
//				{
//
//				}
				FilterNode node = hits.findFilterValue("entitysourcetype");

				
				// log.info(hits.getHitsPerPage());
				//Find counts
				String smaxsize = inReq.findValue("maxcols");
				int targetsize = smaxsize == null? 7:Integer.parseInt(smaxsize);
				
				Map<String,Collection> bytypes = organizeHits(inReq, pageOfHits.iterator(),targetsize);
				
				ArrayList foundmodules = new ArrayList();
				//See if we have enough from one page. If not then run searches to get some results
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
							max = Math.min(total,MEDIASAMPLE);
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
									//TODO: Compine results, avoid dups
									bytypes.put(sourcetype,sthits);
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

				//Put asset into session
				HitTracker assets = (HitTracker)bytypes.get("asset");
				if( assets != null)
				{
					assets.setHitsName("hits");
					assets.setSearcher(archive.getAssetSearcher());
					assets.setDataSource("asset");
					assets.setSessionId("hitsasset" + archive.getCatalogId() );
					log.info(assets.getSessionId());
					inReq.putSessionValue(assets.getSessionId(), assets);
				}
				

				sortModules(foundmodules);
				log.info("organized Modules: " + foundmodules);
				
				if (foundmodules.size() == 0) {
					log.info("####---####--###--### ISSUE HERE ####---####--###--###");
				}
				
				inReq.putPageValue("organizedModules",foundmodules);
				
			}
		}
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
			    	
			        if ( a1 > b1 ) {
			        	return 1;
			        }
			        return -1;
			    } 
			    
			});
		}
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
			term.setDetail( searcher.getDetail(old.getId()) );
		}
		HitTracker more = searcher.search(q);
		return more.getPageOfHits();
	}


	public Map organizeHits(WebPageRequest inReq, Iterator hits, int maxsize) 
	{
		Map bytypes = new HashMap();
		MediaArchive archive = getMediaArchive(inReq);
		
		for (Iterator iterator = hits; iterator.hasNext();)
		{
			SearchHitData data = (SearchHitData) iterator.next();
			String type = data.getSearchHit().getType();
			
			Collection values = (Collection) bytypes.get(type);
			if( values == null)
			{
				values = new ListHitTracker();
				bytypes.put(type,values);
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

	public void showFavorites(WebPageRequest inReq) 
	{
		MediaArchive archive = getMediaArchive(inReq);
		
		//get the user profile and do a module search
		UserProfile profile = inReq.getUserProfile();
		if( profile == null)
		{
			return;
		}
		
		ArrayList<Data> foundmodules = new ArrayList();
		
		Collection<Data> modulestocheck = listSearchModules(archive);

		Collection uids = new ArrayList();
		for (Iterator iterator = modulestocheck.iterator(); iterator.hasNext();)
		{
			Data module = (Data) iterator.next();
			String searchtype = module.getId();
			Collection ids = profile.getValues("favorites_" + searchtype);
			if( ids != null)
			{
				for (Iterator iterator2 = ids.iterator(); iterator2.hasNext();)
				{
					String id = (String) iterator2.next();
					uids.add(id);
				}
			}
		}
		if( !uids.isEmpty())
		{
			Searcher searcher = archive.getSearcher("modulesearch");
			
			SearchQuery query = searcher.addStandardSearchTerms(inReq);
			if( query == null)
			{
				query = searcher.createSearchQuery();
			}
			query.setName("modulehits");
			query.addOrsGroup("id",uids);  //TODO: Filter out duplicates based on type
			query.setHitsPerPage(1000);
			HitTracker hits = searcher.cachedSearch(inReq, query);
			if( hits != null)
			{
				//organizeHits(inReq, hits, hits.getPageOfHits());
				log.info("Found " + hits.size() + " on " + hits.getHitsName());
			}
		}

	}
	protected Collection<Data> listSearchModules(MediaArchive archive)
	{
		Collection<Data> modules = getSearcherManager().getList(archive.getCatalogId(), "module");
		Collection searchmodules = new ArrayList();
		for (Iterator iterator = modules.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			String show = data.get("showonsearch");
			if( !"modulesearch".equals(data.getId() ) && Boolean.parseBoolean(show)) //Permission check?
			{
				searchmodules.add(data);
			}
		}
		return searchmodules;
	}

	public void searchForLiveSuggestions(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String query = inReq.getRequestParameter("description.value");
		
		if( query == null)
		{
			return;
		}
		String lowerquery = query.toLowerCase();
		QueryBuilder dq = archive.query("modulesearch").freeform("description",query).hitsPerPage(50);
		HitTracker unsorted = dq.search();

		Map<String,String> keywordsLower = new HashMap();
		//Set keywords = new HashSet();
		//Loop over the results and get a list of similar keywords
		for (Iterator iterator = unsorted.iterator(); iterator.hasNext();)
		{
			Data hit = (Data) iterator.next();
			
			//Look in keywords and name
			String name = hit.getName();
			//Split with . or spaces
			addMatch(keywordsLower,query,lowerquery, name);
			Collection words = hit.getValues("keywords");
			if( words != null && !words.isEmpty())
			{
				for (Iterator iterator2 = words.iterator(); iterator2.hasNext();)
				{
					String word = (String) iterator2.next();
					addMatch(keywordsLower,query, lowerquery, word);
				}
			}
		}
		List finallist = new ArrayList();
		for (Iterator iterator = keywordsLower.keySet().iterator(); iterator.hasNext();)
		{
			String keyword = (String) iterator.next();
			String keywordcase = keywordsLower.get(keyword);
			LiveSuggestion suggestion = new LiveSuggestion();
			suggestion.setSearchFor(query);
			suggestion.setKeyword(keywordcase);
			finallist.add(suggestion);
		}
		//inReq.setRequestParameter("clearfilters","true");
		//unsorted.getSearchQuery().setValue("description",query); //Not needed?
		//List finallist = new ArrayList(keywords);
		Collections.sort(finallist);
		inReq.putPageValue("livesuggestions",finallist);

	}

	protected void addMatch(Map<String,String> keywords, String query, String lowerquery, String name)
	{
		if( name == null)
		{
			return;
		}
		if( !name.toLowerCase().startsWith(lowerquery) ) 
		{
			return;
		}
		String existing = keywords.get(name.toLowerCase());
		if( existing == null)
		{
			keywords.put(name.toLowerCase(),name);
		}
		else if( !existing.startsWith(query))
		{
			keywords.put(name.toLowerCase(),name);
		}
		
	}

	
	
}
