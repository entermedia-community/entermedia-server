package org.entermediadb.asset.search;

import java.io.File;
import java.util.Collection;

import org.json.simple.JSONObject;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.PropertyDetailsArchive;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.users.User;

public interface DataConnector extends CatalogEnabled
{
	public Data getDataBySourcePath(String inSourcePath);
	public Data getDataBySourcePath(String inSourcePath, boolean inAutocreate);
	
	Data createNewData();

	SearchQuery createSearchQuery();

	HitTracker search(SearchQuery inQuery);

	Object searchByField(String inField, String inValue);
	
	void delete(Data inData, User inUser);

	void deleteFromIndex(String inId);

	void deleteFromIndex(HitTracker inOld);

	void deleteAll(User inUser);

	void saveData(Data inData, User inUser);

	void saveAllData(Collection<Data> inAll, User inUser);

	void updateIndex(Data one);

	public void updateIndex(Collection<Data> inBuffer, User inUser);

	void reIndexAll();
	
	void reindexInternal();

	String getIndexId();

	void clearIndex();

	void flush();

	void setCatalogId(String inCatalogId);

	void setSearchType(String inCatalogId);

	void setPropertyDetailsArchive(PropertyDetailsArchive inArchive);

	void setSearcherManager(SearcherManager inManager);

	void setRootDirectory(File inRoot);

	boolean hasChanged(HitTracker inTracker);

	String nextId();
	//public void updateFilters(WebPageRequest inReq);
	public Data loadData(Data inHit);
	public HitTracker loadHits(WebPageRequest inReq);
	public HitTracker checkCurrent(WebPageRequest inReq, HitTracker inTracker);
	public boolean initialize();
	public void saveJson(Collection inJsonArray);
	public void saveJson(String inId, JSONObject inObject);


}
