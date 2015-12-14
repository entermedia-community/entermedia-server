package org.entermediadb.asset.scanner;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Parse {

	protected Map fieldData;
	
		  
	public  String get(String inMetadata)
	{
		return (String)getData().get(inMetadata);
	}
	public void put( String inId, Object inVal)
	{
		if ( inVal != null)
		{
			getData().put( inId, inVal);
		}
	}

	public Map getData()
	{
		if (fieldData == null)
		{
			fieldData = new HashMap();
		}

		return fieldData;
	}


	public void setData(Map inData)
	{
		fieldData = inData;
	}


	public void setText(String inText)
	{
		put( "body", inText);
	}
	public String getText()
	{
		return get("body");
	}
	public List getList(String inString)
	{
		List list = (List)getData().get(inString);
		if( list == null)
		{
			return Collections.EMPTY_LIST;
		}
		return list;
	}
	public void setTitle(String inTitle)
	{
		put( "title",inTitle);
		
	}
	public String getTitle()
	{
		return get("title");
	}

	public int getPages()
	{
		String pages = get("pages");
		if (pages == null)
		{
			return 1;
		}
		return Integer.parseInt(get("pages"));
	}
	
	public void setPages(int inPages)
	{
		put("pages", String.valueOf(inPages));
	}
}
