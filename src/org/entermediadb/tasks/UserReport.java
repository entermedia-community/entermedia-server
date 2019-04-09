package org.entermediadb.tasks;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openedit.Data;
import org.openedit.MultiValued;

public class UserReport
{
	List points = new ArrayList();
	public List getPoints()
	{
		return points;
	}
	public void setPoints(List inPoints)
	{
		points = inPoints;
	}
	public List getTickets()
	{
		return tickets;
	}
	public void setTickets(List inTickets)
	{
		tickets = inTickets;
	}
	List tickets = new ArrayList();
	
	public void addTicket(Data inTicket)
	{
		tickets.add(inTicket);
	}
	public void addTask(Data inTicket)
	{
		points.add(inTicket);
	}
	
	public Collection getTicketsForWeek(int inweek)
	{
		List tickets = new ArrayList();
		for (Iterator iterator = getTickets().iterator(); iterator.hasNext();)
		{
			MultiValued ticket = (MultiValued) iterator.next();
			Date completedon = ticket.getDate("resolveddate");
			GregorianCalendar completedweek = new GregorianCalendar();
			completedweek.setTime(completedon);
			int week = completedweek.get(Calendar.WEEK_OF_YEAR);
			if( week == inweek)
			{
				tickets.add(ticket);
			}
		}
		Collections.sort(tickets,new Comparator<MultiValued>()
		{
			@Override
			public int compare(MultiValued inO1, MultiValued inO2)
			{
				Date completedon = inO1.getDate("resolveddate");
				Date completedon2 = inO2.getDate("resolveddate");
				return completedon.compareTo(completedon2);
			}
		});
		return tickets;
	}

	public Collection getTasksForWeek(int inWeek)
	{
		List weeksworth = new ArrayList();
		for (Iterator iterator = points.iterator(); iterator.hasNext();)
		{
			MultiValued task = (MultiValued) iterator.next();
			Date completedon = task.getDate("completedon");
			GregorianCalendar completedweek = new GregorianCalendar();
			completedweek.setTime(completedon);
			int week = completedweek.get(Calendar.WEEK_OF_YEAR);
			if( week == inWeek)
			{
				weeksworth.add(task);
			}
		}
		Collections.sort(weeksworth);
		return weeksworth;
	}
	
}
