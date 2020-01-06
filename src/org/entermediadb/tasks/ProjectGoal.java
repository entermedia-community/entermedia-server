package org.entermediadb.tasks;

import java.util.Date;

import org.entermediadb.asset.util.MathUtils;
import org.openedit.data.BaseData;

public class ProjectGoal extends BaseData implements Comparable
{
	public String getAge()
	{
		Date createdon = getDate("creationdate");
		if( createdon == null)
		{
			return null;
		}
		//MathUtils util = new MathUtils();
		long diff = System.currentTimeMillis() - createdon.getTime();
		
		long minute = (diff / (1000 * 60)) % 60;
		long hour = (diff / (1000 * 60 * 60));
		String time = null;
		if( hour > 24)
		{
			double days = (double)hour / 24d;
			hour = hour % 24;
			time = String.format("%2dd:%2dh:%2dm", (int)days,hour, minute);
		}
		else
		{
			time = String.format("%2dh:%02dm", hour, minute);
		}
		return time;
	}
	public int compareTo(Object inO2)
	{
		ProjectGoal pg2 = (ProjectGoal)inO2;
		
		String status1 = get("projectstatus");
		String status2 = pg2.get("projectstatus");
		if( status1 == null) status1 = "open";
		if( status2 == null) status2 = "open";

		if( status1.equals(status2))
		{
			Date date1 = (Date)getDate("creationdate");
			Date date2 = (Date)pg2.getDate("creationdate");
			if( date1 != null && date2 != null)
			{
				return date1.compareTo(date2);
			}
		}
		else if( status1.equals("critical"))
		{
			return 1;
		}
		else if( status2.equals("critical"))
		{
			return -1;
		}
		else if( status1.equals("open"))
		{
			return 1;
		}
		
		return  status1.compareTo(status2); //alfa
	}
}
