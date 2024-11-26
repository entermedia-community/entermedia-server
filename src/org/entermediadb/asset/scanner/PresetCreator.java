package org.entermediadb.asset.scanner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.cache.CacheManager;
import org.openedit.data.Searcher;
import org.openedit.data.ValuesMap;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.PathUtilities;

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
	public Collection getOnImportPresets(MediaArchive inArchive, Asset inAsset)
	{
		if( inAsset == null)
		{
			return Collections.EMPTY_LIST;
		}
		String rendertype = inArchive.getMediaRenderType(inAsset.get("fileformat"));

		if(rendertype == null)
		{
			return Collections.EMPTY_LIST;
		}
		
		Collection both = new ArrayList();
		both.add("all");
		if(rendertype != null) 
		{
			both.add(rendertype);
		}
		
		Collection hits = (Collection)getCacheManager().get("preset_lookup",both.toString());
		if (hits == null)
		{
			Searcher presetsearcher = inArchive.getSearcher("convertpreset");
			SearchQuery query = presetsearcher.createSearchQuery();
			query.addExact("onimport", "true");
			query.addExact("display", "true");
			query.addOrsGroup("inputtype", both);
			query.addSortBy("ordering");
			hits = presetsearcher.search(query);
			getCacheManager().put("preset_lookup",both.toString(), hits);
		}
		return hits;
	}
	
	public Collection getPresets(MediaArchive inArchive, String rendertype)
	{
		if(rendertype == null)
		{
			return Collections.EMPTY_LIST;
		}
		Collection hits = (Collection)getCacheManager().get("preset_lookup",rendertype);
		if (hits == null)
		{
			Searcher presetsearcher = inArchive.getSearcher("convertpreset");
			SearchQuery query = presetsearcher.createSearchQuery();
			query.addMatches("onimport", "true");
			query.addMatches("inputtype", rendertype);
			query.addSortBy("ordering");
			hits = presetsearcher.search(query);
			getCacheManager().put("preset_lookup",rendertype, hits);
		}
		return hits;
	}
	
	public Data getCachedPresetByExternalName(MediaArchive inArchive, String renderType, String inName)
	{
		Data preset = (Data)getCacheManager().get("preset_names",inName);
		Data found = null;
		if( preset == null)
		{
			found = getPresetByOutputName(inArchive, renderType, inName);
			if( found == null)
			{
				found = ValuesMap.NULLDATA;
			}
			getCacheManager().put("preset_names",inName,found);
			return found;
		}
		if(  preset ==  ValuesMap.NULLDATA)
		{
			return null;
		}
		return preset;
	}
	
	public Collection getPushPresets(MediaArchive inArchive, String rendertype)
	{
		if(rendertype == null)
		{
			return Collections.EMPTY_LIST;
		}
		Collection hits = (Collection)getCacheManager().get("push_preset_lookup",rendertype);
		if (hits == null)
		{
			Searcher presetsearcher = inArchive.getSearcher("convertpreset");
			SearchQuery query = presetsearcher.createSearchQuery();
			query.addMatches("onpush", "true");
			//query.addMatches("inputtype", rendertype);
			Collection vals = new ArrayList();
			vals.add(rendertype);
			vals.add("all");
			query.addOrsGroup("inputtype", vals);
			hits = presetsearcher.search(query);
			getCacheManager().put("push_preset_lookup",rendertype, hits);
		}
		return hits;
	}
	
	
	public Collection createMissingOnImport(MediaArchive mediaarchive, Searcher tasksearcher, Data asset)
	{
		Collection found = queueConversions(mediaarchive, tasksearcher, asset, false);
		checkAssetConversions(mediaarchive,  asset,  found ); //Nothing to convert?, try updating status
		return found;
	}
	/**
	 * Called from import code
	 * @param mediaarchive
	 * @param tasksearcher
	 * @param asset
	 */
	public void queueConversions(MediaArchive mediaarchive, Searcher tasksearcher, Data asset)
	{
		if( "needsdownload".equals( asset.get("importstatus") ) )
		{
			return;
		}
		Collection assetconversions = queueConversions(mediaarchive, tasksearcher, asset, true);
		
		checkAssetConversions(mediaarchive,  asset,  assetconversions ); //Nothing to convert, try updating status
		//asset.setProperty("previewstatus","mime");

	}
	
	
	public Collection queueConversions(MediaArchive mediaarchive, Searcher tasksearcher, Data asset, boolean forcererun )
	{
		
		String importstatus = asset.get("importstatus");
		if(importstatus == null || "invalidformat".equals(importstatus))
		{
			//Mime icon
			return Collections.emptyList();
			
		}
		else if ( importstatus.equals("needsmetadata"))
		{
			//if it needsmetadata then dont do it now. The upload will run first
			Asset loaded = (Asset)mediaarchive.getAssetSearcher().loadData(asset);
			ContentItem dest = mediaarchive.getOriginalContent(loaded);
			mediaarchive.getAssetImporter().getAssetUtilities().readMetadata(loaded, dest, mediaarchive);
			asset.setProperty("importstatus", "imported");
			mediaarchive.saveAsset(loaded);

		}
		
		
		String rendertype = mediaarchive.getMediaRenderType(asset.get("fileformat"));
		
		if(rendertype == null)
		{
			if(asset.get("fileformat") == "embedded")
			{
				rendertype = "image";   //assume jpg thumbnail was downloaded
			}
		}
		
		if(rendertype==null)
		{
			//Mime icon
			return Collections.emptyList();
		}
		int added = 0;
		Collection presets = getPresets(mediaarchive,rendertype);
		if( presets.size() == 0)
		{
			return Collections.emptyList();
		}
		boolean missingconversion = false;
		HitTracker conversions = tasksearcher.query().exact("assetid", asset.getId()).orgroup("presetid",presets).search(); //This is slow, we should load up a bunch at once
		HashMap alltasks = new HashMap();
		List tosave = new ArrayList();
		
		boolean settoretry = forcererun;
		
		for (Iterator iterator = conversions.iterator(); iterator.hasNext();)
		{
			Data existing = (Data) iterator.next();
			String page = existing.get("pagenumber");
			if (page == null)
			{
				page = "1";
			}
			if( "error".equals( existing.get("status")))
			{
				settoretry = true;
			}
			else if( !"complete".equals( existing.get("status" ) ) && existing.getValue("submitteddate") == null ) 
			{
				settoretry = true;				
			}
			if( settoretry )
			{
				existing = tasksearcher.loadData(existing);
				existing.setProperty("status","retry");
				existing.setProperty("errordetails",null);
				String nowdate = DateStorageUtil.getStorageUtil().formatForStorage(new Date());
				existing.setProperty("submitteddate", nowdate);
				tosave.add(existing);
				added = added + 1;
			}
			alltasks.put(existing.get("presetid") + page,existing);
		}
		for (Iterator iterator = presets.iterator(); iterator.hasNext();) //Existing ones
		{
			Data preset = (Data) iterator.next();
			added = added + createMissing(mediaarchive, tasksearcher, alltasks, tosave, preset, asset);
		}
		if( tosave.size() > 0)
		{
			tasksearcher.saveAllData(tosave, null);
		}
		return alltasks.values();
	}

	public int createMissing(MediaArchive mediaarchive, Searcher tasksearcher, Map existingtasks, List tosave, Data preset, Data asset)
	{
		int added = 0;
		boolean missingconversion = false;
		
		if (!existingtasks.containsKey(preset.getId() + "1"))//See if the first page is already created.
		{
			missingconversion = true;
			Data created = createPresetsForPage(tasksearcher, preset, asset, 0);
			tosave.add(created);
			existingtasks.put(preset.getId() + "1",created);
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
						if (!existingtasks.containsKey(preset.getId() + pagenum))
						{
							missingconversion = true;
							Data created = createPresetsForPage(tasksearcher, preset, asset, pagenum);
							tosave.add(created);
							added++;
							existingtasks.put(preset.getId() + pagenum,created);
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
		found.setProperty("submitteddate", nowdate);
		if (thepage > 0)
		{
			found.setProperty("pagenumber", String.valueOf(thepage));
		}		
		return found;
	}
	
	public void checkAssetConversions(MediaArchive inArchive, Data asset, Collection assetconversions )
	{
		String validformat = asset.get("importstatus");
		if( "invalidformat".equals(validformat))
		{
			return;
		}
		
		String existingpreviewstatus = asset.get("previewstatus");
		if( log.isDebugEnabled() )
		{
			log.debug("Checking preview status: " + asset.getId() +"/" + existingpreviewstatus);
		}
		boolean allcomplete = true;
		boolean founderror = false;
		
		String existingimportstatus = asset.get("importstatus");
		//check tasks and update the asset status
		for( Object object : assetconversions )
		{
			Data task = (Data)object;
			if( "error".equals( task.get("status") ) )
			{
				//log.info(asset.getId() + " Found an error");
				founderror = true;
				break;
			}
			else if( !"complete".equals( task.get("status") ) )
			{
				allcomplete = false;
				//Keep looking for errors break;
			}
		}	
		//save importstatus
		if( founderror || allcomplete ) 
		{
			//load the asset and save the import status to complete		
			if( asset != null )
			{
				/*
				 * Event fired on importcomplete
				if(founderror && "error".equals(existingimportstatus) || ("complete".equals(existingimportstatus) && "2".equals(existingpreviewstatus)))
				{
					inArchive.fireSharedMediaEvent("publishing/publishassets"); 
					return;						
				}*/
				Asset target = (Asset)inArchive.getAssetSearcher().searchById(asset.getId());
				if( target != null )
				{
					if( founderror)
					{
						target.setProperty("importstatus","error");
						target.setProperty("previewstatus","3");
					}
					else if( target != null)
					{
						target.setProperty("importstatus","complete");
						if( assetconversions.size() > 0)
						{
							target.setProperty("previewstatus","2");
						}
						else if( !"exif".equals(existingpreviewstatus) )  //Is this used?
						{
							target.setProperty("previewstatus","mime");  //Set it to mime most of the time
						}
					}
					inArchive.saveAsset(target, null);
					inArchive.fireMediaEvent("conversionsfinished",null,target); 
				}
				inArchive.fireSharedMediaEvent("importing/importcomplete"); //TODO: add importscomplete for when we have saved a number of items
			}
		}
	}

	public Data getPresetByOutputName(MediaArchive inArchive, String inRenderType, String inFileName)
	{
		return (Data)inArchive.getSearcher("convertpreset").query().exact("generatedoutputfile", inFileName).exact("inputtype", inRenderType).searchOne();
	}


	public void conversionCompleted(MediaArchive inArchive, Asset inAsset)
	{
		Searcher tasksearcher = inArchive.getSearcher("conversiontask");
		HitTracker assetconversions = tasksearcher.query().exact("assetid", inAsset.getId()).search(); //This is slow, we should load up a bunch at once

		checkAssetConversions(inArchive,  inAsset,  assetconversions ); 
		
//		//String existingimportstatus = asset.get("importstatus");
//		String existingpreviewstatus = asset.get("previewstatus");
//		
//		if( !"2".equals( existingpreviewstatus ))
//		{
//			Searcher tasksearcher = getSearcher( "conversiontask");	
//			HitTracker conversions = tasksearcher.query().match("assetid", asset.getId()).search();
//			checkAssetConversions(this, asset, conversions);
//		}

		
	}

	public void clearConversions(MediaArchive inArchive, Searcher tasksearcher, Asset inAsset)
	{
		// TODO We should clear all generated files 
		HitTracker assetconversions = tasksearcher.query().exact("assetid", inAsset.getId()).search(); //This is slow, we should load up a bunch at once
		tasksearcher.deleteAll(assetconversions, null);
	}

	public void reQueueConversions(MediaArchive inArchive, Asset inAsset)
	{
		Searcher tasksearcher = inArchive.getSearcher("conversiontask");

		clearConversions(inArchive,tasksearcher,inAsset);
		queueConversions(inArchive, tasksearcher, inAsset);
	}
	
	public String exportOutputName(MediaArchive inArchive, Data inAsset, String inGeneratedName)
	{ 
		String usefile = inGeneratedName;
		String rendertype = inArchive.getMediaRenderType(inAsset.get("fileformat"));
		String basename = PathUtilities.extractPageName(inGeneratedName);
		String baseextension = PathUtilities.extractPageType(inGeneratedName);
		Data preset = getCachedPresetByExternalName(inArchive,rendertype,basename + ".webp");
		if( preset != null && Boolean.parseBoolean(preset.get("onimport")) )
		{
			usefile = basename + ".webp";
		}
		else
		{
			usefile = basename + "." + baseextension;
		}
		return usefile;
	}

	public ContentItem outPutForGenerated(MediaArchive inArchive, Data inAsset, String inGeneratedType)
	{
		String savedname = exportOutputName(inArchive, inAsset, inGeneratedType);
		String generatedfilename = "/WEB-INF/data/" + inArchive.getCatalogId() + "/generated/" + inAsset.getSourcePath() + "/" + savedname;
		ContentItem output = inArchive.getContent(generatedfilename);
		return output;
		
	}	
	public ContentItem outPutForPreset(MediaArchive inArchive, Asset inAsset, Data inPreset)
	{
		if( "0".equals( inPreset.getId()) )
		{
			Page orig = inArchive.getOriginalDocument(inAsset);
			if(orig.exists() )
			{
				return orig.getContentItem();
			}				
		}
		else
		{
			//Check output file for existance
			String generatedfilename = "/WEB-INF/data/" + inArchive.getCatalogId() + "/generated/" + inAsset.getSourcePath() + "/" + inPreset.get("generatedoutputfile");
			ContentItem output = inArchive.getContent(generatedfilename);
			return output;
		}
		return null;
	}
	public long getLengthOfOutForPreset(MediaArchive inArchive, Asset inAsset, Data inPreset)
	{
		ContentItem item = outPutForPreset(inArchive, inAsset, inPreset);
		if( item == null)
		{
			return -1;
		}
		return item.getLength();
	}
}
