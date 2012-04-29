package org.openedit.entermedia.modules;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.xml.XmlFolderSearcher;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.page.Page;
import com.openedit.page.PageProperty;

public class MediaAdminModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(MediaAdminModule.class);

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
	
	public void deployApp(WebPageRequest inReq) throws Exception
	{
		String applicationid = inReq.findValue("applicationid");
		Searcher searcher = getSearcherManager().getSearcher(applicationid,"site");

		Data site = null;
		String id = inReq.getRequestParameter("id");
		if( id == null)
		{
			site = searcher.createNewData();
		}
		else
		{
			site = (Data)searcher.searchById(id);
		}
		String frontendid = inReq.findValue("frontendid");
		if( frontendid == null)
		{
			throw new OpenEditException("frontendid was null");
		}
		String deploypath = inReq.findValue("deploypath");
		site.setProperty("deploypath",deploypath);
		
		String appcatalogid = inReq.findValue("appcatalogid");
		site.setProperty("appcatalogid",appcatalogid);

		String name = inReq.findValue("sitename");
		site.setName(name);

		site.setProperty("frontendid",frontendid);

		searcher.saveData(site, inReq.getUser());
		Data frontend = getSearcherManager().getData(applicationid,"frontend",frontendid);
		Page copyfrompage = getPageManager().getPage(frontend.get("path"));
		
		Page topage = getPageManager().getPage("/" + site.get("deploypath"));
		if( !topage.exists())
		{
			getPageManager().copyPage(copyfrompage,topage);
		}
		topage = getPageManager().getPage(topage.getPath(),true);
		
		topage.getPageSettings().setProperty("catalogid",site.get("appcatalogid"));
		topage.getPageSettings().setProperty("applicationid",topage.getName());
		
		getPageManager().saveSettings(topage);
		
	}
	
	public void saveRows(WebPageRequest inReq) throws Exception
	{
		String catalogid = inReq.findValue("catalogid");
		Searcher searcher = getSearcherManager().getSearcher(catalogid,"catalogsettings");

		String[] fields = inReq.getRequestParameters("field");
		for (int i = 0; i < fields.length; i++)
		{
			Data existing = (Data)searcher.searchById(fields[i]);
			if( existing == null)
			{
				log.error("No default value"  + fields[i]);
				continue;
			}
			boolean save = false;
			String[] values = inReq.getRequestParameters(fields[i] + ".value");
			if( values != null && values.length > 0)
			{
				if( values.length == 1)
				{
					if( !values[0].equals(existing.get("value")))
					{
						save = true;
						existing.setProperty("value", values[0]);
					}
				}
				else
				{
					save = true;
					StringBuffer buffer = new StringBuffer();
					for (int j = 0; j < values.length; j++)
					{
						buffer.append(values[j]);
						if( j+1 < values.length)
						{
							buffer.append(' ');
						}
					}
					existing.setProperty("value", buffer.toString());
				}
			}
			else
			{
				if( existing.get("value") != null)
				{
					save = true;
					existing.setProperty("value", null);
				}
			}
			if( save )
			{
				searcher.saveData(existing, inReq.getUser());
			}
		}
		
		
		
	}

}
