/*
 * Created on Oct 19, 2004
 */
package org.openedit.data.lucene;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;
import org.openedit.Data;
import org.openedit.data.BaseSearcher;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.entermedia.search.LuceneIndexer;

import com.openedit.OpenEditException;
import com.openedit.OpenEditRuntimeException;
import com.openedit.Shutdownable;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.users.User;
import com.openedit.util.FileUtils;

/**
 * @author cburkey
 * 
 */
public abstract class BaseLuceneSearcher extends BaseSearcher implements Shutdownable
{
	private static final Log log = LogFactory.getLog(BaseLuceneSearcher.class);
	protected Analyzer fieldAnalyzer;
	protected IndexSearcher fieldLiveSearcher;
	protected File fieldRootDirectory;
	protected String fieldIndexPath;
	protected SimpleDateFormat fieldFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
	protected IndexWriter fieldIndexWriter;
	protected String fieldBadSortField = null;
	protected boolean fieldRunningReindex = false;
	protected boolean fieldClearIndex;
	protected LuceneIndexer fieldLuceneIndexer;
	protected String fieldCurrentIndexFolder;
	
	public BaseLuceneSearcher() 
	{
	}
	
	public LuceneIndexer getLuceneIndexer()
	{
		if( fieldLuceneIndexer == null)
		{
			fieldLuceneIndexer = new LuceneIndexer();
			fieldLuceneIndexer.setSearcherManager(getSearcherManager());
			fieldLuceneIndexer.setNumberUtils(getNumberUtils());
		}
		return fieldLuceneIndexer;
	}

	public void setLuceneIndexer(LuceneIndexer inLuceneIndexer)
	{
		fieldLuceneIndexer = inLuceneIndexer;
	}

	protected NumberUtils fieldNumberUtils;

	protected abstract void reIndexAll(IndexWriter inWriter);

	public synchronized void reIndexAll() throws OpenEditException
	{
		String indexname = String.valueOf(System.currentTimeMillis());
		log.info(getSearchType() + " reindexing in " + "(" + getCatalogId() + ") as " + indexname);
		File dir = new File(getRootDirectory(), getIndexPath() + "/" + indexname);
		dir.mkdirs();
		
		Directory indexDir = buildIndexDir(indexname);
		IndexWriter writer = null;
		boolean completed = false;
		try
		{
			writer = new IndexWriter(indexDir, getAnalyzer(), true, IndexWriter.MaxFieldLength.UNLIMITED);
			writer.setMergeFactor(50);
			reIndexAll(writer);
			writer.commit();
			writer.optimize();
			
			setCurrentIndexFolder(indexname);
			clearIndex();
			setIndexWriter(writer);
			completed = true;
			
			//delete the older indexes
			deleteOlderIndexes();
			log.info(getSearchType() + " reindex complete in " + indexname);

		}
		catch (CorruptIndexException e)
		{
			throw new OpenEditException(e);
		}
		catch (IOException e)
		{
			throw new OpenEditException(e);
		}
		finally
		{
			if( !completed)
			{
				log.error("Index did not complete. Cleaning up old creating directory");
				new FileUtils().deleteAll(dir);
			}
		}
	}

	protected void deleteOlderIndexes()
	{
		File indexDir = new File(getRootDirectory(), getIndexPath() + "/");
		File[] files = indexDir.listFiles();
		if( files != null && files.length > 2)
		{
			List sorted = new ArrayList(Arrays.asList(files));
			Collections.sort(sorted);
			Collections.reverse(sorted);
			int goodcount = 0;
			FileUtils utils = new FileUtils();
			for (int i = 0; i < sorted.size(); i++)
			{
				File folder = (File)sorted.get(i);
				char firstchar = folder.getName().charAt(0);
				if(  Character.isDigit(firstchar))
				{
					goodcount++;
					if( goodcount > 2)
					{
						utils.deleteAll(folder);
					}
				}
			}
			if( goodcount > 1)
			{
				for (int i = 0; i < sorted.size(); i++)
				{
					File folder = (File)sorted.get(i);
					char firstchar = folder.getName().charAt(0);
					if( !Character.isDigit(firstchar))
					{
						utils.deleteAll(folder);
					}
				}
			}
		}

	}

	public HitTracker search(SearchQuery inQuery)
	{
		String query = inQuery.toQuery();
		if (query == null || query.length() == 0)
		{
			throw new OpenEditException("Query is blank");
		}
		HitTracker hits = search(query, inQuery.getSorts());
		hits.setSearchQuery(inQuery);
		return hits;
	}

	public HitTracker search(String inQuery, String inOrdering)
	{
		if (inOrdering != null)
		{
			List orders = new ArrayList(1);
			orders.add(inOrdering);
			return search(inQuery, orders);
		}
		else
		{
			return search(inQuery, (List) null);
		}
	}

	public HitTracker search(String inQuery, List inOrdering)
	{
		try
		{
			long start = System.currentTimeMillis();
			QueryParser parser = getQueryParser();
			Query query1 = parser.parse(inQuery);

			IndexSearcher liveone = getLiveSearcher();
			TopDocs hits = null;
			if (inOrdering != null)
			{
				Sort sort = buildSort(inOrdering);
				try
				{
					//log.info("XXX Search now" + query1 + " " + fieldLiveSearcher);
					if( sort.getSort() == null || sort.getSort().length == 0)
					{
						hits = liveone.search(query1, 10000);
					}
					else
					{
						hits = liveone.search(query1,10000, sort);
					}
					fieldBadSortField = null;
				}
				catch (RuntimeException ex)
				{
					if (ex.toString().contains("cannot determine sort type") || ex.toString().contains(" does not appear to be indexed") || ex.toString().contains("there are more terms than documents in field"))
					{
						log.error("Skipping bad sort: " + inOrdering);
						log.error(ex);
						log.info(inOrdering);
						liveone = getLiveSearcher();
						hits = liveone.search(query1,10000);
					}
					else
					{
						if (ex instanceof OpenEditRuntimeException)
						{
							throw (OpenEditRuntimeException) ex;
						}
						throw new OpenEditException(ex);
					}
				}
			}
			else
			{
				hits = liveone.search(query1,10000);
			}
			long end = System.currentTimeMillis() - start;

			log.info(hits.totalHits + " hits query: " + query1 + " sort by " + inOrdering + " in " + (double) end / 1000D + " seconds] on " + getCatalogId() + "/" + getSearchType() );

			LuceneHitTracker tracker = new LuceneHitTracker(liveone,hits);
			// tracker.setQuery(inQuery);
			// tracker.setOrdering(inOrdering);
			tracker.setIndexId(getIndexId());

			return tracker;
		}
		catch (Exception ex)
		{
			log.error(ex);
			if (ex instanceof OpenEditException)
			{
				throw (OpenEditException) ex;
			}
			throw new OpenEditException(ex);
		}
	}

	public QueryParser getQueryParser()
	{
		// Parsers are not thread safe.
		QueryParser parser = new QueryParser(Version.LUCENE_31, "description", getAnalyzer())
		{
			protected Query getPrefixQuery(String field, String termStr) throws ParseException
			{
				// deal with apple books.ep* -> apple* +books.ep*
				Query q = getFieldQuery(field, termStr);
				if (q == null)
				{
					return super.getPrefixQuery(field, termStr);
				}
				String newsearch = q.toString();
				newsearch = newsearch.substring(newsearch.indexOf(":") + 1);
				if (newsearch.indexOf(" ") == -1)
				{
					return super.getPrefixQuery(field, newsearch);
				}
				if (newsearch.startsWith("\""))
				{
					newsearch = newsearch.substring(1, newsearch.length() - 1);
				}
				String[] terms = newsearch.split(" ");
				List queries = new ArrayList();
				for (int i = 0; i < terms.length; i++)
				{
					Query combined = null;
					if (i == terms.length - 1)
					{
						combined = super.getPrefixQuery(field, terms[i]);
					}
					else
					{
						combined = getFieldQuery(field, terms[i]);
					}
					queries.add(combined);
				}
				BooleanQuery result = new BooleanQuery(true);
				for (Iterator iterator = queries.iterator(); iterator.hasNext();)
				{
					Query object = (Query) iterator.next();
					result.add(object, BooleanClause.Occur.MUST);
				}
				return result;
			}
		};
		parser.setDefaultOperator(QueryParser.AND_OPERATOR);
		parser.setLowercaseExpandedTerms(true);
		parser.setAllowLeadingWildcard(true);
		return parser;
	}

	protected Sort buildSort(List listing)
	{
		List sorts = new ArrayList(listing.size());
		for (Iterator iterator = listing.iterator(); iterator.hasNext();)
		{
			String inOrdering = (String) iterator.next();
			SortField sort = null;
//			if (inOrdering.equals("random"))
//			{
//				// SortComparator custom = SampleComparable.getComparator();
//				SortComparator custom = getRandomComparator();
//				sort = new SortField("id", custom);
//			}
//			else
			{
				boolean direction = false;
				if (inOrdering.endsWith("Down"))
				{
					direction = true;
					inOrdering = inOrdering.substring(0, inOrdering.length() - 4);
				}
				else if (inOrdering.endsWith("Up"))
				{
					direction = false;
					inOrdering = inOrdering.substring(0, inOrdering.length() - 2);
				}
				PropertyDetails pdetails = getPropertyDetailsArchive().getPropertyDetailsCached(getSearchType());
				if (pdetails == null)
				{
					//this is bad. Somehow our searcher got created with an invalid searchType
					throw new OpenEditException("Lucene Searcher with no details found. catalogid=" + getPropertyDetailsArchive().getCatalogId() + " type=" + getSearchType());
				}
				PropertyDetail detail = pdetails.getDetail(inOrdering);
				if (detail != null && detail.isDataType("number"))
				{
					sort = new SortField(inOrdering, SortField.LONG, direction);
				}
				else if (detail != null && detail.isDataType("double"))
				{
					sort = new SortField(inOrdering, SortField.FLOAT, direction);
				}
				else
				{
					sort = new SortField(inOrdering, SortField.STRING, direction);
				}
			}
			sorts.add(sort);
		}
		SortField[] fields = (SortField[]) sorts.toArray(new SortField[sorts.size()]);
		Sort sortdone = new Sort(fields);
		return sortdone;
	}

	public Directory buildIndexDir(String inName)
	{
		// TODO: Remove the extra search folder
		File indexDir = new File(getRootDirectory(), getIndexPath() + "/" + inName);
		if (!indexDir.exists())
		{
			indexDir.mkdirs();
		}
		try
		{			
			return new SimpleFSDirectory(indexDir);
		}
		catch( IOException ex)
		{
			throw new OpenEditException(ex);
		}
	}

	protected synchronized void setLiveSearcher(IndexSearcher inSearch)
	{

		if (fieldLiveSearcher != null)
		{
			try
			{
				fieldLiveSearcher.close();
			}
			catch (IOException ex)
			{
				fieldLiveSearcher = null;
				// lets assume its invalid and just set it null so it tries
				// to reload.
				// throw new OpenEditRuntimeException(ex);
			}
		}
		//log.info("XXX Null Now" + inSearch	);

		fieldLiveSearcher = inSearch;
	}

	protected IndexSearcher getLiveSearcher()
	{
		if (fieldClearIndex || fieldLiveSearcher == null)
		{
			synchronized (this)
			{
				if (fieldClearIndex || fieldLiveSearcher == null)
				{
					BooleanQuery.setMaxClauseCount(100000);
					try
					{
						if (fieldClearIndex && fieldLiveSearcher != null)
						{
							fieldLiveSearcher.close();
						}						
						//Hack: We dont flush our writers very often in case we have a bunch of writes in a row
						//So when we search again we needed to flush
						if (fieldIndexWriter != null)
						{
							try
							{
								fieldIndexWriter.commit();
							}
							catch (Exception e)
							{
								throw new OpenEditRuntimeException(e);
							}
						}
						Directory index = buildIndexDir(getCurrentIndexFolder());
						if (!IndexReader.indexExists(index))
						//if( !isValidIndex( getCurrentIndexFolder() )
						{
							log.error("No valid index found in A " + getSearchType());
							reIndexAll();
							//index dir may have changed
							index = buildIndexDir(getCurrentIndexFolder());
						}
						flushRecentChanges();
						index = buildIndexDir(getCurrentIndexFolder());
						fieldLiveSearcher = new IndexSearcher(index);
						fieldClearIndex = false;
					}
					catch (Exception ex)
					{
						if (ex instanceof OpenEditException)
						{
							if (ex.toString().contains("handle"))
							{
								log.error("Trying to recover unlocked index");
								log.error(ex);
								setLiveSearcher(null);
								setIndexWriter(null);

							}
							throw (OpenEditException) ex;
						}
						throw new OpenEditException(ex);
					}
				}
			}
		}
		return fieldLiveSearcher;
	}

	private boolean isValidIndex(String inCurrentIndexFolder)
	{
		// TODO Auto-generated method stub
		return false;
	}

	protected void flushRecentChanges() throws IOException
	{

	}

	public void setAnalyzer(Analyzer inAnalyzer)
	{
		fieldAnalyzer = inAnalyzer;
	}

	public Analyzer getAnalyzer()
	{
		if (fieldAnalyzer == null)
		{
			CompositeAnalyzer composite = new CompositeAnalyzer();
			composite.setAnalyzer("description", new StemmerAnalyzer());
			composite.setAnalyzer("id", new NullAnalyzer());
			//composite.setAnalyzer("id", new RecordLookUpAnalyzer(true));
			composite.setAnalyzer("foldersourcepath", new NullAnalyzer());
			fieldAnalyzer = composite;
		}
		return fieldAnalyzer;
	}

	public File getRootDirectory()
	{
		return fieldRootDirectory;
	}

	public void setRootDirectory(File inSearchDirectory)
	{
		fieldRootDirectory = inSearchDirectory;
	}

	public String getIndexId()
	{
		if (fieldLiveSearcher == null)
		{
			return "-1";
		}
		String id = String.valueOf(getLiveSearcher().hashCode());
		return id + fieldClearIndex; //In case we turned that on. Then this will trigger a search
	}

	public void flush()
	{
		if (fieldIndexWriter != null)
		{
			try
			{
				fieldIndexWriter.commit(); //this flushes right away. This is slow. try not to call this often
			}
			catch (Exception e)
			{
				throw new OpenEditRuntimeException(e);
			}
			clearIndex();
		}
	}

	/**
	 * We must be careful not to allow someone to search at the same time that
	 * we are closing the index
	 */
	public synchronized void clearIndex()
	{
		//		try
		//		{
		//			if (fieldLiveSearcher != null)
		//			{
		//				setLiveSearcher(null);  //this will flush when it get's reloaded
		//			}
		//		} catch (Exception ex)
		//		{
		//			throw new OpenEditRuntimeException(ex);
		//		}
		fieldClearIndex = true;
	}

	public IndexWriter getIndexWriter() 
	{
		if (fieldIndexWriter == null)
		{
			synchronized (this)
			{
				Directory indexDir = buildIndexDir(getCurrentIndexFolder());
				try
				{
					File lock = new File(getRootDirectory(), getIndexPath() + "/" + getCurrentIndexFolder() + "/" + "write.lock");
					lock.delete();
					fieldIndexWriter = new IndexWriter(indexDir, getAnalyzer(),true, IndexWriter.MaxFieldLength.UNLIMITED);
				}
				catch (IOException ex)
				{
					throw new OpenEditException(ex);
				}
			}
		}
		return fieldIndexWriter;
	}

	public void setIndexWriter(IndexWriter inIndexWriter)
	{
		if (fieldIndexWriter != null)
		{
			try
			{
				fieldIndexWriter.close();
			}
			catch (IOException ex)
			{
				log.error(ex);
			}
		}
		fieldIndexWriter = inIndexWriter;
	}

	public SearchQuery createSearchQuery()
	{
		LuceneSearchQuery query = new LuceneSearchQuery();
		query.setPropertyDetails(getPropertyDetails());
		query.setCatalogId(getCatalogId());
		query.setResultType(getSearchType()); //a default
		query.setSearcherManager(getSearcherManager());
		return query;
	}

	public HitTracker loadHits(WebPageRequest inReq, String hitsname) throws OpenEditException
	{
		HitTracker otracker = (HitTracker) inReq.getSessionValue(hitsname + getCatalogId());
		HitTracker tracker = checkCurrent(inReq, otracker);
		if (tracker != otracker)
		{
			inReq.putSessionValue(hitsname + getCatalogId(), tracker);
		}
		if (tracker != null)
		{
			inReq.putPageValue(hitsname, tracker);
		}
		return tracker;

	}

	public String getIndexPath()
	{
		if (fieldIndexPath == null)
		{
			String type = getSearchType();
			if( !type.endsWith("s"))
			{
				type = type + "s";   //user -> users
			}
			fieldIndexPath = "/WEB-INF/data/" + getCatalogId() + "/" + type + "/index";
		}
		return fieldIndexPath;
	}

	public void setIndexPath(String inIndexPath)
	{
		fieldIndexPath = inIndexPath;
	}

	public void saveData(Object inData, User inUser)
	{
		throw new OpenEditRuntimeException("saveData not implemented in this searcher. May need to create an archive");
	}

	public NumberUtils getNumberUtils()
	{
		if (fieldNumberUtils == null)
		{
			fieldNumberUtils = new NumberUtils();

		}

		return fieldNumberUtils;
	}

	public void setNumberUtils(NumberUtils inNumberUtils)
	{
		fieldNumberUtils = inNumberUtils;
	}

	/**
	 * This is much faster for bulk loading of index items
	 * @param inRecords
	 */
	public void updateIndex(List inRecords) 
	{
		updateIndex(getIndexWriter(), inRecords);
		clearIndex();
	}
	public void updateIndex(IndexWriter inWriter,List inRecords)
	{
		if ( inRecords.size() == 0)
		{
			return;
		}
		try
		{
			PropertyDetails details = getPropertyDetailsArchive().getPropertyDetailsCached(getSearchType());
			if( details == null)
			{
				throw new OpenEditException("No " + getSearchType() + "properties.xml file available");
			}
			
			for (Iterator iterator = inRecords.iterator(); iterator.hasNext();)
			{
				Data data = (Data) iterator.next();
				Document doc = new Document();
				updateIndex(data, doc, details);
				Term term = new Term("id", data.getId());
				inWriter.updateDocument(term, doc, getAnalyzer());
				clearIndex();
				if (inWriter.ramSizeInBytes() > (1024000 * 35)) // flush every 35 megs
				{
					log.info("Flush writer in reindex mem: " + inWriter.ramSizeInBytes());
					inWriter.commit();
				}
			}
			inWriter.commit();
			inRecords.clear();
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}
	}
	
	public void updateIndex(Data inData) throws OpenEditException
	{
		updateIndex(getIndexWriter(), inData);
	}

	/** Call updateIndex with a list of data. It is much faster **/
	public void updateIndex(IndexWriter inWriter, Data inData) throws OpenEditException
	{
		try
		{
			Document doc = new Document();
			
			//this should cache
			PropertyDetails details = getPropertyDetailsArchive().getPropertyDetails(getSearchType());
			if( details == null)
			{
				throw new OpenEditException("No " + getSearchType() + "properties.xml file available");
			}
			updateIndex(inData, doc, details);
			Term term = new Term("id", inData.getId());
			inWriter.updateDocument(term, doc, getAnalyzer());
			flush(); //this should not be here is is very slow
			clearIndex();
		}
		catch (Exception ex)
		{
			throw new OpenEditException(ex);
		}
	}

	protected void updateIndex(Data inData, Document doc, PropertyDetails inDetails) 
	{
		getLuceneIndexer().updateIndex(inData, doc, inDetails);
	}

	public void deleteAll(User inUser)
	{
		HitTracker all = getAllHits();
		for (Iterator iterator = all.iterator(); iterator.hasNext();)
		{
			Data object = (Data)iterator.next();
			delete(object, inUser);
		}
	}

	public void delete(Data inData, User inUser)
	{
		deleteData(inData);
		deleteRecord(inData);
	}

	public void deleteRecord(Data inData)
	{
		Term term = new Term("id", inData.getId());
		try
		{
			getIndexWriter().deleteDocuments(term);
			flush();
			clearIndex();
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}
	}

	public void deleteData(Data inData)
	{
		log.info("DELETE NOT IMPLEMENTED");
	}

	public void saveAllData(List inAll, User inUser)
	{
		for (Iterator iterator = inAll.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			saveData(data, inUser);
		}
	}
	public Object searchByField(String inField, String inValue)
	{
		if (inField == null)
		{
			return null;
		}
		SearchQuery query = createSearchQuery();
		PropertyDetail detail = new PropertyDetail();
		detail.setId(inField);
		query.addMatches(detail, inValue);

		HitTracker hits = search(query);
		return hits.first();
	}
	public Object searchById(String inId)
	{
		return searchByField("id",inId);
	}

	public void shutdown()
	{
		setIndexWriter(null);
	}
	
	public String getCurrentIndexFolder()
	{
		if ( fieldCurrentIndexFolder == null)
		{
			//find the biggest file name 
			File indexDir = new File(getRootDirectory(), getIndexPath() + "/");
			File[] files = indexDir.listFiles();
			if( files != null && files.length > 1)
			{
				List sorted = new ArrayList(Arrays.asList(files));
				Collections.sort(sorted);
				Collections.reverse(sorted);
				for (Iterator iterator = sorted.iterator(); iterator.hasNext();)
				{
					File  folder = (File ) iterator.next();						
					char firstchar = folder.getName().charAt(0);
					if(  Character.isDigit(firstchar))
					{
						fieldCurrentIndexFolder = folder.getName();
						break;
					}
				}
			}
			if ( fieldCurrentIndexFolder == null)
			{
				fieldCurrentIndexFolder = "A";
			}	
		}
		return fieldCurrentIndexFolder;
	}

	public void setCurrentIndexFolder(String inCurrentIndexFolder)
	{
		fieldCurrentIndexFolder = inCurrentIndexFolder;
		//TODO: Delete the last two older indexes

	}


}