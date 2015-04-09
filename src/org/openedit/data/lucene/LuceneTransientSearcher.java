package org.openedit.data.lucene;

import java.util.Collection;

import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.index.IndexWriter;
import org.openedit.Data;
import org.openedit.data.BaseData;
import org.openedit.data.XmlFileSearcher;

import com.openedit.OpenEditException;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.users.User;

public class LuceneTransientSearcher extends XmlFileSearcher
{
	public Data createNewData()
	{
		return new BaseData();
	}
	
	public void saveAllData(Collection inAll, User inUser)
	{
		for (Object object : inAll)
		{
			Data data = (Data) object;
			if (data.getId() == null)
			{
				data.setId(nextId());
			}
		}
		updateIndex(inAll);
	}
	
	@Override
	protected void reIndexAll(IndexWriter inWriter, final TaxonomyWriter inTaxonomyWriter)
	{

	}
	
	public void deleteAll(User inUser)
	{
		reIndexAll();
	}
	
	public void saveData(Data data, User inUser)
	{
		if(data.getId() == null)
		{
			data.setId(nextId());
		}
		if( data.getSourcePath() == null)
		{
			String sourcepath = getSourcePathCreator().createSourcePath(data, data.getId() );
			data.setSourcePath(sourcepath);
		}
		updateIndex(data);
	}
	
	public void delete(Data inData, User inUser)
	{
	
		// Remove from Index
		deleteRecord(inData);
	}
	
	

	public Object searchByField(String inId, String inValue)
	{
		SearchQuery query = createSearchQuery();
		query.addExact(inId, inValue);
		HitTracker hits = search(query);
		hits.setHitsPerPage(1);
		Data first = (Data)hits.first();
		if( first == null)
		{
			return null;
		}
		Data baseData = createNewData();
		baseData.setProperties(first.getProperties());
		baseData.setId(first.getId());
		return baseData;
	}

	
}
