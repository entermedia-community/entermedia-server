package org.openedit.entermedia.scanner;

import java.util.Collection;
import java.util.List;

import org.openedit.Data;
import org.openedit.entermedia.MediaArchive;

public interface HotFolderManager
{
	public Collection loadFolders(String inCatalogId);
	
	void deleteFolder(String inCatalogId, Data inExisting);

	void saveFolder(String inCatalogId, Data inNewrow);

	List<String> importHotFolder(MediaArchive inArchive, Data inFolder);
	public void saveMounts(String inCatalogId);
	
	public Data getFolderByPathEnding(String inCatalogId, String inFolder);

}