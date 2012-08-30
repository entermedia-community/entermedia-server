package importing;

import org.openedit.entermedia.MediaArchive 
import org.openedit.*;

import com.openedit.hittracker.*;
import com.openedit.page.Page
import com.openedit.util.Exec
import com.openedit.util.ExecResult

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
		String base = "/WEB-INF/data/" + archive.getCatalogId() + "/originals";
		String name = folder.get("subfolder");
		String path = base + "/" + name ;
		Page page = pageManager.getPage(path + "/.git");
		if( page.exists() )
		{
			Exec exec = moduleManager.getBean("exec");
			List commands = new ArrayList();
			commands.add("--git-dir");
			commands.add(page.getContentItem().getAbsolutePath());
			commands.add("pull");
			ExecResult result = exec.runExec("git",commands);
			if( result.isRunOk() )
			{
				log.info("pulled from git "  + path );
			}
			else
			{
				log.error("Could not pull from "  + path );
			}		
		}
		
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
