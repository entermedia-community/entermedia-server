package org.openedit.entermedia.search;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.FSDirectory;
import org.openedit.data.Searcher;
import org.openedit.data.lucene.BaseLuceneSearcher;
import org.openedit.data.lucene.CompositeAnalyzer;
import org.openedit.data.lucene.NullAnalyzer;
import org.openedit.data.lucene.StemmerAnalyzer;
import org.openedit.entermedia.Category;
import org.openedit.entermedia.CategoryArchive;
import org.openedit.entermedia.MediaArchive;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.hittracker.Term;
import com.openedit.util.FileUtils;

/**
 * Thesaurus searcher for a Lucene index.
 * 
 * @author jvalencia
 * 
 */
public class RelatedKeywordLuceneSearcher extends BaseLuceneSearcher implements RelatedKeywordSearcher
{
	private static Log log = LogFactory.getLog(RelatedKeywordLuceneSearcher.class);

	/**
	 * Gets a list of suggestions for a specific search. This is done looking at
	 * the "description" search term in the search query inside the "hits" page
	 * value set by any fieldSearch method. If success, the
	 * hits.getSearchQuery().setSuggestedSearches() method is being called. If
	 * there's a "searcher" page value in the request, suggested terms with 0
	 * hits will be discarded.
	 * 
	 * @param inReq
	 *            The web request. Needs a HitTracker "hits" page value, a
	 *            "catalogid" and a "type" request parameter. The last one
	 *            should be something like "asset", "job"or "user".
	 * @return Nothing.
	 * @throws OpenEditException
	 */
	public Map<String, String> getSuggestions(HitTracker inTracker, Searcher inTypeSearcher) throws Exception
	{
		if (inTracker == null || inTypeSearcher == null)
		{
			log.error("A tracker and a searcher are needed in order to get suggestions.");
			return null;
		}
		SearchQuery typeQuery = inTracker.getSearchQuery();
		if (typeQuery == null)
		{
			return null;
		}
		Map<String, String> suggestions = new Hashtable<String, String>();
		Term keyword = null;
		for ( Object o: typeQuery.getTerms() )
		{
			Term term = (Term) o;
			if ("description".equals(term.getDetail().getId()))
			{
				keyword = term;
				break;
			}
		}

		if (keyword != null)
		{
			SearchQuery suggestionsQuery = createSearchQuery();
			String nospace = keyword.getValue().replace(' ', '_');
			if (nospace.contains("*"))   //* messes up our logic
			{
				return suggestions;
			}
			//word is a cached version of results
			suggestionsQuery.addMatches("word", nospace);
			HitTracker wordsHits = search(suggestionsQuery);
			if (wordsHits == null || wordsHits.size() == 0)
			{
				indexWord(keyword.getValue(), inTracker, inTypeSearcher);
				wordsHits = search(suggestionsQuery);
			}
			if (wordsHits.size() > 0)
			{
				Object row = wordsHits.get(0);

				/* Check for timestamp */
				String stamp = wordsHits.getValue(row, "timestamp");
				GregorianCalendar timestamp = new GregorianCalendar();
				timestamp.setTime(DateTools.stringToDate(stamp));

				GregorianCalendar yesterday = new GregorianCalendar();
				yesterday.add(Calendar.DATE, -1);

				if (timestamp.before(yesterday))
				{
					/* Reindex */
					getIndexWriter().deleteDocuments(new org.apache.lucene.index.Term("timestamp", stamp));
					indexWord(keyword.getValue(), inTracker, inTypeSearcher);
					wordsHits = search(suggestionsQuery);
					if (wordsHits.size() > 0)
					{
						row = wordsHits.get(0);
					}
				}

				String text = wordsHits.getValue(row, "synonyms");
				if (text != null)
				{
					String[] hits = text.split(";");
					for (int i = 0; i < hits.length; i++)
					{
						String word = hits[i];
						String key = word.substring(0, word.lastIndexOf('('));
						suggestions.put(key, word);
					}
				}
			}
		}
		if (suggestions.size() > 0)
		{
			inTracker.getSearchQuery().setSuggestedSearches(suggestions);
		}
		return suggestions;
	}

	public void indexWord( String inWord, HitTracker inResults, Searcher inTypeSearcher )
		throws Exception
	{
		if (inWord == null || inWord.equals(""))
		{
			return;
		}

		HashSet<String> terms = new HashSet<String>();
		int count = 0;
		for( Object o: inResults )
		{
			count++;
			if( count > 50)
			{
				break; //Dont look over the entire result set
			}
			String keywords = inResults.getValue(o, "keywords");
			if( keywords != null )
			{
				for( String keyword: keywords.split(" ") )
				{
					keyword = keyword.trim();
					if( keyword.length() > 0 && !keyword.equals(inWord) )
					{
						terms.add(keyword);
					}
				}
			}
			if(terms.size() > 9) break;
		}
		//Now check for categories?
		count = 0;
		if( terms.size() < 9)
		{
			for( Object o: inResults )
			{
				count++;
				if( count > 50)
				{
					break; //Dont look over the entire result set
				}
				String catalogid = inResults.getValue(o, "catalogid");
				String categoryid = inResults.getValue(o, "category");
				if( catalogid != null && categoryid != null )
				{
					CategoryArchive archive = getMediaArchive(catalogid).getCategoryArchive();
					for( String keyword: categoryid.split(" ") )
					{
						keyword = keyword.trim();
						if( keyword.length() > 0 && !keyword.equals(inWord) )
						{
							Category cat = archive.getCategory(keyword);
							if( cat != null )
							{
								keyword = cat.getName();
								terms.add(keyword);
							}
						}
					}
				}
				if(terms.size() > 9) break;
			}
		}

		Document doc = new Document();
		StringBuffer saved = new StringBuffer();
		StringBuffer savedenc = new StringBuffer();
		//Find out how many asset hits exists
		for (String synonym: terms)
		{
			SearchQuery typeQuery = inTypeSearcher.createSearchQuery();
			synonym = synonym.replaceAll("\\(.*?\\)", "");
			synonym = synonym.replace("(", "").replace(")", "").replace("-", "");
			typeQuery.addStartsWith("description", synonym);
			int hits = inTypeSearcher.search(typeQuery).getTotal();
			if (hits > 0)
			{
				saved.append(synonym);
				saved.append(" (");
				saved.append(hits);
				saved.append(")");
				saved.append(";");
			}
			synonym = synonym.replace(' ', '_').replace(";", " ");
			savedenc.append(synonym);
			savedenc.append(" ");
		}
		// Need to make sure that the terms they actually searched for got
		// into the index
		if (saved.length() > 0)
		{
			doc.add(new Field("synonyms", saved.toString(), Store.YES, Index.NOT_ANALYZED_NO_NORMS));
		}
		doc.add(new Field("synonymsenc", savedenc.toString(), Store.NO, Index.ANALYZED_NO_NORMS));
		doc.add(new Field("word", inWord.replace(" ", "_"), Store.YES, Index.NOT_ANALYZED_NO_NORMS));
		/* Timestamp */
		String timestamp = DateTools.dateToString(new Date(), Resolution.SECOND);
		doc.add(new Field("timestamp", timestamp, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));

		getIndexWriter().addDocument(doc, getAnalyzer());
		clearIndex();
	}

	public Analyzer getAnalyzer()
	{
		if (fieldAnalyzer == null)
		{
			CompositeAnalyzer composite = new CompositeAnalyzer();
			composite.setAnalyzer("synonymsenc", new StemmerAnalyzer());
			composite.setAnalyzer("synonyms", new NullAnalyzer());
			composite.setAnalyzer("word", new NullAnalyzer());
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
			return "/" + getCatalogId() + "/assets/search/suggestions/index";
		}
		else
		{
			return "/" + getCatalogId() + "/temp/suggestions/" + getSearchType();
		}
	}

	public void reIndexAll(IndexWriter writer) throws OpenEditException
	{
		//do nothing
		try
		{
			writer.setMergeFactor(100);
			writer.setMaxBufferedDocs(2000);
		}
		catch (Exception ex)
		{
			throw new OpenEditException(ex);
		}
	}

	protected MediaArchive getMediaArchive(String inCatalogId)
	{
		return (MediaArchive)getSearcherManager().getModuleManager().getBean(inCatalogId, "mediaArchive");
	}
	
}
