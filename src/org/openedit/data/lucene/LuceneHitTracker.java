/*
 * Created on May 2, 2004
 */
package org.openedit.data.lucene;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.params.FacetSearchParams;
import org.apache.lucene.facet.search.CountFacetRequest;
import org.apache.lucene.facet.search.DrillDownQuery;
import org.apache.lucene.facet.search.FacetResult;
import org.apache.lucene.facet.search.FacetResultNode;
import org.apache.lucene.facet.search.FacetsCollector;
import org.apache.lucene.facet.taxonomy.CategoryPath;
import org.apache.lucene.search.CachingWrapperFilter;
import org.apache.lucene.search.FieldCacheTermsFilter;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.openedit.Data;
import org.openedit.data.PropertyDetail;
import org.openedit.data.Searcher;
import org.openedit.util.DateStorageUtil;

import com.openedit.OpenEditException;
import com.openedit.hittracker.FilterNode;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.Term;

/**
 * @author cburkey
 * 
 */
public class LuceneHitTracker extends HitTracker
{
	private static final Log log = LogFactory.getLog(LuceneHitTracker.class);

	// protected transient TopDocs fieldHits;
	protected transient LuceneConnectionManager fieldLuceneConnectionManager;
	protected transient Query fieldLuceneQuery;
	protected transient Sort fieldLuceneSort;
	// protected Map fieldPages;
	// protected Map<Integer,ScoreDoc> fieldCursors;
	protected Integer fieldSize;
	protected String fieldSearchType;
	protected ScoreDoc[] fieldDocs;
	protected int fieldOpenDocsSearcherHash;
	protected DrillDownQuery fieldDrillDownQuery;

	public DrillDownQuery getDrillDownQuery()
	{
		return fieldDrillDownQuery;
	}

	public void setdDrillDownQuery(DrillDownQuery fieldDrillDownQuery)
	{
		this.fieldDrillDownQuery = fieldDrillDownQuery;
	}

	/**
	 * Data type
	 */
	public String getSearchType()
	{
		return fieldSearchType;
	}

	public void setSearchType(String inSearchType)
	{
		fieldSearchType = inSearchType;
	}

	public LuceneHitTracker()
	{

	}

	public LuceneHitTracker(LuceneConnectionManager inManager, Query inQuery, Sort inSort, Searcher inSearcher)
	{
		super(inSearcher);
		setLuceneConnectionManager(inManager);
		setLuceneQuery(inQuery);
		setLuceneSort(inSort);
	}

	public int size()
	{
		if( !isAllSelected() && isShowOnlySelected() && ( fieldSelections == null || fieldSelections.size() == 0 ) )
		{
			return 0;
		}
		
		if (fieldSize == null)
		{
			getPage(0); // this will never happen because we set the size
						// already
		}
		return fieldSize;
	}

	/*
	 * protected void setCursorForPage(ScoreDoc inDoc, int inPageZeroBased) { if
	 * (fieldCursors == null) { fieldCursors = new HashMap(); } if( inDoc !=
	 * null) //may be zero results { //log.info( getSearchType() + " Page  " +
	 * inPageZeroBased + " ended with " + inDoc.doc + " " + fieldSize );
	 * fieldCursors.put(inPageZeroBased, inDoc); } }
	 * 
	 * protected ScoreDoc getCursorForPage(int inPageZeroBased) { if
	 * (fieldCursors == null) { return null; } return fieldCursors.get(inPa int
	 * docid = lastDoc.doc; final SearchResultStoredFieldVisitor visitor = new
	 * SearchResultStoredFieldVisitor(columns); searcher.doc(docid, visitor);
	 * page.add( visitor.createSearchResult() );geZeroBased); }
	 */
	public void setPage(int inPageOneBased)
	{
		if (inPageOneBased == 0)
		{
			inPageOneBased = 1;
		}
		fieldPage = inPageOneBased;
		fieldCurrentPage = getPage(inPageOneBased - 1);
	}

	/**
	 * @deprecated use getPage(int)
	 */
	public Data get(int inCount)
	{
		int page = inCount / getHitsPerPage();

		// Make sure we are on the current page?

		// get the chunk 1
		List<Data> row = getPage(page);

		// 50 - (1 * 40) = 10 relative
		int indexlocation = inCount - (page * getHitsPerPage());

		return row.get(indexlocation);
	}

	// protected Map<Integer,List<Data>> getPages()
	// {
	// if (fieldPages == null)
	// {
	// //fieldPages = new HashMap<Integer,List<Data>>(); //this will leak for
	// really large resultsets
	// fieldPages = new ReferenceMap(ReferenceMap.HARD,ReferenceMap.SOFT);
	// }
	// return fieldPages;
	// }
	protected List<Data> getPage(int inPageNumberZeroBased)
	{
		// List<Data> page = getPages().get(inPageNumberZeroBased);
		// if( page == null )
		// {
		IndexSearcher searcher = null;
		LuceneConnection refs = null;
		try
		{

			refs = getLuceneConnectionManager().acquire();
			searcher = refs.getIndexSearcher();
			// searcher = getLuceneSearcherManager().acquire();
			if (fieldOpenDocsSearcherHash != searcher.hashCode())
			{
				TopDocs docs = null;
				// do the search and save the reuslts
				int max = Integer.MAX_VALUE;
				if (getHitsPerPage() == 1)
				{
					max = 1; // This causes our array to not have the right
								// number of hits in it
				}
				Filter filter = null;

				//Add security filter first. Then commbine it with selections
				Collection<String> allowedids = getSearchQuery().getSecurityIds();
				if( fieldSelections != null && isShowOnlySelected() )
				{
					allowedids = fieldSelections;
				}
				
				if (allowedids != null && allowedids.size() > 0)
				{
					filter = new FieldCacheTermsFilter("id", allowedids.toArray(new String[allowedids.size()]));
				}

				// List<String> terms = new ArrayList();
				// terms.add("5");
				// results = searcher.search(q, ), numDocs).scoreDocs;
				// assertEquals("Must match nothing", 0, results.length);

				if (getLuceneSort() != null)
				{
					docs = searcher.search(getLuceneQuery(), filter, max, getLuceneSort(), false, false);
				}
				else
				{
					docs = searcher.search(getLuceneQuery(), filter, max);
				}
				if (max > 1)
				{
					if( docs.totalHits > 1)
					{
						log.info(getSearchType() + " " + docs.totalHits + " hits " + getLuceneQuery() + " page " + inPageNumberZeroBased + " sort by: " + getLuceneSort() + " " + getSessionId());
					}
					else
					{
						log.debug(getSearchType() + " " + docs.totalHits + " hits " + getLuceneQuery() + " page " + inPageNumberZeroBased + " sort by: " + getLuceneSort() + " " + getSessionId());
					}
				}
				fieldSize = docs.totalHits;
				fieldDocs = docs.scoreDocs;
				// do we need to reset the selections?
				// Use selected doc ids to reload all the selection data
			}
			fieldOpenDocsSearcherHash = searcher.hashCode();

			List<Data> page = populatePageData(inPageNumberZeroBased, searcher);
			return page;

		}
		catch (Exception ex)
		{
			throw new OpenEditException(ex);
		}
		finally
		{
			getLuceneConnectionManager().release(refs);
		}
	}

	protected List<Data> populatePageData(int inPageNumberZeroBased, IndexSearcher searcher) throws IOException
	{
		int start = getHitsPerPage() * inPageNumberZeroBased;
		int max = start + getHitsPerPage();
		max = Math.min(max, fieldSize);

		List<Data> page = new ArrayList<Data>(getHitsPerPage());

		readPageOfData(searcher, start, max, page);
		return page;
	}

	/*
	 * protected List<Data> cursorSearch(IndexSearcher searcher,int
	 * inPageNumberZeroBased,ScoreDoc after ) throws IOException { TopDocs docs
	 * = null; int start = getHitsPerPage() * inPageNumberZeroBased; int max =
	 * start + getHitsPerPage();
	 * 
	 * if( getLuceneSort() != null ) { docs = searcher.searchAfter( after,
	 * getLuceneQuery(), getHitsPerPage() ,getLuceneSort() ); } else { docs =
	 * searcher.searchAfter( after, getLuceneQuery(),getHitsPerPage()); }
	 * 
	 * List<Data> page = new ArrayList<Data>(getHitsPerPage());
	 * 
	 * ScoreDoc lastDoc = null; Map<String,Integer> columns = new
	 * TreeMap<String,Integer>(); int returned = docs.scoreDocs.length; for (int
	 * i = 0; i < returned; i++) { lastDoc = docs.scoreDocs[i]; int docid =
	 * lastDoc.doc; final SearchResultStoredFieldVisitor visitor = new
	 * SearchResultStoredFieldVisitor(columns); searcher.doc(docid, visitor);
	 * Data lastRecord =visitor.createSearchResult(); page.add( lastRecord ); }
	 * // return lastDoc;
	 * 
	 * //log.info( getSearchType() + " Page  " + inPageNumberZeroBased +
	 * " ended with " + lastDoc.doc + " = " + lastRecord.getId()); //ScoreDoc
	 * lastone = readPageOfData(searcher, 0, docs, page);
	 * setCursorForPage(lastDoc,inPageNumberZeroBased); if( log.isDebugEnabled()
	 * ) { log.debug(getSearchType() + " page " + inPageNumberZeroBased ); }
	 * 
	 * return page; }
	 */
	/*
	 * protected List<Data> fullSearch(IndexSearcher searcher, int
	 * inPageNumberZeroBased) throws IOException { // int start =
	 * getHitsPerPage() * inPageNumberZeroBased; // int max = start +
	 * getHitsPerPage(); TopDocs docs = null;
	 * 
	 * if( getLuceneSort() != null ) { docs = searcher.search(
	 * getLuceneQuery(),Integer.MAX_VALUE ,getLuceneSort() ); } else { docs =
	 * searcher.search( getLuceneQuery(),Integer.MAX_VALUE); } fieldSize =
	 * docs.totalHits; List<Data> page = new ArrayList<Data>(getHitsPerPage());
	 * 
	 * ScoreDoc lastone = readPageOfData(searcher, start, docs, page);
	 * setCursorForPage(lastone,inPageNumberZeroBased); log.info(getSearchType()
	 * + " " + size() + " hits " + getLuceneQuery() + " page " +
	 * inPageNumberZeroBased + " sort by: " + getLuceneSort() + " " +
	 * getCatalogId()); return page;
	 * 
	 * }
	 */
	protected ScoreDoc readPageOfData(IndexSearcher searcher, int start, int max, List<Data> page) throws IOException
	{
		/**
		 * This is optimized to only store string versions of the data we have.
		 * Normally the Document class has FieldType that use a bunch of memory.
		 * Guess Most people do not loop over their entire database as often as
		 * we do. TODO: Find a way to cache more generically instead of one page
		 * at a time?
		 */

		ScoreDoc lastDoc = null;
		Map<String, Integer> columns = new TreeMap<String, Integer>(); //TODO: Test performance vs HashMap
		for (int i = 0; start + i < max; i++)
		{
			int offset = start + i;
			lastDoc = fieldDocs[offset];
			int docid = lastDoc.doc;
			// final SearchResultStoredFieldVisitor visitor = new
			// SearchResultStoredFieldVisitor(columns);
			final SearchResultStoredFieldVisitor visitor = new SearchResultStoredFieldVisitor(columns);
			searcher.doc(docid, visitor);
			Data data = visitor.createSearchResult();
			// if data.getId()
			page.add(data);
		}
		// log.info( getSearchType() + " ended with " + lastDoc.doc + " = " +
		// page.get(page.size() - 1).getId());

		return lastDoc;
	}
	/**
	 * @deprecated not really needed
	 */
	//TODO: Remove this API
	public Collection<String> getSourcePaths()
	{
		List sourcepaths = new ArrayList();

		IndexSearcher searcher = null;
		LuceneConnection refs = null;
		try
		{
			refs = getLuceneConnectionManager().acquire();
			searcher = refs.getIndexSearcher();
			int max = Integer.MAX_VALUE;
			TopDocs docs = null;
			if (getLuceneSort() != null)
			{
				docs = searcher.search(getLuceneQuery(), max, getLuceneSort());
			}
			else
			{
				docs = searcher.search(getLuceneQuery(), max);
			}
			fieldSize = docs.totalHits;
			for (int i = 0; i < fieldSize; i++)
			{
				Document doc = searcher.doc(docs.scoreDocs[i].doc);
				sourcepaths.add(doc.get("sourcepath"));
			}
			log.info(size() + " total query:" + getLuceneQuery() + " session:" + getSessionId());
			return sourcepaths;
		}
		catch (Exception ex)
		{
			throw new OpenEditException(ex);
		}
		finally
		{
			getLuceneConnectionManager().release(refs);
		}
	}

	public Iterator iterator()
	{
		return new HitIterator(this);
	}

	public String toDate(String inValue)
	{
		if (inValue == null)
		{
			return null;
		}
		Date date = toDateObject(inValue);
		return DateStorageUtil.getStorageUtil().formatForStorage(date);
	}

	// This is main date API
	public Date getDateValue(Data inHit, String inField)
	{
		String value = inHit.get(inField);
		if (value == null)
		{
			return null;
		}
		return toDateObject(value);
	}

	public Date toDateObject(String inValue)
	{
		Date date = null;
		try
		{
			date = DateTools.stringToDate(inValue);
		}
		catch (ParseException ex)
		{
			log.error(ex);
			return null;
		}
		return date;
	}

	/**
	 * @deprecated removed for $context.getDateTime
	 * @param inValue
	 * @return
	 */
	public String toDateTime(String inValue)
	{
		return toDate(inValue);
	}

	// Only look for data within the current page
	public Integer findSelf(String inId) throws Exception
	{
		if (inId == null)
		{
			return null;
		}
		int i = getPage() * getHitsPerPage();
		for (Iterator iterator = getPageOfHits().iterator(); iterator.hasNext();)
		{
			i++;
			Data type = (Data) iterator.next();
			if (inId.equals(type.getId()))
			{
				return new Integer(i);
			}
		}
		return null;
	}

	public String previousId(String inId) throws Exception
	{
		Data previous = previous(inId);
		if (previous != null)
		{
			return previous.get("id");
		}
		return null;
	}

	public String nextId(String inId) throws Exception
	{
		Data next = next(inId);
		if (next != null)
		{
			return next.get("id");
		}
		return null;
	}

	public Data previous(String inId) throws Exception
	{
		Integer row = findSelf(inId);
		if (row != null && row.intValue() - 1 >= 0)
		{
			Data hit = (Data) get(row.intValue() - 1);
			return hit;
		}
		return null;
	}

	public Data next(String inId) throws Exception
	{
		Integer row = findSelf(inId);
		if (row != null && row.intValue() + 1 < getTotal())
		{
			Data hit = get(row.intValue() + 1);
			return hit;
		}
		return null;
	}

	// Never call this!!!
	public boolean contains(Object inHit)
	{
		Data contains = (Data) inHit;
		for (Iterator iterator = iterator(); iterator.hasNext();)
		{
			Data type = (Data) iterator.next();
			String id = type.getId();
			if (id != null && id.equals(contains.getId()))
			{
				return true;
			}
		}
		return false;
	}

	public String highlight(Object inDoc, String inField)
	{
		Data doc = (Data) inDoc;
		String value = doc.get(inField);
		if (value != null)
		{
			for (Iterator iterator = getSearchQuery().getTerms().iterator(); iterator.hasNext();)
			{
				Term term = (Term) iterator.next();
				if (term.getValue() != null)
				{
					value = replaceAll(value, term.getValue(), "<span class='hit'>", "</span>");
				}
			}
		}
		value = trim(value, 300);

		return value;

		// String FIELD_NAME = "text";
		// Highlighter highlighter = new Highlighter(new MyBolder(),
		// new QueryScorer(query));
		// highlighter.setTextFragmenter(new SimpleFragmenter(20));
		// for (int i = 0; i < hits.length(); i++) {
		// System.out.println("URL " + (i + 1) + ": " +
		// hits.doc(i).getField("URL").stringValue());
		// String text = hits.doc(i).get(FIELD_NAME);
		// int maxNumFragmentsRequired = 2;
		// String fragmentSeparator = "...";
		// TokenStream tokenStream =
		// analyzer.tokenStream(FIELD_NAME, new StringReader(text));
		//
		// String result =
		// highlighter.getBestFragments(
		// tokenStream,
		// text,
		// maxNumFragmentsRequired,
		// fragmentSeparator);
		// System.out.println("\t" + result);
	}

	private String replaceAll(String inSource, String inFind, String inPreReplace, String inPostReplace)
	{
		String lowercase = inSource.toLowerCase();
		String findlower = inFind.toLowerCase();
		StringBuffer buffer = new StringBuffer();
		int start = 0;
		while (true)
		{
			int hit = lowercase.indexOf(findlower, start);
			if (hit == -1)
			{
				buffer.append(inSource.substring(start, inSource.length()));
				break;
			}
			String before = inSource.substring(start, hit);
			buffer.append(before);
			buffer.append(inPreReplace);
			String existing = inSource.substring(hit, hit + findlower.length());
			buffer.append(existing);
			buffer.append(inPostReplace);

			start = hit + findlower.length();
		}
		return buffer.toString();
	}

	public String trim(String value, int inMax)
	{
		if (value.length() > inMax)
		{
			// trim the begining
			int start = value.indexOf("<span class='hit'>");
			if (start > -1 && start > 10 && start + 150 < value.length())
			{
				int before = value.indexOf(" ", start - 10);
				if (before < start)
				{
					value = value.substring(before, value.length());
				}
				else
				{

				}
				// TODO: Look for needed <span class='res'>
			}
			// if still too long trim the end
			if (value.length() > inMax)
			{
				int end = inMax;
				// Near max distance.
				for (; end > 0; end--)
				{
					if (value.charAt(end) == ' ')
					{
						break;
					}
				}
				value = value.substring(0, end);
				if (value.endsWith("<span"))
				{
					value = value.substring(0, value.length() - 5);
				}
			}
			// TODO: check for the need for </span>
		}
		return value;
	}

	public String getValue(Object inHit, String inKey)
	{
		if (inHit instanceof Data)
		{
			Data hit = (Data) inHit;
			return hit.get(inKey);
		}

		if (inHit instanceof Document)
		{
			Document doc = (Document) inHit;
			return doc.get(inKey);
		}
		else
		{
			log.error("Invalid data type " + inHit);
		}

		return null;

	}

	public Data toData(Object inHit)
	{
		if (inHit instanceof Data)
		{
			return (Data) inHit;
		}
		DocumentData data = new DocumentData((Document) inHit);
		return data;
	}

	public Object[] toArray()
	{
		List list = new ArrayList(size());
		for (Iterator iterator = iterator(); iterator.hasNext();)
		{
			Object hit = iterator.next();
			list.add(hit);
		}
		return list.toArray();
	}

	// public Object getById(String inId)
	// {
	// if(inId == null)
	// {
	// return null;
	// }
	// int size = size();
	// for (int i = 0; i < size; i++)
	// {
	// Document doc = getDoc(i);
	// String id = doc.get("id");
	// if( inId.equals(id))
	// {
	// return toData(doc);
	// }
	// }
	// return null;

	// }

	public LuceneConnectionManager getLuceneConnectionManager()
	{
		return fieldLuceneConnectionManager;
	}

	public void setLuceneConnectionManager(LuceneConnectionManager inLuceneSearcherManager)
	{
		fieldLuceneConnectionManager = inLuceneSearcherManager;
	}

	public Query getLuceneQuery()
	{
		return fieldLuceneQuery;
	}

	public void setLuceneQuery(Query inLuceneQuery)
	{
		fieldLuceneQuery = inLuceneQuery;
	}

	public Sort getLuceneSort()
	{
		return fieldLuceneSort;
	}

	public void setLuceneSort(Sort inLuceneSort)
	{
		fieldLuceneSort = inLuceneSort;
	}

	protected List<FilterNode> getFacetedResults() throws Exception
	{
		IndexSearcher searcher = null;

		LuceneConnection refs = null;

		try
		{
			refs = getLuceneConnectionManager().acquire();
			List<FilterNode> facetNodes = new ArrayList<FilterNode>();

			if( refs.getTaxonomyReader() == null)
			{
				log.error("Tried to get facets on a non-facet index. restart?");
				return facetNodes;
			}
			searcher = refs.getIndexSearcher();

			BaseLuceneSearcher lsearcher = (BaseLuceneSearcher) getSearcher();

			// lsearcher.getTaxonomyWriter());
			// TaxonomyReader newReader = TaxonomyReader.openIfChanged(
			// this.taxoReader );

			ArrayList params = new ArrayList();
			List propertydetails = lsearcher.getPropertyDetails().getDetailsByProperty("filter", "true");

			if (propertydetails.size() > 0)
			{
				for (Iterator iterator = propertydetails.iterator(); iterator.hasNext();)
				{
					PropertyDetail detail = (PropertyDetail) iterator.next();
					if( detail.getId().equals("viewasset") || detail.getId().equals("editasset") )
					{
						continue;
					}
					if( !getSearchQuery().hasFilter(detail.getId()))
					{
						String count = detail.get("facetcount");
						int defaultcount = 20;
						if( count != null)
						{
							defaultcount = Integer.parseInt(count);
						}
						params.add(new CountFacetRequest(new CategoryPath(detail.getId()), defaultcount));  //need to have a show more button on UI
					}
				}
				if( params.isEmpty() )
				{
					return facetNodes;
				}
				FacetSearchParams fsp = new FacetSearchParams(params);
				FacetsCollector facetsCollector = FacetsCollector.create(fsp, searcher.getIndexReader(), refs.getTaxonomyReader() );
				
				if (isShowOnlySelected() && fieldSelections != null && fieldSelections.size() > 0)
				{
					Filter filterids = new FieldCacheTermsFilter("id", fieldSelections.toArray(new String[fieldSelections.size()]));
					searcher.search(getLuceneQuery(),filterids, facetsCollector);
				}
				else
				{
					searcher.search(getLuceneQuery(), facetsCollector);
				}

				//copy the collected results to our data structure
				
				List<FacetResult> facetResults = facetsCollector.getFacetResults();
				
				//a tree of options

				for (FacetResult fres : facetResults)
				{
					FacetResultNode root = fres.getFacetResultNode();
					
					if( root.subResults.isEmpty() )
					{
						continue;
					}
				
					
					FilterNode filterNode = new FilterNode();
					String type = root.label.toString();
					filterNode.setId(type);
					//filterNode.setProperty("path", type);
					PropertyDetail parent = getSearcher().getDetail(type);
					if (parent != null)
					{
						filterNode.setName(parent.getText());
					}
					filterNode.setProperty("count", String.valueOf( Math.round( root.value)) ) ;
					facetNodes.add(filterNode);

					for (FacetResultNode cat : root.subResults)
					{
						FilterNode childnode = new FilterNode();
						String childlabel = cat.label.toString();
						String[] splits = childlabel.split("/");
						String id = splits[1];
						childnode.setId(id);
						String label = null;
						if( parent != null && (parent.isList() || "category".equals( parent.getId() ) ) )
						{
							Data data = getSearcher().getSearcherManager().getData(getCatalogId(), parent.getListId(), id);
							if (data == null)
							{
								continue;
							}
							label = data.getName();
						}
						if( label == null)
						{
							label = id;
						}
						childnode.setName(label);
						childnode.setProperty("path", childlabel);
						childnode.setProperty("count", String.valueOf( Math.round( cat.value)) ) ;
						//log.info("Found " + root.label + " " + id);
						filterNode.addChild(childnode);
					}
					//filterNode.sortChildren();
				}
				return facetNodes;
			}
		}
		finally
		{
			getLuceneConnectionManager().release(refs);
		}
		return null;
	}
}
