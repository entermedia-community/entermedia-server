package org.entermediadb.find;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.data.QueryBuilder;
import org.openedit.hittracker.HitTracker;
import org.openedit.page.Page;
import org.openedit.page.PageLoader;
import org.openedit.page.manage.PageManager;
import org.openedit.servlet.RightPage;
import org.openedit.servlet.SiteData;
import org.openedit.util.PathUtilities;
import org.openedit.util.URLUtilities;

public class FinderPageLoader implements PageLoader, CatalogEnabled
{
	protected ModuleManager fieldModuleManager;
	protected PageManager fieldPageManager;
	protected String fieldCatalogId;
	private static final Log log = LogFactory.getLog(FinderPageLoader.class);
	
	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	protected MediaArchive getMediaArchive()
	{
		return (MediaArchive)getModuleManager().getBean(getCatalogId(),"mediaArchive");
	}
	
	public RightPage getRightPage( URLUtilities util, SiteData sitedata, Page inPage, String requestedPath)
	{
		String appid = inPage.getProperty("applicationid");

		String homepage = String.format("/%s/index.html", appid); 
		if( !homepage.equals(inPage.getPath()))
		{
			return null;
		}

		//TODO: Pass in user and check permissions
		QueryBuilder query = getMediaArchive().query("appsection").named("topmenuhits").all().sort("ordering");
		Data topmenu = (Data)getMediaArchive().getCachedSearch(query).first();
		if( topmenu == null)
		{
			return null;
		}

		String path =  topmenu.get("custompath");
		if( path != null)
		{
			path = inPage.replaceProperty(path);
		}
		if( path == null && topmenu.get("toplevelentity") != null)
		{
			path = String.format("/%s/views/modules/%s/index.html", appid, topmenu.get("toplevelentity")); 
		}
		if( path != null)
		{
			RightPage right = new RightPage();
	//		right.putParam("communitytagcategory" ,  first.getId());
			String[] parts = path.split("[?]");
			if( parts.length > 1)
			{
				Map arguments = PathUtilities.extractArguments(parts[1]);
				right.setParams(arguments);
			}
			Page page = getPageManager().getPage(parts[0]);
			if( !page.exists())
			{
				return null; //?
			}

			
			right.setRightPage(page);
			return right;
		}
			
		return null;
		//We only care about the home page
	}

	public void setCatalogId(String inId)
	{
		fieldCatalogId = inId;
		
	}
	protected String getCatalogId()
	{
		return fieldCatalogId;
	}

}
