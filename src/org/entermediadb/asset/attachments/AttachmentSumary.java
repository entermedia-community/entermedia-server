package org.entermediadb.asset.attachments;

public class AttachmentSumary
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

	public int increment()
	{
		return fieldCount++;
	}
	
}
