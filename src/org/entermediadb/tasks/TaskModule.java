package org.entermediadb.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.projects.LibraryCollection;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.QueryBuilder;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;

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
		QueryBuilder builder = searcher.query().exact("collectionid", collection.getId());
		if( department != null)
		{
			Category selected = archive.getCategory(department);
			Collection goalids = selected.getValues("countdata");
			if( goalids == null || goalids.isEmpty())
			{
				log.info("No goals set");
				return;
			}
			builder.ids(goalids);
		}
		HitTracker tracker = builder.search();
		inReq.putPageValue("goals", tracker);
		Collection topgoals = new ArrayList();
		Collection ten = new ArrayList();
		topgoals.add(ten);
		for (Iterator iterator = tracker.iterator(); iterator.hasNext();)
		{
			Data hit = (Data) iterator.next();
			ten.add(hit);
			if( ten.size() == 5)
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
		inReq.putPageValue("topgoals", topgoals);
		
	}
	
	public void loadGoal(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String goalid = inReq.getRequestParameter("id");
		Data goal = (Data)archive.getData("projectgoal",goalid);
		inReq.putPageValue("data", goal);
		Searcher tasksearcher = (Searcher)archive.getSearcher("goaltask");
		HitTracker tasks = tasksearcher.query().exact("projectgoal", goal.getId()).search();
		inReq.putPageValue("goaltasks", tasks);
	}
	
	
	public void addGoalToCategory(WebPageRequest inReq)
	{
		String goalid = inReq.getRequestParameter("goalid");
		String categoryid = inReq.getRequestParameter("categoryid");
		MediaArchive archive = getMediaArchive(inReq);
		Searcher goalsearcher = archive.getSearcher("projectgoal");
		Data goal = (Data)goalsearcher.searchById(goalid);
		Category cat = archive.getCategory(categoryid);
		//Make goaltask
		Searcher tasksearcher = (Searcher)archive.getSearcher("goaltask");
		
		Data task = tasksearcher.createNewData(); //TODO: Cjheck for existing
		task.setValue("projectgoal",goal.getId());
		task.setValue("projectdepartment",categoryid);
		task.setName(cat.getName()); //TODO: Support comments
		
		Collection goalids = cat.getValues("countdata");
		if( goalids == null)
		{
			goalids = new ArrayList();
		}
		if( !goalids.contains(goalid))
		{
			goalids.add(goalid); //Put in front?
		}
		cat.setValue("countdata",goalids);
		archive.getCategorySearcher().saveData(cat);
		//Add to array on category
		tasksearcher.saveData(task);
		
	}

	public void saveComment(WebPageRequest inReq)
	{
		String taskid = inReq.getRequestParameter("taskid");
		MediaArchive archive = getMediaArchive(inReq);
		Searcher commentsearcher = archive.getSearcher("goaltaskcomments");
		Data newcomment = commentsearcher.createNewData();
		String comment = inReq.getRequestParameter("comment");
		newcomment.setValue("goaltaskid", taskid);
		newcomment.setValue("commenttext", comment);
		newcomment.setValue("author", inReq.getUserName());
		newcomment.setValue("date", new Date());
		commentsearcher.saveData(newcomment);
	}
}
