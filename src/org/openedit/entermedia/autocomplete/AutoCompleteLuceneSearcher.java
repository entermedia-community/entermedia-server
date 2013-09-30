package org.openedit.entermedia.autocomplete;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.Version;
import org.openedit.data.lucene.BaseLuceneSearcher;
import org.openedit.data.lucene.FullTextAnalyzer;
import org.openedit.data.lucene.RecordLookUpAnalyzer;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;

/**
 * Thesaurus searcher for a Lucene index.
 * 
 * @author jvalencia
 * 
 */
public class AutoCompleteLuceneSearcher extends BaseLuceneSearcher implements AutoCompleteSearcher
{
	private static Log log = LogFactory.getLog(AutoCompleteLuceneSearcher.class);
	protected Set fieldCache;
	protected Date fieldCacheDate;
	
	public Date getCacheDate()
	{
		if (fieldCacheDate == null)
		{
			GregorianCalendar tomorrow = new GregorianCalendar();
			tomorrow.set(Calendar.HOUR_OF_DAY, 0);
			tomorrow.set(Calendar.MINUTE, 0);
			tomorrow.add(Calendar.DATE, 1);
			fieldCacheDate = tomorrow.getTime();
		}

		return fieldCacheDate;
	}

	public void setCacheDate(Date inCacheDate)
	{
		fieldCacheDate = inCacheDate;
	}

	protected Set getCache()
	{
		if( fieldCache == null)
		{
			fieldCache = new HashSet(500);
		}
		return fieldCache;
	}

	public Analyzer getAnalyzer()
	{
		if (fieldAnalyzer == null)
		{
			Map map = new HashMap();
			map.put("synonymsenc", new FullTextAnalyzer(Version.LUCENE_41));
			PerFieldAnalyzerWrapper composite = new PerFieldAnalyzerWrapper( new RecordLookUpAnalyzer() , map);
			fieldAnalyzer = composite;
		}
		return fieldAnalyzer;
	}

	public HitTracker getAllHits(WebPageRequest inReq)
	{
		return null;
	}

	public String getIndexPath()
	{
		if (getSearchType().startsWith("asset"))
		{
			return "/WEB-INF/data/" + getCatalogId() + "/assets/search/autocomplete/index";
		}
		else
		{
			return "/" + getCatalogId() + "/temp/autocomplete/" + getSearchType();
		}
	}


	public synchronized void reIndexAll(IndexWriter inWriter, TaxonomyWriter inTaxonomyWriter)
	{
		//do nothing
	}
	public void updateHits(HitTracker tracker, String word) throws Exception
	{
		if( new Date().after(getCacheDate()) )
		{
			//clear the cache once a day
			getCache().clear();
			setCacheDate(null);
		}
		
		if( getCache().contains(word))
		{
			return;
		}
		int size = getCache().size();
		if( size > 2000)
		{
			getCache().clear();
		}
		getCache().add(word);
		//word could be "hot dog"
		int hits = tracker.size();
		if (word == null || hits == 0)
		{
			return;
		}
		
		
		SearchQuery suggestionsQuery = createSearchQuery();
		//String nospace = word.replace(' ', '_'); //hot_dog
		suggestionsQuery.addExact("synonyms", word);

		//Todo: Do a local mem cache with a max of 1000 entries. To keep us from search for the same things

		HitTracker wordsHits = search(suggestionsQuery);
		Field id = new Field("synonyms", word, Store.YES, Index.NOT_ANALYZED_NO_NORMS);
		if (wordsHits.size() == 0)
		{
			Document doc = new Document();

			doc.add(id);
			doc.add(new Field("synonymsenc", word, Store.NO, Index.ANALYZED));
			doc.add(new Field("hits", getNumberUtils().int2sortableStr(hits), Store.NO, Index.NOT_ANALYZED_NO_NORMS));
			doc.add(new Field("hitcount", Integer.toString(hits), Store.YES, Index.NOT_ANALYZED_NO_NORMS));

			/* Timestamp */
			String newstamp = DateTools.dateToString(new Date(), Resolution.SECOND);
			doc.add(new Field("timestamp", newstamp, Field.Store.YES, Field.Index.NOT_ANALYZED));

			getIndexWriter().addDocument(doc, getAnalyzer());
			clearIndex();
		}
		else if (wordsHits.size() > 0)
		{
			Object row = wordsHits.get(0);
			String hitstring = wordsHits.getValue(row, "hitcount");
			int currentcount = Integer.parseInt(hitstring);
			if (currentcount == hits)
			{
				return;
			}
			/* Check for timestamp */
			String stamp = wordsHits.getValue(row, "timestamp");
			GregorianCalendar timestamp = new GregorianCalendar();
			timestamp.setTime(DateTools.stringToDate(stamp));

			GregorianCalendar yesterday = new GregorianCalendar();
			yesterday.add(Calendar.DATE, -1);

			if (timestamp.before(yesterday))
			{
				Document doc = new Document();

				doc.add(id);
				doc.add(new Field("synonymsenc", word, Store.NO, Index.ANALYZED));
				doc.add(new Field("hits", getNumberUtils().int2sortableStr(hits), Store.NO, Index.NOT_ANALYZED_NO_NORMS));
				doc.add(new Field("hitcount", Integer.toString(hits), Store.YES, Index.NOT_ANALYZED_NO_NORMS));

				/* Timestamp */
				String newstamp = DateTools.dateToString(new Date(), Resolution.SECOND);
				doc.add(new Field("timestamp", newstamp, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
				Term term = new Term("synonyms", word);
				getIndexWriter().updateDocument(term, doc, getAnalyzer());
				clearIndex();
			}
		}
	}

}
