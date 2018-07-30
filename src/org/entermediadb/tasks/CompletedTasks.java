package org.entermediadb.tasks;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openedit.MultiValued;

public class CompletedTasks
{
	public CompletedTasks()
	{
		// TODO Auto-generated constructor stub
		fieldCompleted = new ArrayList();
	}
	public CompletedTasks(List inCompleted)
	{
		fieldCompleted = inCompleted;
	}
	protected List fieldCompleted;
	
	public Collection byWeeks()
	{
		Map weeks = new HashMap();
		for (Iterator iterator = fieldCompleted.iterator(); iterator.hasNext();)
		{
			MultiValued task = (MultiValued) iterator.next();
			Date completedon = task.getDate("completedon");
			GregorianCalendar completedweek = new GregorianCalendar();
			completedweek.setTime(completedon);
			int week = completedweek.get(Calendar.WEEK_OF_YEAR);
			Collection saved = (Collection)weeks.get(week);
			if( saved == null)
			{
				saved = new ArrayList();
				weeks.put(week, saved);
			}
			saved.add(task);
		}
		List byweeks = new ArrayList(weeks.keySet());
		Collections.sort(byweeks);
		List sorted = new ArrayList();
		for (Iterator iterator = byweeks.iterator(); iterator.hasNext();)
		{
			Integer weekcount = (Integer) iterator.next();
			List tasks = (List)weeks.get(weekcount);
			Collections.reverse(tasks);
			sorted.add(tasks);
		}
		return sorted;
	
	}
	public void addTask(MultiValued inTask)
	{
		fieldCompleted.add(inTask);
	}
	
}
