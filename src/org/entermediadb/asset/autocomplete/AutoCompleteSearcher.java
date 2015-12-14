package org.entermediadb.asset.autocomplete;

import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;

public interface AutoCompleteSearcher extends Searcher
{
	public void updateHits(HitTracker tracker, String word);
}