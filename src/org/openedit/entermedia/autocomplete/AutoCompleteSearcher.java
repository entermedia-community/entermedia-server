package org.openedit.entermedia.autocomplete;

import org.openedit.data.Searcher;

import com.openedit.hittracker.HitTracker;

public interface AutoCompleteSearcher extends Searcher
{
	public void updateHits(HitTracker tracker, String word);
}