package org.entermediadb.asset.scanner;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.cache.CacheManager;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.util.DateStorageUtil;

public class PresetCreator
{
	private static final Log log = LogFactory.getLog(PresetCreator.class);

	protected CacheManager fieldCacheManager;

	public CacheManager getCacheManager()
	{
		return fieldCacheManager;
	}

	public void setCacheManager(CacheManager inCacheManager)
	{
		fieldCacheManager = inCacheManager;
	}
	public void clearCaches()
	{
		getCacheManager().clear("preset_lookup");
	}
	protected Collection getPresets(MediaArchive inArchive, String rendertype)
	{
		Collection hits = (Collection)getCacheManager().get("preset_lookup",rendertype);
		if (hits == null)
		{
			Searcher presetsearcher = inArchive.getSearcher("convertpreset");
			SearchQuery query = presetsearcher.createSearchQuery();
			query.addMatches("onimport", "true");
			query.addMatches("inputtype", rendertype);
			hits = presetsearcher.search(query);
			getCacheManager().put("preset_lookup",rendertype, hits);
		}
		return hits;
	}
	public int retryConversions(MediaArchive mediaarchive, Searcher tasksearcher, Data asset)
	{
		return queueConversions(mediaarchive, tasksearcher, asset, true);
	}
	public int createMissingOnImport(MediaArchive mediaarchive, Searcher tasksearcher, Data asset)
	{
		return queueConversions(mediaarchive, tasksearcher, asset, false);
	}
	public int queueConversions(MediaArchive mediaarchive, Searcher tasksearcher, Data asset, boolean rerun )
	{
		String rendertype = mediaarchive.getMediaRenderType(asset.get("fileformat"));
		if(rendertype==null){
			//return?
			return 0;
		}
		int added = 0;
		Collection hits = getPresets(mediaarchive,rendertype);
		

		boolean missingconversion = false;
		HitTracker conversions = tasksearcher.query().match("assetid", asset.getId()).search(); //This is so dumb
		HashSet existingtasks = new HashSet();
		List tosave = new ArrayList();
		for (Iterator iterator = conversions.iterator(); iterator.hasNext();)
		{
			Data existing = (Data) iterator.next();
			String page = existing.get("pagenumber");
			if (page == null)
			{
				page = "1";
			}
			existingtasks.add(existing.get("presetid") + page);
			if( rerun || "error".equals( existing.get("status")) )
			{
				existing = tasksearcher.loadData(existing);
				existing.setProperty("status","retry");
				existing.setProperty("errordetails",null);
				String nowdate = DateStorageUtil.getStorageUtil().formatForStorage(new Date());
				existing.setProperty("submitted", nowdate);
				tosave.add(existing);
			}
		}
		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
		{
			Data preset = (Data) iterator.next();
			added = added + createMissing(mediaarchive, tasksearcher, existingtasks, tosave, preset, asset);
		}
		if( tosave.size() > 0)
		{
			tasksearcher.saveAllData(tosave, null);
		}
		else
		{
			//updateAssetImportStatus(mediaarchive,  asset,  conversions ); //Nothing to convert, try updating status
		}
		return added;
	}

	public int createMissing(MediaArchive mediaarchive, Searcher tasksearcher, HashSet existingtasks, List tosave, Data preset, Data asset)
	{
		int added = 0;
		boolean missingconversion = false;
		
		if (!existingtasks.contains(preset.getId() + "1"))//See if the first page is already created.
		{
			missingconversion = true;
			Data created = createPresetsForPage(tasksearcher, preset, asset, 0);
			tosave.add(created);
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
						{
							missingconversion = true;
							Data created = createPresetsForPage(tasksearcher, preset, asset, pagenum);
							tosave.add(created);
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
		//TODO: Remove this 
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
		return found;
	}
	public void updateAssetPreviewStatus(MediaArchive inArchive, Data asset, HitTracker conversions )
	{
		if( conversions.size() == 0 )
		{
			log.info("No conversions queued " + asset.getSourcePath());
			return;
		}
		String existingpreviewstatus = asset.get("previewstatus");
		//is it already complete?
		
		//log.info("existingpreviewstatus" + existingpreviewstatus);
		//update importstatus and previewstatus to complete
		if( log.isDebugEnabled() )
		{
			log.debug("Checking preview status: " + asset.getId() +"/" + existingpreviewstatus);
		}
		boolean allcomplete = true;
		boolean founderror = false;
		
		String existingimportstatus = asset.get("previewstatus");
	
		if( existingpreviewstatus == null || !"2".equals( existingpreviewstatus ) )
		{
			//check tasks and update the asset status
			Searcher tasksearcher = inArchive.getSearcher( "conversiontask");	
			for( Object object : conversions )
			{
				Data task = (Data)object;
				if( "error".equals( task.get("status") ) )
				{
					log.info(asset.getId() + "Found an error");
					founderror = true;
					break;
				}
	
				if( !"complete".equals( task.get("status") ) )
				{
					allcomplete = false;
					log.info("Found an incomplete task - status was: " + task.get("status") + " " + asset.getId());
					String date = task.get("submitted");
					if( "missinginput".equals( task.get("status") ) && date != null)
					{
						Date entered = DateStorageUtil.getStorageUtil().parseFromStorage(date);
						GregorianCalendar cal = new GregorianCalendar();
						cal.add(Calendar.DAY_OF_YEAR, -2);
						if( entered.before(cal.getTime()))
						{
							Data loadedtask = (Data)tasksearcher.loadData(task);
							loadedtask.setProperty("status","error");
							loadedtask.setProperty("errordetails","Image missing more than 24 hours, marked as error");
							tasksearcher.saveData(loadedtask, null);
							founderror = true;
						}
					}
					else
					{
						break;
					}
				}
			}	
		}
		
		
		//save importstatus
		if( founderror || allcomplete )
		{
			//load the asset and save the import status to complete		
			if( asset != null )
			{
				if(founderror && "error".equals(existingimportstatus) || "complete".equals(existingimportstatus))
				{
					return;						
				}
				Asset target =  (Asset)inArchive.getAssetSearcher().loadData(asset);
				if( founderror)
				{
					target.setProperty("importstatus","error");
					target.setProperty("previewstatus","3");
				}
				else
				{
					target.setProperty("importstatus","complete");
					target.setProperty("previewstatus","2");
					
				}
				inArchive.saveAsset(target, null);
			}
		}
	}
}
