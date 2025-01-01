package org.entermediadb.tasks;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.util.DateStorageUtil;

public class UserReport
{
	List points = new ArrayList();
	List tickets = new ArrayList();
	Map roleactions = new HashMap();
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
	
	public void addTicket(Data inTicket)
	{
		tickets.add(inTicket);
	}
	public void addTask(Data inTicket)
	{
		points.add(inTicket);
	}
	public void addUserRole(Data inTask,Map inUserRole,Data inRoleAction)
	{
		String roleid = (String)inUserRole.get("collectiverole");
		String taskid = (String)inUserRole.get("taskid");
		UserRoleWithActions userrole = (UserRoleWithActions)roleactions.get(taskid + roleid);
		if( userrole == null)
		{
			userrole = new UserRoleWithActions();
			userrole.setUserRole(inUserRole);
		}
		userrole.addRoleAction(inRoleAction);
		roleactions.put(taskid + roleid,userrole);
	}
	
	public Collection getTicketsForWeek(int inweek)
	{
		List tickets = new ArrayList();
		for (Iterator iterator = getTickets().iterator(); iterator.hasNext();)
		{
			MultiValued ticket = (MultiValued) iterator.next();
			Date completedon = ticket.getDate("resolveddate");
			Calendar completedweek = DateStorageUtil.getStorageUtil().createCalendar();
			completedweek.setTime(completedon);
			int week = completedweek.get(Calendar.WEEK_OF_MONTH);
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
	public Collection getRoleActionsForWeek(int inweek)
	{
		List weeklyroleactions = new ArrayList<UserRoleWithAction>();
		
		
		for (Iterator iterator = roleactions.values().iterator(); iterator.hasNext();)
		{
			UserRoleWithActions rolewithactions = (UserRoleWithActions) iterator.next();
			for (Iterator iterator2 = rolewithactions.getUserActions().iterator(); iterator2.hasNext();)
			{
				MultiValued action = (MultiValued)iterator2.next();
				Date completedon = action.getDate("date");
				Calendar completedweek = DateStorageUtil.getStorageUtil().createCalendar();
				completedweek.setTime(completedon);
				int week = completedweek.get(Calendar.WEEK_OF_MONTH);
				if( week == inweek)
				{
					UserRoleWithAction oneaction = new UserRoleWithAction();
					oneaction.setUserRole(rolewithactions.getUserRole());
					oneaction.setRoleAction(action);
					weeklyroleactions.add(oneaction);
				}
			}
		}
		Collections.sort(weeklyroleactions,new Comparator<UserRoleWithAction>()
		{
			@Override
			public int compare(UserRoleWithAction inO1, UserRoleWithAction inO2)
			{
				Date completedon = inO1.getDate();
				Date completedon2 = inO2.getDate();
				return completedon.compareTo(completedon2);
			}
		});
		return weeklyroleactions;
	}

	public Collection getTasksForWeek(int inWeek)
	{
		List weeksworth = new ArrayList();
		for (Iterator iterator = points.iterator(); iterator.hasNext();)
		{
			MultiValued task = (MultiValued) iterator.next();
			Date completedon = task.getDate("completedon");
			Calendar completedweek = DateStorageUtil.getStorageUtil().createCalendar();
			completedweek.setTime(completedon);
			
			int weekmonth = completedweek.get(Calendar.WEEK_OF_MONTH);
			if (weekmonth==1) { 
				completedweek.setMinimalDaysInFirstWeek(1);
			}
			else {
				completedweek.setMinimalDaysInFirstWeek(7);
			}
			
			int week = completedweek.get(Calendar.WEEK_OF_MONTH);
			if( week == inWeek)
			{
				weeksworth.add(task);
			}
		}
		Collections.sort(weeksworth);
		return weeksworth;
	}
	
}
