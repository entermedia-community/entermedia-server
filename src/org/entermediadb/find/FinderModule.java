package org.entermediadb.find;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.asset.search.SecurityEnabledSearchSecurity;
import org.entermediadb.find.picker.Picker;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.QueryBuilder;
import org.openedit.data.Searcher;
import org.openedit.hittracker.Highlighter;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.profile.ModuleData;
import org.openedit.profile.UserProfile;
import org.openedit.users.User;
import org.openedit.util.PathUtilities;

public class FinderModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(FinderModule.class);

	
	
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
	
	
	public void searchModuleByQuery(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		HitTracker tracker = null;
		
		String query = inReq.getRequestParameter("search");
		if (query == null) {
			return;
		}
	
		String searchtype = resolveSearchType(inReq);
		if( searchtype == null)
		{
			searchtype = "asset";
		}
		
		Searcher assetsearcher = archive.getSearcher(searchtype);
		SearchQuery search = assetsearcher.addStandardSearchTerms(inReq);
		
		if(search == null) {
			search = assetsearcher.createSearchQuery();
			String	sort = inReq.getRequestParameter(searchtype +"sortby");
			search.addSortBy(sort);
		}
		
		//Search
		String searchby = inReq.getRequestParameter("search");
		if(searchby != null)
		{
			search.addFreeFormQuery("description", searchby);
		}
		
		if( search.getHitsName() == null)
		{
			String hitsname = inReq.getRequestParameter("hitsname");
			if(hitsname == null)
			{
				hitsname = inReq.findValue("hitsname");
			}
			if (hitsname != null )
			{
				search.setHitsName(hitsname);
			}
		}
			
		inReq.setRequestParameter("clearfilters", "true");
		
		tracker = assetsearcher.cachedSearch(inReq, search);

	}
	
	public void organizeHits(WebPageRequest inReq) 
	{
		Collection organizedHits = (Collection)inReq.getPageValue("organizedHits");
		
		if( organizedHits != null)
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
			getResultsManager(inReq).organizeHits(inReq, hits, pageOfHits);
		}
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
		
		long totalhits = 0;
		
		ArrayList<Data> foundmodules = new ArrayList();
		Map<String,Collection> bytypes = null;
		HitTracker<Data> modulestocheck = 	getSearcherManager().getList(archive.getCatalogId(), "module");//listSearchModules(archive);

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
			
			Collection searchmodules = modulestocheck.collectValues("id");
			query.setValue("searchtypes", searchmodules);
			query.addAggregation("entitysourcetype");
			
			query.setName("modulehits");
			query.addOrsGroup("id",uids);  //TODO: Filter out duplicates based on type
			query.setHitsPerPage(1000);
			HitTracker hits = searcher.cachedSearch(inReq, query);
			if( hits != null)
			{
				//organizeHits(inReq, hits, hits.getPageOfHits());
				log.info("Found " + hits.size() + " favorite on " + hits.getHitsName());
			}
			totalhits = totalhits + hits.size(); 
			
			String smaxsize = inReq.findValue("maxcols");
			int targetsize = smaxsize == null? 7:Integer.parseInt(smaxsize);
			bytypes = getResultsManager(inReq).organizeHits(inReq,hits, hits.iterator(),targetsize);

			foundmodules = getResultsManager(inReq).processResults(hits, archive, targetsize, bytypes);
		}
		
		
		//search Assets:assetvotes
		//from MediaSearchModule.java
		Searcher searcher = archive.getSearcherManager().getSearcher(archive.getCatalogId(), "assetvotes");
		SearchQuery query = searcher.createSearchQuery();
		query.setHitsName("favoriteassets");
		
		User user = inReq.getUser();
		
		query.addExact("username", user.getId());
		query.addSortBy("timeDown");
		HitTracker assets = searcher.cachedSearch(inReq, query);
		if( assets.size() > 0)
		{
			
			//Now do a big OR statement
			SearchQuery aquery = archive.getAssetSearcher().createSearchQuery();
			aquery.setSortBy(inReq.findValue("sortby"));
			
			SearchQuery orquery = archive.getAssetSearcher().addStandardSearchTerms(inReq);
			if(orquery == null) {
				orquery = archive.getAssetSearcher().createSearchQuery();
			}
			//orquery.setAndTogether(false);
			assets.setHitsPerPage(999);
			for (Iterator iterator = assets.getPageOfHits().iterator(); iterator.hasNext();)
			{
				Data data = (Data) iterator.next();
				String assetid = data.get("assetid");
				if(assetid != null){
					orquery.addExact("id", data.get("assetid"));
				}
			}
			
			aquery.addChildQuery(orquery); 
			aquery.setHitsName("favoriteassetsmatch");
			
			
			
			HitTracker assethits = archive.getAssetSearcher().cachedSearch(inReq, aquery);
			
			
			if( !assethits.isEmpty())
			{
				Data module = archive.getCachedData("module", "asset");
				foundmodules.add(module);
				if( bytypes == null)
				{
					bytypes = new HashMap();
				}
				bytypes.put("asset",assethits);
				
				totalhits = totalhits + assethits.size();
			}
		}
		
		getResultsManager(inReq).sortModules(foundmodules);
		log.info("Organized Modules: " + foundmodules);
		
		if (foundmodules.size() == 0) {
			log.info("Found no modules.");
		}
		
		inReq.putPageValue("organizedModules",foundmodules);
		inReq.putPageValue("organizedHits", bytypes);
		inReq.putPageValue("organizedHitsSize", totalhits);
		
		 
		

	}
	
	public void removeFavorites(WebPageRequest inReq) 
	{
		MediaArchive archive = getMediaArchive(inReq);
		
		//get the user profile and do a module search
		UserProfile profile = inReq.getUserProfile();
		if( profile == null)
		{
			return;
		}
		
		
		String moduleid = inReq.findValue("moduleid");
		if(moduleid != null) {
			if("asset".equals(moduleid)) 
			{
				Searcher searcher = archive.getSearcherManager().getSearcher(archive.getCatalogId(), "assetvotes");
				SearchQuery query = searcher.createSearchQuery();
				query.setHitsName("favoriteassets");
				
				User user = inReq.getUser();
				
				query.addExact("username", user.getId());
				query.addSortBy("timeDown");
				HitTracker assets = searcher.cachedSearch(inReq, query);
				if( assets.size() > 0)
				{
					List todelete = new ArrayList();
					for (Iterator iterator = assets.iterator(); iterator.hasNext();)
					{
						Data data = (Data) iterator.next();
						String assetid = data.get("assetid");
						todelete.add(data);
					}
					searcher.deleteAll(todelete, null);
				}
			}
			else 
			{
				Data module = archive.getCachedData("module", moduleid);
				if(module != null) {
					profile.setValue("favorites_" + moduleid, "");
				}
				profile.save();
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
		//String query[] = inReq.getRequestParameters("description.value");
		String plainquery = inReq.getRequestParameter("description.value");
		if( plainquery == null)
		{
			return;
		}		
		//String plainquery = String.join(" ", query);
		
		
		QueryBuilder dq = archive.query("modulesearch").addFacet("entitysourcetype").freeform("description",plainquery).hitsPerPage(30);
		dq.getQuery().setIncludeDescription(true);
		
		Collection searchmodules = new ArrayList();
		String mainsearchmodule = inReq.getRequestParameter("mainsearchmodule");
		ResultsManager resultsManager = getResultsManager(inReq);
		if(mainsearchmodule != null) {
			searchmodules.add(mainsearchmodule);
			dq.getQuery().setValue("searchtypes", searchmodules);
		}
		else 
		{
			searchmodules = resultsManager.loadUserSearchTypes(inReq);
			Collection searchmodulescopy = new ArrayList(searchmodules);
			searchmodulescopy.remove("asset");
			dq.getQuery().setValue("searchtypes", searchmodulescopy);
		}
		
		
		SecurityEnabledSearchSecurity security = new SecurityEnabledSearchSecurity();
		security.attachSecurity(inReq, archive.getSearcher("modulesearch"), dq.getQuery());
		
		HitTracker unsorted = dq.search(); //With permissions?
		
		Map<String,String> keywordsLower = new HashMap();
		resultsManager.collectMatches(keywordsLower, plainquery, unsorted);
		
		HitTracker assetunsorted = null;
		if( searchmodules.contains("asset"))
		{
			QueryBuilder assetdq = archive.query("asset").freeform("description",plainquery).hitsPerPage(15);
			assetdq.getQuery().setIncludeDescription(true);
			assetunsorted = assetdq.search(inReq);
			resultsManager.collectMatches(keywordsLower, plainquery, assetunsorted);
			inReq.putPageValue("assethits",assetunsorted);
		
		}		
		List finallist = new ArrayList();
		for (Iterator iterator = keywordsLower.keySet().iterator(); iterator.hasNext();)
		{
			String keyword = (String) iterator.next();
			String keywordcase = keywordsLower.get(keyword);
			finallist.add(keywordcase);
		}
		//inReq.setRequestParameter("clearfilters","true");
		//unsorted.getSearchQuery().setValue("description",query); //Not needed?
		//List finallist = new ArrayList(keywords);
		Collections.sort(finallist);
		inReq.putPageValue("modulehits",unsorted);
		inReq.putPageValue("livesearchfor",plainquery);
		inReq.putPageValue("livesuggestions",finallist);
		inReq.putPageValue("highlighter",new Highlighter());
		
		
		//Include module results
		Collection pageOfHits = unsorted.getPageOfHits();
		pageOfHits = new ArrayList(pageOfHits); 
		resultsManager.organizeHits(inReq, unsorted, pageOfHits);

	}
	
	

	
//	public void aiTakeaways(WebPageRequest inReq) throws Exception
//	{
//		MediaArchive archive = getMediaArchive(inReq);
//		
//		
//		JSONObject arguments = (JSONObject) inReq.getPageValue("arguments");
//		
//		Collection<String> keywords = parseKeywords(arguments.get("keywords"));
//		
//		HitTracker pdfs = archive.query("asset").freeform("description", String.join(" ", keywords)).search();
//		
//		Collection pdfTexts = new ArrayList<String>();
//		
//		for (Iterator iterator = pdfs.iterator(); iterator.hasNext();) {
//			Data pdf = (Data) iterator.next();
//			ContentItem item = getPageManager().getRepository().getStub("/WEB-INF/data/" + archive.getCatalogId() +"/assets/" + pdf.getSourcePath() + "/fulltext.txt");
//			
//			try(InputStream inputStream = item.getInputStream())
//			{				
//				String text = new String(inputStream.readAllBytes());
//				if(text.length() > 0)
//				{
//					pdfTexts.add(text); 					
//				}
//				log.info(text);
//			}
//		}
//
//		String fullText = String.join("\n\n", pdfTexts);
//		
//		if(fullText.replaceAll("\\s|\\n", "").length() == 0)
//		{
//			//TODO: Handle No text found;
//			return;
//		}
//		
//		
//		String model = inReq.findPathValue("model");
//
//		if (model == null)
//		{
//			model = archive.getCatalogSettingValue("gpt-model");
//		}
//		if (model == null)
//		{
//			model = "gpt-4o"; // Default fallback
//		}
//
//		inReq.putPageValue("model", model);
//		inReq.putPageValue("fulltext", fullText);
//		
//		LlmConnection manager = (LlmConnection) archive.getBean("openaiConnection");
//		
//		String chattemplate = "/" + archive.getMediaDbId() + "/gpt/prompts/build_takeaways.html";
//		LlmResponse response = manager.runPageAsInput(inReq, model, chattemplate);
//		
//		String takeaways = response.getMessage();
//		
//		inReq.putPageValue("takeaways", takeaways);
//		
//		
//	}

	
	


	
	
	/*
	public HitTracker searchDefaultModule(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		
		Data firstmenu = (Data)inReq.getPageValue("firstmenu");
		if(firstmenu == null)
		{
			loadTopMenu(inReq);
			firstmenu = (Data)inReq.getPageValue("firstmenu");
		}
		Data module = (Data)inReq.getPageValue("firstmodule");
		if (module != null) 
		{
			inReq.putPageValue("module", module);
			HitTracker hits = null;
			
			String custompath = firstmenu.get("custompath");
			
			if( custompath == null)
			{
				Searcher searcher = archive.getSearcher(module.getId());
				
				if (searcher != null)
				{
					
	//				if(firstmenu.getValue("toplevelentityid") != null) {
	//					String moduleid = firstmenu.get("toplevelentity");
	//					inReq.setRequestParameter("field", moduleid);
	//					inReq.setRequestParameter(moduleid+".value", firstmenu.get("toplevelentityid"));
	//					inReq.setRequestParameter("operation", "exact");
	//				}
					
					hits = searcher.fieldSearch(inReq);
	
					if (hits == null) //this seems unexpected. Should it be a new API such as searchAll?
					{
						String defaultsort = (String) module.getValue("defaultsort");
						if (defaultsort != null) {
							inReq.putPageValue("sortby", defaultsort);
						}
						
						hits = searcher.getAllHits(inReq);
					}
					//log.info("Report ran " +  hits.getSearchType() + ": " + hits.getSearchQuery().toQuery() + " size:" + hits.size() );
					if (hits != null)
					{
						String name = inReq.findValue("hitsname");
						inReq.putPageValue(name, hits);
						inReq.putSessionValue(hits.getSessionId(), hits);
					}
					
				}
				inReq.putPageValue("defaultmodule", module.getId());
				inReq.putPageValue("moduleid", module.getId());
				inReq.putPageValue("searcher", searcher);
			}			
			
			
			return hits;
		}
		
		
		//Legacy defaultmodule logic
		
		UserProfile profile = inReq.getUserProfile();
		String defaultmodule  = ""; 
		if(defaultmodule == null)
		{
			defaultmodule = (String) profile.getValue("defaultmodule");
		}
		if(defaultmodule != null) 
		{
			if(defaultmodule.equals("none"))
			{
				return null;
			}
		}
		if(defaultmodule == null)
		{
			defaultmodule = archive.getCatalogSettingValue("defaultmodule");
		}
		if( defaultmodule == null || defaultmodule.equals(""))
		{
			return null;
		}
		Searcher searcher = archive.getSearcher(defaultmodule);
		HitTracker hits = null;
		if (searcher != null)
		{
			hits = searcher.fieldSearch(inReq);

			if (hits == null) //this seems unexpected. Should it be a new API such as searchAll?
			{
				hits = searcher.getAllHits(inReq);
			}
			//log.info("Report ran " +  hits.getSearchType() + ": " + hits.getSearchQuery().toQuery() + " size:" + hits.size() );
			if (hits != null)
			{
				String name = inReq.findValue("hitsname");
				inReq.putPageValue(name, hits);
				inReq.putSessionValue(hits.getSessionId(), hits);
			}
		}
		inReq.putPageValue("defaultmodule", defaultmodule);
		inReq.putPageValue("searcher", searcher);
		inReq.putPageValue("module", archive.getCachedData("module", defaultmodule));
		inReq.putPageValue("moduleid", defaultmodule);
		return hits;
	}
*/
	
	public void loadTopMenu(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Collection<Data> topmenu = archive.query("appsection").named("topmenuhits").all().sort("ordering").search(inReq);
		List topmenufinal = new ArrayList();
		
		Map vals = new HashedMap();
		vals.put("apphome", (String)inReq.getPageValue("apphome"));
		
		if(topmenu != null && !topmenu.isEmpty())
		{
			UserProfile userprofile = inReq.getUserProfile();
			if (userprofile != null)
			{
				Collection<String> usermodules = inReq.getUserProfile().getModuleIds();
				for (Iterator iterator = topmenu.iterator(); iterator.hasNext();)
				{
					Data data = (Data) iterator.next();
					if(data.getValue("custompath") != null) {

						topmenufinal.add(data);
						
					}
					else 
					{
						if(usermodules.contains(data.getValue("toplevelentity"))) {
							//search submenu
							Collection<Data> topsubmenudata = archive.query("appsubsection").named("topsubmenuhits").exact("parentsection", data.getId()).sort("ordering").search(inReq);
							if (topsubmenudata.size()>0) {
								data.setValue("submenu", topsubmenudata);
								
							}
							topmenufinal.add(data);
						}
					}
				}
			}
			Data first = (Data)topmenu.iterator().next();
			String entityid = (String)first.getValue("toplevelentity");
			Data firstmodule = archive.getCachedData("module", entityid);
			
			inReq.putPageValue("firstmenu", first);
			inReq.putPageValue("firstmodule", firstmodule);
		}
		inReq.putPageValue("topmenu", topmenufinal);
	}
	
	public void loadAppsMenu(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Collection<Data> menu = archive.query("app").named("appmenuhits").all().sort("ordering").search(inReq);
		inReq.putPageValue("appsmenu", menu);
	}
	
	public void loadOrSearchChildren(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String inModule = inReq.findValue("module");
		if(inModule == null) {
			inModule = inReq.findValue("defaultmodule");
		}
		Data topmodule = archive.getCachedData("module", inModule);
		String topentityid = inReq.getRequestParameter("topentityid");
		//String entityid = inReq.getRequestParameter("entityid");

		//String selectedentitytype = inReq.getRequestParameter("entitytype");
		if( topmodule != null)
		{
			//Search for children data. Add to organizedModules
			Collection views = archive.query("view").exact("moduleid",inModule).named("views").exact("rendertype","table").exact("systemdefined","false").exact("showonsearch","true").sort("orderingUp").search(inReq);
			
			
			//String searchingfor = inReq.findActionValue("searchallchildren");
			List organizedModules = new ArrayList();
			Map organizedHits = new HashMap();
			
			List organizedSubModulesIds = new ArrayList();
			Map organizedSubModules = new HashMap();
			Map organizedHitsSubModules = new HashMap();
			
			for (Iterator iterator = views.iterator(); iterator.hasNext();)
			{
				Data view = (Data)iterator.next();
				String searchtype = view.get("rendertable");
				Data amodule = archive.getCachedData("module", searchtype);
				if(amodule != null) {
					organizedModules.add(amodule);
					QueryBuilder builder = archive.query(searchtype).named(searchtype + "hits");
					String renderexternalid = view.get("renderexternalid"); //Not needed?
					if( renderexternalid != null && topentityid != null && renderexternalid.equals(topmodule.getId()))
					{
						builder.exact(renderexternalid, topentityid);
					}
					else
					{
						String field = inReq.getRequestParameter("field");
						String value = inReq.getRequestParameter(field+".value");
						if(field != null && field.equals("description") && value != null) {
							builder.freeform(field, value);
						}
						else 
						{
							builder.all();
						}
					}
					HitTracker hits = (HitTracker)builder.sort("name").search(inReq);
					for (Iterator hitsiterator = hits.iterator(); hitsiterator.hasNext();)
					{
						Data hit = (Data) hitsiterator.next();
						organizedSubModulesIds.add(hit.getId());
					}
					organizedHits.put(amodule.getId(),hits);
				}
			}
			
			for (Iterator iterator = organizedModules.iterator(); iterator.hasNext();) {
				Data amodule = (Data) iterator.next();
			
				//Search Third Level
				Collection views2 = archive.query("view").exact("moduleid",amodule).named("views").exact("rendertype","table").exact("systemdefined","false").exact("showonsearch","true").sort("orderingUp").search(inReq);
				if(views2 != null) {
					//List organizedSubModules = new ArrayList();
					
					String entityid = inReq.getRequestParameter("entityid");
					for (Iterator iterator2 = views2.iterator(); iterator2.hasNext();)
					{
						Data view2 = (Data)iterator2.next();
						String searchtype2 = view2.get("rendertable");
						Data amodule2 = archive.getCachedData("module", searchtype2);
						if(amodule2 != null) {
							organizedSubModules.put(amodule.getId(), amodule2);
							
							QueryBuilder builder2 = archive.query(searchtype2).named(searchtype2 + "hits");
							String renderexternalid2 = view2.get("renderexternalid"); //Not needed?
							Boolean exists = archive.getPropertyDetailsArchive().getPropertyDetailsCached(amodule2.getId()).getDetail(renderexternalid2) != null;
							if( renderexternalid2 != null && entityid != null && exists)
							{
									if (topentityid != null && entityid.equals(topentityid)) {
										builder2.orgroup(renderexternalid2, organizedSubModulesIds);
									}
									else {
										builder2.exact(renderexternalid2, entityid);
									}
									Boolean exists2 = archive.getPropertyDetailsArchive().getPropertyDetailsCached(amodule2.getId()).getDetail(topmodule.getId()) != null;
									if (topentityid != null && exists2) {
										builder2.exact(topmodule.getId(), topentityid);
									}
							}
							else
							{
								//if Parent Selected
								if (topentityid != null) {
									
									builder2.orgroup(renderexternalid2, organizedSubModulesIds);
								}
								else {
									String field = inReq.getRequestParameter("field");
									String value = inReq.getRequestParameter(field+".value");
									if(field != null && field.equals("description") && value != null) {
										builder2.freeform(field, value);
									}
									else 
									{
										builder2.all();
									}
								}
								
								
							}
							HitTracker hits2 = (HitTracker)builder2.sort("name").search(inReq);
							organizedHitsSubModules.put(amodule2.getId(),hits2);
							
						}
					}
				}
			}
			
			
			inReq.putPageValue("organizedModules",organizedModules);
			inReq.putPageValue("organizedHits",organizedHits);
			
			inReq.putPageValue("organizedSubModules",organizedSubModules);
			inReq.putPageValue("organizedHitsSubModules",organizedHitsSubModules);
		}
	}

	public void loadOrSearchByTypes(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Collection searchmodules = getResultsManager(inReq).loadUserSearchTypes(inReq);
		if( searchmodules == null || searchmodules.isEmpty())
		{
			return;
		}
		Searcher modulesearcher = archive.getSearcher("modulesearch");
		SearchQuery search = modulesearcher.addStandardSearchTerms(inReq);

		String excludemodule = inReq.findPathValue("excludemodule");
		searchmodules.remove(excludemodule);
		
		String entityid = inReq.findPathValue("entityid");
		String externalid = inReq.findPathValue("externalid");
		
		if (search == null)
		{
			search = modulesearcher.createSearchQuery();
			if(entityid != null && externalid != null) 
			{
				search.addExact(externalid, entityid);
			}
			else 
			{
				search.addMatches("id", "*");
			}
		}

		search.setValue("searchtypes", searchmodules);
		search.setValue("includeasset", true);
		
		search.addAggregation("entitysourcetype");
		
		search.addSortBy("name");
		
		HitTracker hits = modulesearcher.cachedSearch(inReq, search);  //always skips asset
		//log.info("Report ran " +  hits.getSearchType() + ": " + hits.getSearchQuery().toQuery() + " size:" + hits.size() );
		if (hits != null)
		{
			String name = inReq.findValue("hitsname");
			inReq.putPageValue(name, hits);
			inReq.putSessionValue(hits.getSessionId(), hits);
		}
		inReq.putPageValue("searcher", modulesearcher);
//		if( searchmodules.contains("asset"))
//		{
//			SearchQuery assetsearch = search.copy();
//			assetsearch.setName("assethits");
//			HitTracker assethits = archive.getAssetSearcher().cachedSearch(inReq, search);  //cached
//			log.info("Assets " +  assethits.getSearchType() + ": " + assethits.getSearchQuery().toQuery() + " size:" + assethits.size() );
//		}

	}

	



	public void assignDataPermissionsToCategory(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Data data = (Data)inReq.getPageValue("data");
		if( data != null)
		{
			String moduleid = inReq.findActionValue("searchtype");
			Data module = archive.getCachedData("module", moduleid);
			Category cat = archive.getEntityManager().loadDefaultFolder(module, data, inReq.getUser());
			if( cat != null)
			{
				cat.setValue("viewusers", data.getValues("viewusers"));
				cat.setValue("viewroles", data.getValues("viewroles"));
				cat.setValue("viewgroups", data.getValues("viewgroups"));
				cat.setValue("securityenabled",data.getValue("securityenabled"));
				archive.saveData("category", cat);
			}
		}
	}
	
	
	
	public void assignUserToEntitiesOLD(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Category cat = archive.getCategory(inReq);
		
		if( cat != null)
		{
			if( !cat.containsValue("viewusers", inReq.getUserName()) )
			{
				cat.addValue("viewusers", inReq.getUserName());
				cat.setValue("securityenabled",true);
				//reload profile
				//search again for results
				archive.saveData("category", cat);
				inReq.getUserProfile().addToViewCategories(cat);
			}
			//get all entities and add user
			if( inReq.getUserProfile() == null)
			{
				return;
			}
			Collection entities = inReq.getUserProfile().getEntitiesInParent(cat);
			for (Iterator iterator = entities.iterator(); iterator.hasNext();)
			{
				ModuleData entity = (ModuleData) iterator.next();
				if( !entity.getData().containsValue("viewusers", inReq.getUserName()) )
				{
					entity.getData().addValue("viewusers", inReq.getUserName());
					entity.getData().setValue("securityenabled",true);
					archive.saveData(entity.getModuleId(), entity.getData());
				}					
			}
		}
	}
	
	
	
	
	public void enablePublishingGallery(WebPageRequest inReq) 
	{
		String entityid =  inReq.getRequestParameter("entityid");
		String moduleid = inReq.getRequestParameter("moduleid");
		MediaArchive archive = getMediaArchive(inReq);
		//Data module = archive.getCachedData("module", moduleid);
		
		Searcher searcher = archive.getSearcher(moduleid);
		if(searcher != null) {
		
			Data entity = (Data) searcher.searchByField("id", entityid);
			if(entity != null)
			{
				entity.setProperty("enablepublishinggallery", "true");
				searcher.saveData(entity);
			}
		}
	}
	
	public void enablePublishingCarousel(WebPageRequest inReq) 
	{
		String entityid =  inReq.getRequestParameter("entityid");
		String moduleid = inReq.getRequestParameter("moduleid");
		MediaArchive archive = getMediaArchive(inReq);
		//Data module = archive.getCachedData("module", moduleid);
		
		Searcher searcher = archive.getSearcher(moduleid);
		if(searcher != null) {
		
			Data entity = (Data) searcher.searchByField("id", entityid);
			if(entity != null)
			{
				entity.setProperty("enablepublishingcarousel", "true");
				searcher.saveData(entity);
			}
		}
	}
	
	
	public void getPublishing(WebPageRequest inReq) 
	{
		MediaArchive archive = getMediaArchive(inReq);

		MultiValued entity = null;
		String distributiontype = inReq.findValue("distributiontype");
		if(distributiontype == null || distributiontype == "") {
			distributiontype = "carousel"; //defaults to carousel
		}
		String publishingid =  inReq.getRequestParameter("publishingid");
		if(publishingid == null)
		{
			
			String entityid =  inReq.getRequestParameter("entityid");
			if( entityid != null)
			{
				Searcher searcher = archive.getSearcher("distributiongallery");
				//Data publishing = (Data) searcher.searchByField("entityid", entityid); //What is this?
				QueryBuilder query = searcher.query().exact("entityid", entityid);
				if (distributiontype != null) {
					query.exact("distributiontype", distributiontype);
				}
				Data publishing = query.searchOne();
				if(publishing != null)
				{
					publishingid = publishing.getId();
				}
			}
		}
		if(publishingid == null)
		{
			publishingid =  inReq.getRequestParameter("id"); //saved record
		}
		if(publishingid != null)
		{
			Data publishing = (Data) archive.getData("distributiongallery",publishingid);
			if(publishing != null)
			{
				inReq.putPageValue("publishing", publishing);
				
				if(Boolean.parseBoolean(publishing.get("enabled")))
				{
					entity = (MultiValued) archive.getCachedData(publishing.get("moduleid"),publishing.get("entityid"));
					if(entity != null &&  !entity.getBoolean("enablepublishinggallery"))
					{
						entity.setValue("enablepublishinggallery",true);
						archive.saveData(publishing.get("moduleid"),entity);
					}
				}
				else
				{
					if(!Boolean.parseBoolean(publishing.get("enabled")))
					{
						entity = (MultiValued) archive.getCachedData(publishing.get("moduleid"),publishing.get("entityid"));
						if( entity.getBoolean("enablepublishinggallery"))
						{
							entity.setValue("enablepublishinggallery",false);
							archive.saveData(publishing.get("moduleid"),entity);
						}
					}

				}
				inReq.putPageValue("entity",entity);
			}
		}
	}
	
	public void loadPublishAssets(WebPageRequest inReq)  
	{
		MediaArchive archive = getMediaArchive(inReq);
		
		String entityid = inReq.getRequestParameter("entityid");
		String publishingid =  inReq.getRequestParameter("publishingid");
		String moduleid = inReq.findValue("module");
		String playertype =  inReq.findValue("playertype");
		Data publishing = null;
		if (publishingid != null)
		{
			publishing = (Data) archive.getCachedData("distributiongallery", publishingid);
		}
		if(publishing == null && entityid != null)
		{
			publishing = archive.getSearcher("distributiongallery").query().exact("entityid", entityid).exact("distributiontype", playertype).searchOne();
		}
		if(publishing == null)
		{
			String publishingurl =  inReq.getRequestParameter("url");
			
			if( publishingurl == null)
			{
				publishingurl = inReq.getPath();
			}
			publishingid = PathUtilities.extractPageName(publishingurl);
			publishing = (Data) archive.getCachedData("distributiongallery", publishingid);
					
			//Create if does not exist
			if(publishing == null && entityid != null)
			{
				
				publishing = archive.getSearcher("distributiongallery").createNewData();
				publishing.setValue("entityid", entityid);
				publishing.setValue("moduleid", moduleid);
				publishing.setValue("distributiontype", inReq.getRequestParameter("playertype"));
				publishing.setValue("enabled", "true");
				archive.getSearcher("distributiongallery").saveData(publishing);
			}
		}
		
		if(publishing == null) {	
			log.info("Publishing id not found " +publishingid);
			return;
		}
		
		if (moduleid == null)
		{
			moduleid = publishing.get("moduleid");
		}
		if (moduleid != null)
		{
			Data module = archive.getCachedData("module", moduleid);
			if (module != null)
			{
				inReq.putPageValue("module", module);
				inReq.putPageValue("moduleid", moduleid);
			}
			
		}

		HitTracker tracker = getPublishingAssets(inReq, publishing);
	
	}
	
	
	public HitTracker getPublishingAssets(WebPageRequest inReq, Data inPublishing) 
	{
		inReq.putPageValue("publishing", inPublishing);
		inReq.putPageValue("distributiontype", inPublishing.get("distributiontype"));
		
		MediaArchive archive = getMediaArchive(inReq);
		
		String publishingSortby = inPublishing.get("sortby");
		if(publishingSortby == null) {
			publishingSortby = "ordering";
		}
		
		Data entity = null;
		entity = (Data) inReq.getPageValue("entity");
		if (entity == null) {
			entity = archive.getCachedData(inPublishing.get("moduleid"), inPublishing.get("entityid"));
			inReq.putPageValue("entity",entity);
		}
		
		
		
		String categoryid = inPublishing.get("categoryid");
		
		//Old way to load lightboxes
		if(categoryid == null) 
		{
			String lightboxid = inPublishing.get("lightboxid");
			Data module = archive.getCachedData("module", inPublishing.get("moduleid"));
			if (lightboxid == null)
			{
				lightboxid = "files";
			}
			if (lightboxid != null)
			{
				Data lightbox = archive.getCachedData("emedialightbox", lightboxid);
				Category cat = archive.getEntityManager().loadLightboxCategory(module, entity, "emedialightbox",lightbox, inReq.getUser());
				categoryid = cat.getId();
			}
		}
		
		if( categoryid == null)
		{
			throw new OpenEditException("Set categoryid on gallery");
		}
		String hitsname = "publishingentityassethits";
		HitTracker tracker = archive.query("asset").exact("category",categoryid).sort(publishingSortby).named(hitsname).search();
		tracker.enableBulkOperations();
		//Pagination
		int totalPages = tracker.getTotalPages();
		String page = inReq.getRequestParameter("pagenum");  //why?

		if (page != null)
		{
			int jumpToPage = Integer.parseInt(page);
			if (jumpToPage <= totalPages && jumpToPage > 0)
			{
				tracker.setPage(jumpToPage);
			}
			else
			{
				tracker.setPage(1);
			}
			
		}
		inReq.putPageValue(hitsname, tracker);
		return tracker;
		
		
	}
	

	public void startPicker(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		
		String targettype = inReq.getRequestParameter("pickingtargettype");
		String moduleid = inReq.getRequestParameter("pickingmoduleid");
		
		Picker picker = (Picker)inReq.getPageValue("picker");
		if( targettype != null || moduleid != null)
		{
			String targetfieldid = inReq.getRequestParameter("targetfieldid");

			if( picker == null)
			{
				picker = new Picker();
			}
			picker.setTargetFieldId(targetfieldid);
			picker.setTargetType(targettype);
			picker.setTargetModuleId(moduleid);
			inReq.putSessionValue("picker",picker);
			inReq.putPageValue("picker",picker);
		}
		
	}
}
