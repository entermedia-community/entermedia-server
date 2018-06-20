package org.entermediadb.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.projects.LibraryCollection;
import org.openedit.Data;
import org.openedit.WebPageRequest;
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
			cat.setName("Departments");
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
		
		//TODO: Limit it by department
		HitTracker tracker = searcher.query().exact("collectionid", collection.getId()).search();
		inReq.putPageValue("goals", tracker);
		Collection topgoals = new ArrayList();
		Collection ten = new ArrayList();
		topgoals.add(ten);
		for (Iterator iterator = tracker.iterator(); iterator.hasNext();)
		{
			Data hit = (Data) iterator.next();
			ten.add(hit);
			if( ten.size() == 10)
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
}
