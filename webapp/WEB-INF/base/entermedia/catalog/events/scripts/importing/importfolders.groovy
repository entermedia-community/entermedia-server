package importing;

import org.openedit.entermedia.Asset 
import org.openedit.entermedia.MediaArchive 
import org.openedit.*;

import com.openedit.hittracker.*;
import org.openedit.entermedia.creator.*;

import java.util.Iterator;

import org.openedit.entermedia.MediaArchive;

import org.openedit.entermedia.scanner.HotFolderManager

public void init()
{
	MediaArchive archive = context.getPageValue("mediaarchive");
	
	HotFolderManager manager = (HotFolderManager)moduleManager.getBean("hotFolderManager");

	Collection hits = manager.loadFolders( archive.getCatalogId() );
	for(Iterator iterator = hits.iterator(); iterator.hasNext();)
	{
		Data folder = (Data)iterator.next();
		manager.importHotFolder(archive,folder);
	}
	
	/*
	AssetImporter importer = (AssetImporter)moduleManager.getBean("assetImporter");
	importer.setExcludeFolders("Fonts,Links");
	//importer.setIncludeFiles("psd,tif,pdf,eps");
	importer.setUseFolders(false);
	
	String assetRoot = "/WEB-INF/data/" + archive.getCatalogId() + "/originals/";
		
	List created = importer.processOn(assetRoot, assetRoot, archive, context.getUser());
	log.info("created images " + created.size() );
	*/
	
}

init();
