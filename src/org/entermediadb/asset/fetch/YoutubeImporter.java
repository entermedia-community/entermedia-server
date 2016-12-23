package org.entermediadb.asset.fetch;

import java.io.File;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.modules.update.Downloader;
import org.openedit.page.Page;
import org.openedit.users.User;

public class YoutubeImporter implements UrlMetadataImporter
{
	private static Log log = LogFactory.getLog(YoutubeImporter.class);
	
	protected YoutubeParser fieldParser;
	
	public Asset importFromUrl(MediaArchive inArchive, String inUrl, User inUser, String sourcepath, String inFilename, String inId)
	{
		Map<String, String> data = getParser().parseUrl(inUrl);
		if( data == null )
		{
			return null;
		}
		if(sourcepath == null){
		 sourcepath = "users/" + inUser.getUserName() + "/youtube.com/" + data.get("id");
		}
		Asset asset = inArchive.getAssetBySourcePath(sourcepath);
		if( asset == null)
		{
			asset = inArchive.createAsset(sourcepath);
			asset.setId(inArchive.getAssetSearcher().nextAssetNumber());
		}

		//Page attachments = inArchive.getPageManager().getPage(inArchive.getCatalogHome() + "/data/originals/" + asset.getSourcePath() + "/");
		if(data.containsKey("video"))
		{
			// Download video
			asset.setProperty("downloadurl.video", data.get("video"));
		}
		if(data.containsKey("thumb"))
		{
			// Download thumb
			//http://i4.ytimg.com/vi/Omhy1ZumsPQ/default.jpg
			String url = 	data.get("thumb");
			if (url.endsWith("/default.jpg"))
			{
				url = url.replace("/default.jpg", "/hqdefault.jpg");
			}
			asset.setProperty("downloadurl.thumb", url);
		}
		
		for( String key: data.keySet() )
		{
			if(!key.startsWith("."))
			{
				continue;
			}
			key = key.substring(1);
			if(asset.get(key) == null)
			{
				String value = data.get("." + key);
				asset.setProperty(key, value);
			}
		}
		asset.setFolder(true);
		Category pcat = inArchive.getCategory("users");
		if (pcat == null)
		{
			pcat = new Category("users", "Users");
			inArchive.getCategorySearcher().saveCategory(pcat);
		}
		Category cat = inArchive.getCategory("users_" + inUser.getId());
		if( cat == null)
		{
			cat = new Category();
			cat.setId("users_" + inUser.getId());
			cat.setName(inUser.getScreenName() );
			pcat.addChild(cat);
			inArchive.getCategoryArchive().saveCategory(cat);
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

	public YoutubeParser getParser()
	{
		if(fieldParser == null)
		{
			fieldParser = new YoutubeParser();
		}
		return fieldParser;
	}

}
