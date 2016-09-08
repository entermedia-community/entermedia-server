package importing

import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.scanner.AssetImporter

public void init()
{
	MediaArchive archive = context.getPageValue("mediaarchive");
	AssetImporter importer = (AssetImporter)moduleManager.getBean("assetImporter");
	//importer.setExcludeFolders("Fonts,Links");
	importer.setIncludeMatches("mp3,wav");
	importer.setUseFolders(false);
	
	String assetRoot = "/WEB-INF/data/" + archive.getCatalogId() + "/originals/";
		
	List created = importer.processOn(assetRoot, assetRoot, archive, context.getUser());
	log.info("created audio files " + created.size() );
		
}

init();
