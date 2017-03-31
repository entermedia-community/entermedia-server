package sql

import org.entermediadb.asset.ConvertStatus
import org.entermediadb.asset.MediaArchive

public void init()
{
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");//Search for all files looking for videos
	
	SqlImportConverter converter = new SqlImportConverter();
	converter.setPageManager(pageManager);
	converter.setXmlArchive(moduleManager.getBean("xmlArchive"));
	log.info("Running Import");
	ConvertStatus status = new ConvertStatus();
	
	converter.importAssets(mediaArchive,status);
}

init();
