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

import java.util.Iterator;

import org.openedit.Data;


/**
 * DOCUMENT ME!
 *
 * @author cburkey
 */
public class HitIterator implements Iterator
{
	LuceneHitTracker fieldLuceneHitTracker;
	int hitCount = 0;
	
	public LuceneHitTracker getLuceneHitTracker()
	{
		return fieldLuceneHitTracker;
	}

	public void setLuceneHitTracker(LuceneHitTracker inLuceneHitTracker)
	{
		fieldLuceneHitTracker = inLuceneHitTracker;
	}

	public HitIterator(LuceneHitTracker inTracker)
	{
		setLuceneHitTracker(inTracker);
	}
	
	public boolean hasNext()
	{
		if (hitCount < getLuceneHitTracker().size() )
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
		Data data = getLuceneHitTracker().get(hitCount);
		hitCount++;
		return data;
	}

	/**
	 * @see java.util.Iterator#remove()
	 */
	public void remove()
	{
	}
}
