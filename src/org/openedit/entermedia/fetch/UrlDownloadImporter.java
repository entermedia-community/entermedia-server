package org.openedit.entermedia.fetch;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.Category;
import org.openedit.entermedia.MediaArchive;

import com.openedit.modules.update.Downloader;
import com.openedit.users.User;
import com.openedit.util.PathUtilities;

public class UrlDownloadImporter implements UrlMetadataImporter
{
	private static Log log = LogFactory.getLog(UrlDownloadImporter.class);
	
	public Asset importFromUrl(MediaArchive inArchive, String inUrl, User inUser)
	{
		String filename = PathUtilities.extractFileName(inUrl);
		filename = filename.replaceAll("\\?.*", "");

		String sourcepath = "users/" + inUser.getUserName() + "/url/" + filename;
		Asset asset = inArchive.getAssetBySourcePath(sourcepath);
		if( asset == null)
		{
			asset = inArchive.createAsset(sourcepath);
			asset.setId(inArchive.getAssetSearcher().nextAssetNumber());
		}
		asset.setName(filename);
		asset.setProperty("downloadurl.file", inUrl);
		asset.setFolder(true);
		Category pcat = inArchive.getCategory("users");
		if (pcat == null)
		{
			pcat = new Category("users", "Users");
			inArchive.getCategoryArchive().addChild(pcat);
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
		String path = "/WEB-INF/data/" + asset.getCatalogId() + "/originals/" + asset.getSourcePath();
		File attachments = new File(
				inArchive.getPageManager().getPage(
						path).getContentItem().getAbsolutePath()
				);
		String url = asset.get("downloadurl.file");
		if(url != null)
		{
			String filename = PathUtilities.extractFileName(url);
			filename = filename.replaceAll("\\?.*", "");
			log.info("Downloading " + url + " ->" + path + "/" + filename);
			File target = new File(attachments, filename);
			if( target.exists() || target.length() == 0)
			{
				downloader.download(url, target);
			}
			asset.setProperty("downloadourl", url);
			asset.removeProperty("downloadurl.file");
			asset.setPrimaryFile(filename);
		}
		inArchive.saveAsset(asset, inUser);
	}
}
