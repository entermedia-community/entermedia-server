package org.entermediadb.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.projects.LibraryCollection;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.WebPageRequest;
import org.openedit.data.QueryBuilder;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
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
				QueryBuilder builder = searcher.query().enduser(true).hitsPerPage(100).exact("collectionid", collection.getId());
				builder.notgroup("projectstatus", Arrays.asList("closed","completed"));
				userq = builder.getQuery();
			}
//			else
//			{
//				userq.addChildQuery(builder.getQuery());
//			}
			all = searcher.cachedSearch(inReq, userq);
		}
		
		sortIntoColumns(inReq, archive, all);
		
		inReq.putPageValue("goalhits", all); //Not needed?
		
	}

	private void sortIntoColumns(WebPageRequest inReq, MediaArchive archive, HitTracker all)
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
	}	
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

		int percolumn = 5;
		int perpage = percolumn * 3;

		int thispage = 0;
		int startfrom = 0;
		if( page != null)
		{
			thispage = Integer.parseInt(page);
			startfrom = thispage * perpage;
		}
		Category selected = archive.getCategory(department);
		inReq.putPageValue("selectedcat", selected);

		Collection alltasks = archive.getSearcher("goaltask").query().exact("projectdepartment",department).search();
		alltasks.size();
		Collection opengoalsids = new ArrayList();
		Collection closedgoalids = new ArrayList();

		Collection<String> allgoalsids = new ArrayList<String>();

		for (Iterator iterator = alltasks.iterator(); iterator.hasNext();)
		{
			Data task = (Data) iterator.next();
			allgoalsids.add(task.get("projectgoal"));
		}
		for (Iterator iterator = alltasks.iterator(); iterator.hasNext();)
		{
			Data task = (Data) iterator.next();
			String goalid = task.get("projectgoal");
			if("3".equals( task.get("taskstatus")) )
			{
				closedgoalids.add(goalid);
			}
			else
			{
				opengoalsids.add(goalid);
			}
		}
		Collection opengoals = null;
		if(! opengoalsids.isEmpty() )
		{
			QueryBuilder builder = searcher.query().exact("collectionid", collection.getId());
			builder.notgroup("projectstatus", Arrays.asList("closed","completed"));
			builder.ids(opengoalsids);
			opengoals = builder.search();
		}
		
		if( opengoals == null)
		{
			opengoals = Collections.emptyList();
			topgoals = new ArrayList();
			topgoals.add(new ArrayList());
			topgoals.add(new ArrayList());
			topgoals.add(new ArrayList());
		}
		if( topgoals == null)
		{
			GoalList list = new GoalList(selected,opengoals);
			topgoals = makeColumns(list.getSorted(),percolumn,startfrom);
		}
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

	private Collection findRemovedGoals(HitTracker inGoaltracker, Collection<String> inAllgoalsids)
	{
		List goals = new ArrayList();
		for (Iterator iterator = inGoaltracker.iterator(); iterator.hasNext();)
		{
			Data goal = (Data) iterator.next();
			if( !inAllgoalsids.contains( goal.getId()))
			{
				goals.add(goal);
			}
		}
		return goals;
	}



	protected Collection makeColumns(Collection tracker, int percolumn, int startfrom)
	{
		Collection topgoals = new ArrayList();
		Collection col0 = new ArrayList();
		Collection col1 = new ArrayList();
		Collection col2 = new ArrayList();
		topgoals.add(col0);
		topgoals.add(col1);
		topgoals.add(col2);
		for (Iterator iterator = tracker.iterator(); iterator.hasNext();)
		{
			Data goal = (Data)iterator.next();
			String col = goal.get("goaltrackercolumn");
			if( col == null || col.equals("0"))
			{
				col1.add(goal);
			}
			else if( col.equals("1"))
			{
				col0.add(goal);
			}
			else if( col.equals("2"))
			{
				col2.add(goal);
			}
		}
		return topgoals;
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

		if( taskstatus != null && taskstatus.equals("3"))
		{
			task.setValue("completedby", inReq.getUserName());
			if( task.getValue("completedon") == null )
			{
				task.setValue("completedon", new Date());
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
		//cal.set(Calendar.DAY_OF_MONTH, 1);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		int week = cal.get(Calendar.WEEK_OF_YEAR);
		week = week - 4;
		cal.set(Calendar.WEEK_OF_YEAR,week);
		String monthsback = inReq.getRequestParameter("monthsback");
		if( monthsback != null)
		{
			int count = Integer.parseInt(monthsback);
			//cal.add(Calendar.MONTH, 0 - count);
			week = week - (count*4);
			cal.set(Calendar.WEEK_OF_YEAR,week);
			inReq.putPageValue("monthsback", count+1);
		}
		else
		{
			inReq.putPageValue("monthsback", 1);
		}
		inReq.putPageValue("since", cal.getTime());
		Searcher tasksearcher = archive.getSearcher("goaltask");
		Date start = cal.getTime();
		week = week + 5;
		cal.set(Calendar.WEEK_OF_YEAR,week);

		Date onemonth = cal.getTime();
		String rootid = "tasks" + collection.getId();

		HitTracker all = tasksearcher.query().exact("projectdepartmentparents",rootid)
				.match("completedby", "*").between("completedon", start,onemonth).sort("completedonDown").search();
		log.info("Query: " + all.getSearchQuery());
			
		Map byperson = new HashMap();
		for (Iterator iterator = all.iterator(); iterator.hasNext();)
		{
			MultiValued  task = (MultiValued) iterator.next();
			CompletedTasks completed = (CompletedTasks)byperson.get(task.get("completedby"));
			if( completed == null)
			{
				completed = new CompletedTasks();
				byperson.put(task.get("completedby"),completed);
			}
			completed.addTask(task);
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
	
		QueryBuilder builder = searcher.query().exact("collectionid", collection.getId());
		builder.match("userlikes", "*").sort("owner").sort("userlikes");
		builder.notgroup("projectstatus", Arrays.asList("closed","completed"));
		
		HitTracker likes = builder.search();

		sortIntoColumns(inReq, archive, likes);

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
	
}
