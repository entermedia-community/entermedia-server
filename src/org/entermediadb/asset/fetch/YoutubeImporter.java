package org.entermediadb.asset.fetch;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.BaseCategory;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.modules.update.Downloader;
import org.entermediadb.net.HttpSharedConnection;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.OpenEditException;
import org.openedit.page.Page;
import org.openedit.users.User;

public class YoutubeImporter implements UrlMetadataImporter
{
	private static Log log = LogFactory.getLog(YoutubeImporter.class);
	
	private String getApiKey(MediaArchive inArchive)
	{
		String youtubeDataApiKey = inArchive.getCatalogSettingValue("youtube-data-api-key");
		if( youtubeDataApiKey == null )
		{
			throw new OpenEditException("You must set the youtube-data-api-key catalog setting to use the Youtube importer");
		}
		return youtubeDataApiKey;
	}
	
	public YoutubeMetadataSnippet importVideoMetadata(MediaArchive inArchive, String inUrl)
	{
		YoutubeMetadataSnippet snippet = importMetadataFromUrl(inArchive, inUrl).iterator().next();
		log.info("Imported metadata for " + snippet);
		return snippet;
	}
	
	public Collection<YoutubeMetadataSnippet> importMetadataFromUrl(MediaArchive inArchive, String inUrl)
	{
		String youtubeDataApiKey = getApiKey(inArchive);
		
		String dataApi = "https://www.googleapis.com/youtube/v3/";
		
		YoutubeParser ytParser = getParser(inUrl);
		String id = ytParser.getId();   
		String type = ytParser.getType();
		if( id == null || type == null )
		{
			log.error("Could not parse youtube url: " + inUrl);
			return null;
		}
		if(type.equals("VIDEO"))
		{			
			dataApi += "videos?part=snippet&id=" + id;
		}
		else if(type.equals("PLAYLIST"))
		{
			dataApi += "playlistItems?part=snippet&maxResults=20&playlistId=" + id;
		}
		else if(type.equals("CHANNEL") || type.equals("HANDLE"))
		{
			return null;
		}
		else
		{
			log.error("Youtube url type not supported: " + type);
			return null;
		}
		
		
		dataApi += "&key=" + youtubeDataApiKey;
		
		return callDataApi(ytParser, dataApi);
		
	}
	
	private Collection<YoutubeMetadataSnippet> callDataApi(YoutubeParser ytParser, String dataApi)
	{
		Collection<YoutubeMetadataSnippet> items = new ArrayList<YoutubeMetadataSnippet>();
		return callDataApi(ytParser, dataApi, items, null);
	}
	
	private Collection<YoutubeMetadataSnippet> callDataApi(YoutubeParser ytParser, String dataApi, Collection<YoutubeMetadataSnippet> items, String pageToken)
	{
		String endPoint = dataApi;
		if(pageToken != null)
		{
			endPoint += "&pageToken=" + pageToken;
		}

		JSONObject json = handleHttpRequest(endPoint, ytParser);
		items.addAll(ytParser.parseMetadataSnippets(json));
		
		String nextPageToken = (String) json.get("nextPageToken");
		if( nextPageToken != null && items.size() < 200 )
		{
			callDataApi(ytParser, dataApi, items, nextPageToken);
		}
		return items;
		
	}
	
	private JSONObject handleHttpRequest(String endPoint, YoutubeParser ytParser)
	{
		HttpRequestBase method = new HttpGet(endPoint);
		method.setHeader("Content-Type", "application/json");

		HttpSharedConnection connection = new HttpSharedConnection();

		CloseableHttpResponse resp = connection.sharedExecute(method);
		
		try
		{
			if( resp.getStatusLine().getStatusCode() != 200 )
			{
				log.error("Youtube data API returned " + resp.getStatusLine().getStatusCode() + " for " + ytParser.getType() + " ID: " + ytParser.getId());
				return null;
			}
			JSONObject json = (JSONObject) connection.parseJson(resp);
			return json;
		}
		catch( Exception e)
		{
			throw new OpenEditException(e);
		}
		finally
		{
			connection.release(resp);
		}
	}
	
	public Asset importFromUrl(MediaArchive inArchive, String inUrl, User inUser, String sourcepath, String inFilename, String inId)
	{
		YoutubeParser ytParser = getParser(inUrl);
		String id = ytParser.getId();   
		String type = ytParser.getType();
		if( id == null || !"VIDEO".equals(type) )
		{
			return null;
		}
		if(sourcepath == null){
		 sourcepath = "users/" + inUser.getUserName() + "/youtube.com/" + id;
		}
		Asset asset = inArchive.getAssetBySourcePath(sourcepath);
		if( asset == null)
		{
			asset = inArchive.createAsset(sourcepath);
			asset.setId(inArchive.getAssetSearcher().nextAssetNumber());
		}
		
		YoutubeMetadataSnippet metadata = importVideoMetadata(inArchive, inUrl);
		
		if(metadata == null)
		{
			return null;
		}

		
//		asset.setProperty("downloadurl.video", data.get("video")); ??
		String url = metadata.getThumbnail();
		asset.setProperty("downloadurl.thumb", url);
	
		asset.setKeywords(metadata.getTags());
		
		asset.setFolder(true);
		Category pcat = inArchive.getCategory("users");
		if (pcat == null)
		{
			pcat = new BaseCategory("users", "Users");
			inArchive.getCategorySearcher().saveCategory(pcat);
		}
		Category cat = inArchive.getCategory("users_" + inUser.getId());
		if( cat == null)
		{
			cat = new BaseCategory();
			cat.setId("users_" + inUser.getId());
			cat.setName(inUser.getScreenName() );
			pcat.addChild(cat);
			inArchive.getCategorySearcher().saveCategory(cat);
		}
		asset.addCategory(cat);
		inArchive.saveAsset(asset, inUser);
		//This will download the asset in a catalog event handler
		inArchive.fireMediaEvent("fetchassetadded", inUser, asset);
		return asset;
	}

	public void fetchMediaForAsset(MediaArchive inArchive, Asset asset, User inUser)
	{
		Downloader downloader = new Downloader();
		String path = "/WEB-INF/data/" + asset.getCatalogId() + "/originals/" + asset.getSourcePath() + "/imported.flv";
		Page saveto = inArchive.getPageManager().getPage( path );
		String url = asset.get("downloadurl.video");
		
		if(url != null)
		{
			log.info("Downloading " + url + " ->" + path);
			if( saveto.exists() || saveto.length() == 0)
			{
				downloader.download(url, new File(saveto.getContentItem().getAbsolutePath()));
			}
			asset.setProperty("videourl", url);
			asset.removeProperty("downloadurl.video");
			asset.setPrimaryFile("imported.flv");
			inArchive.getAssetImporter().getAssetUtilities().populateAsset(asset, saveto.getContentItem(), inArchive, asset.getSourcePath(),inUser );		
		}
		url = asset.get("downloadurl.thumb");
		if(url != null)
		{
			path = "/WEB-INF/data/" + asset.getCatalogId() + "/originals/" + asset.getSourcePath() + "/thumb.jpg";
			saveto = inArchive.getPageManager().getPage( path );
			if( saveto.exists() || saveto.length() == 0)
			{
				downloader.download(url, new File(saveto.getContentItem().getAbsolutePath()));
			}
			asset.setProperty("thumburl", url);
			asset.removeProperty("downloadurl.thumb");
			asset.setAttachmentFileByType("image", "thumb.jpg");
		}
		inArchive.saveAsset(asset, inUser);
	}

	public YoutubeParser getParser(String inUrl)
	{
		YoutubeParser fieldParser = new YoutubeParser();
		fieldParser.parse(inUrl);
		return fieldParser;
	}

	public int countVideosInChannel(MediaArchive inArchive, YoutubeParser parser) {
		String youtubeDataApiKey = getApiKey(inArchive);
		String endpoint = "https://www.googleapis.com/youtube/v3/channels?part=contentDetails&key=" + youtubeDataApiKey;
		if(parser.getType().equals("CHANNEL")) 
		{
			endpoint += "&id=" + parser.getId();
		} 
		else if(parser.getType().equals("HANDLE"))
		{
			endpoint += "&forHandle=" + parser.getId();
		} 
		else 
		{
			return 0;
		}

		JSONObject json = handleHttpRequest(endpoint, parser);
		if(json != null) 
		{
			JSONArray items = (JSONArray) json.get("items");
			if(items != null && items.size() > 0) 
			{
				JSONObject item = (JSONObject) items.get(0);
				JSONObject contentDetails = (JSONObject) item.get("contentDetails");
				JSONObject relatedPlaylists = (JSONObject) contentDetails.get("relatedPlaylists");
				String uploadsPlaylistId = (String) relatedPlaylists.get("uploads");
				parser.setId(uploadsPlaylistId);
				parser.setType("PLAYLIST");
				return countVideosInPlaylist(inArchive, parser);
			}
		}
		return 0;
	}

	public int countVideosInPlaylist(MediaArchive inArchive, YoutubeParser parser) {
		String youtubeDataApiKey = getApiKey(inArchive);
		String endpoint = "https://www.googleapis.com/youtube/v3/playlistItems?part=id&maxResults=0&key=" + youtubeDataApiKey + "&playlistId=" + parser.getId();
		
		JSONObject json = handleHttpRequest(endpoint, parser);
		if(json != null) 
		{
			JSONObject pageInfo = (JSONObject) json.get("pageInfo");
			Long totalResults = (Long) pageInfo.get("totalResults");
			if(totalResults != null) 
			{
				return totalResults.intValue();
			}
		}
		return 0;
	}

}
