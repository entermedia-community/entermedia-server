package org.entermediadb.asset.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.util.MathUtils;
import org.entermediadb.video.Timeline;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;

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
		inReq.putPageValue("timeline", timeline);
	}
	
	public void loadTimeLineManager(WebPageRequest inReq)
	{
		
	}

	public void saveClips(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		
		Map values = inReq.getJsonRequest();
		String assetid = (String)values.get("assetid");
		Asset asset = archive.getAsset(assetid);
		
		Collection clips = (Collection)values.get("clips");
		asset.setValue("clips", clips);
		
		archive.saveAsset(asset);
		
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
		
		Collections.sort((List<HashMap>) timeline, new Comparator()
		{
			@Override
			public int compare(Object inO1, Object inO2)
			{
				HashMap first = (HashMap) inO1;
				HashMap second = (HashMap) inO2;
				Double code1 = (Double) first.get("timecodestart");
				Double code2 = (Double) second.get("timecodestart");
				if(code1 == null || code2 == null){
					return 0;
				}
				if(code1 < code2){
					return -1;
				}
				if(code2 < code1){
					return 1;
				}
				return 0;
			}
		});
		asset.setValue(field, timeline);
		assetsearcher.saveData(asset);
		
	}
	
	
	
}
