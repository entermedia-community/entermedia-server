package org.entermediadb.video;

import org.entermediadb.asset.util.MathUtils;

public class Block
{
	protected String fieldLabel;
	protected long fieldStartOffset;
	protected int fieldCounter;
	protected boolean fieldShowThumb;
	public long getStartOffset()
	{
		return fieldStartOffset;
	}
	public double getSeconds()
	{
		return (double)fieldStartOffset / 1000d;
	}
	public void setStartOffset(long inTimeMilli)  //this is in milli
	{
		fieldStartOffset = inTimeMilli;//MathUtils.roundDouble(inTimeMilli,3);
		int totalseconds = (int)Math.round( (double)inTimeMilli / 1000d);
		int hours = (int)MathUtils.divide(totalseconds, (60d*60d));
		
		int remainingseconds = totalseconds - (hours*60*60);
		
		int minutes = (int)MathUtils.divide(remainingseconds , 60d);
		int seconds = (int)(remainingseconds - (minutes * 60));
		
		if(inTimeMilli > (60d*60d*1000d)) // over an hour
		{
			String formated = String.format("%02d:%02d:%02d", hours, minutes, seconds);
			fieldLabel = formated;
		}
		else
		{
			String formated = String.format("%02d:%02d", minutes, seconds);
			fieldLabel = formated;
		}
	}
	public boolean isLabel()
	{
		return fieldLabel != null;
	}
	
	public void setCounter(int inLeft)
	{
		fieldCounter = inLeft;
	}
	public int getCounter()
	{
		return fieldCounter;
	}
	public String getLabel()
	{	
		return fieldLabel;
	}
	public int tick(int inSoFar,int width)
	{
		return inSoFar + width;
	}
	public boolean showThumb()
	{
		return fieldShowThumb;
	}
	public void setShowThumb(boolean inShowThumb)
	{
		fieldShowThumb = inShowThumb;
	}
}
