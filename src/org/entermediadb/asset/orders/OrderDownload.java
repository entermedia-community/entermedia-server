package org.entermediadb.asset.orders;

import java.util.Date;
import java.util.Iterator;

import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.hittracker.HitTracker;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.MathUtils;

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

	public MultiValued getCurrentItem()
	{
		if( fieldCurrentItem == null)
		{
			fieldCurrentItem = (MultiValued)getItemList().first();
		}
		return fieldCurrentItem;
	}
	public void setCurrentItem(MultiValued inCurrentItem)
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
	protected MultiValued fieldCurrentItem;
	
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

	public Date getEstimatedEndDate()
	{
		Date start = getCurrentItem().getDate("downloadstartdate");
		if( start == null)
		{
			return null;
		}
		Date now = new Date();
		float difference = now.getTime() - start.getTime();
		double percent = percentageRemaining();
		if( percent == 0)
		{
			return null;
		}
		/*
		 
		 Spent time         Downloaded size
		 ----------     =    ---------------
		 Total Time         Total Size 
		 
		 
		 Total Time =  Spent time  /ratio		 
		 */
		
		double totaltime = MathUtils.divide(difference , percent);
		long remainingtime = MathUtils.roundUp( totaltime - difference);
		
		Date future = new Date(now.getTime() + remainingtime);
		return future;
	}
	
	public Date getPublishedDate()
	{
		Date date = getCurrentItem().getDate("publisheddate");
		if( date == null)
		{
			return null;
		}
		return date;
	}

	
}
