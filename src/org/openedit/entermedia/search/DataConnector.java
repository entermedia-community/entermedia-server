package org.openedit.entermedia.search;

import java.io.File;
import java.util.Collection;

import org.openedit.Data;
import org.openedit.data.PropertyDetailsArchive;
import org.openedit.data.SearcherManager;

import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.users.User;

public interface DataConnector 
{

	Data createNewData();

	SearchQuery createSearchQuery();

	HitTracker search(SearchQuery inQuery);

	void delete(Data inData, User inUser);

	void deleteFromIndex(String inId);

	void deleteFromIndex(HitTracker inOld);

	void deleteAll(User inUser);

	void saveData(Data inData, User inUser);

	void saveAllData(Collection<Data> inAll, User inUser);

	void updateIndex(Data one);

	void updateIndex(Collection<Data> all, boolean b);

	void reIndexAll();

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

}
