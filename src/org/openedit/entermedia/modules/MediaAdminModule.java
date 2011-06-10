package org.openedit.entermedia.modules;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.entermedia.EnterMedia;

import com.openedit.WebPageRequest;
import com.openedit.page.Page;
import com.openedit.page.PageProperty;

public class MediaAdminModule extends BaseMediaModule
{

	public void listThemes(WebPageRequest inReq)
	{
		String skinsPath = "/themes";
		List children = getPageManager().getChildrenPaths(skinsPath, true);
		Map skins = new HashMap();

		for (Iterator iterator = children.iterator(); iterator.hasNext();)
		{
			String path = (String) iterator.next();
			Page theme = getPageManager().getPage(path);
			if (theme.isFolder() && theme.get("themename") != null)
			{
				skins.put("/themes/" + theme.getName(), theme.get("themename"));
			}
		}
		inReq.putPageValue("themes", skins);
	}

	public void changeTheme(WebPageRequest inReq) throws Exception
	{
		String layout = inReq.getRequestParameter("theme");
		if (layout == null)
		{
			return;
		}
		String path = inReq.getRequestParameter("path");
		if( path == null)
		{
			return;
		}
		//"/" + inReq.findValue("applicationid");
		Page page = getPageManager().getPage(path); //This is the root level for this album
		PageProperty skin = new PageProperty("themeprefix");
		if( "default".equals( layout) )
		{
			page.getPageSettings().removeProperty("themeprefix");
		}
		else
		{
			skin.setValue(layout);
			page.getPageSettings().putProperty(skin);
		}
		getPageManager().saveSettings(page);
	}
	
	public void addSite(WebPageRequest inReq) throws Exception
	{
		String frontend = inReq.getRequestParameter("frontend.value");
		
		Page copyfrompage = getPageManager().getPage(frontend);

		String path = inReq.getRequestParameter("newpath");
		if( !path.startsWith("/"))
		{
			path = "/" + path;
		}
		Page topage = getPageManager().getPage(path);
		if( !topage.exists())
		{
			getPageManager().copyPage(copyfrompage,topage);
		}
		
		EnterMedia media = getEnterMedia(inReq);
		Searcher searcher = getSearcherManager().getSearcher(media.getApplicationId(),"sites");

		Data child = searcher.createNewData();
		String title = inReq.getRequestParameter("title.value");
		child.setName(title);
		child.setProperty("path",path);
		child.setProperty("frontend",frontend);
		//child.setName("created by " + inReq.getUser());
		searcher.saveData(child,inReq.getUser());
		
		String catid = inReq.getRequestParameter("newcatalogid");
		
		//TODO: save catalog id
		topage = getPageManager().getPage(path,true);
		topage.getPageSettings().setProperty("catalogid",catid);
		topage.getPageSettings().setProperty("applicationid",path.substring(1));
		
		getPageManager().saveSettings(topage);
		
	}
}
