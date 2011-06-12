/*
Copyright (c) 2003 eInnovation Inc. All rights reserved

This library is free software; you can redistribute it and/or modify it under the terms
of the GNU Lesser General Public License as published by the Free Software Foundation;
either version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Lesser General Public License for more details.
*/

package org.openedit.data.lucene;

import java.io.IOException;
import java.util.Iterator;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import com.openedit.OpenEditRuntimeException;


/**
 * DOCUMENT ME!
 *
 * @author cburkey
 */
public class HitIterator implements Iterator
{
	protected TopDocs fieldHits;
	protected IndexSearcher fieldIndexSearcher;
	
	public IndexSearcher getIndexSearcher()
	{
		return fieldIndexSearcher;
	}

	public void setIndexSearcher(IndexSearcher inIndexSearcher)
	{
		fieldIndexSearcher = inIndexSearcher;
	}

	protected int hitCount = 0;
	protected int startOffset = 0;

	public HitIterator(IndexSearcher inSearcher, TopDocs inHits)
	{
		setIndexSearcher(inSearcher);
		setHits(inHits);
	}

	public HitIterator()
	{
	}
	public void setStartOffset( int inStart)
	{
		startOffset = inStart;
	}
	/**
	 * DOCUMENT ME!
	 *
	 * @param inHits
	 */
	public void setHits(TopDocs inHits)
	{
		fieldHits = inHits;
	}

	/**
	 * DOCUMENT ME!
	 *
	 * @return
	 */
	public TopDocs getHits()
	{
		return fieldHits;
	}

	/**
	 * @see java.util.Iterator#hasNext()
	 */
	public boolean hasNext()
	{
		if (hitCount < getHits().totalHits)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 * @see java.util.Iterator#next()
	 */
	public Object next()
	{
		try
		{
			ScoreDoc sdoc  = getHits().scoreDocs[startOffset + hitCount];
			Document doc = getIndexSearcher().doc(sdoc.doc);
			DocumentData data = new DocumentData(doc);
			hitCount++;
			return data;
		}
		catch (IOException ex)
		{
			throw new OpenEditRuntimeException(ex);
		}
	}

	/**
	 * @see java.util.Iterator#remove()
	 */
	public void remove()
	{
	}
}
