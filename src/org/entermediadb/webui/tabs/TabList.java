package org.entermediadb.webui.tabs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class TabList
{
	protected String fieldId;
	protected List fieldTabs;
	protected Tab fieldLastSelected;
	
	public String getId()
	{
		return fieldId;
	}
	public void setId(String inId)
	{
		fieldId = inId;
	}
	public List getTabs()
	{
		if (fieldTabs == null)
		{
			fieldTabs = new ArrayList();
		}
		return fieldTabs;
	}
	public int size()
	{
		return getTabs().size();

	}
	public void setTabs(List inTabs)
	{
		fieldTabs = inTabs;
	}
	public void addNewTab(String inId, String inName, String inPath )
	{
		addNewTab( inId, inName, inPath,100);
	}
	public void addNewTab(String inId, String inName, String inPath, int inMaxLevel)
	{
		Tab tab = new Tab();
		//tab.setLink(inPath);
		tab.setName(inName);
		tab.setId(inId);
		tab.setPath(inPath);
		tab.setMaxLevel(inMaxLevel);
		tab.setTimeAdded(new Date());
		getTabs().add(tab);
	}
	public void removeTabById(String inId)
	{
		for (Iterator iterator = getTabs().iterator(); iterator.hasNext();)
		{
			Tab tab = (Tab) iterator.next();
			if( tab.getId() == null || tab.getId().equals(inId))
			{
				getTabs().remove(tab);
				break;
			}
		}
	}
	public Tab getLastSelected()
	{
		return fieldLastSelected;
	}
	public void setLastSelected(Tab inLastSelected)
	{
		fieldLastSelected = inLastSelected;
	}
	
	public void setLastSelectedById( String inTabId )
	{
		for (Iterator iterator = getTabs().iterator(); iterator.hasNext();)
		{
			Tab tab = (Tab) iterator.next();
			if( tab.getId() !=  null && tab.getId().equals(inTabId))
			{
				setLastSelected(tab);
				return;
			}
		}
		setLastSelected((Tab)null);
	}
	public String getSelectedPath()
	{
		if( fieldLastSelected != null)
		{
			return getLastSelected().getPath();
		}
		return null;
	}
	public Tab getTabById(String inId)
	{
		for (Iterator iterator = getTabs().iterator(); iterator.hasNext();)
		{
			Tab	tab = (Tab) iterator.next();
			if( tab.getId() != null && tab.getId().equals(inId))
			{
				return tab;
			}
		}
		return null;
	}
	public void removeOldestTab()
	{
		Tab oldest = null;
		for (Iterator iterator = getTabs().iterator(); iterator.hasNext();)
		{
			Tab tab = (Tab) iterator.next();
			if (oldest == null || tab.getTimeAdded().before(oldest.getTimeAdded()) )
			{
				oldest = tab;
			}
		}
		if (oldest != null)
		{
			removeTabById(oldest.getId());
		}
	}
	public void sortTabs()
	{
		Collections.sort(getTabs());
	}
	public void clearTabs()
	{
		getTabs().clear();
	}
	public void addTab(Tab inTab)
	{
		getTabs().add(inTab);
	}
}
