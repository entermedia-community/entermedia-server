package org.entermediadb.video;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.entermediadb.asset.Asset;
import org.openedit.MultiValued;
import org.openedit.data.SearcherManager;

public class Timeline
{
	
	//20 chunks at 15 * 4 = 60px each
	//20 * 60 = 1200px wide
	
	protected long fieldLength;
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
			
			double chunck = (double)getLength() / 20d;
			for (int i = 0; i < 21; i++)
			{
				Block block = new Block();
				//block.setTime(i * chunck);
				block.setCounter(i);
				double offsetmili = chunck * (double)i;
				block.setStartOffset(Math.round( offsetmili ));
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

	public long getLength()
	{
		return fieldLength;
	}

	public void setLength(long inLength)
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

	public Collection loadClips(MultiValued inParent, String inField)
	{
		fieldClips = new ArrayList();
		if( inParent != null)
		{
			Collection rows = inParent.getValues(inField);
			if( rows != null)
			{
				for (Iterator iterator = rows.iterator(); iterator.hasNext();)
				{
					Map data = (Map) iterator.next();
					Clip clip = new Clip();
					clip.setData(data);
					fieldClips.add(clip);
				}
				Collections.sort((ArrayList)fieldClips);
	
			}
		}
		return fieldClips;
	}
	public int getPxStart(Clip inClip)
	{
		double ratio = (double)inClip.getStart() / (double)getLength();
		double px = (double)getPxWidth() * ratio;
		return (int)Math.round(px);
	}
	public int getPxLength(Clip inClip)
	{
		double ratio = (double)inClip.getLength() / (double)getLength();
		double px = (double)getPxWidth() * ratio;
		if( px < 10)
		{
			px = 10;
		}
		return (int)Math.round(px);
	}
	public double getPxToTimeRatio()
	{
		return (double)getPxWidth() / (double)getLength();
	}

	public void selectClip(String inSelected)
	{
		if( fieldClips == null)
		{
			return;
		}
		for (Iterator iterator = getClips().iterator(); iterator.hasNext();)
		{
			Clip clip = (Clip) iterator.next();
			//clip.getStart()
		}
	}
}
