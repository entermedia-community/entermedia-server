package org.entermediadb.asset.modules;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.upload.FileUpload;
import org.entermediadb.asset.upload.FileUploadItem;
import org.entermediadb.asset.upload.UploadRequest;
import org.entermediadb.asset.util.MathUtils;
import org.entermediadb.video.Timeline;
import org.entermediadb.video.VTT.Cue;
import org.entermediadb.video.VTT.webvtt.WebvttParser;
import org.entermediadb.video.VTT.webvtt.WebvttSubtitle;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.page.Page;
import org.openedit.util.FileUtils;
import org.openedit.util.PathUtilities;

public class TimelineModule extends BaseMediaModule
{
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
		timeline.setLength(videolength);
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
			MathUtils.cleanTypes(clip);			
		}
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
		Collection timeline = asset.getObjects(field);
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
		HitTracker tracks = captionsearcher.query().exact("assetid", asset.getId()).exact("sourcelang", lang).search();

		if( tracks.size() == 0)
		{
			tracks = captionsearcher.query().exact("assetid", asset.getId()).exact("sourcelang", "en").search();
		}
		
		inReq.putPageValue("tracks", tracks);
		inReq.putPageValue("captionsearcher", captionsearcher);
		
	}
	
	public void importTimeline(WebPageRequest inReq) throws Exception
	{
		
		MediaArchive archive = getMediaArchive(inReq);
		
		
		FileUpload command = (FileUpload) archive.getModuleManager().getBean("fileUpload");
		UploadRequest properties = command.parseArguments(inReq);
		
		Asset asset = getAsset(inReq);
	
		
		FileUploadItem item = properties.getFirstItem();
		String fname = item.getName();
		String path = "/WEB-INF/data/" + archive.getCatalogId() + "/generated/"
				+ asset.getSourcePath() + "/" + fname;		
		properties.saveFileAs(properties.getFirstItem(), path, inReq.getUser());

		
		String[] fields = inReq.getRequestParameters("field");
		
		//Collection timeline = new ArrayList();
		
		Page page = archive.getPageManager().getPage(path);
		
		WebvttParser parser = new WebvttParser();
		InputStream input = page.getInputStream();
		WebvttSubtitle titles = parser.parse(input);
		FileUtils.safeClose(input);
		Searcher searcher = archive.getSearcher("videotrack");
		HitTracker existing = searcher.query().exact("assetid", asset.getId()).search();
		searcher.deleteAll(existing, null);
		
		Collection captions = new ArrayList();
		
		Data track = searcher.createNewData();
		if(fields != null)
		{
			searcher.updateData(inReq, fields, track);
		}
		String type = PathUtilities.extractPageName(fname);
		int place = type.lastIndexOf("-");
		if( place == type.length() - 3) 
		{
			String lang = type.substring(place + 1);
			track.setProperty("sourcelang", lang);
		}	
		
		for (Iterator iterator = titles.getCues().iterator(); iterator.hasNext();)
		{
			Cue cue = (Cue) iterator.next();
			
			HashMap cuemap = new HashMap();
			cuemap.put("captiontext", cue.getText().toString());
			cuemap.put("timecodestart", cue.getPosition());
			cuemap.put("alignment", cue.getAlignment());
			cuemap.put("timecodestart", cue.getStartTime());
			cuemap.put("timecodeend", cue.getEndTime());
			captions.add(cuemap);


		}
		track.setValue("captions", captions);
		track.setValue("assetid", asset.getId());
		searcher.saveData(track);
		
	}
	
	
	public void loadCaptionEditor(WebPageRequest inReq)
	{
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
		
		
		HitTracker tracks = captionsearcher.query().exact("assetid", asset.getId()).exact("sourcelang", selectedlang).search();
		
		inReq.putPageValue("tracks", tracks);
		inReq.putPageValue("captionsearcher", captionsearcher);
	}
	
	
	
	
}
