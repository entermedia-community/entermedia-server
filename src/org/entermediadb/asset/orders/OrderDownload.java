package org.entermediadb.asset.orders;

import java.util.Iterator;

import org.entermediadb.asset.util.MathUtils;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.hittracker.HitTracker;

public class OrderDownload
{

	public Order getOrder()
	{
		return fieldOrder;
	}
	public void setOrder(Order inOrder)
	{
		fieldOrder = inOrder;
	}

	public Data getCurrentItem()
	{
		if( fieldCurrentItem == null)
		{
			fieldCurrentItem = (Data)getItemList().first();
		}
		return fieldCurrentItem;
	}
	public void setCurrentItem(Data inCurrentItem)
	{
		fieldCurrentItem = inCurrentItem;
	}
	protected Order fieldOrder;
	protected HitTracker fieldItemList;
	public HitTracker getItemList()
	{
		return fieldItemList;
	}
	
	
	public int getItemCount()
	{
		return getItemList().size();
	}
	public void setItemList(HitTracker inItemList)
	{
		fieldItemList = inItemList;
	}
	protected Data fieldCurrentItem;
	
	public long totalBytes()
	{
		long downloaditemtotalfilesize = 0;
		//TODO: replace with agregation of downloaditemtotalfilesize
		for (Iterator iterator = getItemList().iterator(); iterator.hasNext();)
		{
			MultiValued item = (MultiValued) iterator.next();
			downloaditemtotalfilesize = downloaditemtotalfilesize + item.getLong("downloaditemtotalfilesize");
		}
		return downloaditemtotalfilesize;
	}

	public long totalBytesDownloaded()
	{
		long downloaditemdownloadedfilesize = 0;
		//TODO: replace with agregation of downloaditemdownloadedfilesize
		for (Iterator iterator = getItemList().iterator(); iterator.hasNext();)
		{
			MultiValued item = (MultiValued) iterator.next();
			downloaditemdownloadedfilesize = downloaditemdownloadedfilesize + item.getLong("downloaditemdownloadedfilesize");
		}
		return downloaditemdownloadedfilesize;
	}

	public double percentageRemaining()
	{
		double per = MathUtils.divide(totalBytesDownloaded(),totalBytes());
		return per;
	}

}
