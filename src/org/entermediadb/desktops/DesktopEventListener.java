package org.entermediadb.desktops;

import java.util.Map;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.projects.LibraryCollection;
import org.json.simple.JSONObject;

public interface DesktopEventListener
{
	//public void downloadFiles(String inPath,Collection<String> inSubFolders, Collection inAssets);

	public void importFiles(MediaArchive inArchive,LibraryCollection inCollection, String path);

	public void uploadFile(String path, Map inVariables);

	public void openRemoteFolder(String inPath);

	public void downloadFolders(MediaArchive inArchive,LibraryCollection inCollection, Map inRoot);

	public void replacedWithNewDesktop(Desktop inDesktop);

	public void openAsset(MediaArchive inArchive, Asset inAsset);
	
	public void sendCommand(MediaArchive inArchive, Map inCommandData);

	public Map sendCommandAndWait(MediaArchive inArchive, JSONObject inCommand);

}
