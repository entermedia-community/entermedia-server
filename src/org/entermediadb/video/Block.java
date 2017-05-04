package org.entermediadb.video;

import java.math.BigDecimal;

import org.entermediadb.asset.util.MathUtils;

public class Block
{
	protected String fieldLabel;
	protected int fieldCounter;
	
	public void setLabel(double inTime)
	{
		BigDecimal big = new BigDecimal(inTime);
		
		double d = ( inTime / 86400d);
		int days = (int)MathUtils.divide(inTime , 86400d);
		double remaining = inTime % 86400d;
		int hours = (int)(remaining / 3600d);
		int seconds = (int)(remaining - hours);
		double millis = seconds % 1;
		
		if(inTime > 86400) // //day
		{
			String formated = String.format("%02d:%02d:%02d", days, hours, seconds);
			if( millis > 0)
			{
				formated = formated + "." + Math.floor( millis );
			}
			fieldLabel = formated;
		}
		else
		{
			String formated = String.format("%02d:%02d", hours, seconds);
			if( millis > 0)
			{
				formated = formated + "." + Math.floor( millis );
			}	
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
	
}
