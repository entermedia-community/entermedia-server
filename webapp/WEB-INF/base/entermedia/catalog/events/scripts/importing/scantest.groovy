package importing;

import static groovy.io.FileType.FILES

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.openedit.page.Page


MediaArchive archive = context.getPageValue("mediaarchive");
archive.getAssetSearcher().deleteAll(null);
ArrayList assets = new ArrayList();
Page test = archive.getPageManager().getPage("/WEB-INF/data/media/catalogs/public/originals/Dropbox");
int length = "/WEB-INF/data/media/catalogs/public/originals/".size();
new File(test.getContentItem().getAbsolutePath()).eachFileRecurse(FILES) {
		println it
		Asset asset = archive.getAssetSearcher().createNewData();
		asset.setName(it.getAbsolutePath());
		asset.setValue("importstatus", "needsmetadata");
		String filepath = it.getAbsolutePath();
		String finalpath = filepath.substring(length, filepath.size());
		asset.setSourcePath(finalpath);
		asset.setPrimaryFile(it.getName());
		assets.add(asset);
		//archive.getAssetSearcher().saveData(asset);
}

archive.getAssetSearcher().saveAllData(assets, null);

