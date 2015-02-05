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
	public String getHistoryType()
	{
		return get("type");
	}
	public void setHistoryType(String inS)
	{
		setProperty("type", inS);
	}
	public String toString()
	{
		return getHistoryType();
	}

	public int addItemCount()
	{
		return addCount("itemcount");
	}
	public int addItemSuccessCount()
	{
		return addCount("itemsuccesscount");
	}
	public int addItemErrorCount()
	{
		return addCount("itemerrorcount");
	}
	
	protected int addCount(String inField)
	{
		String existing = get(inField);
		if( existing == null)
		{
			existing = "0";
		}
		int count = Integer.parseInt(existing)  + 1;
		setProperty(inField, String.valueOf(count));
		return count;
	}
	
//	public void setAssetIds(List<String> inAssetids)
//	{
//		StringBuffer assets = new StringBuffer();
//		for( String id : inAssetids)
//		{
//			if( assets.length() > 0)
//			{
//				assets.append(" ");
//			}
//			assets.append(id);
//		}
//		setProperty("assetids", assets.toString());
//	}
}
