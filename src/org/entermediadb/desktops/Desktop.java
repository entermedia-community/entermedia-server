package org.entermediadb.desktops;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.projects.LibraryCollection;
import org.json.simple.JSONObject;
import org.openedit.ModuleManager;
import org.openedit.MultiValued;
import org.openedit.hittracker.HitTracker;

public class Desktop
{
	protected DesktopEventListener fieldListener;
	protected boolean fieldBusy;
	
	public boolean isBusy()
	{
		return fieldBusy;
	}

	public void setBusy(boolean inBusy)
	{
		fieldBusy = inBusy;
	}

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
	protected Set fieldEditedCollections;
	
	public boolean isEdited(String inCollectionName)
	{
	
		if( getEditedCollections().contains(inCollectionName))
		{
			return true;
		}
		return false;
	}
	public Set getEditedCollections()
	{
		if( fieldEditedCollections == null)
		{
			fieldEditedCollections = new HashSet();
		}
		return fieldEditedCollections;
	}
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
	
		setBusy(true);

		Category cat = inCollection.getCategory();
		//Build one tree. Have the client pull the data for each one till it's done
		Map root = addChildren(cat.getParentCategory().getCategoryPath(),cat);
		getDesktopListener().downloadFolders(inArchive,inCollection,root);
	//	downloadCat(inArchive, inCollection, cat); //this checks out a ton of folders one chunck at a time

	}
	
	protected Map addChildren(String inRootPath, Category inCat)
	{
		Map inParent = new HashMap();
		inParent.put("name",inCat.getName());
		String remaining = inCat.getCategoryPath().substring(inRootPath.length());
		inParent.put("categoryid",inCat.getId());
		inParent.put("subpath",remaining);
		
		Collection inChildren = new ArrayList();
		inParent.put("children",inChildren);
		for (Iterator iterator = inCat.getChildren().iterator(); iterator.hasNext();)
		{
			Category cat = (Category) iterator.next();
			Map child = addChildren(inRootPath,cat);
			inChildren.add(child);
		}
		return inParent;
	}

	/*
	 * 1. UI is clicked
	 * 2. We tell the client to send us a list of files they have MediaBoatConnection.collectFileList
	 * 3. Once we have the files we diff it in "handledesktopsync" that calls Desktop.checkinCollection
	 * 4. 
	 */
	public void importCollection(MediaArchive inArchive, LibraryCollection inCollection)
	{
		//Category cat = inCollection.getCategory();
		setBusy(true);

		String path = getHomeFolder() + "/EnterMedia/" + inCollection.getName();
		getDesktopListener().importFiles(inArchive, inCollection, path);

	}
	public void openRemoteFolder(MediaArchive inArchive, LibraryCollection inCollection)
	{	
		String path = getHomeFolder() + "/EnterMedia/" + inCollection.getName();
		getDesktopListener().openRemoteFolder(path);
	}
	
	public void uploadFile(JSONObject inMap)
	{
		String path = (String) inMap.get("path");
		getDesktopListener().uploadFile(path,inMap);
		
	}

	public void addEditedCollection(String inName)
	{
		getEditedCollections().add(inName);
	}

	public void replacedWithNewDesktop(Desktop inDesktop)
	{
		getDesktopListener().replacedWithNewDesktop(inDesktop);
	}
}
