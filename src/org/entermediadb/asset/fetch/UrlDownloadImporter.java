package org.entermediadb.asset.fetch;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.modules.update.Downloader;
import org.openedit.users.User;
import org.openedit.util.PathUtilities;

public class UrlDownloadImporter implements UrlMetadataImporter {
	private static Log log = LogFactory.getLog(UrlDownloadImporter.class);

	public Asset importFromUrl(MediaArchive inArchive, String inUrl,
			User inUser, String sourcepath, String inFileName, String id) {
		String filename = PathUtilities.extractFileName(inUrl);
		filename = filename.replaceAll("\\?.*", "");
		if(inFileName != null){
			filename = inFileName;
		}
		
		if (sourcepath == null) {
			sourcepath = "users/" + inUser.getUserName() + "/url/" + filename;
		}
		Asset asset = inArchive.getAssetBySourcePath(sourcepath);
		if (asset == null) {
			asset = inArchive.createAsset(sourcepath);
		}
		asset.setName(filename);
		if(id != null){
			asset.setId(id);
		}
		asset.setPrimaryFile(filename);
		asset.setProperty("downloadurl-file", inUrl);
		if(inFileName != null){
			asset.setProperty("downloadurl-filename", inFileName);
		}
		//asset.setFolder(true);
//		Category pcat = inArchive.getCategorySearcher().createCategoryPath(sourcepath);
//		asset.addCategory(pcat);

		// This will download the asset in a catalog event handler
		fetchMediaForAsset(inArchive, asset, inUser);
		inArchive.saveAsset(asset, inUser);
		return asset;
	}

	public void fetchMediaForAsset(MediaArchive inArchive, Asset asset,
			User inUser) {
		Downloader downloader = new Downloader();
		String path = "/WEB-INF/data/" + asset.getCatalogId() + "/originals/"
				+ asset.getSourcePath();
		File attachments = new File(inArchive.getPageManager().getPage(path)
				.getContentItem().getAbsolutePath());
		String url = asset.get("downloadurl-file");
		if (url != null) {
			String filename = asset.get("downloadurl-filename");
			
			if(filename == null){
			 filename = PathUtilities.extractFileName(url);
			}
			filename = filename.replaceAll("\\?.*", "");
			log.info("Downloading " + url + " ->" + path + "/" + filename);
			File target = new File(attachments, filename);
			if (target.exists() || target.length() == 0) {
				try
				{
					downloader.download(url, target);
				}
				catch( Exception ex)
				{
					asset.setProperty("importstatus", "error");
					log.error(ex);
					inArchive.saveAsset(asset, inUser);
					return;
				}
			}
			asset.setName(filename);
			asset.setPrimaryFile(filename);
			//asset.setFolder(true);
			asset.setProperty("importstatus", "created");
			asset.setProperty("downloadourl", url);
			asset.removeProperty("downloadurl-file");
			asset.removeProperty("downloadurl-filename");
			//inArchive.saveAsset(asset, inUser);
			//inArchive.fireSharedMediaEvent("importing/assetscreated");
		}
	}
}
