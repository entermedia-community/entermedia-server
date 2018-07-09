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
			cat.setName("Teams");
			archive.getCategorySearcher().saveData(cat);
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
		QueryBuilder builder = searcher.query().exact("collectionid", collection.getId());
		builder.notgroup("projectstatus", Arrays.asList("closed","completed"));
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
		
		HitTracker tracker = null;
		if( department != null)
		{
			Category selected = archive.getCategory(department);
			inReq.putPageValue("selectedcat", selected);
			Collection goalids = selected.getValues("countdata");
			if( goalids == null || goalids.isEmpty())
			{
				log.info("No goals set");
				return;
			}
			log.info("loaded goalids" + selected.getId() + "="+ goalids);
			Collection copy = new ArrayList();
			for (Iterator iterator = goalids.iterator(); iterator.hasNext();)
			{
				String gid = (String) iterator.next();
				if( gid != null)
				{
					copy.add(gid);
				}
			}
			builder.ids(copy);
			tracker = builder.search();
			
			if( tracker.size() != goalids.size() )
			{
				copy = new ArrayList();
				for (Iterator iterator = tracker.iterator(); iterator.hasNext();)
				{
					Data hit = (Data) iterator.next();
					copy.add(hit.getId());
				}
				selected.setValue("countdata",copy);
				archive.getCategorySearcher().saveCategory(selected);
			}
			
			GoalList list = new GoalList(selected,tracker);
			topgoals = makeColumns(list.getSorted(),percolumn,startfrom);
		}
		else
		{
			tracker = builder.sort("creationdateDown").search();
			topgoals = makeColumns(tracker,percolumn,startfrom);
		}
		inReq.putPageValue("topgoals", topgoals);
		inReq.putPageValue("goals", tracker);
		if( tracker.size() > (thispage + 1) * perpage )
		{
			inReq.putPageValue("nextpage", thispage + 1);
		}
		
		Collection archived = searcher.query().orgroup("projectstatus", Arrays.asList("closed","completed"))
				.exact("collectionid", collection.getId()).search(inReq);
		inReq.putPageValue("closedgoals", archived);
		
		
	}

	protected Collection makeColumns(Collection tracker, int percolumn, int startfrom)
	{
		
		Collection topgoals = new ArrayList();
		Collection ten = new ArrayList();
		topgoals.add(ten);
		for (Iterator iterator = tracker.iterator(); iterator.hasNext();)
		{
			if( startfrom > 0)
			{
				startfrom--;
				continue;
			}
			Data hit = (Data) iterator.next();
			ten.add(hit);
			if( ten.size() == percolumn)  //so 15 per page
			{
				if( topgoals.size() == 3)
				{
					break;
				}
				ten = new ArrayList();
				topgoals.add(ten);
			}
		}
		if( topgoals.size() ==0)
		{
			topgoals.add(new ArrayList());
		}
		if( topgoals.size() == 1)
		{
			topgoals.add(new ArrayList());
		}
		if( topgoals.size() == 2)
		{
			topgoals.add(new ArrayList());
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
		
		HitTracker tasks = tasksearcher.query().exact("projectgoal", goal.getId()).search();
		//Legacy: Make sure all tasks have parents
		List tosave = new ArrayList();
		for (Iterator iterator = tasks.iterator(); iterator.hasNext();)
		{
			MultiValued existig = (MultiValued) iterator.next();
			if( existig.getValue("projectdepartmentparents") == null)
			{
				Category child = archive.getCategory(existig.get("projectdepartment"));
				existig.setValue("projectdepartmentparents",child.getParentCategories());
				tosave.add(existig);
			}
		}
		tasksearcher.saveAllData(tosave, null);
		TaskList goaltasks = new TaskList(goal,tasks);
		inReq.putPageValue("tasklist", goaltasks);
		inReq.putPageValue("tasks", goaltasks.getSortedTasks());
		

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
			task.setValue("completedon", new Date());
		}
		else if( taskstatus != null && taskstatus.equals("1"))
		{
			task.setValue("startedby", inReq.getUserName());
			task.setValue("startedon", new Date());
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
		cal.set(Calendar.DAY_OF_MONTH, 1);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);

		String monthsback = inReq.getRequestParameter("monthsback");
		if( monthsback != null)
		{
			int count = Integer.parseInt(monthsback);
			cal.add(Calendar.MONTH, 0 - count);
			inReq.putPageValue("monthsback", count+1);
		}
		else
		{
			inReq.putPageValue("monthsback", 1);
		}
		inReq.putPageValue("since", cal.getTime());
		Searcher tasksearcher = archive.getSearcher("goaltask");
		Date start = cal.getTime();
		cal.add(Calendar.MONTH,1);
		Date onemonth = cal.getTime();
		String rootid = "tasks" + collection.getId();

		HitTracker all = tasksearcher.query().exact("projectdepartmentparents",rootid)
				.match("completedby", "*").between("completedon", start,onemonth).sort("completedonDown").search();
		log.info("Query: " + all.getSearchQuery());
		Map byperson = new HashMap();
		for (Iterator iterator = all.iterator(); iterator.hasNext();)
		{
			Data  task = (Data ) iterator.next();
			List completed = (List)byperson.get(task.get("completedby"));
			if( completed == null)
			{
				completed = new ArrayList();
			}
			completed.add(task);
			byperson.put(task.get("completedby"),completed);
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
		builder.match("userlikes", "*");
		builder.notgroup("projectstatus", Arrays.asList("closed","completed"));
		builder.sort("recorddate");
		
		HitTracker likes = builder.search();
		inReq.putPageValue("likes", likes);
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

}
