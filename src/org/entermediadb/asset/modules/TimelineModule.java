package org.entermediadb.asset.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.video.Block;
import org.openedit.WebPageRequest;
import org.openedit.data.BaseData;
import org.openedit.data.Searcher;

public class TimelineModule extends BaseMediaModule
{
	public void loadChart(WebPageRequest inReq)
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
		//divide into 60 blocks
		List blocks = new ArrayList();
		
		double chunck = videolength / 20d;
		for (int i = 0; i < 21; i++)
		{
			Block block = new Block();
			//block.setTime(i * chunck);
			block.setCounter(i);
			block.setStartOffset(chunck * (double)i);
			if( i < 20)
			{
				block.setShowThumb((i % 2) == 0);
			}	
			blocks.add(block);
		}
		inReq.putPageValue("blocks", blocks);
	}
	
	
	
	public void addCuepoint(WebPageRequest inReq){
		
		Asset asset = getAsset(inReq);
	
		
		MediaArchive archive = getMediaArchive(inReq);
		if(asset == null){
			String id = inReq.getRequestParameter("assetid.value");
			if(id != null){
				asset = archive.getAsset(id);
			}
		}

		
		String timecodes = inReq.getRequestParameter("timecode.value");
		String end = inReq.getRequestParameter("outtime.value");

		double start = Double.parseDouble(timecodes);
		double endtime =  15;
		
		if(end != null){
			endtime = Double.parseDouble(end);
		}
		endtime = start + endtime;
		
		String field = inReq.getRequestParameter("targetfield");
		
		Object currentcodes = asset.getValue(field);
		
		Collection timeline = null;
		if(currentcodes != null && currentcodes instanceof Collection){
			timeline = (Collection) currentcodes;
		} else {
			timeline = new ArrayList();
			if(currentcodes != null){
				timeline.add(currentcodes);
			}
		}
		if(currentcodes == null){
			timeline = new ArrayList();			
		}
		
		String [] fields = inReq.getRequestParameters("field");
		Searcher assetsearcher = archive.getAssetSearcher();
		HashMap codemap = new HashMap();
		codemap.put("timecodestart", start);
		codemap.put("timecodeend", endtime);
		for (int i = 0; i < fields.length; i++)
		{
			String id = fields[i];
			String val = inReq.getRequestParameter(id);
			if(id != null && val != null){
				codemap.put(id, val);
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
