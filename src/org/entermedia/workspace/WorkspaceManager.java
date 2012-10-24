package org.entermedia.workspace;

import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.zip.ZipOutputStream;

import org.dom4j.Attribute;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.openedit.Data;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.PropertyDetailsArchive;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.xml.XmlArchive;
import org.openedit.xml.XmlFile;

import com.openedit.OpenEditException;
import com.openedit.page.Page;
import com.openedit.page.PageProperty;
import com.openedit.page.PageSettings;
import com.openedit.page.manage.PageManager;
import com.openedit.util.PageZipUtil;
import com.openedit.util.PathUtilities;
import com.openedit.util.XmlUtil;
import com.openedit.util.ZipUtil;

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
		String appid = apppage.get("applicationid");

		PageZipUtil pageZipUtil = new PageZipUtil(getPageManager());
		// pageZipUtil.setFolderToStripOnZip(false);

		ZipOutputStream finalZip = new ZipOutputStream(inOut);
		Collection files = getSearcherManager().getList("media", "workspacefiles");
		for (Iterator iterator = files.iterator(); iterator.hasNext();)
		{
			Data folder = (Data) iterator.next();
			String path = folder.getName();
			path = apppage.replaceProperty(path);
			pageZipUtil.zip(path, finalZip);
		}
		Element root = DocumentHelper.createElement("application");
		root.addElement("applicationid").addAttribute("id", appid);
		root.addElement("catalogid").addAttribute("id", catalogid);

		Data app = getSearcherManager().getData("media", "site", appid);
		if (app != null)
		{
			root.addElement("name").setText(app.getName());
		}
		// root.addElement("deploypath").addAttribute("id",catalogid);
		pageZipUtil.addTozip(root.asXML(), ".emapp.xml", finalZip);

		finalZip.close();
	}

	public String createTable(String catalogid, String tablename, String inPrefix) throws Exception
	{
		String searchtype = PathUtilities.makeId(tablename);
		searchtype = searchtype.toLowerCase();
		PropertyDetailsArchive archive = getSearcherManager().getPropertyDetailsArchive(catalogid);
		PropertyDetails details = archive.getPropertyDetails(searchtype);
		if (details == null)
		{
			PropertyDetails defaultdetails = archive.getPropertyDetails("default");
			details = new PropertyDetails();
			details.setDetails(defaultdetails.getDetails());
		}
		details.setPrefix(inPrefix);
		// will default to defaults
		if (details.getDetail("sourcepath") == null)
		{
			PropertyDetail sourcepath = new PropertyDetail();
			sourcepath.setId("sourcepath");
			sourcepath.setName("SourcePath");
			details.addDetail(sourcepath);
		}
		details.setBeanName("xmlFileSearcher");
		archive.savePropertyDetails(details, searchtype, null);

		// edit beans.xml
//		XmlFile file = getXmlArchive().getXml("/" + catalogid + "/configuration/beans.xml");
//		Element element = file.getElementById(searchtype + "Searcher");
//		if (element == null)
//		{
//			element = file.addNewElement();
//			element.addAttribute("id", searchtype + "Searcher");
//			element.addAttribute("bean", "xmlFileSearcher");
//			getXmlArchive().saveXml(file, null);
//		}
		//getSearcherManager().clear();
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

		String template = "/" + catalogid + "/data/lists/view/default.xml";
		String path = "/WEB-INF/data/" + catalogid + "/lists/view/" + module.getId() + ".xml";
		copyXml(catalogid, template, path, module);
		getSearcherManager().removeFromCache(catalogid, "view");

		String templte2 = "/" + catalogid + "/data/lists/settingsmenumodule/default.xml";
		String path2 = "/WEB-INF/data/" + catalogid + "/lists/settingsmenumodule/" + module.getId() + ".xml";
		copyXml(catalogid, templte2, path2, module);

		
		String templte3 = "/" + catalogid + "/data/lists/settingsmodulepermissionsdefault.xml";
		String path3 = "/WEB-INF/data/" + catalogid + "/lists/settingsmodulepermissions" + module.getId() + ".xml";
		copyXml(catalogid, templte3, path3, module);

		getSearcherManager().removeFromCache(catalogid, "settingsmenumodule");

		// add settings menu
		createTable(catalogid, module.getId(), module.getId());
		//getPageManager().clearCache();
	}

	protected void copyXml(String catalogid, String inTemplatePath, String inEndingPath, Data module)
	{
		if (!getPageManager().getPage(inEndingPath).exists())
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
				// String id = row.attributeValue("id");
				// row.addAttribute("id", id);
				// row.addAttribute("module", module.getId());
				//
				// String parentid = row.attributeValue("parentid");
				// if( parentid != null )
				// {
				// parentid = parentid.replace("default", module.getId());
				// row.addAttribute("parentid", parentid);
				// }
			}
			// Now copy the views default list
			file.setPath(inEndingPath);
			getXmlArchive().saveXml(file, null);
		}
	}

	public void deployUploadedApp(Page zip)
	{
		Page dest = getPageManager().getPage("/");
		try
		{
			new ZipUtil().unzip(zip.getContentItem().getAbsolutePath(), dest.getContentItem().getAbsolutePath());
			Page def = getPageManager().getPage("/.emapp.xml");
			Element root = new XmlUtil().getXml(def.getReader(), "UTF-8");
			String applicationid = root.element("applicationid").attributeValue("id");
			String catalogid = root.element("catalogid").attributeValue("id");

			Searcher searcher = getSearcherManager().getSearcher(applicationid, "site");

			Data site = (Data) searcher.searchByField("deploypath", applicationid);
			if (site == null)
			{
				site = searcher.createNewData();
			}
			// String frontendid = inReq.findValue("frontendid");
			// if( frontendid == null)
			// {
			// throw new OpenEditException("frontendid was null");
			// }

			if (applicationid != null)
			{
				site.setProperty("deploypath", applicationid);
			}
			if (catalogid != null)
			{

				site.setProperty("appcatalogid", catalogid);
			}

			String name = root.elementText("name");
			if (name != null)
			{
				site.setName(name);
			}

			searcher.saveData(site, null);

			Searcher catsearcher = getSearcherManager().getSearcher("media", "catalogs");
			Data cat = (Data) catsearcher.searchById(catalogid);
			if (cat == null)
			{
				cat = catsearcher.createNewData();
				cat.setId(catalogid);
				catsearcher.saveData(cat, null);
			}
		}
		catch (Exception ex)
		{
			throw new OpenEditException(ex);
		}

	}
}
