package org.openedit.entermedia;

import org.openedit.Data;

public class ChunkySourcePathCreator implements SourcePathCreator
{

	public String createSourcePath(Data inAsset, String inStoragePath)
	{
		StringBuffer sourcepath = new StringBuffer();
		
		//cut off the last part of the id +3
		for (int i = 0; i + 3 < inStoragePath.length(); i++)
		{
			if( i > 0 && i % 3 == 0)
			{
				sourcepath.append("/");
			}
			sourcepath.append(inStoragePath.charAt(i));
		}
		//Beware For small ID's the path will be nothing or 000?
		if( sourcepath.length() < 4 )
		{
			return "000";
		}
		return sourcepath.toString();
	}

}
