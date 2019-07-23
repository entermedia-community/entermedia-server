package org.entermediadb.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.asset.util.MathUtils;
import org.entermediadb.projects.LibraryCollection;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.WebPageRequest;
import org.openedit.data.QueryBuilder;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.users.Group;
import org.openedit.users.User;

public class TaskModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(TaskModule.class);

	public void createTree(WebPageRequest inReq) throws Exception
	{
		//Make sure the tree root exists
		MediaArchive archive = getMediaArchive(inReq);
		LibraryCollection collection = (LibraryCollection)inReq.getPageValue("librarycol");
		if( collection == null)
		{
			log.info("Collection not found");
			return;
		}
		String id = "tasks" + collection.getId();
		Category cat = archive.getCategory(id);
		if( cat == null)
		{
			cat = (Category)archive.getCategorySearcher().createNewData();
			cat.setId(id);
			collection.getCategory().addChild(cat);
			cat.setName("Inbox");
			archive.getCategorySearcher().saveData(cat);
		}
	}
	
	public void searchGoals(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		Searcher searcher = archive.getSearcher("projectgoal");
		LibraryCollection collection = (LibraryCollection)inReq.getPageValue("librarycol");
		if( collection == null)
		{
			log.info("Collection not found");
			return;
		}
		
		HitTracker all = (HitTracker)inReq.getPageValue("goalhits");
		
		if( all == null)
		{
			SearchQuery userq = searcher.addStandardSearchTerms(inReq);
			if( userq == null) 
			{
				QueryBuilder builder = searcher.query().enduser(true).hitsPerPage(500).exact("collectionid", collection.getId());
				builder.orgroup("projectstatus", Arrays.asList("open","critical"));
				userq = builder.getQuery();
				Collection filter = inReq.getUserProfile().getValues("goaltrackercolumns");
				if( filter != null && !filter.isEmpty())
				{
					userq.addOrsGroup("goaltrackercolumn", filter);
				}
			}
			all = searcher.cachedSearch(inReq, userq);
		}
		if( all == null)
		{
			return;
		}
		Map results = sortIntoColumns(inReq, archive, all);
		
		inReq.putPageValue("goalhits", all); 
		List keys = new ArrayList(results.keySet());
		Collections.sort(keys);
		inReq.putPageValue("projectids", keys); 
		
	}

	private Map sortIntoColumns(WebPageRequest inReq, MediaArchive archive, HitTracker all)
	{
		Searcher searcher = archive.getSearcher("projectgoal");

		Map priorities = new HashMap();
		for (Iterator iterator = all.iterator(); iterator.hasNext();)
		{
			Data hit = (Data) iterator.next();
			ProjectGoal goal = (ProjectGoal)searcher.loadData(hit);
			String p = goal.get("goaltrackercolumn");
			if( p == null)
			{
				p = "0";
			}
			List list = (List)priorities.get(p);
			if( list == null)
			{
				list = new ArrayList();
				priorities.put(p,list);
			}
			if( list.size() > 99)
			{
				inReq.putPageValue("toomanygoals", true);
			}
			else
			{
				list.add(goal);
			}
		}
		
		for (Iterator iterator = priorities.keySet().iterator(); iterator.hasNext();)
		{
			String p = (String) iterator.next();
			List values = (List)priorities.get(p);
			Collections.sort(values);
			inReq.putPageValue("goalhits" + p, values);
		}
		return priorities;
		
	}	
	protected Map sortIntoPriorities(WebPageRequest inReq, MediaArchive archive, HitTracker all)
	{
		Searcher searcher = archive.getSearcher("projectgoal");

		Map priorities = new HashMap();
		for (Iterator iterator = all.iterator(); iterator.hasNext();)
		{
			Data hit = (Data) iterator.next();
			ProjectGoal goal = (ProjectGoal)searcher.loadData(hit);
			Collection likes = goal.getValues("userlikes");
			for (Iterator iterator2 = likes.iterator(); iterator2.hasNext();)
			{
				String userlike = (String) iterator2.next();
				List values = (List)priorities.get(userlike);
				if( values == null)
				{
					values = new ArrayList();
					priorities.put(userlike,values);
				}
				values.add(goal);
			}
		}
		
		for (Iterator iterator = priorities.keySet().iterator(); iterator.hasNext();)
		{
			String p = (String) iterator.next();
			List values = (List)priorities.get(p);
			Collections.sort(values);
			inReq.putPageValue("goalhits" + p, values);
		}
		return priorities;
	}	
	
	protected List sortIntoDates(WebPageRequest inReq, MediaArchive archive, Collection all, GregorianCalendar thismonday, int dayofweek, String collectionid)
	{
		List week = new ArrayList();
		Date today = new Date();
		for (int i = 0; i < 5; i++) //5 days
		{
			List todaysgoals = new ArrayList();
			week.add(todaysgoals);
			for (Iterator iteratorv = all.iterator(); iteratorv.hasNext();)
			{
				MultiValued goal = (MultiValued) iteratorv.next();
				Date rd = goal.getDate("resolveddate");
				if( rd != null && today.after(rd) && "open".equals( goal.get("projectstatus") ) )
				{
					goal.setValue("projectstatus", "critical");
				}

				if( rd == null )
				{
					if( dayofweek == i )
					{
						todaysgoals.add(goal);
					}
				}
				else
				{
					GregorianCalendar rg = new GregorianCalendar();
					rg.setTime(rd);
					int dow = rg.get(Calendar.DAY_OF_WEEK) - 2;
					if( dow == i)
					{
						todaysgoals.add(goal);
					}
				}
			}
		}	
		for (Iterator iterator = week.iterator(); iterator.hasNext();)
		{
			List values = (List ) iterator.next();
			Collections.sort(values, new Comparator<ProjectGoal>()
			{
				@Override
				public int compare(ProjectGoal pg1, ProjectGoal pg2)
				{
						String status1 = pg1.get("projectstatus");
						String status2 = pg2.get("projectstatus");
						if( status1 == null) status1 = "open";
						if( status2 == null) status2 = "open";
						if( status1.equals(status2))
						{
							Date date1 = (Date)pg1.getDate("creationdate");
							Date date2 = (Date)pg2.getDate("creationdate");
							if( date1 != null && date2 != null)
							{
								return date2.compareTo(date1);
							}
						}
						else if( status1.equals("active"))
						{
							return 1;
						}
						else if( status2.equals("active"))
						{
							return -1;
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
							return -1;
						}
						else if( status2.equals("open"))
						{
							return 0;
						}
						
						return 0;
					}
			});
			Collections.reverse(values);
		}
		return week;
	}
	/*
	public void loadGoals(WebPageRequest inReq) throws Exception
	{
		//Each category points to a bunch of stories (sorted)
		//search stories that contain a department
		//First load all stories for root category
		MediaArchive archive = getMediaArchive(inReq);
		Searcher searcher = archive.getSearcher("projectgoal");
		LibraryCollection collection = (LibraryCollection)inReq.getPageValue("librarycol");
		if( collection == null)
		{
			log.info("Collection not found");
			return;
		}
	
		String department = inReq.getRequestParameter("nodeID");
		if(department == null)
		{
			department = inReq.getRequestParameter("categoryid");
		}
		if( department == null)
		{
			department = "tasks" + collection.getId();
		}
		Collection topgoals = null;
		String page = inReq.getRequestParameter("page");

		Category selected = archive.getCategory(department);
		inReq.putPageValue("selectedcat", selected);

		Collection alltasks = archive.getSearcher("goaltask").query().exact("projectdepartment",department).search();

		Collection projects = archive.getSearcher("collectiveproject").query().exact("parentcollectionid",collection.getId()).search();
		
		topgoals = makeColumns(list.getSorted(),percolumn,startfrom);

		Collection goalids = selected.getValues("countdata");
		if ( goalids == null)
		{
			goalids = Collections.emptyList();
		}
		if( opengoals.size() != goalids.size() )
		{
			Collection ids = new ArrayList();
			for (Iterator iterator = opengoals.iterator(); iterator.hasNext();)
			{
				Data goal = (Data) iterator.next();
				ids.add(goal.getId());
			}
			selected.setValue("countdata",ids);
			archive.getCategorySearcher().saveCategory(selected);
		}
		
		inReq.putPageValue("topgoals", topgoals);
		if( opengoals != null && opengoals.size() > (thispage + 1) * perpage )
		{
			inReq.putPageValue("nextpage", thispage + 1);
		}
		
		Collection archived = searcher.query().orgroup("projectstatus", Arrays.asList("closed","completed"))
				.ids(allgoalsids).exact("collectionid", collection.getId()).search(inReq);
		inReq.putPageValue("closedgoals", archived);
		StringBuffer out = new  StringBuffer();
		for (Iterator iterator = archived.iterator(); iterator.hasNext();)
		{
			Data goal = (Data) iterator.next();
			out.append(goal.getId());
			if( iterator.hasNext())
			{
				out.append("|");
			}
		}
		inReq.putPageValue("closedids",out.toString());
		
		
	}
	*/

//	private Collection findRemovedGoals(HitTracker inGoaltracker, Collection<String> inAllgoalsids)
//	{
//		List goals = new ArrayList();
//		for (Iterator iterator = inGoaltracker.iterator(); iterator.hasNext();)
//		{
//			Data goal = (Data) iterator.next();
//			if( !inAllgoalsids.contains( goal.getId()))
//			{
//				goals.add(goal);
//			}
//		}
//		return goals;
//	}



//	protected Map makeColumns(Collection tracker, int percolumn, int startfrom)
//	{
//		Map topgoals = new HashMap();
//		for (Iterator iterator = tracker.iterator(); iterator.hasNext();)
//		{
//			Data goal = (Data)iterator.next();
//			Collection goals = getGoals(topgoals,goal);
//			goals.add(goal);
//		}
//		return topgoals;
//	}
	protected Collection getGoals(Map topgoals, Data inRow)
	{
		String col = inRow.get("goaltrackercolumn");
		if( col == null)
		{
			col = "0";
		}
		Collection goals = (Collection)topgoals.get(col);
		if( goals == null)
		{
			goals = new ArrayList();
			topgoals.put(col, goals);
		}
		return goals;
	}
	public void loadGoal(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		MultiValued goal = (MultiValued)inReq.getPageValue("goal");
		if( goal == null)
		{
			String goalid = inReq.getRequestParameter("goalid");
			if(goalid == null)
			{
				goalid = inReq.getRequestParameter("id");
			}
			if (goalid != null) 
			{
				goal = (MultiValued)archive.getData("projectgoal",goalid);
				inReq.putPageValue("data", goal);
				inReq.putPageValue("selectedgoal", goal);
				inReq.putPageValue("goal", goal);
			}
		}
			
		Searcher tasksearcher = (Searcher)archive.getSearcher("goaltask");
		
		//		#set( $tasks = $mediaarchive.getSearcher("goaltask").query().exact("projectgoal", $goal.getId()).not("taskstatus","complete").search() )
		if( goal != null)
		{
			HitTracker tasks = tasksearcher.query().exact("projectgoal", goal.getId()).search();
			//Legacy: Make sure all tasks have parents
			List tosave = new ArrayList();
			Collection values = goal.getValues("countdata");
			if( values == null)
			{
				values = new ArrayList();
			}
			Collection alltaskids = new ArrayList(values);
			Collection extrataskids = new ArrayList(values);
			for (Iterator iterator = tasks.iterator(); iterator.hasNext();)
			{
				MultiValued existigtask = (MultiValued) iterator.next();
				if( existigtask.getValue("projectdepartmentparents") == null)
				{
					Category child = archive.getCategory(existigtask.get("projectdepartment"));
					if( child != null)
					{
						existigtask.setValue("projectdepartmentparents",child.getParentCategories());
						tosave.add(existigtask);
					}
				}
				extrataskids.remove(existigtask.getId());
				if(!alltaskids.contains(existigtask.getId()))
				{
					alltaskids.add(existigtask.getId());
				}
			}
			alltaskids.removeAll(extrataskids);
			if( !alltaskids.equals(values))
			{
				goal.setValue("countdata",alltaskids);
				archive.saveData("projectgoal", goal);
			}
			
			tasksearcher.saveAllData(tosave, null);
			TaskList goaltasks = new TaskList(goal,tasks);
			inReq.putPageValue("tasklist", goaltasks);
			inReq.putPageValue("tasks", goaltasks.getSortedTasks());
			if( goal.getValue("projectstatus") == null)
			{
				goal.setValue("projectstatus","open");
				archive.saveData("projectgoal", goal);
			}
		}	
	}
	
	public void checkGoalCount(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		MultiValued goal = (MultiValued)inReq.getPageValue("data");
		String projectstatus = goal.get("projectstatus");
		//search for goals
		Collection tasks = archive.query("goaltask").exact("projectgoal", goal.getId()).search();
		for (Iterator iterator = tasks.iterator(); iterator.hasNext();)
		{
			Data task = (Data) iterator.next();
			Category cat = archive.getCategory( task.get("projectdepartment") );
			if( cat != null)
			{
				List goalids = (List)cat.getValues("countdata");
				if( goalids == null)
				{
					goalids = new ArrayList();
				}
				else
				{
					goalids = new ArrayList(goalids);
				}
				boolean changed = false;
				if( goalids.contains(goal.getId()))
				{
					if( projectstatus.equals("completed") || projectstatus.equals("closed") )
					{
						goalids.remove(goal.getId());
						changed = true;
					}
				}
				else if( !(projectstatus.equals("completed") || projectstatus.equals("closed") ) )
				{
					changed = true;
					//Add to the front
					if( goalids.isEmpty() )
					{
						goalids.add(goal.getId());
					}
					else
					{
						goalids.add(0,goal.getId());
					}
				}
				if( changed )
				{
					cat.setValue("countdata",goalids);
					archive.getCategorySearcher().saveData(cat);
				}
			}
		}
	}
	
	public void addGoalToCategory(WebPageRequest inReq)
	{
		String goalid = inReq.getRequestParameter("goalid");
		if( goalid== null)
		{
			goalid = inReq.getRequestParameter("id");
		}
		if( goalid == null)
		{
			log.error("No goals");
			return;
		}
		String categoryid = inReq.getRequestParameter("targetcategoryid");
		if( categoryid == null)
		{
			categoryid = inReq.getRequestParameter("categoryid");
		}
		if( categoryid == null)
		{
			return;
		}
		MediaArchive archive = getMediaArchive(inReq);
		Searcher goalsearcher = archive.getSearcher("projectgoal");
		Data goal = (Data)goalsearcher.searchById(goalid);
		Category cat = archive.getCategory(categoryid);
		//Make goaltask
		Searcher tasksearcher = (Searcher)archive.getSearcher("goaltask");
		
		Data task = tasksearcher.createNewData(); //TODO: Cjheck for existing
		task.setValue("projectgoal",goal.getId());
		task.setValue("projectdepartment",categoryid);
		
		task.setValue("projectdepartmentparents",cat.getParentCategories());
		
		task.setValue("creationdate",new Date());
		
		task.setName(cat.getName()); //TODO: Support comments
		
		List goalids = (List)cat.getValues("countdata");
		if( goalids == null)
		{
			goalids = new ArrayList();
		}
		else
		{
			goalids = new ArrayList(goalids);
		}
		//if( !goalids.contains(goalid))
		if( goalids.isEmpty())
		{	
			goalids.add(goalid);
		}
		else
		{
			goalids.add(0,goalid); //Put in front?
		}
		cat.setValue("countdata",goalids);
		archive.getCategorySearcher().saveData(cat);
		//Add to array on category
		tasksearcher.saveData(task);
		addComment(archive, task.getId(), inReq.getUser(),"0",null);
		
		if( goal.getValue("projectstatus") == null)
		{
			goal.setValue("projectstatus","open");
			archive.saveData("projectgoal", goal);
		}
		
		inReq.putPageValue("goal", goal);
	}

	public void saveComment(WebPageRequest inReq)
	{
		String taskid = inReq.getRequestParameter("taskid");
		MediaArchive archive = getMediaArchive(inReq);
		String comment = inReq.getRequestParameter("comment");
		addComment(archive, taskid, inReq.getUser(),null,comment);
	}

	protected void addComment(MediaArchive archive, String taskid, User inUser, String taskstatus, String inComment)
	{
		Searcher commentsearcher = archive.getSearcher("goaltaskcomments");
		Data newcomment = commentsearcher.createNewData();
		newcomment.setValue("goaltaskid", taskid);
		newcomment.setValue("commenttext", inComment);
		newcomment.setValue("author", inUser.getId());
		newcomment.setValue("date", new Date());
		newcomment.setValue("date", new Date());
		newcomment.setValue("statuschange", taskstatus);
		
		commentsearcher.saveData(newcomment);
	}
	public void saveTaskStatus(WebPageRequest inReq)
	{
		String taskid = inReq.getRequestParameter("taskid");
		MediaArchive archive = getMediaArchive(inReq);
		Searcher tasksearcher = archive.getSearcher("goaltask");
		Data task = (Data)tasksearcher.searchById(taskid);
		String taskstatus = inReq.getRequestParameter("taskstatus");
		task.setValue("taskstatus", taskstatus);

		if(taskstatus != null && taskstatus.equals("3"))
		{
			if( task.getValue("completedon") == null )
			{
				task.setValue("completedon", new Date());
			}
			if( task.getValue("completedby") == null )
			{
				task.setValue("completedby", inReq.getUserName());
			}

			//remove task from tree
			removeCount(archive, task);
			
		}
		else if( taskstatus != null && taskstatus.equals("1"))
		{
			task.setValue("startedby", inReq.getUserName());
			if( task.getValue("startedon") == null )
			{
				task.setValue("startedon", new Date());
			}	
		}
		String completedby = inReq.getRequestParameter("completedby");
		if( completedby != null)
		{
			task.setValue("completedby", completedby);
		}
		
		tasksearcher.saveData(task);	
		inReq.putPageValue("task", task);
		if( taskstatus == null)
		{
			taskstatus = "0";
		}
		addComment(archive, taskid, inReq.getUser(),taskstatus, null);

	}
	public void removeTask(WebPageRequest inReq)
	{
		String taskid = inReq.getRequestParameter("taskid");
		MediaArchive archive = getMediaArchive(inReq);
		Searcher tasksearcher = archive.getSearcher("goaltask");
		Data task = (Data)tasksearcher.searchById(taskid);
		tasksearcher.delete(task,null);
		removeCount(archive, task);
		
	}

	protected void removeCount(MediaArchive archive, Data task)
	{
		String cat = task.get("projectdepartment");
		Category folder = archive.getCategory(cat);
		ArrayList list = new ArrayList(folder.getValues("countdata"));
		list.remove(task.get("projectgoal"));
		folder.setValue("countdata",list);
		archive.getCategorySearcher().saveData(folder);
	}
	//Everything over 15
	public void searchClosedGoals(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		Searcher searcher = archive.getSearcher("projectgoal");
		LibraryCollection collection = (LibraryCollection)inReq.getPageValue("librarycol");
		if( collection == null)
		{
			log.info("Collection not found");
			return;
		}
	
		QueryBuilder builder = searcher.query().exact("collectionid", collection.getId());
		builder.orgroup("projectstatus", Arrays.asList("closed","completed"));
		String ids = inReq.getRequestParameter("ids");
		if( ids != null)
		{
			builder.orgroup("id", ids);
		}
		Collection archived = builder.search();
		inReq.putPageValue("closedgoals", archived);
		
	}
	
	public void insertGoal(WebPageRequest inReq)
	{
		String goalid = inReq.getRequestParameter("goalid");
		String targetgoalid = inReq.getRequestParameter("targetgoalid");
		String categoryid = inReq.getRequestParameter("categoryid");
		if( categoryid == null)
		{
			return;
		}
		MediaArchive archive = getMediaArchive(inReq);
		Category selected = archive.getCategory(categoryid);
		List goalids = (List)selected.getValues("countdata");
		if(goalids ==  null )
		{
			goalids = new ArrayList();
		}
		else
		{
			goalids = new ArrayList(goalids);
		}
		if(!goalids.contains(goalid))
		{
			goalids.add(goalid);
		}
		if(!goalids.contains(targetgoalid))
		{
			goalids.add(targetgoalid);
		}
		int location = goalids.indexOf(targetgoalid);
		goalids.remove(goalid);
		if( location == goalids.size())
		{
			goalids.add(goalid); //Put at the end
		}
		else
		{
			location = goalids.indexOf(targetgoalid);
			goalids.add(location, goalid);
		}
		selected.setValue("countdata",goalids);
		archive.getCategorySearcher().saveCategory(selected);
		log.info("saved goalids" + selected.getId() + "="+ goalids);

	}

	public void insertTask(WebPageRequest inReq)
	{
		String goalid = inReq.getRequestParameter("goalid");
		
		String taskid = inReq.getRequestParameter("taskid");
		String targettaskid = inReq.getRequestParameter("targettaskid");
		if( targettaskid == null)
		{
			return;
		}
		MediaArchive archive = getMediaArchive(inReq);
		MultiValued selectedgoal = (MultiValued)archive.getData("projectgoal",goalid);
		List taskids = (List)selectedgoal.getValues("countdata");
		if(taskids ==  null )
		{
			taskids = new ArrayList();
		}
		else
		{
			taskids = new ArrayList(taskids);
		}
		if(!taskids.contains(taskid))
		{
			taskids.add(taskid);
		}
		if(!taskids.contains(targettaskid))
		{
			taskids.add(targettaskid);
		}
		int targetlocation = taskids.indexOf(targettaskid);
		int oldlocation = taskids.indexOf(taskid);
		
		//moving to the right:
		taskids.remove(taskid);
		if( targetlocation < oldlocation)
		{
			if( targetlocation > taskids.size())
			{
				targetlocation = taskids.size();
			}
		}
		
		taskids.add(targetlocation, taskid);
		selectedgoal.setValue("countdata",taskids);
		archive.getSearcher("projectgoal").saveData(selectedgoal);
		log.info("saved taskids on goal" + selectedgoal.getId() + "="+ taskids);

	}

	public void loadDashboard(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		LibraryCollection collection = (LibraryCollection)inReq.getPageValue("librarycol");
		if( collection == null)
		{
			log.info("Collection not found");
			return;
		}
		//Search for all tasks with updated dates?
		GregorianCalendar cal = new GregorianCalendar();
		String monthsback = inReq.getRequestParameter("monthsback");
		if( monthsback == null)
		{
			monthsback = "0";
		}
		int count = Integer.parseInt(monthsback);
		cal.set(Calendar.DAY_OF_MONTH,1);
		cal.add(Calendar.MONTH, 0 - count);
		inReq.putPageValue("monthsback", count+1);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		inReq.putPageValue("since", cal.getTime());
		Searcher tasksearcher = archive.getSearcher("goaltask");
		Date start = cal.getTime();
		int days = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.add(Calendar.DAY_OF_MONTH,days - 1);
		
		Date onemonth = cal.getTime();

		String rootid = "tasks" + collection.getId();
		HitTracker all = tasksearcher.query().exact("projectdepartmentparents",rootid)
				.match("completedby", "*").between("completedon", start,onemonth).sort("completedonDown").search();
		log.info("Query: " + all.getSearchQuery());
		CompletedTasks completed = new CompletedTasks();
		for (Iterator iterator = all.iterator(); iterator.hasNext();)
		{
			MultiValued  task = (MultiValued) iterator.next();
			String userid = task.get("completedby");
			completed.addTask(userid,task);
		}
		
		inReq.putPageValue("completed", completed);		
		
		HitTracker alltickets = archive.query("projectgoal")
				.match("resolveusers", "*").between("resolveddate", start,onemonth).sort("resolveddateDown").search();		
		
		for (Iterator iterator = alltickets.iterator(); iterator.hasNext();)
		{
			MultiValued  ticket = (MultiValued) iterator.next();
			Collection users = ticket.getValues("resolveusers");
			if( users != null)
			{
				double each = MathUtils.divide(20, users.size());
				ticket.setValue("points",each);
				for (Iterator iterator2 = users.iterator(); iterator2.hasNext();)
				{
					String userid = (String) iterator2.next();
					completed.addTicket(userid,ticket);
				}
			}
		}
		ArrayList users = new ArrayList();

		for (Iterator iterator = completed.getUserIds().iterator(); iterator.hasNext();)
		{
			String userid = (String) iterator.next();
			User user = archive.getUser(userid);
			if( user != null)
			{
				users.add(user);
			}
		}
		Collections.sort(users);
		inReq.putPageValue("users", users);

	}
	
	public void loadTaskByStatus(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		LibraryCollection collection = (LibraryCollection)inReq.getPageValue("librarycol");
		if( collection == null)
		{
			log.info("Collection not found");
			return;
		}
		Searcher tasksearcher = archive.getSearcher("goaltask");
		
		
		String status = inReq.getRequestParameter("taskstatus");
		if( status == null)
		{
			status = "1";
		}
		String rootid = "tasks" + collection.getId();
		HitTracker all = tasksearcher.query().exact("taskstatus",status).exact("projectdepartmentparents",rootid ).search();
		Map byperson = new HashMap();
		for (Iterator iterator = all.iterator(); iterator.hasNext();)
		{
			Data  task = (Data ) iterator.next();
			String startedby = task.get("startedby");
			if( startedby == null)
			{
				startedby = "admin";
			}
			List completed = (List)byperson.get(startedby);
			if( completed == null)
			{
				completed = new ArrayList();
			}
			completed.add(task);
			byperson.put(startedby,completed);
		}
		
		inReq.putPageValue("byperson", byperson);

		ArrayList users = new ArrayList();
		for (Iterator iterator = byperson.keySet().iterator(); iterator.hasNext();)
		{
			String userid = (String) iterator.next();
			User user = archive.getUser(userid);
			if( user != null)
			{
				users.add(user);
			}
		}
		Collections.sort(users);
		inReq.putPageValue("users", users);

	}

	public void loadLikes(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		Searcher searcher = archive.getSearcher("projectgoal");
		LibraryCollection collection = (LibraryCollection)inReq.getPageValue("librarycol");
		if( collection == null)
		{
			log.info("Collection not found");
			return;
		}
	
		QueryBuilder builder = searcher.query().exact("collectionid", collection.getId()).hitsPerPage(500);
		builder.match("userlikes", "*").sort("owner").sort("userlikes");
		builder.notgroup("projectstatus", Arrays.asList("closed","completed"));
//		Collection filter = inReq.getUserProfile().getValues("goaltrackercolumns");
//		if( filter != null && !filter.isEmpty())
//		{
//			builder.orgroup("goaltrackercolumn", filter);
//		}

		HitTracker likes = builder.search();

		Map priorities = sortIntoPriorities(inReq, archive, likes);
		List users = new ArrayList();
		for (Iterator iterator = priorities.keySet().iterator(); iterator.hasNext();)
		{
			String userid = (String) iterator.next();
			User user = archive.getUser(userid);
			if( user != null)
			{
				users.add(user);
			}
		}
		Collections.sort(users);
		inReq.putPageValue("users", users);
	}
	
	public void toggleGoalLike(WebPageRequest inReq)
	{
		MultiValued goal = (MultiValued)inReq.getPageValue("goal");
		MediaArchive archive = getMediaArchive(inReq);
		Searcher searcher = archive.getSearcher("projectgoal");
		if( goal == null)
		{
			String goalid = inReq.getRequestParameter("goalid");
			goal = (MultiValued)searcher.searchById(goalid);
		}	
		Collection found = goal.getValues("userlikes");
		if( found == null)
		{
			found = new ArrayList();
		}
		else
		{
			found = new ArrayList(found);
		}
		if( found.contains(inReq.getUserName()))
		{
			found.remove(inReq.getUserName());
		}
		else
		{
			found.add(inReq.getUserName());
		}
		goal.setValue("userlikes",found);
		searcher.saveData(goal);
		inReq.putPageValue("goal", goal);

	}

	public void moveToColumn(WebPageRequest inReq)
	{
		String goalid = inReq.getRequestParameter("goalid");
		String col = inReq.getRequestParameter("col");
		MediaArchive archive = getMediaArchive(inReq);
		MultiValued selectedgoal = (MultiValued)archive.getData("projectgoal",goalid);
		selectedgoal.setValue("goaltrackercolumn",col);
		archive.saveData("projectgoal",selectedgoal);
		
	}

	public void loadTask(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String taskid = inReq.getRequestParameter("id");
		Data task = archive.getData("goaltask",taskid);
		if( task != null)
		{
			String goalid = task.get("projectgoal");
			
			Data goal =  archive.getData("projectgoal",goalid);
			inReq.putPageValue("selectedgoal", goal);
			inReq.putPageValue("task", task);
		}
		
	}		

	public void loadTickets(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		Searcher searcher = archive.getSearcher("projectgoal");
		LibraryCollection collection = (LibraryCollection)inReq.getPageValue("librarycol");
		if( collection == null)
		{
			log.info("Collection not found");
			return;
		}
	
		QueryBuilder builder = searcher.query().exact("collectionid", collection.getId());
		
		String seeuser = inReq.getRequestParameter("goaltrackerstaff");//inReq.getUserProfile().get("goaltrackerstaff");
		if( seeuser == null || seeuser.isEmpty())
		{
			seeuser = inReq.getUserName();
		}
		builder.match("userlikes", seeuser);
		builder.notgroup("projectstatus", Arrays.asList("closed","completed"));
		HitTracker likesopen = builder.search();
		//sort users by date?
		GregorianCalendar thismonday = new GregorianCalendar();
		thismonday.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		thismonday.set(Calendar.MINUTE, 0);
		thismonday.set(Calendar.HOUR, 0);
		builder = searcher.query().exact("collectionid", collection.getId());
		builder.match("userlikes", seeuser);
		builder.orgroup("projectstatus", Arrays.asList("closed","completed"));
		builder.after("resolveddate", thismonday.getTime());
		HitTracker likesclosed = builder.search();

		List all = new ArrayList();
		for (Iterator iterator = likesopen.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			all.add( searcher.loadData(data) );
		}
		for (Iterator iterator = likesclosed.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			all.add( searcher.loadData(data) );
		}
		//log.info(builder.getQuery().toQuery() + " " + likes.size());
		GregorianCalendar todayc = new GregorianCalendar();
		int selectedday1 = todayc.get(Calendar.DAY_OF_WEEK);
		int selectedday0 = selectedday1 - 2; //zero based  
		if( selectedday0 < 0 || selectedday0 > 5)
		{
			selectedday0 = 0; //monday
		}
		List week = sortIntoDates(inReq, archive, all, thismonday, selectedday0, collection.getId());
//		List types = new ArrayList();
//		for (Iterator iterator = tickets.keySet().iterator(); iterator.hasNext();)
//		{
//			String id = (String) iterator.next();
//			Data type = archive.getData("tickettype",id);
//			types.add(type);
//		}
//		Collections.sort(types);
		//inReq.putPageValue("tickets", tickets);
		//inReq.putPageValue("tickettypes", types);
		inReq.putPageValue("selectedday0", selectedday0);
		inReq.putPageValue("week", week);
		
		builder = searcher.query().exact("collectionid", collection.getId());
		builder.match("userlikes", "*");
		builder.notgroup("projectstatus", Arrays.asList("closed","completed"));
		int totalpriority = builder.search().size();
		
		builder = searcher.query().exact("collectionid", collection.getId());
		builder.notgroup("projectstatus", Arrays.asList("closed","completed"));
		int totalopen = builder.search().size();

		builder = searcher.query().exact("collectionid", collection.getId());
		builder.orgroup("projectstatus", Arrays.asList("closed","completed"));
		int totalclosed = builder.search().size();
			
		inReq.putPageValue("totallikes", totalpriority);
		inReq.putPageValue("totalopen", totalopen);
		inReq.putPageValue("totalclosed", totalclosed);
	
	}

	public void resolveTicket(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String goalid = inReq.getRequestParameter("goalid");
		MultiValued selectedgoal = (MultiValued)archive.getData("projectgoal",goalid);
		if( selectedgoal.getValue("resolveddate") == null)
		{
			selectedgoal.setValue("resolveddate",new Date());
		}
		Collection users = selectedgoal.getValues("userlikes");
		selectedgoal.setValue("resolveusers",users);
		selectedgoal.setValue("projectstatus","completed");
		
		archive.saveData("projectgoal",selectedgoal);
		addStatus(archive, selectedgoal,inReq.getUserName());

	}

	public void savedGoal(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String goalid = inReq.getRequestParameter("id");
		MultiValued selectedgoal = (MultiValued)archive.getData("projectgoal",goalid);

		addStatus(archive, selectedgoal,inReq.getUserName());
	}

	protected void addStatus(MediaArchive archive, MultiValued selectedgoal, String editedby)
	{
		
		Collection userids = new HashSet();
		Collection likes = selectedgoal.getValues("userlikes");
		if (likes != null) 
		{
			userids.addAll(likes);
		}
			String owner = selectedgoal.get("owner");
		if( owner != null)
		{
			userids.add(owner);
		}
		

		String collectionid = selectedgoal.get("collectionid");
		//Find all the users
		Collection team = archive.query("librarycollectionusers").
				exact("collectionid",collectionid).
				exact("ontheteam","true").search();

		for (Iterator iterator = team.iterator(); iterator.hasNext();)
		{
			Data follower = (Data)iterator.next();
			String userid = follower.get("followeruser");
			if( userid != null)
			{
				userids.add(userid);
			}
		}		
		
		Group agents = archive.getGroup("agents");
		if( agents != null)
		{
			Collection users = archive.getUserManager().getUsersInGroup(agents);
			for (Iterator iterator = users.iterator(); iterator.hasNext();)
			{
				User auser = (User) iterator.next();
				userids.add(auser.getId());
			}
		}
		
		Collection tosave = new ArrayList();
		
		for (Iterator iterator = userids.iterator(); iterator.hasNext();)
		{
			String userid = (String) iterator.next();
			MultiValued status = (MultiValued)archive.query("statuschanges").exact("goalid", selectedgoal.getId()).exact("userid", userid).searchOne();

			String existingstatus = (String)selectedgoal.get("projectstatus");
			if( status == null)
			{
				status = (MultiValued)archive.getSearcher("statuschanges").createNewData();
				status.setValue("goalid",selectedgoal.getId());
				status.setValue("userid",userid);
				status.setValue("previousstatus",existingstatus);
			}
			else
			{
//				String previous = status.get("previousstatus");
//				if( !existingstatus.equals(previous))
//				{
//					status.setValue("previousstatus",existingstatus);
					status.setValue("notified",false);
//				}
			}
			status.setValue("collectionid",collectionid);
			status.setValue("date",new Date());
			status.setValue("editedbyid",editedby);
			
			tosave.add(status);
		}
		archive.saveData("statuschanges", tosave);
	}
	public void clearNotify(WebPageRequest inReq)
	{
		QueryBuilder query = getMediaArchive(inReq).query("statuschanges").
				exact("userid", inReq.getUserName());
		
		String collectionid = inReq.getRequestParameter("collectionid");
		if( collectionid != null)
		{
			query.exact("collectionid",collectionid);
		}
		Collection results = query.search();

		Collection tosave = new ArrayList();
		for (Iterator iterator = results.iterator(); iterator.hasNext();)
		{
			Data status = (Data) iterator.next();
			status.setValue("notified",true);
			tosave.add(status);
		}
		getMediaArchive(inReq).getSearcher("statuschanges").saveAllData(tosave,null);
	}
	public void chatEvent(WebPageRequest inReq)
	{
		String collectionid = inReq.getRequestParameter("collectionid");
		String topic = inReq.getRequestParameter("channel");
		String content = inReq.getRequestParameter("content");

		MediaArchive archive = getMediaArchive(inReq);
		Searcher searcher = archive.getSearcher("projectgoal");
		MultiValued goal = (MultiValued)searcher.query().exact("goaltrackercolumn", topic).
			exact("tickettype", "chat").orgroup("projectstatus", "open|critical").searchOne();
		if( goal == null)
		{
			goal = (MultiValued)searcher.createNewData();
			goal.setValue("goaltrackercolumn", topic);
			goal.setValue("tickettype", "chat");
			goal.setValue("projectstatus", "open");
			goal.setValue("creationdate",new Date());
			goal.setValue("collectionid", collectionid);
			goal.setValue("owner", inReq.getUserName());
			if( content != null && content.length() > 200)
			{
				content = content.substring(0,100) + "...";
			}
			goal.setName("Chat: " + content);
			searcher.saveData(goal);
			addStatus(archive, goal,inReq.getUserName());
		}


	}
}
