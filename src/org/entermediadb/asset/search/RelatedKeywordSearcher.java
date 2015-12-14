package org.entermediadb.asset.search;

import java.util.Map;

import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;

public interface RelatedKeywordSearcher extends Searcher
{
	public void indexWord(String inWord, HitTracker inResults, Searcher inTypeSearcher) throws Exception;

	public Map<String, String> getSuggestions(HitTracker inTracker, Searcher inTypeSearcher) throws Exception;
}