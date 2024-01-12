package org.entermediadb.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.entermediadb.asset.MediaArchive;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.MultiValued;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.users.Group;
import org.openedit.users.User;

public class GoalManager implements CatalogEnabled
{
	protected ModuleManager fieldModuleManager;
	
	public GoalManager()
	{
		// TODO Auto-generated constructor stub
	}
	
	protected ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}
	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}
	protected String getCatalogId()
	{
		return fieldCatalogId;
	}
	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}
	protected String fieldCatalogId;
	
	protected MediaArchive getMediaArchive()
	{
		MediaArchive archive = (MediaArchive)getModuleManager().getBean(getCatalogId(),"mediaArchive");
		return archive;
	}
	
	
	protected Data addOne( String taskid, String collectiverole, String roleuserid)
	{
		MediaArchive archive = getMediaArchive(); 
		Data task = (Data)archive.getData("goaltask", taskid);

		Searcher searcher = archive.getSearcher("goaltaskuserrole");
		Collection existing = searcher.query().exact("roleuserid",roleuserid).exact("collectiverole",collectiverole).exact("goaltaskid",taskid).search();
		
		Data newpoints = (MultiValued)searcher.createNewData();
			//Set theuser based on the team?
			newpoints.setValue("roleuserid",roleuserid);
			newpoints.setValue("collectiverole",collectiverole);
			newpoints.setValue("goaltaskid",taskid);
		newpoints.setValue("date",new Date());
		newpoints.setValue("actioncount",1);
		searcher.saveData(newpoints);
		
		Map role = findRole(task,collectiverole, roleuserid);
		Object totalcountobj = role.get("actioncount");
		int totalcount = Integer.parseInt(totalcountobj.toString());
		totalcount++;
		role.put("actioncount",existing.size());
		archive.saveData("goaltask",task);
		return task;
	}

	public Map findRole(String inTaskId, String collectiverole, String roleuserid)
	{
		Data task = (Data)getMediaArchive().getCachedData("goaltask", inTaskId);

		Collection roles = (Collection)task.getValue("taskroles");
		Map role = findRole(roles,collectiverole, roleuserid);
		return role;
	}

	public Map findRole(Data inTask, String collectiverole, String roleuserid)
	{
		Collection roles = (Collection)inTask.getValue("taskroles");
		Map role = findRole(roles,collectiverole, roleuserid);
		return role;
	}
	protected Map findRole(Collection<Map> inRows, String inCollectiverole, String inroleuserid)
	{
		if( inRows == null|| inRows.isEmpty())
		{
			return null;
		}
		for(Map row : inRows)
		{
			String roleid = (String)row.get("collectiverole");
			if(inCollectiverole.equals(roleid))
			{
				String roleuserid = (String)row.get("roleuserid");
				if( roleuserid == null || inroleuserid.equals(inroleuserid))
				{
					return row;
				}
			}
		}
		return null;
	}

	public Collection listActions(String inTaskId, String inRoleId, String roleuserid)
	{
		Searcher searcher = getMediaArchive().getSearcher("goaltaskuserrole");
		
		Collection points = searcher.query().exact("roleuserid",roleuserid).exact("collectiverole",inRoleId)
				.exact("goaltaskid",inTaskId).sort("date").search();
		
		return points;
		
	}

	public void addStatus(MediaArchive archive, MultiValued selectedgoal, String editedby)
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
				Data auser = (Data)iterator.next();
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
	
	public Data createGoal(WebPageRequest inReq, Data message)
	{
		MediaArchive archive = getMediaArchive();
		Searcher chatsearcher = archive.getSearcher("chatterbox");
		
		String topic = message.get("channel");
		String content = message.get("message");
		String collectionid = inReq.getRequestParameter("collectionid");

		Searcher searcher = archive.getSearcher("projectgoal");
		MultiValued goal = (MultiValued)searcher.query().exact("chatparentid", message.getId()).searchOne();
		if( goal == null)
		{
			goal = (MultiValued)searcher.createNewData();
			goal.setValue("goaltrackercolumn", topic);
			goal.setValue("tickettype", "chat");
			goal.setValue("ticketlevel", "2");
			goal.setValue("projectstatus", "open");
			goal.setValue("creationdate",new Date());
			goal.setValue("collectionid", collectionid);
			goal.setValue("chatparentid", message.getId());
			goal.addValue("userlikes",inReq.getUserName());
			goal.setValue("owner", inReq.getUserName());
			goal.addValue("details",content);
			if( content != null && content.length() > 70)
			{
				content = content.substring(0,70) + "...";
			}
			User user = archive.getUser(message.get("user"));
			if( user != null)
			{
				goal.setName(user.getScreenName() + ": " + content);
			}
			else
			{
				goal.setName("Anonymous : " + content);				
			}
			searcher.saveData(goal);
			addStatus(archive, goal,inReq.getUserName());

			//Create actions/Tasks on this goal
			createActions(goal,message);
			
		}
		
		
		return goal;
	}
	
	
	
	public void createActions(MultiValued inGoal, Data inMessage)
	{
		Searcher tasksearcher = (Searcher)getMediaArchive().getSearcher("goaltask");

		String content = inMessage.get("message");
		
		//Split on numbers
		String[] tasks = content.split("[\\r\\n]+"); //, /gm);
		//"[{}]"
		Collection tosave = new ArrayList();
		for (int i = 0; i < tasks.length; i++)
		{
			//TODO: Check for items with numbers?
			//Spit out everything else back to chat?
			
			Data task = tasksearcher.createNewData();
			String userid = inMessage.get("user");
			task.setValue("projectgoal",inGoal.getId());
			String collectionid = inGoal.get("collectionid");
			task.setValue("collectionid",collectionid);
			//task.setValue("projectdepartment",categoryid);
			task.setValue("completedby", userid );
			task.setValue("taskstatus", "6"); //On Agenda
			//task.setValue("projectdepartmentparents",cat.getParentCategories());
			task.setValue("creationdate",new Date());
			//TODO add Ordering and add when pyshing agendas
			task.setValue("comment", tasks[i]);
			//task.setName(tasks[i]);
			tosave.add(task);
			
		}
		tasksearcher.saveAllData(tosave,null);
		
	}	
	
}