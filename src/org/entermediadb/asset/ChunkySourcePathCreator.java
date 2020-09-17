package org.entermediadb.asset;

import org.openedit.Data;
import org.openedit.util.PathUtilities;

public class ChunkySourcePathCreator implements SourcePathCreator
{

	protected int fieldSplitSize = 2;
	
	public ChunkySourcePathCreator()
	{
		// TODO Auto-generated constructor stub
	}
	public ChunkySourcePathCreator(int splitsize)
	{
		fieldSplitSize = splitsize;
	}
	
	public int getSplitSize()
	{
		return fieldSplitSize;
	}


	public void setSplitSize(int inSplitSize)
	{
		fieldSplitSize = inSplitSize;
	}


	public String createSourcePath(Data inAsset, String inStoragePath)
	{
		if( inStoragePath.length() < 3 )
		{
			return "0";
		}
		String cleanedup = PathUtilities.extractId(inStoragePath);
		StringBuffer sourcepath = new StringBuffer();
		
		//cut off the last part of the id +3
		for (int i = 0; i < inStoragePath.length(); i++)
		{
			if( i > 0 && (i % getSplitSize()) == 0)
			{
				sourcepath.append("/");
			}
			sourcepath.append(cleanedup.charAt(i));
		}
		return sourcepath.toString();
	}

}
