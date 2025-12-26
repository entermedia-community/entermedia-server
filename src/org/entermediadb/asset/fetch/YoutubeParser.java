package org.entermediadb.asset.fetch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.OpenEditException;
import org.openedit.util.DateStorageUtil;

public class YoutubeParser
{
	private static Log log = LogFactory.getLog(YoutubeParser.class);
	
	protected String id = null;
	protected String type = null;
	
	public void setId(String inId)
	{
		id = inId;
	}
	
	public String getId()
	{
		return id;
	}
	
	public void setType(String inType)
	{
		type = inType;
	}
	
	public String getType()
	{
		return type;
	}
	
	protected String ytUrl;
	
	private String getVideoUrl()
	{
		return ytUrl;
	}
	
	protected void setVideoUrl(String inVideoUrl)
	{
		ytUrl = inVideoUrl;
	}
	
	public void parse(String url)
	{
		
        if (url == null || url.isEmpty()) {
            return;
        }
        setVideoUrl(url);
	    
        // Priority 1: Try to get playlist ID
        String playlistId = getYouTubePlaylistId();
        if (playlistId != null) {
        	setId(playlistId);
            setType("PLAYLIST");
            return;
        }
        
        // Priority 2: Try to get video ID
        String videoId = getYouTubeVideoId();
        if (videoId != null) {
            setId(videoId);
            setType("VIDEO");
            return;
        }
	        
        // Priority 3: Try to get handle
        String handle = getYouTubeHandle();
        if (handle != null) {
        	setId(handle);
			setType("HANDLE");
			return;
        }
        
        // Priority 4: Try to get channel ID
        String channelId = getYouTubeChannelId();
        if (channelId != null) {
			setId(channelId);
			setType("CHANNEL");
			return;
        }
	}
	
    public String getYouTubeVideoId() {
    	String url = getVideoUrl();
        
        // Handle youtu.be short links
        Pattern youtuBePattern = Pattern.compile("youtu\\.be/([a-zA-Z0-9_-]{11})");
        Matcher youtuBeMatcher = youtuBePattern.matcher(url);
        if (youtuBeMatcher.find()) {
            return youtuBeMatcher.group(1);
        }
        
        // Handle youtube.com links with v parameter
        Pattern vParamPattern = Pattern.compile("[?&]v=([a-zA-Z0-9_-]{11})");
        Matcher vParamMatcher = vParamPattern.matcher(url);
        if (vParamMatcher.find()) {
            return vParamMatcher.group(1);
        }
        
        // Handle youtube.com/embed/ links
        Pattern embedPattern = Pattern.compile("/embed/([a-zA-Z0-9_-]{11})");
        Matcher embedMatcher = embedPattern.matcher(url);
        if (embedMatcher.find()) {
            return embedMatcher.group(1);
        }
        
        // Handle youtube.com/v/ links
        Pattern vSlashPattern = Pattern.compile("/v/([a-zA-Z0-9_-]{11})");
        Matcher vSlashMatcher = vSlashPattern.matcher(url);
        if (vSlashMatcher.find()) {
            return vSlashMatcher.group(1);
        }
        
        return null;
    }
    
    public String getYouTubePlaylistId() {
    	String url = getVideoUrl();
        
        // Handle playlist parameter (list=)
        Pattern playlistPattern = Pattern.compile("[?&]list=([a-zA-Z0-9_-]+)");
        Matcher playlistMatcher = playlistPattern.matcher(url);
        if (playlistMatcher.find()) {
            return playlistMatcher.group(1);
        }
        
        // Handle /playlist?list= format
        Pattern playlistSlashPattern = Pattern.compile("/playlist\\?list=([a-zA-Z0-9_-]+)");
        Matcher playlistSlashMatcher = playlistSlashPattern.matcher(url);
        if (playlistSlashMatcher.find()) {
            return playlistSlashMatcher.group(1);
        }
        
        return null;
    }
    
    public String getYouTubeHandle() {
    	String url = getVideoUrl();
        
        // Handle @handle format (e.g., youtube.com/@username)
        Pattern handlePattern = Pattern.compile("/@([a-zA-Z0-9_.-]+)");
        Matcher handleMatcher = handlePattern.matcher(url);
        if (handleMatcher.find()) {
            return handleMatcher.group(1);
        }
        
        return null;
    }
    
    public String getYouTubeChannelId() {
    	String url = getVideoUrl();
        
        // Handle /channel/ format (e.g., youtube.com/channel/UC...)
        Pattern channelPattern = Pattern.compile("/channel/([a-zA-Z0-9_-]+)");
        Matcher channelMatcher = channelPattern.matcher(url);
        if (channelMatcher.find()) {
            return channelMatcher.group(1);
        }
        
        return null;
    }
	
	public Collection<YoutubeMetadataSnippet> parseMetadataSnippets( JSONObject inJson )
	{
		Collection<YoutubeMetadataSnippet> metadatas = new ArrayList<YoutubeMetadataSnippet>();
		try
		{
			JSONArray items = (JSONArray) inJson.get("items");
			for (Iterator iterator = items.iterator(); iterator.hasNext();) 
			{
				JSONObject item = (JSONObject) iterator.next();
				
				JSONObject snippet = (JSONObject) item.get("snippet");
				if( snippet != null)
				{
					Map<String, Object> metadataMap = new HashMap<String, Object>();
					
					String videoId = null;
					
					String kind = (String) item.get("kind");
					
					if("youtube#video".equals(kind))
					{
						videoId = (String) item.get("id");
					}
					else if("youtube#playlistItem".equals(kind))
					{
						JSONObject resourceId = (JSONObject) snippet.get("resourceId");
						if( resourceId != null)
						{
							videoId = (String) resourceId.get("videoId");
						}
					}
					
					if( videoId == null)
					{
						continue;
					}
					
					metadataMap.put("id", videoId);
					
					metadataMap.put("title", (String) snippet.get("title"));
					metadataMap.put("description", (String) snippet.get("description"));
					JSONObject thumbnails = (JSONObject) snippet.get("thumbnails");
					if( thumbnails != null)
					{
						if(thumbnails.keySet().contains("maxres"))
						{
							JSONObject maxres = (JSONObject) thumbnails.get("maxres");
							metadataMap.put("thumbnail", (String) maxres.get("url"));
						}
						else if(thumbnails.keySet().contains("standard"))
						{
							JSONObject standard = (JSONObject) thumbnails.get("standard");
							metadataMap.put("thumbnail", (String) standard.get("url"));
						}
						else if(thumbnails.keySet().contains("high"))
						{
							JSONObject high = (JSONObject) thumbnails.get("high");
							metadataMap.put("thumbnail", (String) high.get("url"));
						}
						else if(thumbnails.keySet().contains("medium"))
						{
							JSONObject high = (JSONObject) thumbnails.get("medium");
							metadataMap.put("thumbnail", (String) high.get("url"));
						}
						else if(thumbnails.keySet().contains("default"))
						{
							JSONObject high = (JSONObject) thumbnails.get("default");
							metadataMap.put("thumbnail", (String) high.get("url"));
						}
					}
					Collection<String> tags = new ArrayList<String>();
					JSONArray tagsjson = (JSONArray) snippet.get("tags");
					if( tagsjson != null)
					{
						for( int i = 0; i < tagsjson.size(); i++ )
						{
							tags.add((String) tagsjson.get(i));
						}
						metadataMap.put("tags", tags);
					}
					String assetcreationdate = (String) snippet.get("publishedAt");
					if(assetcreationdate != null)
					{
						metadataMap.put("publishedAt", DateStorageUtil.getStorageUtil().parseFromObject(assetcreationdate));
					}
					metadataMap.put("channelTitle", (String) snippet.get("channelTitle"));
					
					YoutubeMetadataSnippet metadata = new YoutubeMetadataSnippet();
					metadata.setSnippet(metadataMap);
					
					metadatas.add(metadata);
				}
			}
		}
		catch(Exception e)
		{
			throw new OpenEditException(e);
		}
		return metadatas;
	}
	
}
