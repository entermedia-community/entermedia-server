package importing

import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.scanner.AssetImporter

public void init()
{
	MediaArchive archive = context.getPageValue("mediaarchive");
	AssetImporter importer = (AssetImporter)moduleManager.getBean("assetImporter");
	//importer.setExcludeFolders("Fonts,Links");
	importer.setIncludeMatches("mp4,mov,wmv");
	importer.setUseFolders(false);
	
	String assetRoot = "/WEB-INF/data/" + archive.getCatalogId() + "/originals/";
		
	List created = importer.processOn(assetRoot, assetRoot, archive, context.getUser());
	log.info("created videos " + created.size() );
		
}

init();
