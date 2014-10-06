/*
 * Created on Oct 19, 2004
 */
package org.openedit.data.lucene;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.params.FacetIndexingParams;
import org.apache.lucene.facet.search.DrillDownQuery;
import org.apache.lucene.facet.search.SearcherTaxonomyManager;
import org.apache.lucene.facet.taxonomy.CategoryPath;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.join.JoinUtil;
import org.apache.lucene.search.join.ScoreMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.SimpleFSLockFactory;
import org.apache.lucene.util.Version;
import org.entermedia.cache.CacheManager;
import org.openedit.Data;
import org.openedit.data.BaseSearcher;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;

import com.openedit.OpenEditException;
import com.openedit.OpenEditRuntimeException;
import com.openedit.Shutdownable;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.FilterNode;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.Join;
import com.openedit.hittracker.SearchQuery;
import com.openedit.users.User;
import com.openedit.util.FileUtils;

/**
 * @author cburkey
 * 
 */
public abstract class BaseLuceneSearcher  extends BaseSearcher implements Shutdownable
{
	private static final Log log = LogFactory.getLog(BaseLuceneSearcher.class);
	protected Analyzer fieldAnalyzer;
	protected File fieldRootDirectory;
	protected String fieldIndexPath;
	protected SimpleDateFormat fieldFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
	protected IndexWriter fieldIndexWriter;
	protected String fieldBadSortField = null;
	protected boolean fieldPendingCommit;
	protected String fieldIndexId;
	protected LuceneIndexer fieldLuceneIndexer;
	protected String fieldCurrentIndexFolder;
	protected String fieldIndexRootFolder;
	protected CacheManager fieldCacheManager;
	protected DirectoryTaxonomyWriter fieldTaxonomyWriter;
	protected LuceneConnectionManager fieldLuceneConnectionManager;

	public BaseLuceneSearcher()
	{

	}

	public DirectoryTaxonomyWriter getTaxonomyWriter()
	{
		return fieldTaxonomyWriter;
	}

	public void setTaxonomyWriter(DirectoryTaxonomyWriter inTaxonomyWriter)
	{
		fieldTaxonomyWriter = inTaxonomyWriter;
	}

	public CacheManager getCacheManager()
	{
		return fieldCacheManager;
	}

	public void setCacheManager(CacheManager inCacheManager)
	{
		fieldCacheManager = inCacheManager;
	}
	public LuceneConnectionManager getLuceneConnectionManager()
	{
		if (fieldLuceneConnectionManager == null)
		{
			IndexWriter writer = getIndexWriter();

			fieldLuceneConnectionManager = 	new LuceneConnectionManager(writer, true, new SearcherFactory(), getTaxonomyWriter() );
		}
		//fieldLuceneConnectionManager.maybeRefresh();
		//mayberefresh
		
		return fieldLuceneConnectionManager;
	}

	public String getIndexRootFolder()
	{
		return fieldIndexRootFolder;
	}

	public void setIndexRootFolder(String inIndexRootFolder)
	{
		fieldIndexRootFolder = inIndexRootFolder;
	}

	public LuceneIndexer getLuceneIndexer()
	{
		if (fieldLuceneIndexer == null)
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

	protected abstract void reIndexAll(IndexWriter inWriter, TaxonomyWriter inTaxonomy);

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
			IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_45, getAnalyzer());
			config.setOpenMode(OpenMode.CREATE);

			// LogMergePolicy lmp = new LogDocMergePolicy();
			// //lmp.setMaxMergeDocs(3);
			// lmp.setMergeFactor(100);
			//
			// // writer.mergeFactor = 10;
			// // writer.setMergeFactor(100);
			// // writer.setMaxBufferedDocs(2000);
			//
			// config.setMergePolicy(lmp);
			//
			writer = new IndexWriter(indexDir, config);

			// writer = new IndexWriter(indexDir, , true,
			// IndexWriter.MaxFieldLength.UNLIMITED);
			// writer.setMergeFactor(50);
			
			//Make sure we have facets
			List facetlist = getPropertyDetails().getDetailsByProperty("filter", "true");
			DirectoryTaxonomyWriter taxonomywriter = null;
			if( facetlist.size() > 0)
			{
				Directory taxoDir = buildIndexDir(indexname + "facets");
				taxonomywriter = new DirectoryTaxonomyWriter(taxoDir, OpenMode.CREATE);
			}

			reIndexAll(writer, taxonomywriter);
			// writer.optimize();
			//The indexer already did this.
			//taxonomywriter.commit();
			//writer.commit();
			
			setCurrentIndexFolder(indexname);
			// setCurrentIndexFolder(indexname + "facets");
			
			setIndexWriter(writer, taxonomywriter);
			

			
			clearIndex();
			completed = true;
			

			// delete the older indexes
			deleteOlderIndexes();
			log.info(getSearchType() + " reindex complete in folder /" + indexname);

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
			if (!completed)
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
		if (files != null && files.length > 2)
		{
			List sorted = new ArrayList(Arrays.asList(files));
			Collections.sort(sorted);
			Collections.reverse(sorted);
			FileUtils utils = new FileUtils();
			Set keepers = new HashSet();
			for (int i = 0; i < sorted.size(); i++)
			{
				File folder = (File) sorted.get(i);
				char firstchar = folder.getName().charAt(0);
				if (Character.isDigit(firstchar))
				{
					keepers.add(folder);
					if (keepers.size() > 6)
					{
						utils.deleteAll(folder);
					}
				}
			}
			// delete any A folders legacy
			if (keepers.size() > 1)
			{
				for (int i = 0; i < sorted.size(); i++)
				{
					File folder = (File) sorted.get(i);
					char firstchar = folder.getName().charAt(0);
					if (!Character.isDigit(firstchar))
					{
						utils.deleteAll(folder);
					}
				}
			}
		}

	}

	public HitTracker search(SearchQuery inQuery)
	{
		if (inQuery == null)
		{
			return null;
		}
		HitTracker hits = search(inQuery, inQuery.getSorts());
		hits.setSearchQuery(inQuery);
		return hits;
	}

	// protected HitTracker search(String inQuery, String inOrdering)
	// {
	// if (inOrdering != null)
	// {
	// List orders = new ArrayList(1);
	// orders.add(inOrdering);
	// return search(inQuery, orders);
	// }
	// else
	// {
	// return search(inQuery, (List) null);
	// }
	// }

	protected HitTracker search(SearchQuery inQuery, List inOrdering)
	{
		try
		{
			String query = inQuery.toQuery();
			if (query != null && query.length() == 0)
			{
				query = null;
			}
			if (inQuery.getParentJoins() == null && (query == null))
			{
				throw new OpenEditException("Query is blank");
			}

			Query query1 = null;
			if (query != null)
			{
				if (inQuery != null && inQuery.equals("id:(*)"))
				{
					query1 = new MatchAllDocsQuery();
				}
				else
				{
					QueryParser parser = getQueryParser();
					query1 = parser.parse(query);
				}
			}
			if (fieldPendingCommit)
			{
				flush();
			}
			getLuceneConnectionManager().maybeRefresh();
			/**
			 * 
			 IndexSearcher articleSearcher = ... 2 IndexSearcher
			 * commentSearcher = ... 3 String fromField = "id"; 4 boolean
			 * multipleValuesPerDocument = false; 5 String toField =
			 * "article_id"; 6 // This query should yield article with id 2 as
			 * result 7 BooleanQuery fromQuery = new BooleanQuery(); 8
			 * fromQuery.add(new TermQuery(new Term("title", "byte")),
			 * BooleanClause.Occur.MUST); 9 fromQuery.add(new TermQuery(new
			 * Term("title", "norms")), BooleanClause.Occur.MUST); 10 Query
			 * joinQuery = JoinUtil.createJoinQuery(fromField,
			 * multipleValuesPerDocument, toField, fromQuery, articleSearcher);
			 * 11 TopDocs topDocs = commentSearcher.search(joinQuery, 10);
			 */

			// Now deal with the join or statement
			if (inQuery.getParentJoins() != null)
			{

				for (Join join : inQuery.getParentJoins())
				{
					/*
					 * IndexSearcher articleSearcher = ... 2 IndexSearcher
					 * commentSearcher = ... 3 String remoteField = "id"; 4
					 * boolean multipleValuesPerDocument = false; 5 String
					 * lField = "article_id"; 6 // This query should yield
					 * article with id 2 as result 7 BooleanQuery fromQuery =
					 * new BooleanQuery(); 8 fromQuery.add(new TermQuery(new
					 * Term("title", "byte")), BooleanClause.Occur.MUST); 9
					 * fromQuery.add(new TermQuery(new Term("title", "norms")),
					 * BooleanClause.Occur.MUST); 10 Query joinQuery =
					 * JoinUtil.createJoinQuery(fromField,
					 * multipleValuesPerDocument, toField, fromQuery,
					 * articleSearcher); 11 TopDocs topDocs =
					 * commentSearcher.search(joinQuery, 10);
					 */
					//

					BaseLuceneSearcher tosearcher = (BaseLuceneSearcher) getSearcherManager().getSearcher(getCatalogId(), join.getRemoteSearchType());
					
					LuceneConnection  connection = tosearcher.getLuceneConnectionManager().acquire();
					
					//IndexSearcher searcher = toIndexSearcher(manager);
					
					try
					{
						Query filter = tosearcher.getQueryParser().parse(join.getRemoteQuery().toQuery());
						filter = JoinUtil.createJoinQuery(join.getRemoteColumn(), join.isRemoteHasMultiValues(), join.getLocalColumn(), filter, connection.getIndexSearcher(), ScoreMode.None);
						if (query1 != null)
						{
							BooleanQuery finalQuery = new BooleanQuery();
							finalQuery.add(filter, BooleanClause.Occur.MUST);
							finalQuery.add(query1, BooleanClause.Occur.MUST);
							query1 = finalQuery;
						}
						else
						{
							query1 = filter;
						}
					}
					finally
					{
						tosearcher.getLuceneConnectionManager().release(connection);
					}
				}

			}
			DrillDownQuery ddq = null;
			boolean empty = true;

			if (inQuery.hasFilters())
			{
				for (Iterator iterator = inQuery.getFilters().iterator(); iterator.hasNext();)
				{
					FilterNode rootnode = (FilterNode) iterator.next();
					ddq = new DrillDownQuery(FacetIndexingParams.DEFAULT, query1);  //this is recursive, is that the fastest way to do it?
					
					//Lets not do multiple paths just yet. For now support a single list of filters
					
//					for (Iterator iterator2 = rootnode.getChildren().iterator(); iterator2.hasNext();)
//					{
//						FilterNode node = (FilterNode) iterator2.next();
//						if (node.isSelected())
//						{
//					String [] path = node.get("label").split("/");
					String [] path = new String[] { rootnode.getId(), rootnode.get("value") };
					ddq.add(new CategoryPath(path));
//						}
//					}
					//ddq.add(query1, BooleanClause.Occur.MUST);
					query1 = ddq;
				}
			}
			Sort sort = null;
			if (inOrdering != null && inOrdering.size() > 0)
			{
				sort = buildSort(inOrdering);
			}
			LuceneHitTracker tracker = new LuceneHitTracker(getLuceneConnectionManager(), query1, sort, this);
			tracker.setSearchType(getSearchType());
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

	protected IndexSearcher toIndexSearcher(Object inManager) throws IOException
	{
		if( inManager instanceof SearcherManager )
		{
			return (IndexSearcher)((SearcherManager)inManager).acquire();
		}
		else //if( inManager instanceof SearcherTaxonomyManager)
		{
			return ((SearcherTaxonomyManager)inManager).acquire().searcher;
		}
	}

	public QueryParser getQueryParser()
	{
		// TODO: use a threadgroup

		// Parsers are not thread safe.
		QueryParser parser = new QueryParser(Version.LUCENE_41, "description", getAnalyzer())
		{

			protected org.apache.lucene.search.Query getRangeQuery(String field, String low, String high, boolean inclusivelow, boolean incluseivehigh) throws ParseException
			{
				PropertyDetail detail = getDetail(field);
				if (detail != null && ( detail.isDataType("number") || detail.isDataType("long")))
				{
					Long lv = Long.parseLong(low);
					Long hv = Long.parseLong(high);
					return NumericRangeQuery.newLongRange(field, lv, hv, true, true);
				}

				if (detail != null && detail.isDataType("double"))
				{
					Double lv = Double.parseDouble(low);
					Double hv = Double.parseDouble(high);
					return NumericRangeQuery.newDoubleRange(field, lv, hv, true, true);
				}
				if(detail == null && field.contains("lat") || field.contains("lng")){
					//this is a position search
					Double lv = Double.parseDouble(low);
					Double hv = Double.parseDouble(high);
					return NumericRangeQuery.newDoubleRange(field, lv, hv, true, true);
				}
				
				return super.getRangeQuery(field, low, high, inclusivelow, incluseivehigh);
			}
		};

		/*
		 * { protected Query getPrefixQuery(String field, String termStr) throws
		 * ParseException { // deal with apple books.ep* -> apple* +books.ep*
		 * Query q = getFieldQuery(field, termStr); if (q == null) { return
		 * super.getPrefixQuery(field, termStr); } String newsearch =
		 * q.toString(); newsearch = newsearch.substring(newsearch.indexOf(":")
		 * + 1); if (newsearch.indexOf(" ") == -1) { return
		 * super.getPrefixQuery(field, newsearch); } if
		 * (newsearch.startsWith("\"")) { newsearch = newsearch.substring(1,
		 * newsearch.length() - 1); } String[] terms = newsearch.split(" ");
		 * List queries = new ArrayList(); for (int i = 0; i < terms.length;
		 * i++) { Query combined = null; if (i == terms.length - 1) { combined =
		 * super.getPrefixQuery(field, terms[i]); } else { combined =
		 * getFieldQuery(field, terms[i]); } queries.add(combined); }
		 * BooleanQuery result = new BooleanQuery(true); for (Iterator iterator
		 * = queries.iterator(); iterator.hasNext();) { Query object = (Query)
		 * iterator.next(); result.add(object, BooleanClause.Occur.MUST); }
		 * return result; } };
		 */
		parser.setDefaultOperator(QueryParser.AND_OPERATOR);
		parser.setLowercaseExpandedTerms(true);
		parser.setAllowLeadingWildcard(true);
		parser.setEnablePositionIncrements(true);

		return parser;
	}

	protected Sort buildSort(List listing)
	{
		List sorts = new ArrayList(listing.size());
		for (Iterator iterator = listing.iterator(); iterator.hasNext();)
		{
			String inOrdering = (String) iterator.next();
			SortField sort = null;
			if (inOrdering.equals("random"))
			{
				Sort randomsort = new Sort(

				new SortField("", new FieldComparatorSource()
				{

					@Override
					public FieldComparator<Integer> newComparator(String fieldname, int numHits, int sortPos, boolean reversed) throws IOException
					{
						RandomOrderFieldComparator c = new RandomOrderFieldComparator();
						// c.setScorer(getIndexWriter().get
						return new RandomOrderFieldComparator();
						// return new RandomOrderFieldComparator();
					}

				}));
				return randomsort;
			}
			else
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
					// this is bad. Somehow our searcher got created with an
					// invalid searchType
					throw new OpenEditException("Lucene Searcher with no details found. catalogid=" + getPropertyDetailsArchive().getCatalogId() + " type=" + getSearchType());
				}
				PropertyDetail detail = pdetails.getDetail(inOrdering);
				String sortfield = inOrdering;
				if (detail != null)
				{
					sortfield = detail.getSortProperty();
				}
				if (detail != null && detail.isDataType("number"))
				{
					sort = new SortField(sortfield, SortField.Type.LONG, direction);

					// sort = new SortField(sortfield, SortField.Type.STRING,
					// direction);
				}
				else if (detail != null && detail.isDataType("long"))
				{
					sort = new SortField(sortfield, SortField.Type.LONG, direction);
					// sort = new SortField(sortfield, SortField.Type.STRING,
					// direction);
				}
				else if (detail != null && detail.isDataType("double"))
				{
					sort = new SortField(sortfield, SortField.Type.DOUBLE, direction);
				}
				else
				{
					sort = new SortField(sortfield, SortField.Type.STRING, direction);
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
			Directory dir = FSDirectory.open(indexDir);
			dir.setLockFactory(new SimpleFSLockFactory());
			return dir;
		}
		catch (IOException ex)
		{
			throw new OpenEditException(ex);
		}
	}

	// protected synchronized void setLiveSearcher(IndexSearcher inSearch)
	// {
	//
	// if (fieldLiveSearcher != null)
	// {
	// try
	// {
	// fieldLiveSearcher.close();
	// }
	// catch (IOException ex)
	// {
	// fieldLiveSearcher = null;
	// // lets assume its invalid and just set it null so it tries
	// // to reload.
	// // throw new OpenEditRuntimeException(ex);
	// }
	// }
	// //log.info("XXX Null Now" + inSearch );
	//
	// fieldLiveSearcher = inSearch;
	// }

	protected boolean checkExists(String indexname) throws IOException
	{
		boolean exists = false;
		if (DirectoryReader.indexExists(buildIndexDir(indexname)))
		{
			exists = true;
		}
		// }
		// catch ( org.apache.lucene.index.IndexNotFoundException ex)
		// {
		// exists = false;
		// File indexDir = new File(getRootDirectory(), getIndexPath() + "/" +
		// indexname);
		// new FileUtils().deleteAll(indexDir);
		// }
		return exists;
	}

	public void setAnalyzer(Analyzer inAnalyzer)
	{
		fieldAnalyzer = inAnalyzer;
	}

	public Analyzer getAnalyzer()
	{
		if (fieldAnalyzer == null)
		{
			Map analyzermap = new HashMap();

			analyzermap.put("description", new FullTextAnalyzer(Version.LUCENE_41));

			// The ID column is special since it is used to load records from
			// the index.
			// When we do a Lucene Update we would have to lowerCase the id
			PropertyDetails details = getPropertyDetails();
			NullAnalyzer nua = new NullAnalyzer();
			//analyzermap.put("id", );
			for (Iterator iterator = details.iterator(); iterator.hasNext();)
			{
				PropertyDetail detail = (PropertyDetail) iterator.next();
				String field = detail.getId();
				if( field.contains("sourcepath") || field.equals("id") )
				{
					analyzermap.put(field, nua);
				}
				else if( detail.isList() && !detail.isMultiValue()) //multi needs to be tokenized
				{
					analyzermap.put(field, nua);
				}
			}
			
			PerFieldAnalyzerWrapper composite = new PerFieldAnalyzerWrapper(new RecordLookUpAnalyzer(), analyzermap);

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

	/**
	 * Not needed any more? TODO: use a last modification time?
	 * 
	 */
	public String getIndexId()
	{
		// if (fieldLiveSearcher == null)
		// {
		// return null;
		// }
		if (fieldIndexId == null)
		{
			fieldIndexId = String.valueOf(System.currentTimeMillis()) + getIndexWriter().hashCode();
		}
		return fieldIndexId;
	}

	public void flush()
	{
		if(fieldTaxonomyWriter != null){
			try
			{
				fieldTaxonomyWriter.commit();

			}
			catch (Exception e)
			{
				throw new OpenEditRuntimeException(e);
			}
		}
		if (fieldIndexWriter != null)
		{
			try
			{
				fieldIndexWriter.commit(); // this flushes right away. This is
											// slow. try not to call this often

			}
			catch (Exception e)
			{
				throw new OpenEditRuntimeException(e);
			}
		}
		fieldPendingCommit = false;
	}

	/**
	 * We must be careful not to allow someone to search at the same time that
	 * we are closing the index
	 */
	public synchronized void clearIndex()
	{
		// try
		// {
		// if (fieldLiveSearcher != null)
		// {
		// setLiveSearcher(null); //this will flush when it get's reloaded
		// }
		// } catch (Exception ex)
		// {
		// throw new OpenEditRuntimeException(ex);
		// }
		fieldPendingCommit = true;
		fieldIndexId = null;
	}

	public IndexWriter getIndexWriter()
	{
		if (fieldPendingCommit)
		{
			flush();
		}

		if (fieldIndexWriter == null )
		{
			synchronized (this)
			{
				if (fieldIndexWriter == null )
				{
					BooleanQuery.setMaxClauseCount(100000);

					String folder = getCurrentIndexFolder();
					if (folder == null)
					{
						reIndexAll(); // infinite loop?
						return fieldIndexWriter;
					}
					Directory indexDir = buildIndexDir(folder);

					try
					{
						// Leaving a lock fils seems to help the writer pick up
						// where it left off. Not sure what is going on
						File lock = new File(getRootDirectory(), getIndexPath() + "/" + folder + "/" + "write.lock");
						if (lock.exists() && !lock.delete())
						{
							// Invalid lock errors are returned when the index
							// has no valid files in it
							log.error("Could not delete lock");
							IndexWriter.unlock(indexDir);
						}
						if (getSearchType().equals("asset"))
						{
							log.info(getCatalogId() + " asset writer opened in " + folder);
						}
						IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_CURRENT, getAnalyzer());
						config.setOpenMode(OpenMode.CREATE_OR_APPEND);
						fieldIndexWriter = new IndexWriter(indexDir, config);

						fieldTaxonomyWriter = null;
						if( getPropertyDetails().getDetailsByProperty("filter", "true").size() > 0 )
						{
							String name = folder + "facets";
							Directory taxoDir = buildIndexDir(name);
							File taxlock = new File(getRootDirectory(), getIndexPath() + "/" + name + "/" + "write.lock");
							if (taxlock.exists() && !taxlock.delete())
							{
								// Invalid lock errors are returned when the
								// index
								// has no valid files in it
								log.error("Could not delete lock");
								IndexWriter.unlock(taxoDir);
							}
							fieldTaxonomyWriter = new DirectoryTaxonomyWriter(taxoDir, OpenMode.CREATE_OR_APPEND);
						}
					}
					catch(IndexNotFoundException e){
						reIndexAll();
					}
					catch (IOException ex)
					{
						throw new OpenEditException(ex);
					}
				}
			}
		}
		return fieldIndexWriter;
	}

	/**
	 * Used after a reindex
	 * 
	 * @param inIndexWriter
	 */
	public void setIndexWriter(IndexWriter inIndexWriter, DirectoryTaxonomyWriter inTaxoWriter)
	{
		if(fieldTaxonomyWriter != null){
			try
			{
				
				fieldTaxonomyWriter.close();
				
				
				// fieldLiveSearcher = null;
			}
			catch (IOException ex)
			{
				log.error(ex);
			}
		}
		
		if (fieldIndexWriter != null)
		{
			try
			{
				
				
				fieldIndexWriter.close(); // This should flush if needed
				// fieldLiveSearcher = null;
			}
			catch (IOException ex)
			{
				log.error(ex);
			}
		}
		
		
		fieldIndexWriter = inIndexWriter;
		fieldTaxonomyWriter = inTaxoWriter;
		fieldLuceneConnectionManager = null;
		if (fieldCacheManager != null)
		{
			getCacheManager().clear(getIndexPath());
		}
//		try
//		{
//		
//				fieldLuceneSearcherManager = new SearcherTaxonomyManager(inIndexWriter, true, new SearcherFactory(), inTaxoWriter);
//		
//		}
//		catch (IOException ex)
//		{
//			log.error(ex);
//		}

	}

	public SearchQuery createSearchQuery()
	{
		LuceneSearchQuery query = new LuceneSearchQuery();
		query.setPropertyDetails(getPropertyDetails());
		query.setCatalogId(getCatalogId());
		query.setResultType(getSearchType()); // a default
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
		if (fieldIndexRootFolder != null)
		{
			return "/WEB-INF/data/" + getCatalogId() + "/" + getIndexRootFolder() + "/" + getSearchType();
		}

		String folder = getSearchType();
		if (!folder.endsWith("s"))
		{
			folder = folder + "s";
		}
		return "/WEB-INF/data/" + getCatalogId() + "/" + folder + "/index";
	}

	public void setIndexPath(String inIndexPath)
	{
		fieldIndexPath = inIndexPath;
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

	public void updateIndex(Collection inList)
	{
		updateIndex(inList, false);
	}

	/**
	 * This is much faster for bulk loading of index items
	 * 
	 * @param inRecords
	 */
	public void updateIndex(Collection inRecords, boolean optimize)
	{
		updateIndex(getIndexWriter(), getTaxonomyWriter(), inRecords);
		clearIndex();
	}

	public void updateIndex(IndexWriter inWriter, TaxonomyWriter inTaxonomyWriter, Collection inRecords)
	{
		if (inRecords.size() == 0)
		{
			return;
		}
		try
		{
			PropertyDetails details = getPropertyDetailsArchive().getPropertyDetailsCached(getSearchType());
			if (details == null)
			{
				throw new OpenEditException("No " + getSearchType() + "properties.xml file available");
			}

			Term[] terms = new Term[inRecords.size()];
			List<Document> docs = new ArrayList<Document>(inRecords.size());
			
			int i = 0;
			for (Iterator iterator = inRecords.iterator(); iterator.hasNext();)
			{
				Data data = (Data) iterator.next();
				if (data.getId() == null)
				{
					log.error("Could not index " + data + " " + getSearchType());
					continue;
				}
				Document doc = new Document();
				updateIndex(data, doc, details);
				Term term = new Term("id", data.getId());
				terms[i++] = term;
				docs.add(doc);
				getLuceneIndexer().updateFacets(details,doc, inTaxonomyWriter);
				if (fieldCacheManager != null)
				{
					getCacheManager().remove(getIndexPath(), data.getId());
				}
			}
			inWriter.deleteDocuments(terms);
			inWriter.addDocuments(docs);
			
			//inWriter.updateDocument(term, doc, getAnalyzer());
			clearIndex();

			
			if( inTaxonomyWriter != null)
			{
				inTaxonomyWriter.commit();
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
		// Already pending, just add another one on
		fieldPendingCommit = false;
		IndexWriter writer = getIndexWriter();
		updateIndex(writer, getTaxonomyWriter(), inData);
		clearIndex();
	}

	/** Call updateIndex with a list of data. It is much faster **/
	public void updateIndex(IndexWriter inWriter, TaxonomyWriter inTaxonomyWriter, Data inData) throws OpenEditException
	{
		try
		{
			Document doc = new Document();

			// this should cache
			PropertyDetails details = getPropertyDetailsArchive().getPropertyDetailsCached(getSearchType());
			if (details == null)
			{
				throw new OpenEditException("No " + getSearchType() + "properties.xml file available");
			}
			updateIndex(inData, doc, details);

			getLuceneIndexer().updateFacets(details,doc, inTaxonomyWriter);

			Term term = new Term("id", inData.getId());
			inWriter.updateDocument(term, doc, getAnalyzer());
			if (fieldCacheManager != null)
			{
				getCacheManager().remove(getIndexPath(), inData.getId());
			}

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
		int size = 0;
		do
		{

			all.setHitsPerPage(1000);
			size = all.size();
			for (Iterator iterator = all.getPageOfHits().iterator(); iterator.hasNext();)
			{
				Data object = (Data) iterator.next();
				delete(object, inUser);
			}
			all = getAllHits();
		}
		while (size > all.size());

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
			if (fieldCacheManager != null)
			{
				getCacheManager().remove(getIndexPath(), inData.getId());
			}
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

	public void saveAllData(Collection inAll, User inUser)
	{
		// check that all have ids
		for (Object object : inAll)
		{
			Data data = (Data) object;
			if (data.getId() == null)
			{
				data.setId(nextId());
			}
			saveData(data, inUser);// Actually save the darn thing.
		}
		updateIndex(inAll);
		// getLiveSearcher(); //should flush the index
	}

	public Object searchByField(String inField, String inValue)
	{
		if (inField == null)
		{
			return null;
		}
		if (fieldCacheManager != null && "id".equals(inField))
		{
			Object cached = getCacheManager().get(getIndexPath(), inValue);
			if (cached != null)
			{
				return cached;
			}
		}
		SearchQuery query = createSearchQuery();
		PropertyDetail detail = new PropertyDetail();
		detail.setId(inField);
		query.addMatches(detail, inValue);

		HitTracker hits = search(query);
		hits.setHitsPerPage(1);
		Object cached = hits.first();
		if (fieldCacheManager != null && cached != null && "id".equals(inField))
		{
			// TODO: Come up with a way to put custom objects in here such as
			// Order, Asset etc
			getCacheManager().put(getIndexPath(), inValue, cached);
		}
		return cached;
	}

	public Object searchById(String inId)
	{
		if(inId ==  null)
		{
			return null;
		}
		Object cached = searchByField("id", inId);
		return cached;
	}

	public void shutdown()
	{
		// setIndexWriter(null);
		if (fieldIndexWriter != null)
		{
			try
			{
				if( fieldTaxonomyWriter != null)
				{
					fieldTaxonomyWriter.close();
				}
				fieldIndexWriter.close();
			}
			catch (IOException ex)
			{
				log.error(ex);
			}
		}

	}

	public String getCurrentIndexFolder()
	{
		if (fieldCurrentIndexFolder == null)
		{
			// find the biggest file name
			File indexDir = new File(getRootDirectory(), getIndexPath() + "/");
			File[] files = indexDir.listFiles();
			if (files != null && files.length > 0)
			{
				List sorted = new ArrayList(Arrays.asList(files));
				Collections.sort(sorted);
				Collections.reverse(sorted);
				for (Iterator iterator = sorted.iterator(); iterator.hasNext();)
				{
					File folder = (File) iterator.next();
					char firstchar = folder.getName().charAt(0);
					if (Character.isDigit(firstchar) && !folder.getName().contains("facet"))
					{
						fieldCurrentIndexFolder = folder.getName();
						break;
					}
				}
			}
			if (fieldCurrentIndexFolder == null)
			{
				File a = new File(getRootDirectory(), getIndexPath() + "/A");
				if (a.exists())
				{
					fieldCurrentIndexFolder = "A";
				}
			}
		}
		return fieldCurrentIndexFolder;
	}

	public void setCurrentIndexFolder(String inCurrentIndexFolder)
	{
		fieldCurrentIndexFolder = inCurrentIndexFolder;
		// TODO: Delete the last two older indexes

	}

	

}