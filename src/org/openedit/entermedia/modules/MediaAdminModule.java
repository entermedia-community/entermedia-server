package org.openedit.entermedia.modules;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.entermedia.EnterMedia;

import com.openedit.OpenEditException;
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
	
	public void deploySite(WebPageRequest inReq) throws Exception
	{
		EnterMedia media = getEnterMedia(inReq);
		Searcher searcher = getSearcherManager().getSearcher(media.getApplicationId(),"site");

		String id = inReq.getRequestParameter("id");
		if( id == null)
		{
			throw new OpenEditException("Id was null");
		}
		Data site = (Data)searcher.searchById(id);
		String frontendid = site.get("frontendid");
		if( frontendid == null)
		{
			throw new OpenEditException("frontendid was null");
		}
		Data frontend = getSearcherManager().getData(media.getApplicationId(),"frontends",frontendid);
		Page copyfrompage = getPageManager().getPage(frontend.get("path"));
		Page topage = getPageManager().getPage("/" + site.get("appid"));
		if( !topage.exists())
		{
			getPageManager().copyPage(copyfrompage,topage);
		}
		//TODO: save catalog id
		topage = getPageManager().getPage(topage.getPath(),true);
		
		topage.getPageSettings().setProperty("catalogid",site.get("appcatalogid"));
		topage.getPageSettings().setProperty("applicationid",topage.getName());
		
		getPageManager().saveSettings(topage);
		
	}
}
