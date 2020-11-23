package org.entermediadb.asset.modules;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.WebPageRequest;

public class AssetStatsModule extends BaseMediaModule
{

	public void addPreViewEvent(WebPageRequest inReq) throws Exception
	{
		//save to logger
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = (Asset)inReq.getPageValue("asset");
		if(asset != null){
			archive.getAssetStatsManager().logAssetPreview(archive,asset, inReq.getUser());
		}
	}
	
	public void saveVideoLog(WebPageRequest inReq) throws Exception
	{
		String logid = inReq.getRequestParameter("logid");
		MediaArchive archive = getMediaArchive(inReq);
		Data videolog = null;
		if( logid != null)
		{
			videolog = archive.getData("videoretention", logid);
		}
		if( videolog == null)
		{
			videolog = archive.getSearcher("videoretention").createNewData();
		}
		
//		int browserwindowwidth = ;
//		
//		int maxvolume = 0;
//		int minvolume = 0;
//		Date startviewing = null;
//		Date endviewing = null;
		
//		if( videolog.getValue("totalviewingtime") != null)
//		{
//			totalviewingtime = (Integer)videolog.getValue("totalviewingtime");
//		}
		
		Map stats = inReq.getJsonRequest();
		
		String assetid = inReq.getRequestParameter("assetid");
		videolog.setValue("assetid",assetid);
		
		List events = (List)stats.get("events");
		Map firstone = (Map)events.get(0);
		Map lastone = (Map)events.get(events.size()-1);
		
		//get the last poistion and subtract anyome someone seeks
		Integer totalviewingtime = (Integer)lastone.get("position");
		videolog.setValue("totalviewingtime",totalviewingtime);
		
		//TODO: Now subtract any seeks
		//addAllSeeks(events);
		boolean didfullscreen = findTrue(events,"isFullScreen");
		videolog.setValue("didfullscreen",didfullscreen);
		
		long started = (long)firstone.get("datetime");
		videolog.setValue("startviewingtime",new Date(started));
		
		long ended = (long)lastone.get("datetime");
		videolog.setValue("endviewingtime",new Date(ended));
		
		if(inReq.getUser() != null )
		{
			videolog.setValue("user",inReq.getUserName());
		}
		
		archive.saveData("videoretention", videolog);
		inReq.putPageValue("data",videolog);
		
		/*
		 
		   {
    "eventType": "playing",
    "position": 0,
    "videoType": "application/x-mpegURL",
    "duration": 560.7815329999997,
    "isEnd": false,
    "isFullScreen": false,
    "bufferPercent": 0.856804248580704,
    "volume": 100,
    "datetime": 1606159756553
  },
  {
    "eventType": "pause",
    "position": 0.9575,
    "videoType": "application/x-mpegURL",
    "duration": 560.7815329999997,
    "isEnd": false,
    "isFullScreen": false,
    "bufferPercent": 3.71282233361276,
    "volume": 100,
    "datetime": 1606159757364
  }

		 */
		
		//Start time 
		//End Time
		//was it ever full screen 
		//the max volume
		//the min volume
		//The total amount of minutes watched
		
	}

	protected boolean findTrue(List inEvents, String inString)
	{
		for (Iterator iterator = inEvents.iterator(); iterator.hasNext();)
		{
			Map event = (Map) iterator.next();
			Object istrue = event.get(inString);
			if( istrue != null && Boolean.parseBoolean(istrue.toString()))
			{
				return true;
			}
		}
		return false;
	}
	
//	public void updateAssetStats(WebPageRequest inReq) throws Exception
//	{
//		WebEvent inEvent = (WebEvent)inReq.getPageValue("webevent");
//		if ( inEvent != null)
//		{
//			MediaArchive archive = getMediaArchive(inEvent.getCatalogId());
//			Asset asset = archive.getAssetBySourcePath(inEvent.getSourcePath());
//			//archive.getAssetStatsManager().getViewsForAsset(asset); //should already be done
//			archive.getAssetSearcher().updateIndex(asset);
//		}
//	}	
}
