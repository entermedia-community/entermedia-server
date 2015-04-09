package org.openedit.data.lucene;

import java.io.IOException;

import org.apache.lucene.facet.taxonomy.SearcherTaxonomyManager;
import org.apache.lucene.facet.taxonomy.SearcherTaxonomyManager.SearcherAndTaxonomy;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;

import com.openedit.OpenEditException;

public class LuceneConnectionManager
{
	protected transient SearcherTaxonomyManager fieldSearcherTaxonomyManager;
	protected transient SearcherManager fieldSearcherManager;

	public LuceneConnectionManager(IndexWriter inWriter, boolean inB, SearcherFactory inSearcherFactory, DirectoryTaxonomyWriter inTaxonomyWriter)
	{
		try
		{
			if( inTaxonomyWriter != null)
			{
				fieldSearcherTaxonomyManager = new SearcherTaxonomyManager(inWriter,inB, inSearcherFactory, inTaxonomyWriter);
			}
			else
			{
				fieldSearcherManager = new SearcherManager(inWriter,inB, inSearcherFactory);
			}
		}
		catch( IOException ex)
		{
			throw new OpenEditException(ex);
		}
	}
	public void maybeRefresh()
	{	
		try
		{
			if( fieldSearcherManager != null)
			{
				fieldSearcherManager.maybeRefresh();
			}
			else
			{
				fieldSearcherTaxonomyManager.maybeRefresh();
			}
		}
		catch( IOException ex)
		{
			throw new OpenEditException(ex);
		}
	}

	public LuceneConnection acquire()
	{
		try
		{
			LuceneConnection connection = new LuceneConnection();
			if( fieldSearcherManager != null)
			{
				IndexSearcher searcher = fieldSearcherManager.acquire();
				connection.setIndexSearcher(searcher);
			}
			else
			{
				SearcherAndTaxonomy st = fieldSearcherTaxonomyManager.acquire();
				connection.setSearcherAndTaxonomy(st);
			}
			return connection;
		}
		catch( IOException ex)
		{
			throw new OpenEditException(ex);
		}
	}

	public void release(LuceneConnection inConnection)
	{
		try
		{
			if( inConnection.getSearcherAndTaxonomy() == null)
			{
				fieldSearcherManager.release(inConnection.getIndexSearcher());
			}
			else
			{
				fieldSearcherTaxonomyManager.release(inConnection.getSearcherAndTaxonomy());
			}
		}
		catch( IOException ex)
		{
			throw new OpenEditException(ex);
		}
		
	}
	
//	
//	else if( inManager instanceof SearcherManager )
//	{
//		return (IndexSearcher)((SearcherManager)inManager).acquire();
//	}

}
