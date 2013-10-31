package org.openedit.data.lucene;

import org.apache.lucene.facet.search.SearcherTaxonomyManager.SearcherAndTaxonomy;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.search.IndexSearcher;

public class LuceneConnection
{
	protected IndexSearcher fieldIndexSearcher;
	protected SearcherAndTaxonomy fieldSearcherAndTaxonomy;

	public SearcherAndTaxonomy getSearcherAndTaxonomy()
	{
		return fieldSearcherAndTaxonomy;
	}
	public void setSearcherAndTaxonomy(SearcherAndTaxonomy inSearcherAndTaxonomy)
	{
		fieldSearcherAndTaxonomy = inSearcherAndTaxonomy;
	}
	public IndexSearcher getIndexSearcher()
	{
		if( fieldIndexSearcher == null)
		{
			return getSearcherAndTaxonomy().searcher;
		}
		return fieldIndexSearcher;
	}
	public void setIndexSearcher(IndexSearcher inIndexSearcher)
	{
		fieldIndexSearcher = inIndexSearcher;
	}
	
	public TaxonomyReader getTaxonomyReader()
	{
		if( fieldSearcherAndTaxonomy == null)
		{
			return null;
		}
		return getSearcherAndTaxonomy().taxonomyReader;
	}
	
}
