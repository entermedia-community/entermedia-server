package org.entermediadb.asset.modules;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.fetch.YoutubeMetadataSnippet;
import org.entermediadb.asset.fetch.YoutubeImporter;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;

public class FetchModule extends BaseMediaModule
{
	public void getYoutubeData(WebPageRequest inReq)
	{
		String url = inReq.getRequestParameter("youtubeurl");
		if(url != null)
		{
			MediaArchive archive = getMediaArchive(inReq);
			YoutubeImporter importer = (YoutubeImporter) archive.getBean("youtubeImporter");
			
			YoutubeMetadataSnippet metadata = importer.importVideoMetadata(archive, url);
			inReq.putPageValue("youtubedata", metadata);
		}
	}
	
	public void importFromUrl(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String url = inReq.getRequestParameter("importurl");
		Asset asset = archive.getAssetImporter().createAssetFromFetchUrl(archive, url, inReq.getUser(), null, null, null);
		inReq.putPageValue("asset", asset);
	}
	
	public void fetchMediaForAsset(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String sourcepath = inReq.getRequestParameter("sourcepath");
		if( sourcepath == null )
		{
			throw new OpenEditException("sourcepath is required");
		}
		Asset asset = archive.getAssetBySourcePath(sourcepath);
		archive.getAssetImporter().fetchMediaForAsset(archive, asset, inReq.getUser());
	}

}
