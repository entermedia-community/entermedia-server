package org.openedit.data.lucene;

import java.io.IOException;
import java.util.Random;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.FieldComparator;

/**
 * 
 * @deprecated
 * This API needs to be migrated to Lucene 4.1 i.e. create a RandomScorer
 * @author cburkey
 *
 */
public class RandomOrderFieldComparator extends FieldComparator<Integer> {

    private final Random random = new Random();

    @Override
    public int compare(int slot1, int slot2) {
        return random.nextInt();
    }

    @Override
    public int compareBottom(int doc) throws IOException {
        return random.nextInt();
    }

    @Override
    public void copy(int slot, int doc) throws IOException {
    }

    @Override
    public void setBottom(int bottom) {
    }

//    @Override
//    public void setNextReader(IndexReader reader, int docBase) throws IOException {
//    }

    @Override
    public Integer value(int slot) {
        return random.nextInt();
    }

	@Override
	public FieldComparator<Integer> setNextReader(AtomicReaderContext inContext) throws IOException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int compareDocToValue(int inDoc, Integer inValue) throws IOException
	{
		// TODO Auto-generated method stub
		return 0;
	}

}