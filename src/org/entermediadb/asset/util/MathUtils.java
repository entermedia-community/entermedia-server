package org.entermediadb.asset.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.openedit.OpenEditException;

public class MathUtils
{

	public BigDecimal getBigDecimal(String val)
	{
		if( val == null || val.contains(".") )
		{
			return new BigDecimal(0);
		}
		return new BigDecimal(val);
	}
	public BigDecimal getBigDecimal(double val)
	{
		return new BigDecimal(val);
	}
	public float getPercentage(BigDecimal now, BigDecimal total)
	{
		if( total.doubleValue() > 0)
		{
			BigDecimal percentage=now.divide(total,2,RoundingMode.HALF_UP);
			return percentage.floatValue();
		}
		return 0;
		
	}
	public float getPercentage(int inSoFar, int inTotal)
	{
		BigDecimal now= new BigDecimal(inSoFar);
		BigDecimal total = new BigDecimal(inTotal);
		return getPercentage(now, total);
	}
	public float getPercentage(String inSoFar, String inTotal)
	{
		BigDecimal now=getBigDecimal(inSoFar);
		BigDecimal total = getBigDecimal(inTotal);
		return getPercentage(now, total);
	}

	public static double divide(double intop, double inbottom)
	{
		BigDecimal top = new BigDecimal(intop);
		BigDecimal bottom = new BigDecimal(inbottom);
		if( bottom.doubleValue() > 0)
		{
			BigDecimal percentage=top.divide(bottom,5,RoundingMode.HALF_UP);
			return percentage.doubleValue();
		}
		return 0;
		
	}
	public static String toString(double inD, int inDigits)
	{
		BigDecimal top = new BigDecimal(inD);
		BigDecimal result = top.divide(new BigDecimal(1d),inDigits,RoundingMode.HALF_UP);
		return result.toPlainString();
	}
	public static double roundDouble(double inD, int inDigits)
	{
		BigDecimal top = new BigDecimal(inD);
		BigDecimal result = top.divide(new BigDecimal(1d),inDigits,RoundingMode.HALF_UP);
		return result.doubleValue();
	}

	public static String toDuration(long inDuration)
	{
		//00:03:03.167
		//HOURS:MM:SS.MICROSECONDS
		long second = (inDuration / 1000) % 60;
		long minute = (inDuration / (1000 * 60)) % 60;
		long hour = (inDuration / (1000 * 60 * 60)) % 24;
		String millis = String.valueOf( inDuration );
		if( millis.length() > 3)
		{
			millis = millis.substring(millis.length() - 3);
		}
		else
		{
			millis = "000";
		}
		if( hour > 0)
		{
			String time = String.format("%02d:%02d:%02d", hour, minute, second);
			time = time + "." + millis;
			return time;
		}
		else
		{
			String time = String.format("%02d:%02d", minute, second);
			time = time + "." + millis;
			return time;
		}
	}
	
	public static double parseDuration(String inSeconds)
	{
		if( inSeconds == null)
		{
			return 0;
		}
		if( inSeconds.contains("s") )
		{
			inSeconds = inSeconds.split("\\.")[0];
		}
		else
		{
			String[] parts = inSeconds.split(":");
			if( parts.length == 1)
			{
				return Double.parseDouble(parts[0]);
			}
			if( parts.length == 2)
			{
				double totals = 60L * Double.parseDouble(parts[0]);
				totals = totals +  Double.parseDouble(parts[1]);
				return totals;
			}	
			if( parts.length == 3)
			{
				double totals =  60L * 60L * Double.parseDouble(parts[0]);				
				totals = totals +  60L * Double.parseDouble(parts[1]);
				totals = totals +  Double.parseDouble(parts[2]);
				return totals;
			}	
			throw new OpenEditException("Could not parse " + inSeconds);
		}
		return  Double.parseDouble(inSeconds);
	}
	public static void cleanTypes(Map inMap)
	{
		Collection keys = new ArrayList(inMap.keySet());
		for (Iterator iterator = keys.iterator(); iterator.hasNext();)
		{
			String type = (String) iterator.next();
			Object m = inMap.get(type);
			if( m instanceof BigDecimal)
			{
				inMap.put(type, ((BigDecimal)m).doubleValue() );
			}
		}
	}
	
}
