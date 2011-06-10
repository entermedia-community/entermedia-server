package org.openedit.data.lucene;

import java.util.List;

import org.openedit.Data;
import org.openedit.data.CompositeSearcher;

import com.openedit.OpenEditException;
import com.openedit.hittracker.SearchQuery;
import com.openedit.users.User;

public class CompositeLuceneSearcher extends CompositeSearcher
{
	
	//private static final Log log = LogFactory.getLog(CompositeLuceneSearcher.class);

	public SearchQuery createSearchQuery()
	{
		LuceneSearchQuery query = new LuceneSearchQuery();
		query.setPropertyDetails(getPropertyDetails());
		query.setCatalogId(getCatalogId());
		query.setSearcherManager(getSearcherManager());
		return query;
	}

	public void clearIndex()
	{
		
	}

	public void delete(Data inData, User inUser)
	{
		
	}

	public void deleteAll(User inUser)
	{
		
	}

	public String getIndexId()
	{
		return null;
	}

	public void reIndexAll() throws OpenEditException
	{
		
	}

	public void saveAllData(List inAll, User inUser)
	{
		
	}

	public void saveData(Object inData, User inUser)
	{
		
	}

	
}
