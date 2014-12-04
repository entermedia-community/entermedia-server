package org.openedit.entermedia.scanner;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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

	protected Map<String, Collection> fieldPresetCache;

	protected Collection getPresets(String rendertype)
	{
		if (fieldPresetCache == null)
		{
			fieldPresetCache = new HashMap<String, Collection>();
		}
		return fieldPresetCache.get(rendertype);
	}

	public int createMissingOnImport(MediaArchive mediaarchive, Searcher tasksearcher, Data asset)
	{
		String rendertype = mediaarchive.getMediaRenderType(asset.get("fileformat"));

		int added = 0;
		Collection hits = getPresets(rendertype);
		if (hits == null)
		{
			Searcher presetsearcher = mediaarchive.getSearcher("convertpreset");
			SearchQuery query = presetsearcher.createSearchQuery();
			query.addMatches("onimport", "true");
			query.addMatches("inputtype", rendertype);
			hits = presetsearcher.search(query);
			fieldPresetCache.put(rendertype, hits);
		}

		boolean missingconversion = false;
		HitTracker taskq = tasksearcher.query().match("assetid", asset.getId()).search(); //This is so dumb
		HashSet existingtasks = new HashSet();
		for (Iterator iterator = taskq.iterator(); iterator.hasNext();)
		{
			Data existing = (Data) iterator.next();
			String page = existing.get("pagenumber");
			if (page == null)
			{
				page = "1";
			}
			existingtasks.add(existing.get("presetid") + page);

		}

		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
		{

			Data preset = (Data) iterator.next();
			added = added + createMissing(mediaarchive, tasksearcher, existingtasks, preset, asset);
		}
		
			mediaarchive.updateAssetConvertStatus(asset);
		
		return added;
	}

	public int createMissing(MediaArchive mediaarchive, Searcher tasksearcher, HashSet existingtasks, Data preset, Data asset)
	{
		int added = 0;
		boolean missingconversion = false;
		if (!existingtasks.contains(preset.getId() + "1"))//See if the first page is already created.
		{
			missingconversion = true;
			createPresetsForPage(tasksearcher, preset, asset, 0);
			added++;
		}
		Boolean onlyone = Boolean.parseBoolean(preset.get("singlepage"));
		if (!onlyone)
		{
			String pages = asset.get("pages");
			if (pages != null)
			{
				int npages = Integer.parseInt(pages);
				if (npages > 1)
				{
					for (int i = 1; i < npages; i++)
					{
						int pagenum = i + 1;
						if (!existingtasks.contains(preset.getId() + pagenum))
							;
						{
							missingconversion = true;
							createPresetsForPage(tasksearcher, preset, asset, pagenum);
							added++;
						}
					}
				}
			}
		}
		return added;
	}

	public Data createPresetsForPage(Searcher tasksearcher, Data preset, Asset asset)
	{
		return createPresetsForPage(tasksearcher, preset, asset, 0);
	}

	public Data createPresetsForPage(Searcher tasksearcher, Data preset, Data asset, int thepage)
	{
		Data found = createPresetsForPage(tasksearcher, preset, asset, thepage, false);
		return found;
	}

	public Data createPresetsForPage(Searcher tasksearcher, Data preset, Data asset, int thepage, boolean createall)
	{
		Data found = tasksearcher.createNewData();
		found.setSourcePath(asset.getSourcePath());
		found.setProperty("status", "new");
		found.setProperty("assetid", asset.getId());
		found.setProperty("presetid", preset.getId());
		found.setProperty("ordering", preset.get("ordering"));
		String nowdate = DateStorageUtil.getStorageUtil().formatForStorage(new Date());
		found.setProperty("submitted", nowdate);
		if (thepage > 0)
		{
			found.setProperty("pagenumber", String.valueOf(thepage));
		}
		tasksearcher.saveData(found, null);
		
		return found;

	}
}
