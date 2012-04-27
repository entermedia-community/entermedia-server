package org.openedit.entermedia.model;

import java.util.Collection;

import org.openedit.Data;
import org.openedit.entermedia.BaseEnterMediaTest;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.cluster.NodeManager;
import org.openedit.entermedia.scanner.HotFolderManager;

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
		Data newrow = manager.getFolderSearcher(catalogId).createNewData();
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
