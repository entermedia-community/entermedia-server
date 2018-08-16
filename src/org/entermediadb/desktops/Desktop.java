package org.entermediadb.desktops;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.projects.LibraryCollection;
import org.entermediadb.projects.ProjectManager;
import org.json.simple.JSONObject;
import org.openedit.ModuleManager;
import org.openedit.MultiValued;
import org.openedit.hittracker.HitTracker;

public class Desktop
{
	protected DesktopEventListener fieldListener;

	public void setListener(DesktopEventListener inListener)
	{
		fieldListener = inListener;
	}

	protected DesktopEventListener getDesktopListener()
	{
		return fieldListener;
	}

	protected String fieldUserId;

	public String getUserId()
	{
		return fieldUserId;
	}

	public void setUserId(String inUserId)
	{
		fieldUserId = inUserId;
	}

	public String getDesktopId()
	{
		return fieldDesktopId;
	}

	public void setDesktopId(String inDesktopId)
	{
		fieldDesktopId = inDesktopId;
	}

	public String getLastCommand()
	{
		return fieldLastCommand;
	}

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	public void setLastCommand(String inLastCommand)
	{
		fieldLastCommand = inLastCommand;
	}

	protected String fieldDesktopId;
	protected String fieldLastCommand;
	protected int fieldLastCompletedPercent;
	protected String fieldHomeFolder;
	protected String fieldSpaceLeft;
	protected String fieldServerName;
	protected ModuleManager fieldModuleManager;

	public String getServerName()
	{
		return fieldServerName;
	}

	public void setServerName(String inServerName)
	{
		fieldServerName = inServerName;
	}

	public String getSpaceLeft()
	{
		return fieldSpaceLeft;
	}

	public void setSpaceLeft(String inSpaceLeft)
	{
		fieldSpaceLeft = inSpaceLeft;
	}

	public String getHomeFolder()
	{
		return fieldHomeFolder;
	}

	public void setHomeFolder(String inHomeFolder)
	{
		fieldHomeFolder = inHomeFolder;
	}

	public int getLastCompletedPercent()
	{
		return fieldLastCompletedPercent;
	}

	public void setLastCompletedPercent(int inLastCompletedPercent)
	{
		fieldLastCompletedPercent = inLastCompletedPercent;
	}

	public void checkoutCollection(MediaArchive inArchive, LibraryCollection inCollection)
	{
		Category cat = inCollection.getCategory();
		downloadCat(inArchive, inCollection, cat);

	}

	public void importCollection(MediaArchive inArchive, LibraryCollection inCollection)
	{
		Category cat = inCollection.getCategory();

		String path = getHomeFolder() + "/EnterMedia/" + inCollection.getName();
		getDesktopListener().collectFileList(inArchive, inCollection, path);

	}

	private void downloadCat(MediaArchive inArchive, LibraryCollection inCollection, Category inCat)
	{
		List tosend = new ArrayList();

		String root = inCollection.getCategory().getCategoryPath();
		String folder = inCat.getCategoryPath().substring(root.length());
		String path = getHomeFolder() + "/EnterMedia/" + inCollection.getName();
		if (!folder.isEmpty())
		{
			path = path + "/" + folder;
		}

		HitTracker assets = inArchive.query("asset").exact("category-exact", inCat.getId()).search();
		assets.enableBulkOperations();
		for (Iterator iterator = assets.iterator(); iterator.hasNext();)
		{
			MultiValued asset = (MultiValued) iterator.next();
			Map map = new HashMap();
			map.put("id", asset.getId());

			String assetpath = inArchive.asLinkToOriginal(asset);

			String url = getServerName() + "/" + inArchive.getMediaDbId() + "/services/module/asset/downloads/originals/" + assetpath;
			map.put("url", url);

			String primaryImageName = asset.get("primaryfile");
			if (primaryImageName == null)
			{
				primaryImageName = asset.getName();
			}
			String savepath = path + "/" + primaryImageName;
			map.put("savepath", savepath);

			map.put("filesize", asset.get("filesize"));
			long time = asset.getDate("assetmodificationdate").getTime();
			if (time > 0)
			{
				map.put("assetmodificationdate", String.valueOf(time));
			}
			tosend.add(map);
		}
		getDesktopListener().downloadFiles(tosend);
		for (Iterator iterator = inCat.getChildren().iterator(); iterator.hasNext();)
		{
			Category child = (Category) iterator.next();
			downloadCat(inArchive, inCollection, child);
		}

	}

	public void checkinCollection(JSONObject map)
	{		
		String catalogid = (String) map.get("catalogid");
		MediaArchive archive = (MediaArchive) getModuleManager().getBean(catalogid, "mediaArchive");
		ProjectManager pm = archive.getProjectManager();
		pm.saveCheckinRequest(this,map);
		
	}

	public void uploadFile(JSONObject inMap)
	{
		String path = (String) inMap.get("path");
		getDesktopListener().uploadFile(path,inMap);
		
	}
}
