package org.entermediadb.tasks;

import java.util.Comparator;
import java.util.Date;

import org.openedit.MultiValued;

public class GoalSorter implements Comparator<MultiValued>
{
	@Override
	public int compare(MultiValued inO1, MultiValued inO2)
	{
		Date date1 = (Date)inO1.getDate("creationdate");
		Date date2 = (Date)inO2.getDate("creationdate");
		if( date1 != null && date2 != null)
		{
			return date1.compareTo(date2);
		}
		return 0;
	}
}
