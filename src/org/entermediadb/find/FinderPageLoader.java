package org.entermediadb.find;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.data.QueryBuilder;
import org.openedit.page.Page;
import org.openedit.page.PageLoader;
import org.openedit.page.manage.PageManager;
import org.openedit.servlet.RightPage;
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
	
	public RightPage getRightPage( URLUtilities util, org.openedit.servlet.Site site, Page inPage)
	{
		String appid = inPage.getProperty("applicationid");
		String moduleid = util.getRequestParameter("entitymoduleid");
		if( moduleid != null)
		{
			String path  = String.format("/%s/views/modules/%s/index.html", appid, moduleid);  //Permission check?
			Page page = getPageManager().getPage(path);
			if( page.exists())
			{
				RightPage right = new RightPage();
				right.setRightPage(page);
				return right;
			}
		}
		
		boolean loadmodule = false;
		String homepage = String.format("/%s/index.html", appid); 
		if( homepage.equals(inPage.getPath()))
		{
			loadmodule = true;
		}
		else
		{
			homepage = String.format("/%s/", appid); 
			if( homepage.equals(inPage.getPath()))
			{
				loadmodule = true;
			}
		}

		if( !loadmodule )
		{
			//No need to preload a module. Not on a home page
			//log.info("Could not load home page " + appid + " " + requestedPath);
			return null;
		}
		//TODO: Pass in user and check permissions
		QueryBuilder query = getMediaArchive().query("appsection").named("topmenuhits").all().sort("ordering");
		Data topmenu = (Data)getMediaArchive().getCachedSearch(query).first();
		if( topmenu == null)
		{
			log.info("No top menu");
			return null;
		}
		


		String firstmenupath =  topmenu.get("custompath");
		if( firstmenupath != null)
		{
			firstmenupath = inPage.replaceProperty(firstmenupath);
		}
		if( firstmenupath == null && topmenu.get("toplevelentity") != null)
		{
			firstmenupath = String.format("/%s/views/modules/%s/index.html", appid, topmenu.get("toplevelentity")); 
		}
		if( firstmenupath != null)
		{
			RightPage right = new RightPage();
	//		right.putParam("communitytagcategory" ,  first.getId());
			String[] parts = firstmenupath.split("[?]");
			if( parts.length > 1)
			{
				Map arguments = PathUtilities.extractArguments(parts[1]);
				right.setParams(arguments);
			}
			Page page = getPageManager().getPage(parts[0]);
			if( !page.exists())
			{
				//log.info("page missing "+ parts[0]);
				return null; //?
			}
			right.setRightPage(page);
			return right;
		}
			
	    //log.info("Page:" + inPage.getPath()  + " for url: " + requestedPath);
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
