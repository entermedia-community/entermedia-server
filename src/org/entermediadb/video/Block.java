package org.entermediadb.video;

import org.entermediadb.asset.util.MathUtils;

public class Block
{
	protected String fieldLabel;
	protected double fieldStartOffset;
	protected int fieldCounter;
	protected boolean fieldShowThumb;
	public double getStartOffset()
	{
		return fieldStartOffset;
	}
	public void setStartOffset(double inTime)
	{
		fieldStartOffset = MathUtils.roundDouble(inTime,3);
		int totalseconds = (int)Math.round( inTime);
		int hours = (int)MathUtils.divide(totalseconds, (60d*60d));
		
		int remainingseconds = totalseconds - (hours*60*60);
		
		int minutes = (int)MathUtils.divide(remainingseconds , 60d);
		int seconds = (int)(remainingseconds - (minutes * 60));
		
		if(inTime > (60d*60d)) // hours
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
