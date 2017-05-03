package org.entermediadb.video;

import java.math.BigDecimal;

import org.entermediadb.asset.util.MathUtils;

public class Block
{
	protected String fieldLabel;
	
	
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
			String formated = String.format("%02d:%02d:%d", days, hours, seconds);
			if( millis > 0)
			{
				formated = formated + "." + Math.floor( millis );
			}
			fieldLabel = formated;
		}
		else
		{
			String formated = String.format("%02d:%d", hours, seconds);
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
	
	protected int fieldLeft;
	public void setLeft(int inLeft)
	{
		fieldLeft = inLeft;
	}
	public int getLeft()
	{
		return fieldLeft;
	}
	public String getLabel()
	{	
		return fieldLabel;
	}
}
