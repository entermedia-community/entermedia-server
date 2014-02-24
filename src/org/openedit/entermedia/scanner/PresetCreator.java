package org.openedit.entermedia.scanner;


import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.util.DateStorageUtil;

import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;


public class PresetCreator
{
	private static final Log log = LogFactory.getLog(PresetCreator.class);

	protected Map<String,Collection> fieldPresetCache;
	
	protected Collection getPresets(String rendertype)
	{
		if (fieldPresetCache == null)
		{
			fieldPresetCache = new HashMap<String,Collection>();
		}
		return fieldPresetCache.get(rendertype);
	}
	
	public int createMissingOnImport(MediaArchive mediaarchive, Searcher tasksearcher, Asset asset)
	{
		String rendertype = mediaarchive.getMediaRenderType(asset.getFileFormat());
		
		int added = 0;
		Collection hits = getPresets(rendertype);
		if( hits == null )
		{
			Searcher presetsearcher = mediaarchive.getSearcher("convertpreset");
			SearchQuery query = presetsearcher.createSearchQuery();
			query.addMatches("onimport", "true");
			query.addMatches("inputtype", rendertype);
			hits = presetsearcher.search(query);
			fieldPresetCache.put(rendertype, hits);
		}
		
		boolean missingconversion = false;
		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
		{
			Data preset = (Data) iterator.next();
			//Data preset = (Data) presetsearcher.searchById(it.id);
			added = added + createMissing(mediaarchive,tasksearcher,preset,asset);
		}
		return added;
	}

	
	public int createMissing(MediaArchive mediaarchive, Searcher tasksearcher, Data preset, Asset asset)
	{
		int added = 0;
		boolean missingconversion = false;
		if (!mediaarchive.doesAttachmentExist(asset,preset,0) )
		{
			missingconversion = true;
			createPresetsForPage(tasksearcher, preset, asset,0);
			added++;
		}
		String pages = asset.get("pages");
		if( pages != null )
		{
			int npages = Integer.parseInt(pages);
			if( npages > 1 )
			{
				for (int i = 1; i < npages; i++)
				{
					int pagenum = i + 1;
					if (!mediaarchive.doesAttachmentExist(asset,preset,pagenum) )
					{
						missingconversion = true;
						createPresetsForPage(tasksearcher, preset, asset, pagenum);
						added++;
					}
				}
			}
		}
		return added;
	}
	public Data createPresetsForPage(Searcher tasksearcher,Data preset,Asset asset)
	{
		return createPresetsForPage(tasksearcher,preset,asset,0);
	}
	public Data createPresetsForPage(Searcher tasksearcher,Data preset,Asset asset,int thepage)
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
				if( !"new".equals( found.get("status") ) )
				{
					found = (Data)tasksearcher.searchById(found.getId());
					if( found != null )
					{
						found.setProperty("status", "new");
						tasksearcher.saveData(found, null);
						return found;
					}
					else
					{
						log.error("Conversion tasks index is out of date ${found.getId()}");
					}
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
				if( thepage > 0 )
				{
					found.setProperty("pagenumber", String.valueOf(thepage));
				}
				tasksearcher.saveData(found, null);
				return found;
			}
			return found;
		}
		
	}
