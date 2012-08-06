package assets.model;

import org.openedit.data.Searcher
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.*
import org.openedit.entermedia.creator.*
import org.openedit.entermedia.edit.*
import org.openedit.entermedia.episode.*
import org.openedit.entermedia.modules.*
import org.openedit.util.DateStorageUtil;
import org.openedit.xml.*

import com.openedit.hittracker.*
import com.openedit.page.*
import com.openedit.util.*

import conversions.*

public class PresetCreator
{
	public void createPresetsForPage(Searcher tasksearcher,Data preset,Asset asset,int thepage)
	{
		SearchQuery taskq = tasksearcher.createSearchQuery().append("assetid", asset.id).append("presetid", preset.id);
		if( thepage > 0 )
		{
			taskq.append("pagenumber",String.valueOf(thepage));
		}
		Data found = null;
		HitTracker hits = tasksearcher.search(taskq);
		if ( hits.size() == 1 )
		{
			found = (Data)hits.first(); //there will be only once most of the time
		}
		else if ( hits.size() > 1 )
		{
			for( Data hit: hits )//Look for the right one
			{
				if( hit.get("pagenumber") == null )
				{
					found = hit;
					break;
				}
			}
		}
		if( found != null )
		{
			//If it is complete then the converter will mark it complete again
			if( found.get("status") != "new")
			{
				found = (Data)tasksearcher.searchById(found.getId());
				found.setProperty("status", "new");
				tasksearcher.saveData(found, null);
			}
		}
		else
		{
			found = tasksearcher.createNewData();
			found.setSourcePath(asset.getSourcePath());
			found.setProperty("status", "new");
			found.setProperty("assetid", asset.id);
			found.setProperty("presetid", preset.getId() );
			found.setProperty("ordering", preset.get("ordering") );
			String nowdate = DateStorageUtil.getStorageUtil().formatForStorage(new Date() );
			found.setProperty("submitted", nowdate);
			tasksearcher.saveData(found, null);
			if( thepage > 0 )
			{
				found.setProperty("pagenumber", String.valueOf(thepage));
			}
			tasksearcher.saveData(found, null);
		}
	}
	
}