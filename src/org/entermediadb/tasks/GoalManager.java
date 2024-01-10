package org.entermediadb.tasks;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.entermediadb.asset.MediaArchive;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.MultiValued;
import org.openedit.data.Searcher;

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
	
	
	
}
