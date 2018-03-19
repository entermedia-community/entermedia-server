package org.entermediadb.asset.util;

import java.util.Date;

public class TimeCalculator
{
	public Time findTimeFrom(Date inNow)
	{
		long thetime = inNow.getTime() - System.currentTimeMillis();
		Time time = new Time();
		if( thetime < 0)
		{
			time.setBefore(true);
		}
		long abs = Math.abs(thetime);
		long oneyear = 1000L*60L*60L*24L*365L;
		long oneday = 1000L*60L*60L*24L;
		if( abs > oneyear )
		{
			time.setType("y");
			float round = (float)abs/(float)(1000*60*60*24*365);
			time.setValue(round);
		}
		else if( abs > oneday)
		{
			time.setType("d");
			float round = (float)abs/(float)(1000*60*60*24);
			time.setValue(round);
		}
		else if( abs > 1000*60*60)
		{
			time.setType("h");
			float round = (float)abs/(float)(1000*60*60);
			time.setValue(round);
		}
		else if( abs > 1000*60)
		{
			time.setType("m");
			float round = (float)abs/(float)(1000*60);
			time.setValue(round);
		}
		else if( abs > 1000)
		{
			time.setType("s");
			float round = (float)abs/(float)(1000);
			time.setValue(round);
		}
		else
		{
			time.setType("milliseconds");
			time.setValue(abs);
		}
		return time;
	}
}
