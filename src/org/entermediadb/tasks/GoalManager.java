package org.entermediadb.tasks;

import java.util.Collection;
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
	protected ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}
	protected void setModuleManager(ModuleManager inModuleManager)
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
		MediaArchive archive = (MediaArchive)getModuleManager().getBean("mediaArchive",getCatalogId());
		return archive;
	}
	
	
	protected Data addOne( String taskid, String collectiverole, String roleuserid)
	{
		MediaArchive archive = getMediaArchive(); 
		Data task = (Data)archive.getData("goaltask", taskid);

		Searcher searcher = archive.getSearcher("goaltaskuserrole");
		MultiValued newpoints = (MultiValued)searcher.query().exact("roleuserid",roleuserid).exact("collectiverole",collectiverole).exact("goaltaskid",taskid).searchOne();
		int count = 0;
		if( newpoints == null)
		{
			newpoints = (MultiValued)searcher.createNewData();
			//Set theuser based on the team?
			newpoints.setValue("roleuserid",roleuserid);
			newpoints.setValue("collectiverole",collectiverole);
			newpoints.setValue("goaltaskid",taskid);
		}
		else
		{
			count = newpoints.getInt("actioncount");
		}
		count++;
		
		newpoints.setValue("actioncount",count);
		searcher.saveData(newpoints);
		
		Map role = findRole(task,collectiverole, roleuserid);
		Object totalcountobj = role.get("actioncount");
		int totalcount = Integer.parseInt(totalcountobj.toString());
		totalcount++;
		role.put("actioncount",totalcount);
		archive.saveData("goaltask",task);
		return task;
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

	
}
