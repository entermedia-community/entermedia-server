package org.entermediadb.asset.autocomplete;

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
		String keyword = getKeyword().toLowerCase();
		String searchfor = getSearchFor().toLowerCase();
		//Loop over any spaces and words
		if(getKeyword().length()>2) {
			StringBuffer buffer = new StringBuffer();
			
			Integer start = keyword.indexOf(searchfor);
			String sub  = keyword.substring(start, start+searchfor.length());
			buffer.append(keyword.substring(0, start));
			buffer.append("<b>");
			buffer.append(URLUtilities.xmlEscape(sub));
			buffer.append("</b>");
			String ending = keyword.substring(start+searchfor.length());
			buffer.append(URLUtilities.xmlEscape(ending));
			return buffer.toString();
		}
		return getKeyword();
		
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
