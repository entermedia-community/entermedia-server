package org.openedit.entermedia.autocomplete;

import java.util.List;

import org.openedit.Data;
import org.openedit.data.Searcher;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.users.User;

public interface AutoCompleteSearcher extends Searcher
{

	public void clearIndex();

	public SearchQuery createSearchQuery();

	public void delete(Data inData, User inUser);

	public void deleteAll(User inUser);

	public HitTracker getAllHits(WebPageRequest inReq);

	public String getIndexId();

	public void reIndexAll() throws OpenEditException;

	public void saveAllData(List inAll, User inUser);

	public void saveData(Object inData, User inUser);

	public HitTracker search(SearchQuery inQuery);

	public HitTracker cachedSearch(WebPageRequest inReq, SearchQuery inQuery);

	

	public void updateHits(HitTracker tracker, String word) throws Exception;
}