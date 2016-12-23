package org.entermediadb.asset.modules;

import java.util.Map;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.fetch.YoutubeParser;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;

public class FetchModule extends BaseMediaModule
{
	public void getYoutubeData(WebPageRequest inReq)
	{
		String url = inReq.getRequestParameter("youtubeurl");
		if(url != null)
		{
			YoutubeParser parser = new YoutubeParser();
			Map<String, String> data = parser.parseUrl(url);
			inReq.putPageValue("youtubedata", data);
		}
	}
	
	public void importFromUrl(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String url = inReq.getRequestParameter("importurl");
		Asset asset = archive.getAssetImporter().createAssetFromFetchUrl(archive, url, inReq.getUser(), null, null, null);
		inReq.putPageValue("asset", asset);
		//This will download the asset in a catalog event handler
	}
	public void fetchMediaForAsset(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String sourcepath = inReq.getRequestParameter("sourcepath");
		if( sourcepath == null)
		{
			throw new OpenEditException("sourcepath is required");
		}
		Asset asset = archive.getAssetBySourcePath(sourcepath);
		archive.getAssetImporter().fetchMediaForAsset(archive, asset, inReq.getUser());
	}

}
