package org.entermediadb.asset.modules;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.upload.FileUpload;
import org.entermediadb.asset.upload.FileUploadItem;
import org.entermediadb.asset.upload.UploadRequest;
import org.entermediadb.asset.util.MathUtils;
import org.entermediadb.video.CloudTranscodeManager;
import org.entermediadb.video.Timeline;
import org.entermediadb.video.VTT.Cue;
import org.entermediadb.video.VTT.webvtt.WebvttParser;
import org.entermediadb.video.VTT.webvtt.WebvttSubtitle;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.locks.Lock;
import org.openedit.modules.translations.Translation;
import org.openedit.page.Page;
import org.openedit.util.FileUtils;

public class TimelineModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(TimelineModule.class);

	public void loadTimeline(WebPageRequest inReq)
	{
		Asset asset = (Asset)inReq.getPageValue("asset");
		MediaArchive archive = getMediaArchive(inReq);
		if(asset == null){
			String id = inReq.getRequestParameter("assetid");
			if(id != null){
				asset = archive.getAsset(id);
			}
		}
		if(asset == null){
			return;
		}
		Double videolength = (Double)asset.getDouble("length");
		if( videolength == null)
		{
			return;
		}
		Timeline timeline = new Timeline();
		long mili = Math.round( videolength*1000d );
		timeline.setLength(mili);
		timeline.setPxWidth(1200);
		
//		String selected = inReq.getRequestParameter("timecodejump");
//		timeline.selectClip(selected);
		
		inReq.putPageValue("timeline", timeline);
	}
	
	public void loadTimeLineManager(WebPageRequest inReq)
	{
		
	}

	public void saveClips(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		
		Map values = inReq.getJsonRequest();
		if( values == null)
		{
			throw new OpenEditException("No clips received");
		}
		String assetid = (String)values.get("assetid");
		Asset asset = archive.getAsset(assetid);
		
		Collection clips = (Collection)values.get("clips");
		for (Iterator iterator = clips.iterator(); iterator.hasNext();)
		{
			Map clip = (Map) iterator.next();
			MathUtils.cleanLongTypes(clip);			
		}
		log.info(clips);
		asset.setValue("clips", clips);
		
		archive.saveAsset(asset);
	}
	public static void cleanTypes(Map inMap)
	{
		Collection keys = new ArrayList(inMap.keySet());
		for (Iterator iterator = keys.iterator(); iterator.hasNext();)
		{
			String type = (String) iterator.next();
			Object m = inMap.get(type);
			if( m instanceof BigDecimal)
			{
				inMap.put(type, ((BigDecimal)m).doubleValue() );
			}
			else if( m instanceof Integer)
			{
				inMap.put(type, Double.parseDouble( m.toString() ) );
			}
		}
	}	
	public void addClip(WebPageRequest inReq)
	{
		
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		
		String field = inReq.getRequestParameter("parentfield");
		Collection timeline = asset.getValues(field);
		if(timeline == null)
		{
			timeline = new ArrayList();
		}
		
		String [] fields = inReq.getRequestParameters("field");
		Searcher assetsearcher = archive.getAssetSearcher();
		HashMap codemap = new HashMap();
		for (int i = 0; i < fields.length; i++)
		{
			String id = fields[i];
			String val = inReq.getRequestParameter(id + ".value");
			if(val != null)
			{
				if( id.equals("timecodestart") || id.equals("timecodelength"))
				{
					double duration = MathUtils.parseDuration(val);
					codemap.put(id, duration);
				}
				else //More Data typing ie. date??
				{
					codemap.put(id, val);
				}	
			}
		}
		timeline.add(codemap);
		
		asset.setValue(field, timeline);
		assetsearcher.saveData(asset);
		
	}
	
	public void loadClosedCaption(WebPageRequest inReq) 
	{
		if( inReq.getResponse() != null)
		{
			inReq.getResponse().setHeader("Access-Control-Allow-Origin","*");
		}
		MediaArchive archive = getMediaArchive(inReq);
		
		Searcher captionsearcher = archive.getSearcher("videotrack");
		Asset asset = getAsset(inReq);
		
		String type = inReq.getContentPage().getPageName();
		int place = type.lastIndexOf("-");
		String lang = null;
		if( place == type.length() - 3) 
		{
			lang = type.substring(place + 1);
			//track.setProperty("sourcelang", lang);
		}
		if( lang == null)
		{
			lang = inReq.getLanguage();
		}
		
		MultiValued track = (MultiValued)captionsearcher.query().exact("assetid", asset.getId()).exact("sourcelang", lang).sort("timecodestart").searchOne();
		track = (MultiValued)captionsearcher.loadData(track);
		inReq.putPageValue("track", track);
		inReq.putPageValue("captionsearcher", captionsearcher);
		
	}
	
	
	public void loadClosedCaptions(WebPageRequest inReq) 
	{
		if( inReq.getResponse() != null)
		{
			inReq.getResponse().setHeader("Access-Control-Allow-Origin","*");
		}
		MediaArchive archive = getMediaArchive(inReq);
		
		Searcher captionsearcher = archive.getSearcher("videotrack");
		Asset asset = getAsset(inReq);
		
		Collection tracks = captionsearcher.query().exact("assetid", asset.getId()).sort("sourcelang").search(inReq);
		inReq.putPageValue("tracks", tracks);
		inReq.putPageValue("captionsearcher", captionsearcher);
		
	}
	
	public void importCaptions(WebPageRequest inReq) throws Exception
	{
		
		MediaArchive archive = getMediaArchive(inReq);
		
		
		FileUpload command = (FileUpload) archive.getModuleManager().getBean("fileUpload");
		UploadRequest properties = command.parseArguments(inReq);
		
		Asset asset = getAsset(inReq);
	
		
		FileUploadItem item = properties.getFirstItem();
		String fname = item.getName();
		String path = "/WEB-INF/temp/" + archive.getCatalogId() + "/uploads-"
				+ inReq.getUserName() + "-" + fname;		
		properties.saveFileAs(properties.getFirstItem(), path, inReq.getUser());

		
		String[] fields = inReq.getRequestParameters("field");
		
		//Collection timeline = new ArrayList();
		
		Page page = archive.getPageManager().getPage(path);
		
		WebvttParser parser = new WebvttParser();
		InputStream input = page.getInputStream();
		WebvttSubtitle titles = parser.parse(input);
		FileUtils.safeClose(input);
		Searcher searcher = archive.getSearcher("videotrack");
		
		Collection captions = new ArrayList();
		
		Data track = searcher.createNewData();
		if(fields != null)
		{
			searcher.updateData(inReq, fields, track);
		}
		String selectedlang = inReq.getRequestParameter("selectedlang");
		if( selectedlang == null)
		{	
			selectedlang = inReq.getLocale();
		}
		track.setProperty("sourcelang", selectedlang);
		
		for (Iterator iterator = titles.getCues().iterator(); iterator.hasNext();)
		{
			Cue cue = (Cue) iterator.next();
			
			HashMap cuemap = new HashMap();
			cuemap.put("cliplabel", cue.getText().toString());
			cuemap.put("alignment", cue.getAlignment());
			long start = cue.getStartTime();
			cuemap.put("timecodestart", start);
			long length = cue.getEndTime() - cue.getStartTime();
			cuemap.put("timecodelength", length);
			captions.add(cuemap);
		}
		track.setValue("captions", captions);
		track.setValue("assetid", asset.getId());

		//Remove old ones
		HitTracker existing = searcher.query().exact("assetid", asset.getId()).exact("sourcelang", selectedlang).search(); //TODO per lang
		searcher.deleteAll(existing, null);
		
		searcher.saveData(track);
		getPageManager().removePage(page);
		
	}
	public void addCaption(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		
		Searcher captionsearcher = archive.getSearcher("videotrack");
		Asset asset = getAsset(inReq);
	
		String selectedlang = (String)inReq.getSessionValue("selectedlang");
		if( selectedlang == null)
		{
			selectedlang = inReq.getLanguage();
		}
		Data lasttrack = captionsearcher.query().exact("assetid", asset.getId()).exact("sourcelang", selectedlang).searchOne();
		if( lasttrack == null)
		{
			log.info("Creating track " + asset.getId() + " " + selectedlang);
			lasttrack = captionsearcher.createNewData();
			lasttrack.setProperty("sourcelang", selectedlang);
			lasttrack.setProperty("assetid",  asset.getId());
		}
		else
		{
			lasttrack = captionsearcher.loadData(lasttrack);
		}
		Collection captions = (Collection)lasttrack.getValue("captions");
		if( captions == null)
		{
			captions = new ArrayList();
		}
		Map cuemap = new HashMap();
		
		String cliplabel = inReq.getRequestParameter("cliplabel");
		cuemap.put("cliplabel", cliplabel);

		String starttime = inReq.getRequestParameter("timecodestart");
		cuemap.put("timecodestart", Long.parseLong( starttime) );

		String timecodelength = inReq.getRequestParameter("timecodelength");
		cuemap.put("timecodelength", Long.parseLong( timecodelength ));
		log.info("Saved " + starttime + " " + timecodelength);
		captions = removeDuplicate(captions,cuemap);
		if( cliplabel != null && !cliplabel.isEmpty() )
		{
			captions.add(cuemap);			
		}
		lasttrack.setValue("captions",captions);
		captionsearcher.saveData(lasttrack);
	}	
	protected Collection removeDuplicate(Collection inCaptions, Map inCuemap)
	{
		Long start = (Long)inCuemap.get("timecodestart");
		Long length = (Long)inCuemap.get("timecodelength");
		
		Collection copy = new ArrayList(inCaptions);
		
		for (Iterator iterator = inCaptions.iterator(); iterator.hasNext();)
		{
			Map existing = (Map) iterator.next();
			Long exstart = (Long)existing.get("timecodestart");
			Long exlength = (Long)existing.get("timecodelength");
			if(exstart.equals(start) && exlength.equals( length))
			{
				copy.remove(existing);
			}
		}
		return copy;
		
	}

	public void deleteTrack(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		
		Searcher captionsearcher = archive.getSearcher("videotrack");
		Asset asset = getAsset(inReq);
	
		String selectedlang = (String)inReq.getSessionValue("selectedlang");
		if( selectedlang == null)
		{
			selectedlang = inReq.getLanguage();
		}
		Data lasttrack = captionsearcher.query().exact("assetid", asset.getId()).exact("sourcelang", selectedlang).searchOne();
	
		captionsearcher.delete(lasttrack,null);
	}	
	public void switchLang(WebPageRequest inReq)
	{
		String selectedlang = (String)inReq.getRequestParameter("selectedlang");
		inReq.putSessionValue("selectedlang",selectedlang);
	}
	public void loadCaptionEditor(WebPageRequest inReq)
	{
		Timeline timeline = (Timeline)inReq.getPageValue("timeline");
		if( timeline != null )
		{
			return;
		}
		MediaArchive archive = getMediaArchive(inReq);
		
		Searcher captionsearcher = archive.getSearcher("videotrack");
		Asset asset = getAsset(inReq);
		//Select the current language
		String selectedlang = (String)inReq.getSessionValue("selectedlang");
		if( selectedlang == null)
		{
			selectedlang = inReq.getLanguage();
		}
		inReq.putPageValue("selectedlang", selectedlang);
		
		//Available languages from a list?
		
		MultiValued track = (MultiValued)captionsearcher.query().exact("assetid", asset.getId()).exact("sourcelang", selectedlang).sort("timecodestart").searchOne();
		track = (MultiValued)captionsearcher.loadData(track);
		inReq.putPageValue("track", track);
		inReq.putPageValue("captionsearcher", captionsearcher);
		
		Double videolength = (Double)asset.getDouble("length");
		if( videolength == null)
		{
			return;
		}
		timeline = new Timeline();
		long mili = Math.round( videolength*1000d );
		timeline.setLength(mili);
		timeline.setPxWidth(1200);
		timeline.loadClips(track,"captions");
		inReq.putPageValue("timeline", timeline);

		//		String selected = inReq.getRequestParameter("timecodejump");
//		timeline.selectClip(selected);
		
		inReq.putPageValue("timeline", timeline);
	}
	
	public void addAutoTranscode(WebPageRequest inReq) throws Exception
	{
		//<path-action name="PathEventModule.runSharedEvent" runpath="/${catalogid}/events/conversions/autotranscode.html" allowduplicates="true" />
		MediaArchive archive = getMediaArchive(inReq);

		CloudTranscodeManager manager = (CloudTranscodeManager)getModuleManager().getBean(archive.getCatalogId(), "cloudTranscodeManager");

		
		
		String selectedlang = inReq.getRequestParameter("selectedlang");

		Asset asset = getAsset(inReq);
		
		
		Data lasttrack = manager.addAutoTranscode(archive, selectedlang, asset, inReq.getUserName());
		
		
		inReq.putPageValue("track", lasttrack);
//		inRe
//		CloudTranscodeManager manager = (CloudTranscodeManager)getModuleManager().getBean(catalogid, "cloudTranscodeManager");
//		manager.transcodeCaptions(asset,selectedlang);
		archive.fireSharedMediaEvent("asset/autotranscribe");
	}
	public void autoTranscodeQueue(WebPageRequest inReq) throws Exception
	{
		//<path-action name="PathEventModule.runSharedEvent" runpath="/${catalogid}/events/conversions/autotranscode.html" allowduplicates="true" />

		MediaArchive archive = getMediaArchive(inReq);
		CloudTranscodeManager manager = (CloudTranscodeManager)getModuleManager().getBean(archive.getCatalogId(), "cloudTranscodeManager");

		Searcher captionsearcher = archive.getSearcher("videotrack");

		String selectedlang = inReq.getRequestParameter("selectedlang");
		Collection hits = captionsearcher.query().or().exact("transcribestatus", "inprogress").exact("transcribestatus", "needstranscribe").search();
		log.info("Transcribing: " + hits.size() );
		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
		{
			Data track = (Data) iterator.next();
			Lock lock = archive.getLockManager().lockIfPossible("transcode" + track.get("assetid"), "autoTranscodeQueue");
			try
			{
				if( lock != null)
				{
//					manager.transcodeCaptions(track);
//					track.setValue("transcribestatus", "complete");
					

					manager.asyncTranscodeCaptions(track);
				}
			}
			catch (Throwable ex)
			{
				log.error("Could not prcoess" , ex);
				track.setValue("transcribestatus", "error");				
			} 
			finally
			{
				captionsearcher.saveData(track);
				archive.releaseLock(lock);
			}
		}
	}	
	public void addAutoTranslate(WebPageRequest inReq) throws Exception
	{
		//<path-action name="PathEventModule.runSharedEvent" runpath="/${catalogid}/events/conversions/autotranscode.html" allowduplicates="true" />
		MediaArchive archive = getMediaArchive(inReq);
		String selectedlang = inReq.getRequestParameter("selectedlang");
		String targetlang = inReq.getRequestParameter("targetlang");

		Asset asset = getAsset(inReq);
		Searcher captionsearcher = archive.getSearcher("videotrack");
		Data lasttrack = captionsearcher.query().exact("assetid", asset.getId()).exact("sourcelang", selectedlang).searchOne();
		if( lasttrack == null)
		{
			throw new OpenEditException("No existing translation available to translate lang:" + selectedlang);
		}

		Data newtrack = captionsearcher.query().exact("assetid", asset.getId()).exact("sourcelang", targetlang).searchOne();
		if( newtrack == null)
		{
			log.info("Creating track " + asset.getId() + " " + targetlang);
			newtrack = captionsearcher.createNewData();
			newtrack.setProperty("sourcelang", targetlang);
			newtrack.setProperty("assetid",  asset.getId());
		}

		
		Collection<Map> existingcaptions = (Collection)lasttrack.getValue("captions");
		Collection<Map> captions = new ArrayList(existingcaptions);
		Translation server = new Translation();
		int counter = 0;
		for(Map caption : captions)
		{
			String cliplabel = (String)caption.get("cliplabel"); 
			if( cliplabel != null && !cliplabel.isEmpty() )
			{
				counter++;
				cliplabel = server.webTranslate(cliplabel,selectedlang,targetlang);
				caption.put("cliplabel", cliplabel);
				newtrack.setValue("captions", captions);
				if( counter == 25)
				{
					counter =0;
					captionsearcher.saveData(newtrack);
					Thread.sleep(1000);
				}	

			}
		}
		newtrack.setValue("transcribestatus", "complete");
		newtrack.setValue("captions", captions);
		captionsearcher.saveData(newtrack);
		inReq.putPageValue("track", newtrack);
		inReq.putSessionValue("selectedlang",targetlang);
	}
	
	
	
	public void tagFrames(WebPageRequest inReq) {
		
	}
	
	
	
	
}
