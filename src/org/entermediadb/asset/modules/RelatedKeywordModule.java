package org.entermediadb.asset.modules;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.search.RelatedKeywordSearcher;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.HitTracker;
import org.openedit.modules.BaseModule;

public class RelatedKeywordModule extends BaseModule
{

	private static final Log log = LogFactory.getLog(RelatedKeywordModule.class);

	protected SearcherManager getSearcherManager()
	{
		return (SearcherManager) getBeanLoader().getBean("searcherManager");
	}

	public RelatedKeywordSearcher getSuggestionsSearcher(WebPageRequest inReq)
	{
		return getSuggestionsSearcher(inReq, true);
	}

	public RelatedKeywordSearcher getSuggestionsSearcher(WebPageRequest inReq, boolean inPutPageValue) throws OpenEditException
	{
		String catalogid = inReq.findPathValue("catalogid");
		String searchType = inReq.findPathValue("searchtype");
		if( searchType == null)
		{
			searchType = "asset";
		}

		String type = searchType  + "RelatedKeyword";
		RelatedKeywordSearcher searcher = (RelatedKeywordSearcher) getSearcherManager().getSearcher(catalogid, type); // e.g.
		// "assetThesaurusSearcher"
		if (searcher == null)
		{
			log.warn("Could not find a searcher for type '" + type + "' in catalog " + catalogid);
		}
		else if (inPutPageValue)
		{
			inReq.putPageValue("searcher", searcher);
		}
		return searcher;
	}
	/**
	 * @deprecated use facets
	 * @param inReq
	 * @return
	 * @throws Exception
	 */
	public Map<String, String> getSuggestions(WebPageRequest inReq) throws Exception
	{
		String searchType = inReq.findPathValue("searchtype");
		if( !"asset".equals(searchType))
		{
			return null;
		}

		
		String catalogid = inReq.findPathValue("catalogid");
		//searcher = (Searcher) inReq.getPageValue("searcher");
		Searcher searcher = getSearcherManager().getSearcher(catalogid, searchType);
		
		HitTracker tracker = searcher.loadHits(inReq);
		if (tracker == null)
		{
			tracker = (HitTracker) inReq.getPageValue("hits");
		}
		//saves the values to the search
		if( tracker != null)
		{
			inReq.putPageValue("query", tracker.getSearchQuery());
			getSuggestionsSearcher(inReq).getSuggestions(tracker, searcher);
		}
		return null;
	}
/*
	public HitTracker getSynonyms(WebPageRequest inReq) throws Exception
	{

		SearchQuery query = getSuggestionsSearcher(inReq).createSearchQuery();
		String searchString = inReq.getRequestParameter("description.value");
		query.addMatches("synonyms", searchString);
		HitTracker wordsHits = getSuggestionsSearcher(inReq).cachedSearch(inReq, query);
		return wordsHits;
	}
*/
}
