package org.entermediadb.asset.util;

import java.time.Duration;

public class TimeParser {

	
	
	public  Duration parseDuration(String expr) {
	    try {
	        return Duration.parse(expr); // supports ISO-8601
	    } catch (Exception ignore) {
	        long millis = new TimeParser().parse(expr); // fallback
	        return Duration.ofMillis(millis);
	    }
	}
	
	
	public long parse(String inPeriodString) 
	{
		inPeriodString = inPeriodString.trim();

		if (inPeriodString.endsWith("M"))
		{
			long months = Long.parseLong(inPeriodString.substring(0, inPeriodString.length() - 1));
			long period = months * 30 * 24 * 60L * 60L * 1000L;
			return period;
		}
		else if (inPeriodString.endsWith("d"))
		{
			long days = Long.parseLong(inPeriodString.substring(0, inPeriodString.length() - 1));
			long period = days * 24 * 60L * 60L * 1000L;
			return period;
		}
		else if (inPeriodString.endsWith("h"))
		{
			long hours = Long.parseLong(inPeriodString.substring(0, inPeriodString.length() - 1));
			long period = hours * 60L * 60L * 1000L;
			return period;
		}
		else if (inPeriodString.endsWith("m"))
		{
			long min = Long.parseLong(inPeriodString.substring(0, inPeriodString.length() - 1));
			long period = min * 60L * 1000L;
			return period;
		}
		else if (inPeriodString.endsWith("s"))
		{
			long sec = Long.parseLong(inPeriodString.substring(0, inPeriodString.length() - 1));
			long period = sec * 1000L;
			return period;
		}
		else
		{
			long period = Long.parseLong(inPeriodString);
			return period;
		}
	}
}
