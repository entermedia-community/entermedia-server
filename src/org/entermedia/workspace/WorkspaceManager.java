package org.entermedia.workspace;

import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.zip.ZipOutputStream;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.openedit.Data;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.PropertyDetailsArchive;
import org.openedit.data.SearcherManager;
import org.openedit.xml.XmlArchive;
import org.openedit.xml.XmlFile;

import com.openedit.page.Page;
import com.openedit.page.PageProperty;
import com.openedit.page.PageSettings;
import com.openedit.page.manage.PageManager;
import com.openedit.util.PageZipUtil;
import com.openedit.util.PathUtilities;

public class WorkspaceManager
{
	protected SearcherManager fieldSearcherManager;
	protected PageManager fieldPageManager;
	protected XmlArchive fieldXmlArchive;

	public XmlArchive getXmlArchive()
	{
		return fieldXmlArchive;
	}
	public void setXmlArchive(XmlArchive inXmlArchive)
	{
		fieldXmlArchive = inXmlArchive;
	}
	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}
	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}
	public PageManager getPageManager()
	{
		return fieldPageManager;
	}
	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}
	
	public void exportWorkspace(String apppath, OutputStream inOut) throws Exception
	{
		Page apppage = getPageManager().getPage(apppath);
		String catalogid = apppage.get("catalogid"); 
		
		
		PageZipUtil pageZipUtil = new PageZipUtil(getPageManager());
		//pageZipUtil.setFolderToStripOnZip(false);
		
		ZipOutputStream finalZip = new ZipOutputStream(inOut);
		Collection files = getSearcherManager().getList("media","workspacefiles");
		for (Iterator iterator = files.iterator(); iterator.hasNext();)
		{
			Data folder = (Data) iterator.next();
			String path = folder.getName();
			path = apppage.replaceProperty(path);
			pageZipUtil.zip(path, finalZip);
		}
		finalZip.close();
	}
	
	
	public String createTable(String catalogid, String tablename, String inPrefix) throws Exception
	{
		String searchtype = PathUtilities.makeId(tablename);
		searchtype = searchtype.toLowerCase();
		PropertyDetailsArchive archive = getSearcherManager().getPropertyDetailsArchive(catalogid);
		PropertyDetails details = archive.getPropertyDetails(searchtype);
		if( details == null)
		{
			PropertyDetails defaultdetails = archive.getPropertyDetails("default");
			details = new PropertyDetails();
			details.setDetails(defaultdetails.getDetails());
		}
		details.setPrefix(inPrefix);
		//will default to defaults
		if( details.getDetail("sourcepath") == null )
		{
			PropertyDetail sourcepath = new PropertyDetail();
			sourcepath.setId("sourcepath");
			sourcepath.setName("SourcePath");
			details.addDetail(sourcepath);
		}
		archive.savePropertyDetails(details, searchtype, null);

		//edit beans.xml
		XmlFile file = getXmlArchive().getXml("/" + catalogid + "/configuration/beans.xml");
		Element element = file.getElementById(searchtype + "Searcher");
		if( element == null)
		{
			element = file.addNewElement();
			element.addAttribute("id",searchtype + "Searcher" );
			element.addAttribute("bean","xmlFileSearcher");
			getXmlArchive().saveXml(file, null);
			getSearcherManager().clear();
		}
		return searchtype;
	}
	
	public void saveModule(String catalogid, String appid, Data module) throws Exception
	{
		Page home = getPageManager().getPage("/" + appid + "/views/modules/" + module.getId() + "/_site.xconf");
		PageSettings homesettings = home.getPageSettings();
		homesettings.setProperty("module", module.getId());
		PageProperty prop = new PageProperty("fallbackdirectory");
		prop.setValue("/" + appid + "/views/modules/default");
		homesettings.putProperty(prop);
		getPageManager().getPageSettingsManager().saveSetting(homesettings);
		
		Page settings = getPageManager().getPage("/" + appid + "/views/settings/modules/" + module.getId() + "/_site.xconf");
		PageSettings modulesettings = settings.getPageSettings();
		modulesettings.setProperty("module", module.getId());
		prop = new PageProperty("fallbackdirectory");
		prop.setValue("/" + appid + "/views/settings/modules/default");
		modulesettings.putProperty(prop);
		getPageManager().getPageSettingsManager().saveSetting(modulesettings);

		String path = "/WEB-INF/data/" + catalogid + "/lists/view/" + module.getId() + ".xml";
		String template = "/" + catalogid + "/data/lists/view/default.xml";
		copyXml(catalogid,template,path, module);
		getSearcherManager().removeFromCache(catalogid,"view");


		String path2 = "/WEB-INF/data/" + catalogid + "/lists/settingsmenumodule/" + module.getId() + ".xml";
		String templte2 = "/" + catalogid + "/data/lists/settingsmenumodule/default.xml";
		copyXml(catalogid,templte2,path2,	module);
		getSearcherManager().removeFromCache(catalogid,"settingsmenumodule");

		//add settings menu
		createTable(catalogid, module.getId(), module.getId());

	}
	protected void copyXml(String catalogid, String inTemplatePath, String inEndingPath, Data module)
	{
		if( !getPageManager().getPage(inEndingPath).exists() )
		{
			XmlFile file = getXmlArchive().getXml(inTemplatePath);
			for (Iterator iterator = file.getElements("property"); iterator.hasNext();)
			{
				Element row = (Element) iterator.next();
				for (Iterator iterator2 = row.attributeIterator(); iterator2.hasNext();)
				{
					Attribute attr = (Attribute) iterator2.next();
					String val = attr.getValue();
					val = val.replace("default", module.getId());
					attr.setValue(val);
				}
//				String id = row.attributeValue("id");
//				row.addAttribute("id", id);
//				row.addAttribute("module", module.getId());
//				
//				String parentid = row.attributeValue("parentid");
//				if( parentid != null )
//				{
//					parentid = parentid.replace("default", module.getId());
//					row.addAttribute("parentid", parentid);
//				}
			}
			//Now copy the views default list
			file.setPath(inEndingPath);
			getXmlArchive().saveXml(file, null);
		}
	}
}
