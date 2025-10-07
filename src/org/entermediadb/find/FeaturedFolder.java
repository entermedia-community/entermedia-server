package org.entermediadb.find;

import org.openedit.data.BaseData;

public class FeaturedFolder extends BaseData implements Comparable
{
	protected int fieldCount;

	public int getCount()
	{
		return fieldCount;
	}

	public void setCount(int inCount)
	{
		fieldCount = inCount;
	}
	
	@Override
	public int compareTo(Object inO)
	{
		FeaturedFolder folder = (FeaturedFolder)inO;
		int ret = Integer.compare(fieldCount, fieldCount);
		return ret;
	}
	
}
