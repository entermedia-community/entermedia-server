package org.entermediadb.desktops;

import java.util.Map;

import org.entermediadb.asset.MediaArchive;
import org.entermediadb.projects.LibraryCollection;

public interface DesktopEventListener
{
	//public void downloadFiles(String inPath,Collection<String> inSubFolders, Collection inAssets);

	public void importFiles(MediaArchive inArchive,LibraryCollection inCollection, String path);

	public void uploadFile(String path, Map inVariables);

	public void openRemoteFolder(String inPath);

	public void downloadFolders(MediaArchive inArchive,LibraryCollection inCollection, Map inRoot);

	public void replacedWithNewDesktop(Desktop inDesktop);
	

}
