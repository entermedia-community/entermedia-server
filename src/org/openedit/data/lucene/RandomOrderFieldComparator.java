package org.openedit.data.lucene;

import java.io.IOException;
import java.util.Random;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.Scorer;

/**
 * 
 * 
 * @author cburkey
 *
 */
public class RandomOrderFieldComparator extends FieldComparator<Integer> {

    private final Random random = new Random();

    @Override
    public int compare(int slot1, int slot2) {
        int i = random.nextInt();
    	return i;
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
		return this;
	}

	@Override
	public int compareDocToValue(int inDoc, Integer inValue) throws IOException
	{
		// TODO Auto-generated method stub
		return 0;
	}
	
	

}