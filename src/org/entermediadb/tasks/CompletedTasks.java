package org.entermediadb.tasks;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.users.User;

public class CompletedTasks
{
	Map<String,UserReport> byUserUserReport = new HashMap<String,UserReport>();
	
	public List getUserIds()
	{
		ArrayList users = new ArrayList(byUserUserReport.keySet());
		Collections.sort(users);
		return users;
	}
	
	public List weeksInMonth(Date month)
	{
		GregorianCalendar completedweek = new GregorianCalendar();
		completedweek.setTime(month);
		int max = completedweek.getActualMaximum(Calendar.WEEK_OF_MONTH);
		int week = completedweek.get(Calendar.WEEK_OF_YEAR);
		List weeks = new ArrayList();
		for (int i = 0; i < max; i++)
		{
			weeks.add(week + i);
		}
		return weeks;
	}

	
	public void addTask(String inUserId,MultiValued inTask)
	{
		if( inUserId == null)
		{
			return;
		}

		UserReport report = byUserUserReport.get(inUserId);
		if( report == null) {
			report = new UserReport();
			byUserUserReport.put(inUserId, report);
		}
		report.addTask(inTask);
				
	}
	public void addRole(Map<String,Object> inRole,Data inTask, MultiValued inRoleAction)
	{
		String inUserId = (String)inRole.get("roleuserid");
		if( inUserId == null)
		{
			return;
		}
		UserReport report = byUserUserReport.get(inUserId);
		if( report == null) {
			report = new UserReport();
			byUserUserReport.put(inUserId, report);
		}
		
		report.addUserRole(inTask,inRole, inRoleAction);
				
	}

	
	public void addTicket(String inUserId,MultiValued inTicket)
	{
		if( inUserId == null)
		{
			return;
		}
		UserReport report = byUserUserReport.get(inUserId);
		if( report == null) {
			report = new UserReport();
			byUserUserReport.put(inUserId, report);
		}
		report.addTicket(inTicket);
	}
	
	public Collection getTasksForWeek(User inUser, int inWeek)
	{
		UserReport report = byUserUserReport.get(inUser.getId());
		return report.getTasksForWeek(inWeek);
	}

	public Collection getTicketsForWeek(User inUser, int inWeek)
	{
		UserReport report = byUserUserReport.get(inUser.getId());
		return report.getTicketsForWeek(inWeek);
	}

	public Collection getRoleActionsForWeek(User inUser, int inWeek)
	{
		UserReport report = byUserUserReport.get(inUser.getId());
		Collection hits = report.getRoleActionsForWeek(inWeek);
		return hits;
	}


}
