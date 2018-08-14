package org.entermediadb.desktops;

import java.util.Collection;

import org.json.simple.JSONObject;

public interface DesktopEventListener
{
	public void downloadFiles(Collection inAssets);

	public void collectFileList(String path);
}
