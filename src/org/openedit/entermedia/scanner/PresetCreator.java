package org.openedit.entermedia.scanner;


import java.util.Date;
import java.util.Iterator;

import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.entermedia.Asset;
import org.openedit.util.DateStorageUtil;

import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;


public class PresetCreator
{
		public void createPresetsForPage(Searcher tasksearcher,Data preset,Asset asset,int thepage)
		{
			SearchQuery taskq = tasksearcher.createSearchQuery().append("assetid", asset.getId() ).append("presetid", preset.getId() );
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
				for (Iterator iterator = hits.iterator(); iterator.hasNext();)
				{
					Data hit = (Data)iterator.next();
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
				found.setProperty("assetid", asset.getId() );
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
