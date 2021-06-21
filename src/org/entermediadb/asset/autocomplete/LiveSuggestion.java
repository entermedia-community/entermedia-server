package org.entermediadb.asset.autocomplete;

import java.util.Comparator;

import org.openedit.util.URLUtilities;

public class LiveSuggestion implements Comparable<LiveSuggestion>
{
	String fieldKeyword;
	String fieldSearchFor;

	public String getSearchFor()
	{
		return fieldSearchFor;
	}

	public void setSearchFor(String inSearchFor)
	{
		fieldSearchFor = inSearchFor;
	}

	public String getKeyword()
	{
		return fieldKeyword;
	}

	public void setKeyword(String inKeyword)
	{
		fieldKeyword = inKeyword;
	}
	
	public String getBold()
	{
		//Loop over any spaces and words
		StringBuffer buffer = new StringBuffer();
		buffer.append("<b>");
		
		String sub  = getKeyword().substring(0,getSearchFor().length());
		buffer.append(URLUtilities.xmlEscape(sub));
		buffer.append("</b>");
		String ending = getKeyword().substring(getSearchFor().length());
		buffer.append(URLUtilities.xmlEscape(ending));
		return buffer.toString();
		
	}

	public int length()
	{
		return getKeyword().length();
	}

	@Override
	public int compareTo(LiveSuggestion inArg0)
	{
		if( inArg0.length() == length())
		{
			return 0;
		}
		if( inArg0.length() > length())
		{
			return -1;
		}
			
		return 1;
	}
	
}
