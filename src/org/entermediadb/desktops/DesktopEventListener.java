package org.entermediadb.desktops;

import java.util.Collection;
import java.util.Map;

import org.entermediadb.asset.MediaArchive;
import org.entermediadb.projects.LibraryCollection;

public interface DesktopEventListener
{
	public void downloadFiles(String inPath,Collection<String> inSubFolders, Collection inAssets);

	public void collectFileList(MediaArchive inArchive,LibraryCollection inCollection, String path);

	public void uploadFile(String path, Map inVariables);


}
