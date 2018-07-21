package org.entermediadb.tasks;

import java.util.Date;

import org.entermediadb.asset.util.MathUtils;
import org.openedit.data.BaseData;

public class ProjectGoal extends BaseData
{
	public String getAge()
	{
		Date createdon = getDate("creationdate");
		if( createdon == null)
		{
			return null;
		}
		MathUtils util = new MathUtils();
		long diff = System.currentTimeMillis() - createdon.getTime();
		
		long minute = (diff / (1000 * 60)) % 60;
		long hour = (diff / (1000 * 60 * 60));
		String time = String.format("%02d:%02d", hour, minute);
		return time;
	}
	public int compareTo(Object inO2)
	{
		ProjectGoal pg2 = (ProjectGoal)inO2;
		Date date1 = (Date)getDate("creationdate");
		Date date2 = (Date)pg2.getDate("creationdate");
		if( date1 != null && date2 != null)
		{
			return date2.compareTo(date1);
		}
		return 0;
	}
}
