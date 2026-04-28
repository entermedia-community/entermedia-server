package org.entermediadb.asset.importer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.AgentEnabled;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.BaseCategory;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;   
import org.entermediadb.asset.fetch.YoutubeMetadataSnippet;
import org.entermediadb.asset.fetch.YoutubeParser;
import org.entermediadb.modules.update.Downloader;
import org.entermediadb.net.HttpSharedConnection;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.page.Page;
import org.openedit.users.User;

public class YoutubeImporterSkill extends BaseSkill
{
	private static Log log = LogFactory.getLog(YoutubeImporterSkill.class);

	@Override
	public void process(AgentContext inContext)
	{
		AgentEnabled currentEnabled = inContext.getCurrentAgentEnable();
		
		User user = getMediaArchive().getUser("agent");
		importFromUrl(inContext, user);

		super.process(inContext);
	} 

	public YoutubeMetadataSnippet importVideoMetadata(AgentContext inContext, String inUrl)
	{
		YoutubeMetadataSnippet snippet = importMetadataFromUrl(inContext, inUrl).iterator().next();
		log.info("Imported metadata for " + snippet);
		return snippet;
	}

	public Collection<YoutubeMetadataSnippet> importMetadataFromUrl(AgentContext inContext, String inUrl)
	{
		String youtubeDataApiKey = (String) inContext.getContextValue("youtube-data-api-key");

		String dataApi = "https://www.googleapis.com/youtube/v3/";

		YoutubeParser ytParser = getParser(inUrl);
		String id = ytParser.getId();
		String type = ytParser.getType();
		if (id == null || type == null)
		{
			log.error("Could not parse youtube url: " + inUrl);
			return null;
		}
		if (type.equals("VIDEO"))
		{
			dataApi += "videos?part=snippet&id=" + id;
		}
		else
			if (type.equals("PLAYLIST"))
			{
				dataApi += "playlistItems?part=snippet&maxResults=50&playlistId=" + id;
			}
			else
				if (type.equals("CHANNEL") || type.equals("HANDLE"))
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
		if (pageToken != null)
		{
			endPoint += "&pageToken=" + pageToken;
		}

		JSONObject json = handleHttpRequest(endPoint, ytParser);

		items.addAll(ytParser.parseMetadataSnippets(json));

		String nextPageToken = (String) json.get("nextPageToken");
		if (nextPageToken != null)
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
			if (resp.getStatusLine().getStatusCode() != 200)
			{
				log.error("Youtube data API returned " + resp.getStatusLine().getStatusCode() + " for " + ytParser.getType() + " ID: " + ytParser.getId());
				return null;
			}
			JSONObject json = (JSONObject) connection.parseJson(resp);
			return json;
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}
		finally
		{
			connection.release(resp);
		}
	}

	public Asset importFromUrl(AgentContext inContext, User inUser)
	{
		MediaArchive inArchive = getMediaArchive();
		
		String url = (String) inContext.getContextValue("url");
		if (url == null)
		{
			inContext.error("No url provided for youtube importer agent");
			return null;
		}

		YoutubeParser ytParser = getParser(url);
		String id = ytParser.getId();
		String type = ytParser.getType();
		if (id == null || !"VIDEO".equals(type))
		{
			return null;
		}

		String moduleid = (String) inContext.getContextValue("moduleid");
		if (moduleid == null)
		{
			moduleid = "entityvideo";
		}
		Data module = (Data) inArchive.query("module").exact("id", moduleid).searchOne();
		if (module == null)
		{
			inContext.error("Module not found: " + moduleid);
			return null;
		}

		YoutubeMetadataSnippet metadata = importVideoMetadata(inContext, url);

		if (metadata == null)
		{
			return null;
		}

		Data entity = (Data) inArchive.query(moduleid).exact("embeddedid", id).exact("embeddedtype", "youtube").searchOne();
		if(entity == null) 
		{
			entity = inArchive.getSearcher(moduleid).createNewData();
			entity.setValue("embeddedid", id);
			entity.setValue("embeddedtype", "youtube");
		}
		
		String name = metadata.getTitle();
		entity.setName(name);
		entity.setValue("longdescription", metadata.getDescription());
		inArchive.getSearcher(moduleid).saveData(entity);

		String sourcepath = inArchive.getEntityManager().loadUploadSourcepath(module, entity, inUser);

		Asset asset = inArchive.getAssetBySourcePath(sourcepath);
		
		if (asset == null)
		{
			asset = (Asset) inArchive.getAssetSearcher().createNewData();
			asset.setName(name);
		}
		
		String thumbnail = metadata.getThumbnail();

		asset.setValue("downloadurl.thumb", thumbnail);

		asset.setKeywords(metadata.getTags());

		inArchive.saveAsset(asset, inUser);
		
		entity.setValue("primarymedia", asset.getId());
		inArchive.getSearcher(moduleid).saveData(entity);

		inArchive.fireMediaEvent("fetchassetadded", inUser, asset);

		return asset;
	}

	public YoutubeParser getParser(String inUrl)
	{
		YoutubeParser fieldParser = new YoutubeParser();
		fieldParser.parse(inUrl);
		return fieldParser;
	}

	public int countVideosInChannel(MediaArchive inArchive, YoutubeParser parser)
	{
		String youtubeDataApiKey = getApiKey(inArchive);
		String endpoint = "https://www.googleapis.com/youtube/v3/channels?part=contentDetails&key=" + youtubeDataApiKey;
		if (parser.getType().equals("CHANNEL"))
		{
			endpoint += "&id=" + parser.getId();
		}
		else
			if (parser.getType().equals("HANDLE"))
			{
				endpoint += "&forHandle=" + parser.getId();
			}
			else
			{
				return 0;
			}

		JSONObject json = handleHttpRequest(endpoint, parser);
		if (json != null)
		{
			JSONArray items = (JSONArray) json.get("items");
			if (items != null && items.size() > 0)
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

	public int countVideosInPlaylist(MediaArchive inArchive, YoutubeParser parser)
	{
		String youtubeDataApiKey = getApiKey(inArchive);
		String endpoint = "https://www.googleapis.com/youtube/v3/playlistItems?part=id&maxResults=0&key=" + youtubeDataApiKey + "&playlistId=" + parser.getId();

		JSONObject json = handleHttpRequest(endpoint, parser);
		if (json != null)
		{
			JSONObject pageInfo = (JSONObject) json.get("pageInfo");
			Long totalResults = (Long) pageInfo.get("totalResults");
			if (totalResults != null)
			{
				return totalResults.intValue();
			}
		}
		return 0;
	}

}
