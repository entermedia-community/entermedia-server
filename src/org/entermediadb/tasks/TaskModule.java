package org.entermediadb.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.projects.LibraryCollection;
import org.entermediadb.projects.ProjectManager;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.QueryBuilder;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.ListHitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.profile.UserProfile;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.MathUtils;

import com.google.common.collect.ComparisonChain;

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
		if (collection.getCategory() == null) 
		{
			log.info("Collection have not category");
			return;
		}
		Category cat = archive.getCategory(id);
		if( cat == null)
		{
			cat = (Category)archive.getCategorySearcher().createNewData();
			cat.setId(id);
			cat.setName("Tasks");
			collection.getCategory().addChild(cat);
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
			//log.info("Collection not found");
			return;
		}
		
		HitTracker all = (HitTracker)inReq.getPageValue("goalhits");
		
		//if( all == null)
		//{
			SearchQuery userq = searcher.addStandardSearchTerms(inReq);
			if( userq == null) 
			{
				QueryBuilder builder = searcher.query().enduser(true).hitsPerPage(500).exact("collectionid", collection.getId());
				builder.orgroup("projectstatus", "open|critical|completed");
				
				//Within 6 months?
				Date old = DateStorageUtil.getStorageUtil().addDaysToDate(new Date(), 30*-6);
				builder.after("creationdate",old);
				
				userq = builder.getQuery();
				//Collection filter = inReq.getUserProfile().getValues("goaltrackercolumns");
//				if( filter != null && !filter.isEmpty())
//				{
//					userq.addOrsGroup("goaltrackercolumn", filter);
//				}
			}
			else if( !userq.contains("collectionid"))
			{
				userq.addExact("collectionid", collection.getId());
			}
			all = searcher.cachedSearch(inReq, userq);
		//}
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
			if( !values.isEmpty())
			{
				Collections.sort(values);
				Collections.reverse(values);
			}
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
	
	protected List sortIntoDates(WebPageRequest inReq, MediaArchive archive, Collection all, Calendar thismonday, int dayofweek)
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
					Calendar rg = DateStorageUtil.getStorageUtil().createCalendar();
					rg.setTime(rd);
					int dow = rg.get(Calendar.DAY_OF_WEEK) - 2;
					if( dow == i)
					{
						todaysgoals.add(goal);
					}
				}
			}
		}	
		final List order = Arrays.asList(new String[] {"critical","active","open"});
		
		for (Iterator iterator = week.iterator(); iterator.hasNext();)
		{
			List values = (List) iterator.next();
			Collections.sort(values, new Comparator<ProjectGoal>()
			{
				@Override
				public int compare(ProjectGoal pg1, ProjectGoal pg2)
				{
						String status1 = pg1.get("projectstatus");
						String status2 = pg2.get("projectstatus");
						if( status1 == null) status1 = "open";
						if( status2 == null) status2 = "open";
						int order1 = order.indexOf(status1);
						int order2 = order.indexOf(status2);

						Date date1 = (Date)pg1.getDate("creationdate");
						Date date2 = (Date)pg2.getDate("creationdate");
						if( date1 == null)
						{
							date1 = new Date(0);
						}
						if( date2 == null)
						{
							date2 = new Date(0);							
						}
						
						int r = ComparisonChain.start().compare(order1, order2).
						 	compare(date1, date2).result();  
					        
						return r;
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
	}
	public void loadTasksForGoal(WebPageRequest inReq)
	{
		MultiValued goal = (MultiValued)inReq.getPageValue("goal");
		Collection tasks = (Collection)inReq.getPageValue("tasks");
		
		if( goal != null && tasks == null)
		{
			loadTasksForGoal(inReq,goal);
		}
	}

	protected void loadTasksForGoal(WebPageRequest inReq, MultiValued goal)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Searcher tasksearcher = (Searcher)archive.getSearcher("goaltask");
		
		QueryBuilder query = tasksearcher.query().exact("projectgoal", goal.getId());
		
		String selectedcollectiverole = (String)inReq.getPageValue("selectedcollectiverole");
		if( selectedcollectiverole != null)
		{
			query.exact("taskroles.collectiverole",selectedcollectiverole);
		}
		
		String onlyopen = inReq.getRequestParameter("onlyopen");
		if( Boolean.parseBoolean(onlyopen))
		{
			query.not("taskstatus", "3");
		}
		String onlyuser = inReq.getRequestParameter("onlyuser");
		if( Boolean.parseBoolean(onlyuser))
		{
			String selected = inReq.getRequestParameter("goaltrackerstaff");
			if( selected == null)
			{
				selected = inReq.getUserName();
			}
			query.exact("completedby", selected);
		}
		
		String keyword = inReq.getRequestParameter("keyword");
		if( keyword != null)
		{
			query.exact("keywords", keyword);
		}
		
		query.sort("creationdate");
		HitTracker tasks = query.search(inReq);
		//Legacy: Make sure all tasks have parents
		Set tosave = new HashSet();
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
			if( existigtask.getValue("collectionid") == null)
			{
				existigtask.setValue("collectionid",goal.getValue("collectionid"));
				tosave.add(existigtask);
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
	
	public void loadTasksForUser(WebPageRequest inReq) {
		String user = inReq.getRequestParameter("user");
		if( user == null)
		{
			user = inReq.getUserName();
		}
		loadTasksForUser(inReq, user);
	}
	protected void loadTasksForUser(WebPageRequest inReq, String inUserId)
	{
		MediaArchive archive = getMediaArchive(inReq);
		
		
		
		Searcher goalsearcher = archive.getSearcher("projectgoal"); //Al Projects
		QueryBuilder opengoalbuilder = goalsearcher.query();
		opengoalbuilder.not("projectstatus", "closed").not("projectstatus", "completed").sort("projectstatus").sort("creationdateUp");
		HitTracker opengoalresults = opengoalbuilder.search();
		
		List opentickets = new ArrayList();
		Map tasklookup = new HashMap();
		inReq.putPageValue("opentickets", opentickets);
		inReq.putPageValue("searcher", opengoalresults.getSearcher());
		
		for (Iterator iterator = opengoalresults.iterator(); iterator.hasNext();)
		{
			//All tasks for Project Goal
			Data goal = (Data) iterator.next();
			Collection tasks = archive.query("goaltask")
										.not("taskstatus", "3")
										.match("projectgoal", goal.getId())
										.exact("taskroles.roleuserid", inUserId)
										.sort("creationdateDown")
										.search();
			
			if (!tasks.isEmpty())
			{
			opentickets.add( goalsearcher.loadData(goal) );
			tasklookup.put(goal.getId(),tasks);
			inReq.putPageValue("tasksearcher",archive.getSearcher("goaltask"));
			inReq.putPageValue("tasklookup",tasklookup);
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
		String completedby = inReq.getRequestParameter("completedby");
		String goalid = inReq.getRequestParameter("goalid");
		String comment = inReq.getRequestParameter("comment");
		String taskstatus = inReq.getRequestParameter("taskstatus");
		
		if(taskstatus==null)
		{
			taskstatus = "0";
		}
		if( goalid== null)
		{
			goalid = inReq.getRequestParameter("id");
		}
		if( goalid== null) {
			Data savedgoal = inReq.getData();
			if (savedgoal != null) {
				goalid = savedgoal.getId();
			}
		}
		if( goalid == null)
		{
			log.error("No goals");
			return;
		}
		inReq.putPageValue("goalid", goalid);
		if( completedby == null)
		{
			log.error("No user completedby");
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
		String collectionid = inReq.getRequestParameter("collectionid");
		
		if( collectionid == null)
		{
			collectionid = inReq.getRequestParameter("collectionid.value");
			if( collectionid == null)
			{
				throw new OpenEditException("No collection id found");
			}
		}
		inReq.putPageValue("collectionid", collectionid);
		MediaArchive archive = getMediaArchive(inReq);
		Searcher goalsearcher = archive.getSearcher("projectgoal");
		Data goal = (Data)goalsearcher.searchById(goalid);
		Category cat = archive.getCategory(categoryid);
		//Make goaltask
		Searcher tasksearcher = (Searcher)archive.getSearcher("goaltask");
		
		Data task = tasksearcher.createNewData(); //TODO: Cjheck for existing
		task.setValue("projectgoal",goal.getId());
		task.setValue("collectionid",collectionid);
		task.setValue("projectdepartment",categoryid);
		/* Added completedby and comment to goal saving */
		task.setValue("completedby", completedby );
		task.setValue("comment", comment);
		task.setValue("taskstatus", taskstatus);
		
		task.setValue("projectdepartmentparents",cat.getParentCategories());
		
		task.setValue("creationdate",new Date());
		
		task.setName(cat.getName()); //TODO: Support comments
		
		/* Set staff member on ticket to staff member from task */
		Collection found = goal.getValues("userlikes");
		if( !found.contains(completedby))
		{
			found.add(completedby);
			goal.setValue("userlikes",found );
			archive.saveData("projectgoal", goal);
		}
		
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
		addComment(archive, task.getId(), inReq.getUser(),"0",comment);
		
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
		String taskcomment = inReq.getRequestParameter("comment");
		MediaArchive archive = getMediaArchive(inReq);
		Searcher tasksearcher = archive.getSearcher("goaltask");
		Data task = null;
		if( taskid != null )
		{
			task = (Data)tasksearcher.searchById(taskid);
		}
		else
		{
			task = (Data)tasksearcher.createNewData();		
			String goalid = inReq.getRequestParameter("goalid");
			task.setValue("projectgoal",goalid);
		}
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
			Data goal = archive.getData("projectgoal", task.get("projectgoal"));
			if( "critical".equals( goal.get("projectstatus") ) )
			{
				task.setValue("completedurgent","true" );
			}
			
			//remove task from tree
			removeCount(archive, task);
			
		}
		//else if( taskstatus != null && taskstatus.equals("1"))
		if( task.getValue("startedby")  == null)
		{
			task.setValue("startedby", inReq.getUserName());
		}
		if( task.getValue("startedon") == null )
		{
			task.setValue("startedon", new Date());
		}	
		
		if( task.getValue("creationdate") == null )
		{
			task.setValue("creationdate", new Date());
		}	
		
		String projectdepartment = inReq.getRequestParameter("projectdepartment");

		if( projectdepartment != null )
		{
			task.setValue("projectdepartment", projectdepartment);
		}	
		
		String completedby = inReq.getRequestParameter("completedby");
		if( completedby == null)
		{
			completedby = inReq.getUserName();
		}
		task.setValue("completedby", completedby);
		if( taskcomment != null)
		{
			task.setValue("comment",taskcomment);
		}
		
		String[] keywords = inReq.getRequestParameters("keywords.value");
		if( keywords != null)
		{
			task.setValue("keywords", keywords);
		}
		
		
		tasksearcher.saveData(task);	
		inReq.putPageValue("task", task);
		if( taskstatus == null)
		{
			taskstatus = "0";
		}
		//addComment(archive, taskid, inReq.getUser(),taskstatus, null);

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
		if (folder != null && folder.getValues("countdata") != null) {
			ArrayList list = new ArrayList(folder.getValues("countdata"));
			list.remove(task.get("projectgoal"));
			folder.setValue("countdata",list);
			archive.getCategorySearcher().saveData(folder);
		}
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
		String collectionid =  inReq.getRequestParameter("collectionid");
		
		if(collectionid == null) 
		{
			collectionid = "*";
		}
		//Search for all tasks with updated dates?
		Calendar cal = DateStorageUtil.getStorageUtil().createCalendar();
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
		
		/*
		int days = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH);
		days = days - 1;
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.add(Calendar.DAY_OF_MONTH,days);
		*/
		
		int days = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
		cal.set(Calendar.DAY_OF_MONTH, days);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.SECOND, 59);
		
		Date end = cal.getTime();
		
		populateResults(inReq, archive, collectionid, tasksearcher, start, end);

	}
	
	public void loadDashboardWeekly(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String collectionid =  inReq.getRequestParameter("collectionid");
		
		if(collectionid == null) 
		{
			collectionid = "*";
		}
		//Search for all tasks with updated dates?
		Calendar cal = DateStorageUtil.getStorageUtil().createCalendar();
		String monthsback = inReq.getRequestParameter("weeksback");
		if( monthsback == null)
		{
			monthsback = "0";
		}
		int count = Integer.parseInt(monthsback);
		cal.set(Calendar.DAY_OF_MONTH,1);
		int existingweek = cal.get(Calendar.WEEK_OF_YEAR);
		cal.set(Calendar.WEEK_OF_YEAR, existingweek - count);
		inReq.putPageValue("weeksback", count+1);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		inReq.putPageValue("since", cal.getTime());
		Searcher tasksearcher = archive.getSearcher("goaltask");
		Date start = cal.getTime();
		
		cal.set(Calendar.WEEK_OF_YEAR, existingweek - count + 1);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.SECOND, 0);
		
		Date end = cal.getTime();
		populateResults(inReq, archive, collectionid, tasksearcher, start, end);

	}

	protected void populateResults(WebPageRequest inReq, MediaArchive archive, String collectionid, Searcher tasksearcher, Date start, Date onemonth)
	{
		//String rootid = "tasks" + collection.getId();
		QueryBuilder q = tasksearcher.query();
		if( !collectionid.equals("*") )
		{
				q.exact("collectionid",collectionid);
		}
		QueryBuilder qall = q.match("completedby", "*").between("completedon", start,onemonth);
		UserProfile profile = inReq.getUserProfile();
		if (profile != null && profile.isInRole("administrator"))
		{
			//See all
		}
		else
		{
			if( inReq.getUser() == null)
			{
				return;
			}
			Collection projects = archive.getProjectManager().listCollectionsOnTeam(inReq.getUser());
			qall.orgroup("collectionid",projects);
		}
		HitTracker all = qall.sort("completedonDown").search();
		log.info("Tasks completed: " + all);
		CompletedTasks completed = new CompletedTasks();
		for (Iterator iterator = all.iterator(); iterator.hasNext();)
		{
			MultiValued  task = (MultiValued) iterator.next();
			String userid = task.get("completedby");
			completed.addTask(userid,task);
		}
		log.info("CompletedTasks: " + completed.getUserIds() );
		
		inReq.putPageValue("completed", completed);		
		
		QueryBuilder gq = archive.query("projectgoal");
		if( !collectionid.equals("*") )
		{
				gq.exact("collectionid",collectionid);
		}
		HitTracker alltickets = gq.match("resolveusers", "*").between("resolveddate", start,onemonth).sort("resolveddateDown").search();		
		
		log.info(" Resolved by user: " + alltickets);
		
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
		
		Searcher searcher = archive.getSearcher("goaltaskuserrole");
		Collection existingactions = searcher.query().exact("roleuserid","*").between("date", start,onemonth).sort("dateDown").search();

//		
//		QueryBuilder gq = archive.query("projectgoal");
//		if( !collectionid.equals("*") )
//		{
//				gq.exact("collectionid",collectionid);
//		}
		GoalManager goalm = (GoalManager)archive.getBean("goalManager");
		for (Iterator iterator = existingactions.iterator(); iterator.hasNext();)
		{
			MultiValued  roleaction = (MultiValued) iterator.next();
			String userid = roleaction.get("roleuserid");
			if( userid != null)
			{		
				String goaltaskid = roleaction.get("goaltaskid");
				Data task = archive.getCachedData("goaltask", goaltaskid);
				String collectiverole = roleaction.get("collectiverole");
				Map rolemap = goalm.findRole(task,collectiverole,userid);
				if( rolemap != null)
				{
					completed.addRole(rolemap, task, roleaction);
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

	
	//Old?
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
		//String rootid = "tasks" + collection.getId();
		HitTracker all = tasksearcher.query().exact("taskstatus",status).exact("collectionid",collection.getId()).search();
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
		if( inReq.getUser() == null)
		{
			return;
		}
		
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
		else
		{
			String goalid = inReq.getRequestParameter("goalid");
			Data goal =  archive.getData("projectgoal",goalid);
			inReq.putPageValue("selectedgoal", goal);

		}
		
	}		

	public void loadTickets(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		Searcher searcher = archive.getSearcher("projectgoal");
		
		String seeuser = inReq.getRequestParameter("goaltrackerstaff");//inReq.getUserProfile().get("goaltrackerstaff");
		
		Boolean isAgent =  inReq.getUserProfile().isInRole("administrator");  //For now Admins can see all tickets
				
		QueryBuilder builder = searcher.query();
		Collection userprojects = new HashSet();;
		//if user is agent?
		if( seeuser != null) {
				builder.match("userlikes", seeuser);
		}
		
		String collectionid = inReq.getRequestParameter("collectionid");
		//String collectionid= "*";
		if (collectionid != null) 
		{
			userprojects.add(collectionid);
		}
		else 
		{
			if( !isAgent ) 
				{
					//search only in project the user belongs
					String currentuser = inReq.getUserName();
					Collection allprojectsuser = archive.query("librarycollectionusers").
							exact("followeruser",currentuser).
							exact("ontheteam","true").search();
					if(allprojectsuser.size()<1)
					{
						return;
					}
					for (Iterator iterator = allprojectsuser.iterator(); iterator.hasNext();)
					{
						Data librarycol = (Data)iterator.next();
						String colid = librarycol.get("collectionid");
						if( colid != null)
						{
							userprojects.add(colid);
						}
					}
				}
		}
		
		if(userprojects.size()>0) 
		{
			builder.orgroup("collectionid", userprojects);
		}
		
		builder.orgroup("projectstatus", "open|critical");
		HitTracker likesopen = builder.search();
		
		//sort users by date?
		Calendar thismonday = DateStorageUtil.getStorageUtil().createCalendar();
		thismonday.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		builder = searcher.query();
		if(userprojects != null) 
		{
			builder.orgroup("collectionid", userprojects);
		}
		if( seeuser != null)
		{
			builder.match("userlikes", seeuser);
		}
		builder.orgroup("projectstatus", Arrays.asList("closed","completed"));
		builder.after("resolveddate", thismonday.getTime());
		HitTracker likesclosed = builder.search();

		inReq.putPageValue("selecteduser",archive.getUser(seeuser));
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
		Calendar todayc = DateStorageUtil.getStorageUtil().createCalendar();
		int selectedday1 = todayc.get(Calendar.DAY_OF_WEEK);
		int selectedday0 = selectedday1 - 2; //zero based  
		if( selectedday0 < 0 || selectedday0 > 5)
		{
			selectedday0 = 0; //monday
		}
		List week = sortIntoDates(inReq, archive, all, thismonday, selectedday0);
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
		
		builder = searcher.query();
		if(userprojects.size()>0) 
		{
			builder.orgroup("collectionid", userprojects);
		}
		
		builder.match("userlikes", "*");
		builder.notgroup("projectstatus", Arrays.asList("closed","completed"));
		int totalpriority = builder.search().size();
		
		builder = searcher.query();
		if(userprojects.size()>0) 
		{
			builder.orgroup("collectionid", userprojects);
		}
		builder.notgroup("projectstatus", Arrays.asList("closed","completed"));
		int totalopen = builder.search().size();

		builder = searcher.query();
		if(userprojects.size()>0) 
		{
			builder.orgroup("collectionid", userprojects);
		}
		builder.orgroup("projectstatus", Arrays.asList("closed","completed"));
		int totalclosed = builder.search().size();
			
		inReq.putPageValue("totallikes", totalpriority);
		inReq.putPageValue("totalopen", totalopen);
		inReq.putPageValue("totalclosed", totalclosed);
	
	}
	public void loadUserTickets(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		Searcher goalsearcher = archive.getSearcher("projectgoal");
		
		String staffid = inReq.getRequestParameter("userid");//inReq.getUserProfile().get("goaltrackerstaff");
		if( staffid == null)
		{
			staffid = inReq.getRequestParameter("goaltrackerstaff");//inReq.getUserProfile().get("goaltrackerstaff");
		}
		//Boolean isAgent =  inReq.getUserProfile().isInRole("administrator");  //For now Admins can see all tickets
		if( staffid == null)
		{
			staffid = inReq.getUserName();
		}
		
		QueryBuilder opengoalbuilder = goalsearcher.query();
		Collection userprojects = new HashSet();

		User selecteduser = null;
		if( staffid != null) 
		{
			//opengoalbuilder.match("userlikes", staffid);
			selecteduser = archive.getUser(staffid);
		}
		else
		{
			selecteduser = inReq.getUser();
		}
		inReq.putPageValue("selecteduser",selecteduser);
		
		String collectionid = inReq.getRequestParameter("collectionid");
		String currentuser = staffid;
		if( staffid == null)
		{
			currentuser = inReq.getUserName(); 
		}
		//String collectionid= "*";
		if (collectionid != null) 
		{
			if( !collectionid.equals("*") )
			{
				userprojects.add(collectionid);
			}
		}
		else 
		{
			//search only in project the user belongs
			Collection allprojectsuser = null;
			
			
			if( staffid.equals(inReq.getUserName()))
			{
				//gather up common projects
				allprojectsuser = archive.query("librarycollectionusers").
				exact("followeruser",currentuser).
				exact("ontheteam","true").search();
			}
			else
			{
				List bothusers = new ArrayList();
				HitTracker someprojects = archive.query("librarycollectionusers").
				exact("followeruser",inReq.getUserName()).
				exact("ontheteam","true").search();
				if( !someprojects.isEmpty() )
				{
					Collection projects = someprojects.collectValues("collectionid");
					allprojectsuser = archive.query("librarycollectionusers").
					orgroup("collectionid",projects).
					exact("followeruser",currentuser).
					exact("ontheteam","true").search();
				}
			}
			
			if(allprojectsuser == null || allprojectsuser.isEmpty())
			{
				//No Tickets
				inReq.putPageValue("opentickets", new ListHitTracker());
				return;
			}
			
			for (Iterator iterator = allprojectsuser.iterator(); iterator.hasNext();)
			{
				Data librarycol = (Data)iterator.next();
				String colid = librarycol.get("collectionid");
				if( colid != null)
				{
					userprojects.add(colid);
				}
			}
		}
		
		if(userprojects != null && !userprojects.isEmpty()) 
		{
			opengoalbuilder.orgroup("collectionid", userprojects);
		}
		opengoalbuilder.not("projectstatus", "closed").not("projectstatus", "completed").sort("projectstatus").sort("creationdateUp");
		//closedgoalbuilder.orgroup("projectstatus", "active|open|critical").sort("projectstatus").sort("creationdateDown");
		HitTracker opengoalresults = opengoalbuilder.search();

		List opentickets = new ArrayList();
		inReq.putPageValue("opentickets", opentickets);
		inReq.putPageValue("searcher", opengoalresults.getSearcher());
		
		Map tasklookup = new HashMap();
		for (Iterator iterator = opengoalresults.iterator(); iterator.hasNext();)
		{
			Data goal = (Data) iterator.next();
			Collection tasks = archive.query("goaltask").not("taskstatus", "3").match("projectgoal", goal.getId()).sort("creationdateDown").search();
			Collection found = new ArrayList();
			boolean hasone = false;
			for (Iterator ta = tasks.iterator(); ta.hasNext();)
			{
				Data task = (Data) ta.next();
				if( currentuser.equals( task.get("completedby")) )
				{
					found.add(task);
					if( !"5".equals( task.get("taskstatus") ))
					{
						hasone = true;
					}
				}
				else if("5".equals( task.get("taskstatus") ))
				{
					found.add(task);
				}
			}
			if( !found.isEmpty() && hasone)
			{
				opentickets.add( goalsearcher.loadData(goal) );
				tasklookup.put(goal.getId(),found);
			}
		}
		inReq.putPageValue("tasksearcher",archive.getSearcher("goaltask"));
		
		inReq.putPageValue("tasklookup",tasklookup);
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
		selectedgoal.setValue("lasteditedby", inReq.getUser());
		archive.saveData("projectgoal",selectedgoal);
		GoalManager goalm = (GoalManager)archive.getBean("goalManager");
		goalm.addStatus(archive, selectedgoal,inReq.getUserName());
		inReq.putPageValue("goal",selectedgoal);
		Map params = new HashMap();
		params.put("dataid", selectedgoal.getId());
		archive.fireGeneralEvent(inReq.getUser(),"projectgoal","saved", params);
	}

	public void savedGoal(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String goalid = inReq.getRequestParameter("dataid");
		if( goalid == null )
		{
			goalid = inReq.getRequestParameter("id");  //legacy
		}
		MultiValued selectedgoal = (MultiValued)archive.getData("projectgoal",goalid);

		GoalManager goalm = (GoalManager)archive.getBean("goalManager");

		goalm.addStatus(archive, selectedgoal,inReq.getUserName());
		
		recalculateSessions(archive, selectedgoal, inReq.getUserName());
		
		
	}

	protected void recalculateSessions(MediaArchive inArchive, MultiValued inSelectedgoal, String inUserName)
	{
		String collectionid = inSelectedgoal.get("collectionid");
		ProjectManager manager = inArchive.getProjectManager();
		manager.recalculateSessions(collectionid);
		
	}

	public void  recalculateSessions(WebPageRequest inReq) {
		MediaArchive archive = getMediaArchive(inReq);
		String collectionid = inReq.findValue("collectionid");
		
		Searcher collectionsearcher = archive.getSearcher("librarycollection");
		HitTracker cols;
		if(collectionid != null) {
			cols = collectionsearcher.fieldSearch("id", collectionid);
		} else {
			cols = collectionsearcher.getAllHits();
		}
		for (Iterator iterator = cols.iterator(); iterator.hasNext();)
		{
			Data object = (Data) iterator.next();
			archive.getProjectManager().recalculateSessions(object.getId());
			
		}
		
	}
	
	
	

	
	public void clearNotify(WebPageRequest inReq)
	{
		QueryBuilder query = getMediaArchive(inReq).query("statuschanges").exact("notified","false").
				exact("userid", inReq.getUserName()).sort("dateDown");
		
		String collectionid = inReq.getRequestParameter("collectionid");
		if( collectionid != null)
		{
			query.exact("collectionid",collectionid);
		}
		Collection results = query.search();
		inReq.putPageValue("recentactivities",results);
		Collection tosave = new ArrayList();
		for (Iterator iterator = results.iterator(); iterator.hasNext();)
		{
			Data status = (Data) iterator.next();
			status.setValue("notified",true);
			tosave.add(status);
		}
		getMediaArchive(inReq).getSearcher("statuschanges").saveAllData(tosave,null);
	}
	
	public void showRecentActivity(WebPageRequest inReq)
	{
		QueryBuilder query = getMediaArchive(inReq).query("statuschanges").
				exact("userid", inReq.getUserName()).sort("dateDown");
		
		String collectionid = inReq.getRequestParameter("collectionid");
		if( collectionid != null)
		{
			query.exact("collectionid",collectionid);
		}
		Collection results = query.search();
		inReq.putPageValue("recentactivities",results);
	}
	
	public void createGoalFromMessage(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Searcher chatsearcher = archive.getSearcher("chatterbox");
		String messageid = inReq.getRequestParameter("messageid");

		Data message = (Data)chatsearcher.searchById(messageid);

		GoalManager goalm = (GoalManager)archive.getBean("goalManager");

		String taskstatus = inReq.getRequestParameter("taskstatus");

		Data goal = goalm.createGoal(inReq, message, taskstatus);
		goalm.createTask((MultiValued)goal,message, taskstatus);

//		Searcher searcher = archive.getSearcher("projectgoal");
//		searcher.saveData(goal);

		inReq.putPageValue("chat",message);
	}

	public void createAgendaFromMessage(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Searcher chatsearcher = archive.getSearcher("chatterbox");
		String messageid = inReq.getRequestParameter("messageid");

		Data message = (Data)chatsearcher.searchById(messageid);

		//Remove all existing level 1s
		Searcher searcher = archive.getSearcher("projectgoal");
		String collectionid = message.get("collectionid");
		if( collectionid == null)
		{
			throw new OpenEditException("No collectionid on " + messageid);
		}
		List tosave = new ArrayList();
		
		//Save level 1 to the Agenda
		HitTracker all = searcher.query().exact("collectionid",collectionid).exact("ticketlevel", "1").search();
		for (Iterator iterator = all.iterator(); iterator.hasNext();)
		{
			Data record = (Data) iterator.next();
			record.setValue("ticketlevel", "2");
			tosave.add(record);
		}
		searcher.saveAllData(tosave, inReq.getUser());
		tosave.clear();
		
		GoalManager goalm = (GoalManager)archive.getBean("goalManager");

		String taskstatus = inReq.getRequestParameter("taskstatus");

		Data goal = goalm.createGoal(inReq, message, taskstatus);
		//Create actions/Tasks on this goal
		goalm.createTasks((MultiValued)goal,message, taskstatus);

		goal.setValue("ticketlevel", "1");
		searcher.saveData(goal);
		
		/*
		Searcher tasksearcher = archive.getSearcher("goaltask");
		Collection agendatasks = tasksearcher.query().exact("collectionid",collectionid).exact("taskstatus", "6").search(); //6 is agenda
		
		
		for (Iterator iterator = agendatasks.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			data.setValue("projectgoal",goal.getId());
			tosave.add(data);
		}
		tasksearcher.saveAllData(tosave, null);
		*/
		
		inReq.putPageValue("chat",message);
	}

	


	public void loadTicketReport(WebPageRequest inReq) throws Exception
	{
		long ago = System.currentTimeMillis() - 24*60*60*1000;
		Date todaysDate = new Date(ago);
		inReq.putPageValue("date",todaysDate);
	}

	public Map getAllTasksForHits(WebPageRequest inReq) throws Exception
	{
		HitTracker hits = (HitTracker)inReq.getPageValue("hits");
		if( hits != null)
		{
			Map<String,Collection> goalhits = new HashMap();
			
			MediaArchive archive = getMediaArchive(inReq);
			Collection sorted = archive.query("goaltask").named("goaltasks").orgroup("projectgoal", hits.getPageOfHits()).sort("creationdate").search();
			
			for (Iterator iterator = sorted.iterator(); iterator.hasNext();)
			{
				Data task = (Data) iterator.next();
				String goalid = task.get("projectgoal");
				Collection tasks = goalhits.get(goalid);
				if( tasks == null)
				{
					tasks = new ArrayList();
					goalhits.put(goalid,tasks);
				}
				tasks.add(task);
			}
			inReq.putPageValue("goalhits",goalhits);
			return goalhits;
		}
		return null;
	}

	public void saveTaskRole(WebPageRequest inReq)
	{
		String taskid = inReq.getRequestParameter("taskid");
		String collectionid = inReq.getRequestParameter("collectionid");
		MediaArchive archive = getMediaArchive(inReq);

		Searcher tasksearcher = archive.getSearcher("goaltask");
		Data task = (Data)tasksearcher.searchById(taskid);
		
		List roles = (List)task.getValue("taskroles");
		
		if( roles == null)
		{
			roles = new ArrayList();
		}
		
		Map addrole = new HashMap();
		addrole.put("date",new Date());
		
		String roleid = inReq.getRequestParameter("collectiverole");
		addrole.put("collectiverole",roleid);
		addrole.put("actioncount",0);
		Data user = archive.query("librarycollectionusers").exact("collectionid",collectionid).exact("teamroles",roleid).searchOne();

		if( user != null)
		{
			addrole.put("roleuserid",user.get("followeruser"));
		}
		
		
		//addrole.put("id",tasksearcher.nextId());
		addrole.put("id",roleid);
	
		roles.add(addrole);
		
		task.setValue("taskroles",roles);
		tasksearcher.saveData(task);
		
		inReq.putPageValue("task", task);
	}
	public void taskRoleAddOne(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);

		String taskid = inReq.getRequestParameter("taskid");
		String roleuserid = inReq.getRequestParameter("roleuserid");
		String collectiverole = inReq.getRequestParameter("collectiverole");
		
		GoalManager goalm = (GoalManager)archive.getBean("goalManager");
		
		Data task = goalm.addAction(inReq.getUserName(),taskid, collectiverole, roleuserid);
		inReq.putPageValue("task", task);
	}
	public void taskRoleRemoveAction(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);

		String actionid = inReq.getRequestParameter("roleactionid");
		
		GoalManager goalm = (GoalManager)archive.getBean("goalManager");

		Data task = goalm.removeAction(actionid);
		inReq.putPageValue("task", task);
	}
	public void taskRoleLoad(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);

		String taskid = inReq.getRequestParameter("taskid");
		String roleuserid = inReq.getRequestParameter("roleuserid");
		String collectiverole = inReq.getRequestParameter("collectiverole");
		
		GoalManager goalm = (GoalManager)archive.getBean("goalManager");
		
		Map role = goalm.findRole(taskid,collectiverole, roleuserid);
		inReq.putPageValue("taskrole", role);
		Data task = (Data)archive.getData("goaltask", taskid);
		inReq.putPageValue("task", task);
	}
	public void taskRoleSave(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);

		String taskid = inReq.getRequestParameter("taskid");
		String roleuserid = inReq.getRequestParameter("roleuserid");
		String name = inReq.getRequestParameter("name");
		String collectiverole = inReq.getRequestParameter("collectiverole");
		
		GoalManager goalm = (GoalManager)archive.getBean("goalManager");
		
		Map role = goalm.saveRole(taskid,collectiverole, roleuserid, name);

		Data task = (Data)archive.getData("goaltask", taskid);
		inReq.putPageValue("task", task);
		Data goal = (MultiValued)archive.getData("projectgoal",task.get("projectgoal"));

		inReq.putPageValue("goal", goal);
		
	}

	public void taskRoleDelete(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);

		String taskid = inReq.getRequestParameter("taskid");
		String roleuserid = inReq.getRequestParameter("roleuserid");
		String name = inReq.getRequestParameter("name");
		String collectiverole = inReq.getRequestParameter("collectiverole");
		
		GoalManager goalm = (GoalManager)archive.getBean("goalManager");
		
		goalm.removeRole(taskid,collectiverole, roleuserid);

		Data task = (Data)archive.getData("goaltask", taskid);
		inReq.putPageValue("task", task);
	}


}
