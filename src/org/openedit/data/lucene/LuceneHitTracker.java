/*
 * Created on May 2, 2004
 */
package org.openedit.data.lucene;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.openedit.Data;
import org.openedit.util.DateStorageUtil;

import com.openedit.OpenEditException;
import com.openedit.OpenEditRuntimeException;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.Term;

/**
 * @author cburkey
 * 
 */
public class LuceneHitTracker extends HitTracker 
{
	private static final Log log = LogFactory.getLog(LuceneHitTracker.class);
	
	protected transient TopDocs fieldHits;
	protected transient IndexSearcher fieldIndexSearcher;

	public LuceneHitTracker() {

	}


	public IndexSearcher getIndexSearcher()
	{
		return fieldIndexSearcher;
	}

	public void setIndexSearcher(IndexSearcher inIndexSearcher)
	{
		fieldIndexSearcher = inIndexSearcher;
	}

	public LuceneHitTracker(IndexSearcher inSearcher, TopDocs inHits) {
		setIndexSearcher(inSearcher);
		setHits(inHits);
		
	}

	public TopDocs getHits() {
		return fieldHits;
	}

	public void setHits(TopDocs inHits) 
	{
		fieldHits = inHits;
		setPage(1);
	}

	public int size() {
		if (getHits() == null) {
			return 0;
		} else {
			return getHits().totalHits;
		}
	}
	public Document getDoc(int count) 
	{
		try 
		{
			Document doc = getIndexSearcher().doc(getHits().scoreDocs[count].doc);
			return doc;
		} catch (IOException ex) {
			throw new OpenEditRuntimeException(ex);
		}
	}

	public Data get(int count) {
			Document doc = getDoc(count);
			DocumentData data = new DocumentData(doc);
			return data;
	}

	public Iterator iterator() {
		return new HitIterator(getIndexSearcher(),getHits());
	}

	public String toDate(String inValue) 
	{
		if (inValue == null) {
			return null;
		}
		Date date = toDateObject(inValue);
		return DateStorageUtil.getStorageUtil().formatForStorage(date);
	}

	//This is main date API
	public Date getDateValue(Data inHit, String inField)
	{
		String value = inHit.get(inField);
		if( value == null)
		{
			return null;
		}
		return toDateObject(value);
	}
	public Date toDateObject(String inValue)
	{
		Date date = null;
		try {
			date = DateTools.stringToDate(inValue);
		} catch (ParseException ex) {
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
	
	
	public Integer findSelf(String inId) throws Exception {
		if (inId == null) {
			return null;
		}
		for (int i = 0; i < getTotal(); i++) {
			Document hit = (Document) getDoc(i);
			if (inId.equals(hit.get("id"))) {
				return new Integer(i);
			}
		}
		return null;
	}

	public String previousId(String inId) throws Exception 
	{
		Document previous = previous(inId);
		if (previous != null)
		{
			return previous.get("id");
		}
		return null;
	}

	public String nextId(String inId) throws Exception 
	{
		Document next = next(inId);
		if (next != null)
		{
			return next.get("id");
		}
		return null;
	}
	
	public Document previous(String inId) throws Exception
	{
		Integer row = findSelf(inId);
		if (row != null && row.intValue() - 1 >= 0) {
			Document hit = (Document) getDoc(row.intValue() - 1);
			return hit;
		}
		return null;
	}
	
	public Document next(String inId) throws Exception
	{
		Integer row = findSelf(inId);
		if (row != null && row.intValue() + 1 < getTotal()) {
			Document hit = (Document) getDoc(row.intValue() + 1);
			return hit;
		}
		return null;
	}
	
	public boolean contains(Object inHit) {
		for (int i = 0; i < getTotal(); i++) {
			Document hit = (Document) getDoc(i);
			String id = getValue(inHit, "id");
			if (id != null && id.equals(  hit.get("id") ) ) {
				return true;
			}
		}
		return false;
	}

	public String highlight(Object inDoc, String inField) {
		Data doc = (Data) inDoc;
		String value = doc.get(inField);
		if (value != null) {
			for (Iterator iterator = getSearchQuery().getTerms().iterator(); iterator
					.hasNext();) {
				Term term = (Term) iterator.next();
				if (term.getValue() != null) {
					value = replaceAll(value, term.getValue(),
							"<span class='hit'>", "</span>");
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

	private String replaceAll(String inSource, String inFind,
			String inPreReplace, String inPostReplace) {
		String lowercase = inSource.toLowerCase();
		String findlower = inFind.toLowerCase();
		StringBuffer buffer = new StringBuffer();
		int start = 0;
		while (true) {
			int hit = lowercase.indexOf(findlower, start);
			if (hit == -1) {
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

	public String trim(String value, int inMax) {
		if (value.length() > inMax) {
			// trim the begining
			int start = value.indexOf("<span class='hit'>");
			if (start > -1 && start > 10 && start + 150 < value.length()) {
				int before = value.indexOf(" ", start - 10);
				if (before < start) {
					value = value.substring(before, value.length());
				} else {

				}
				// TODO: Look for needed <span class='res'>
			}
			// if still too long trim the end
			if (value.length() > inMax) {
				int end = inMax;
				// Near max distance.
				for (; end > 0; end--) {
					if (value.charAt(end) == ' ') {
						break;
					}
				}
				value = value.substring(0, end);
				if (value.endsWith("<span")) {
					value = value.substring(0, value.length() - 5);
				}
			}
			// TODO: check for the need for </span>
		}
		return value;
	}

	public String getValue(Object inHit, String inKey)
	{
		if(inHit instanceof Data)
		{
			Data hit = (Data) inHit;
			return hit.get(inKey);
		}

		if( inHit instanceof Document)
		{
			Document doc = (Document)inHit;
			return doc.get(inKey);				
		}
		else
		{
			log.error("Invalid data type " + inHit);
		}

		return  null;
		
	}
	
	public Data toData(Object inHit)
	{
		if( inHit instanceof Data)
		{
			return (Data)inHit;
		}
		DocumentData data = new DocumentData((Document)inHit);
		return data;
	}
	
	public Object[] toArray()
	{
		List list = new ArrayList(size());
		for (Iterator iterator = iterator(); iterator.hasNext();) {
			Object hit = iterator.next();
			list.add(hit);
		}
		return list.toArray();
	}
	public Data get(String inId)
	{
		if(inId == null){
			return null;
		}
		for (Iterator iterator = iterator(); iterator.hasNext();)
		{
			DocumentData doc = (DocumentData) iterator.next();
			if (inId.equals(doc.get("id")))
			{
				return doc;
			}

		}
		return null;
	}
	
	public Object getById(String inId)
	{
		int size = size();
		for (int i = 0; i < size; i++)
		{
			Document doc = getDoc(i);
			String id = doc.get("id");
			if( inId.equals(id))
			{
				return toData(doc);
			}
		}
		return null;
	}
}
