package org.openedit.entermedia.orders;

import java.util.List;

import org.openedit.xml.ElementData;

public class OrderHistory extends ElementData
{
	public static final OrderHistory EMPTY = new OrderHistory();
	
	public String getNoteSnip(int inSize)
	{
		String note = getNote();
		if( note != null)
		{
			String snip =  note.substring(0,Math.min(note.length(), inSize));
			if( snip.length() < note.length())
			{
				snip = snip + "...";
			}
			return snip;
		}
		return null;
	}
	
	public String getNote()
	{
		return get("note");
	}
	public String getUserStatus()
	{
		return get("userstatus");
	}
	public void setUserStatus(String inS)
	{
		setProperty("userstatus", inS);
	}
	public String toString()
	{
		return getUserStatus();
	}
	public boolean isClosed()
	{
		return "closed".equals(getUserStatus());
	}

	public void setAssetIds(List<String> inAssetids)
	{
		StringBuffer assets = new StringBuffer();
		for( String id : inAssetids)
		{
			if( assets.length() > 0)
			{
				assets.append(" ");
			}
			assets.append(id);
		}
		setProperty("assetids", assets.toString());
	}
}
