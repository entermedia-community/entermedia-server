package org.entermediadb.video;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.data.SearcherManager;

public class Timeline
{
	private static final Log log = LogFactory.getLog(Timeline.class);
	
	//20 chunks at 15 * 4 = 60px each
	//20 * 60 = 1200px wide
	
	protected long fieldLength;
	protected int fieldPxWidth;
	protected Map facerows = new HashMap();
	protected Collection fieldTicks;
	protected Collection fieldClips;
	protected SearcherManager fieldSearcherManager;
	protected MediaArchive fieldMediaArchive;
	
	public MediaArchive getMediaArchive()
	{
		return fieldMediaArchive;
	}

	public void setMediaArchive(MediaArchive inMediaArchive)
	{
		fieldMediaArchive = inMediaArchive;
	}

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}

	public Collection<Clip> getClips()
	{
		return fieldClips;
	}

	public void setClips(Collection<Clip> inClips)
	{
		fieldClips = inClips;
	}

	public Collection<Block> getTicks()
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
			Set existingfaceprofilegroups = new HashSet();
			if( rows != null)
			{
				for (Iterator iterator = rows.iterator(); iterator.hasNext();)
				{
					Map data = (Map) iterator.next();
					
					Clip clip = new Clip();
					clip.setData(data);
					
					if( data.get("faceprofilegroup") != null)
					{
						existingfaceprofilegroups.add(data.get("faceprofilegroup") + String.valueOf(clip.getStart() ) );
					}
					
					fieldClips.add(clip);
				}
				Collections.sort((ArrayList)fieldClips);
	
			}
			//Now look for facial recognition stuff and create records if needed. Once they save its done
			Collection faceprofiles = inParent.getValues("faceprofiles");
			if( faceprofiles != null)
			{
				for (Iterator iterator = faceprofiles.iterator(); iterator.hasNext();)
				{
					Map<String,Object> profile = (Map) iterator.next();
					if( !existingfaceprofilegroups.contains(profile.get("faceprofilegroup") + String.valueOf(profile.get("timecodestart") ) ) )
					{
						//Add it
						String groupid = (String)profile.get("faceprofilegroup");
						if( groupid == null)
						{
							log.error("Must have a groupid");
							continue;
						}
						Map data = new HashMap();
						data.put( "timecodestart",profile.get("timecodestart"));
						data.put( "timecodelength",profile.get("timecodelength"));
						
						Data groupprofile = getMediaArchive().getCachedData("faceprofilegroup",groupid);
						if(groupprofile != null)
						{
							data.put( "verticaloffset", getFaceRow(groupprofile.get("facecounter")) );
							data.put( "faceprofilegroup",groupid);
						}
						
						//TODO: Set the heights bassed on profilegroup. Like one row per each?
						Clip clip = new Clip();
						clip.setData(data);
						fieldClips.add(clip);
					}
				}
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
		if( px < 90)
		{
			px = 90;
		}
		int pxi = (int)Math.round(px);
		return pxi;
	}
	
	public int getPxFaceStart(Map inFace)
	{
		double ratio = toDouble(inFace.get("facedatastarttime")) / (double)getLength();
		double px = (double)getPxWidth() * ratio;
		return (int)Math.round(px);
	}
	public int getPxFaceLength(Map inFace)
	{
		double ratio = toDouble(inFace.get("facedataendtime")) / (double)getLength();
		double px = (double)getPxWidth() * ratio;
		if( px < 90)
		{
			px = 90;
		}
		int pxi = (int)Math.round(px);
		return pxi;
	}
	
	public int getPxVertical(Clip inClip)
	{
		int vertical = inClip.getData().getInteger("verticaloffset");
		return vertical;
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
	private double toDouble(Object inString)
	{
		if( inString == null)
		{
			return 0;
		}
		
		return Long.parseLong(inString.toString());
	}

	public int getFaceRow(String inFaceCounter)
	{
		String counter= inFaceCounter;
		if( counter == null)
		{
			counter = "uncounted";
		}
		Integer row = (Integer)facerows.get(counter);
		if( row == null)
		{
			row = facerows.size() * 30 + 200; //px
			facerows.put(counter,row);
		}
		return row;
	}
}
