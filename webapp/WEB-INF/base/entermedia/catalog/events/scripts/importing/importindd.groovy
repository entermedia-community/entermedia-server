package importing

import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.scanner.AssetImporter

public void init()
{
	MediaArchive archive = context.getPageValue("mediaarchive");
	AssetImporter importer = (AssetImporter)moduleManager.getBean("assetImporter");
	importer.setExcludeFolderMatch("Fonts,Links");
	importer.setIncludeMatches("INDD,indd");
	importer.setUseFolders(true);
	
	String assetRoot = "/WEB-INF/data/" + archive.getCatalogId() + "/originals/";
		
	List created = importer.processOn(assetRoot, assetRoot, archive, context.getUser());
	log.info("created indd " + created.size() );
		
}

init();
