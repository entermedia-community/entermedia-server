package org.entermediadb.webui.tabs;

import java.util.Iterator;

import org.openedit.WebPageRequest;
import org.openedit.modules.BaseModule;

public class TabModule extends BaseModule
{

	public void selectTab(WebPageRequest inReq) throws Exception
	{
		TabList list = loadTabs(inReq);
		if( list != null)
		{
			
			String tabid = inReq.getRequestParameter("tabid");
			if( tabid == null)
			{
				tabid = inReq.findValue(list.getId() + "tabid");
			}
			if( tabid != null)
			{
				list.setLastSelectedById(tabid);
				return;
			}
			String path = inReq.getContentPage().getPath();
			for (Iterator iterator = list.getTabs().iterator(); iterator.hasNext();)
			{
				Tab	tab = (Tab) iterator.next();
				if( tab.getPath().startsWith(path))
				{
					list.setLastSelected(tab);
					break;
				}
			}
		}
	}
	public void redirectToSelected(WebPageRequest inReq) throws Exception
	{
		TabList list = loadTabs(inReq);
		if( list.getLastSelected() != null)
		{
			String path = inReq.getContentPage().getPath();
			String show = list.getLastSelected().getPath();
			if( !path.equals(show))
			{
				inReq.redirect(show);
			}
		}
	}
	public void addNewTab(WebPageRequest inReq) throws Exception
	{
		TabList list = loadTabs(inReq);
		String id = inReq.findValue("tabid");
		String path = inReq.findValue("tabpath");
		if( id == null || path == null)
		{
			return;
		}
		Tab tab = list.getTabById(id);
		if( tab != null)
		{
			list.setLastSelected(tab);
			return;
		}
		String name = inReq.findValue("tabname");
		list.addNewTab(id,name,path);
		list.setLastSelectedById(id);
		
		String sort = inReq.findValue("sorttabs");
		if( Boolean.parseBoolean(sort))
		{
			list.sortTabs();
		}
		String editor = inReq.getRequestParameter("editorpath");
		if( editor != null )
		{
			String max = inReq.getRequestParameter("oemaxlevel");
			if( max == null)
			{
				inReq.redirect(editor);
			}
			else
			{
				inReq.redirect(editor + "&oemaxlevel=" + max); //Does this even work?				
			}
		}

	}
		
	public void clearUnneededTabs(WebPageRequest inReq) throws Exception
	{
		String maxtabcount = inReq.findValue("maxtabcount");
		if( maxtabcount != null)
		{
			TabList list = loadTabs(inReq);
			if (list.size() > Integer.parseInt(maxtabcount))
			{
				list.removeOldestTab();
			}
		}
	}
	public void removeTab(WebPageRequest inReq) throws Exception
	{
		TabList list = loadTabs(inReq);
		String name = inReq.findValue("tabid");
		list.removeTabById(name);
	}
	public TabList loadTabs(WebPageRequest inReq) throws Exception
	{
		String tabid = inReq.findValue("tablistname");
		String id  =  inReq.findPathValue("catalogid");
		if( id == null)
		{
			id = inReq.findValue("applicationid");
		}
		TabList list = (TabList)inReq.getSessionValue(tabid + id);
		if( list == null)
		{
			list = new TabList();
			list.setId(tabid);
			inReq.putSessionValue(tabid + id,list);
		}
		inReq.putPageValue(tabid,list);
		return list;
	}
	public void clearHistory( WebPageRequest inReq )throws Exception
	{
		TabList list = loadTabs(inReq);
		Tab tab = list.getLastSelected();
		list.clearTabs();
		list.addTab(tab);
		list.setLastSelected(tab);
	}
}
