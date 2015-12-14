package org.entermediadb.model;

import java.util.Collection;

import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.scanner.HotFolderManager;
import org.openedit.Data;

public class HotFolderTest extends BaseEnterMediaTest
{
	public void testAdd() throws Exception
	{
		
		HotFolderManager manager = (HotFolderManager)getBean("hotFolderManager");
		MediaArchive archive = getMediaArchive();		
		String catalogId = archive.getCatalogId();
		String path = "/WEB-INF/data/" + catalogId + "/originals/test";
		Data existing = manager.getFolderByPathEnding(catalogId, "test");
		if( existing != null)
		{
			manager.deleteFolder(catalogId,existing);
		}
		Data newrow = archive.getSearcher("hotfolder").createNewData();
		newrow.setName("Test");
		newrow.setProperty("subfolder", "test");
		newrow.setProperty("externalpath", path);
		newrow.setProperty("includes", path);
		newrow.setProperty("excludes", path);
			
		
		manager.saveFolder(catalogId,newrow);
		Collection folders = manager.loadFolders(catalogId);
		assertTrue(folders.size() > 0);
	}
}
