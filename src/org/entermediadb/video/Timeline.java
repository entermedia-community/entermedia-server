package org.entermediadb.video;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.entermediadb.asset.Asset;
import org.openedit.data.PropertyDetails;
import org.openedit.data.SearcherManager;

public class Timeline
{
	
	//20 chunks at 15 * 4 = 60px each
	//20 * 60 = 1200px wide
	
	protected double fieldLength;
	protected int fieldPxWidth;
	protected Collection fieldTicks;
	protected Collection fieldClips;
	protected SearcherManager fieldSearcherManager;
	
	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}

	public Collection getClips()
	{
		return fieldClips;
	}

	public void setClips(Collection inClips)
	{
		fieldClips = inClips;
	}

	public Collection getTicks()
	{
		if (fieldTicks == null)
		{
			//divide into 60 blocks
			fieldTicks = new ArrayList();
			
			double chunck = getLength() / 20d;
			for (int i = 0; i < 21; i++)
			{
				Block block = new Block();
				//block.setTime(i * chunck);
				block.setCounter(i);
				block.setStartOffset(chunck * (double)i);
				if( i < 20)
				{
					block.setShowThumb((i % 2) == 0);
				}	
				fieldTicks.add(block);
			}
		}
		return fieldTicks;
	}

	public void setTicks(Collection inTicks)
	{
		fieldTicks = inTicks;
	}

	public double getLength()
	{
		return fieldLength;
	}

	public void setLength(double inLength)
	{
		fieldLength = inLength;
	}

	public int getPxWidth()
	{
		return fieldPxWidth;
	}

	public void setPxWidth(int inPxWidth)
	{
		fieldPxWidth = inPxWidth;
	}

	public Collection loadClips(Asset inAsset, String inField)
	{
		fieldClips = new ArrayList();
		Collection rows = inAsset.getValues(inField);
		if( rows != null)
		{
			for (Iterator iterator = rows.iterator(); iterator.hasNext();)
			{
				Map data = (Map) iterator.next();
				Clip clip = new Clip();
				clip.setData(data);
				fieldClips.add(clip);
			}
		}
		return fieldClips;
	}
	public int getPxStart(Clip inClip)
	{
		double ratio = inClip.getStart() / getLength();
		double px = (double)getPxWidth() * ratio;
		return (int)Math.round(px);
	}
	public int getPxLength(Clip inClip)
	{
		double ratio = inClip.getLength() / getLength();
		double px = (double)getPxWidth() * ratio;
		return (int)Math.round(px);
	}
	public double getPxToTimeRatio()
	{
		return (double)getPxWidth() / getLength();
	}
}
