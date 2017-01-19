package org.entermediadb.workspace;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

import org.dom4j.Attribute;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.PropertyDetailsArchive;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.node.NodeManager;
import org.openedit.page.Page;
import org.openedit.page.PageProperty;
import org.openedit.page.PageSettings;
import org.openedit.page.manage.PageManager;
import org.openedit.util.PageZipUtil;
import org.openedit.util.PathUtilities;
import org.openedit.util.Replacer;
import org.openedit.util.XmlUtil;
import org.openedit.util.ZipUtil;
import org.openedit.xml.XmlArchive;
import org.openedit.xml.XmlFile;

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
		root.addAttribute("version", "9");
		
		Data app = (Data) getSearcherManager().getSearcher(catalogid, "app").searchByField("deploypath", "/" + appid);
		//Data app = getSearcherManager().getData(catalogid, "app", appid);
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
		String path = "/WEB-INF/data/" + catalogid + "/fields/" + searchtype + ".xml";
		if( getPageManager().getPage(path).exists() )
		{
			return searchtype;
		}
		PropertyDetails details = archive.getPropertyDetails(searchtype);
		if (details == null)
		{
			PropertyDetails defaultdetails = archive.getPropertyDetails("default");
			details = new PropertyDetails(archive,searchtype);
			details.setDetails(defaultdetails.getDetails());
		}
		if( details.getPrefix() == null )
		{
			details.setPrefix(inPrefix);
		}
		// will default to defaults
		if (details.getDetail("sourcepath") == null)
		{
			PropertyDetail sourcepath = new PropertyDetail();
			sourcepath.setId("sourcepath");
			sourcepath.setName("SourcePath");
			details.addDetail(sourcepath);
		}
		if( details.getBeanName() == null )
		{
			details.setBeanName("dataSearcher");
		}
		
		for (Iterator iterator = details.iterator(); iterator.hasNext();) {
			PropertyDetail detail = (PropertyDetail) iterator.next();
			archive.savePropertyDetail(detail, searchtype, null);

			
		}

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
		/** APP STUFF **/
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
		
		/** DATABASE STUFF **/
		String template = "/" + catalogid + "/data/lists/view/default.xml";
		String path = "/WEB-INF/data/" + catalogid + "/lists/view/" + module.getId() + ".xml";
		copyXml(catalogid, template, path, module);
		Searcher views = getSearcherManager().getSearcher(catalogid, "view");

		Collection valuesdir = getPageManager().getChildrenPaths("/" + catalogid + "/data/views/defaults/",true );
		for (Iterator iterator = valuesdir.iterator(); iterator.hasNext();)
		{
			String copypath = (String) iterator.next();
			Page input = getPageManager().getPage(copypath);
			Page destpath = getPageManager().getPage( "/WEB-INF/data/" + catalogid + "/views/" + module.getId() + "/" + module.getId()+ input.getName());
			getPageManager().copyPage(input, destpath);
			
		}
		views.reIndexAll();

		
		String templte2 = "/" + catalogid + "/data/lists/settingsmenumodule/default.xml";
		String path2 = "/WEB-INF/data/" + catalogid + "/lists/settingsmenumodule/" + module.getId() + ".xml";
		copyXml(catalogid, templte2, path2, module);
		
		Searcher settingsmenumodule = getSearcherManager().getSearcher(catalogid, "settingsmenumodule");
		settingsmenumodule.reIndexAll();
		
		String templte3 = "/" + catalogid + "/data/lists/settingsmodulepermissionsdefault.xml";
		String path3 = "/WEB-INF/data/" + catalogid + "/lists/settingsmodulepermissions" + module.getId() + ".xml";
		copyXml(catalogid, templte3, path3, module);

		
		createMediaDbModule(catalogid,module);
		
		
		getSearcherManager().removeFromCache(catalogid, "settingsmenumodule");

		// add settings menu
		createTable(catalogid, module.getId(), module.getId());
		//getPageManager().clearCache();
				
		
	}

	protected void createMediaDbModule(String inCatalogId, Data inModule)
	{
		//Data setup
		Data setting = getSearcherManager().getData(inCatalogId, "catalogsettings", "mediadbappid");
		String mediadb = setting.get("value");
		
		
		Replacer replacer = new Replacer();
		Map lookup = new HashMap();
		lookup.put("mediadbappid",mediadb);
		lookup.put("moduleid",inModule.getId());
		lookup.put("module",inModule);
		
		Searcher sectionSearcher = getSearcherManager().getSearcher(inCatalogId, "docsection");
		Data section = (Data)sectionSearcher.searchById("module" + inModule.getId() );
		if( section == null )
		{
			section = sectionSearcher.createNewData();
			section.setId("module" + inModule.getId());
			section.setName(inModule.getName());
			sectionSearcher.saveData(section, null);
		}
		
		Searcher endpointSearcher = getSearcherManager().getSearcher(inCatalogId, "endpoint");
		Collection templates = getSearcherManager().getList(inCatalogId, "endpointmoduletemplate");
		for (Iterator iterator = templates.iterator(); iterator.hasNext();)
		{
			Data row = (Data) iterator.next();
			Data endpoint = (Data)endpointSearcher.searchById(section.getId() + row.getId());
			if( endpoint == null )
			{
				endpoint = endpointSearcher.createNewData();
			}
			//endpoint.setProperties(row.getProperties());
			for (Iterator iterator2 = row.keySet().iterator(); iterator2.hasNext();)
			{
				String key = (String) iterator2.next();
				String val = row.get(key);
				val = replacer.replace(val, lookup);
				endpoint.setProperty(key, val);
			}
			
			endpoint.setId(section.getId() + row.getId() );
			endpoint.setProperty( "docsection",section.getId() );
			endpointSearcher.saveData(endpoint, null);
		}
		
		//Files
		Page home = getPageManager().getPage("/" + mediadb + "/services/module/" + inModule.getId() + "/_site.xconf");
		if (!home.exists())
		{
			PageSettings homesettings = home.getPageSettings();
			homesettings.setProperty("module", inModule.getId());
			PageProperty prop = new PageProperty("fallbackdirectory");
			prop.setValue("/" + mediadb + "/services/module/default");
			homesettings.putProperty(prop);
			prop = new PageProperty("searchtype");
			prop.setValue(inModule.getId());
			homesettings.putProperty(prop);
			getPageManager().getPageSettingsManager().saveSetting(homesettings);
		}
		getPageManager().clearCache();
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

	public void fixFiles(Page inFolder, String inOldCatalogId)
	{
		Page upload = getPageManager().getPage(inFolder.getPath() + "/WEB-INF/data/" + inOldCatalogId + "/lists/settingsgroup.xml");
		
		XmlUtil util = new XmlUtil();
		Element root = util.getXml(upload.getReader(),"utf-8");
		for(Iterator iterator = root.elementIterator(); iterator.hasNext();)
		{
			Element row = (Element)iterator.next();
			StringBuffer perms = new StringBuffer();
			List atrribs = new ArrayList(row.attributes());
			
			for(Iterator iterator2 = row.attributes().iterator(); iterator2.hasNext();)
			{
				Attribute attr = (Attribute)iterator2.next();
				if( Boolean.valueOf(attr.getValue() ) )
				{
					atrribs.remove(attr);
					if (perms.length() > 0) {
						perms.append("|");
					}
					perms.append(attr.getQualifiedName() );
				}
			}
			row.setAttributes(atrribs);
			row.addAttribute("permissions", perms.toString());
			
		}
		OutputStream out = getPageManager().saveToStream(upload);
		util.saveXml(root, out, "utf-8");
	}

	
	public void deployUploadedApp(String inAppcatalogid, String inDestinationAppId, Page zip)
	{
		Page dest = getPageManager().getPage("/WEB-INF/temp/appunzip");
		try
		{
			getPageManager().removePage(dest);
			
			new ZipUtil().unzip(zip.getContentItem().getAbsolutePath(), dest.getContentItem().getAbsolutePath());
			
			Page def = getPageManager().getPage(dest.getPath() + "/.emapp.xml");
			Element root = new XmlUtil().getXml(def.getReader(), "UTF-8");
			String oldapplicationid = root.element("applicationid").attributeValue("id");
			String oldcatalogid = root.element("catalogid").attributeValue("id");

			
			String version = root.attributeValue("version");
			if( version == null || version.equals("8"))
			{
				//fix settingsgroups.xml
				fixFiles(dest, oldcatalogid);
			}

			
			//We need to delete the incoming list of apps
			Page appdata = getPageManager().getPage(dest.getPath() + "/WEB-INF/data/" + oldcatalogid + "/lists/app/custom.xml" );
			getPageManager().removePage(appdata);
			
			//move the files in place
			Page apphome = getPageManager().getPage(dest.getPath() + "/" + oldapplicationid);
			Page appdest = getPageManager().getPage( "/" + inDestinationAppId);
			getPageManager().removePage(appdest);
			getPageManager().copyPage(apphome, appdest);

			//tweak the xconf
			PageSettings homesettings = getPageManager().getPageSettingsManager().getPageSettings("/" + inDestinationAppId + "/_site.xconf");
			homesettings.setProperty("applicationid", inDestinationAppId);
			homesettings.setProperty("catalogid", inAppcatalogid);
			if( homesettings.getProperty("fallbackdirectory") == null )
			{
				homesettings.setProperty("fallbackdirectory","/WEB-INF/base/emshare");
			}
			getPageManager().getPageSettingsManager().saveSetting(homesettings);

			
			Page cataloghome = getPageManager().getPage(dest.getPath() + "/" + oldcatalogid);
			if( cataloghome.exists() )
			{
				Page catalogdest = getPageManager().getPage( "/" + inAppcatalogid);
				getPageManager().removePage(catalogdest);
				getPageManager().copyPage(cataloghome, catalogdest);
				
				PageSettings catsettings = getPageManager().getPageSettingsManager().getPageSettings("/" + inAppcatalogid + "/_site.xconf");
				catsettings.setProperty("catalogid", inAppcatalogid);
				catsettings.setProperty("fallbackdirectory","/media/catalog");

				getPageManager().getPageSettingsManager().saveSetting(catsettings);
			}

			Page dataold = getPageManager().getPage(dest.getPath() + "/WEB-INF/data/" + oldcatalogid);
			Page datadest = getPageManager().getPage( "/WEB-INF/data/" + inAppcatalogid);
			if( dataold.exists() )
			{
				getPageManager().removePage(datadest);
				getPageManager().copyPage(dataold, datadest);
			}
			//Save the app data
			Searcher searcher = getSearcherManager().getSearcher(inAppcatalogid, "app");
			Data site = (Data) searcher.searchByField("deploypath", "/" + inDestinationAppId);
			if (site == null)
			{
				site = searcher.createNewData();
			}
			// String frontendid = inReq.findValue("frontendid");
			// if( frontendid == null)
			// {
			// throw new OpenEditException("frontendid was null");
			// }

			if (inDestinationAppId != null)
			{
				site.setProperty("deploypath", "/" + inDestinationAppId);
			}
//			if (catalogid != null)
//			{
//
//				site.setProperty("appcatalogid", catalogid);
//			}

			String name = root.elementText("name");
			if (name != null)
			{
				site.setName(name);
			}

			searcher.saveData(site, null);

			Searcher catsearcher = getSearcherManager().getSearcher("system", "catalog");
			Data cat = (Data) catsearcher.searchById(inAppcatalogid);
			if (cat == null)
			{
				cat = catsearcher.createNewData();
				cat.setId(inAppcatalogid);
				catsearcher.saveData(cat, null);
			}
			MediaArchive archive  = (MediaArchive)getSearcherManager().getModuleManager().getBean(inAppcatalogid,"mediaArchive");
			archive.clearAll();
			//Reset mapping
			NodeManager nodemanager = (NodeManager)getSearcherManager().getModuleManager().getBean(inAppcatalogid, "nodeManager");
			nodemanager.reindexInternal(inAppcatalogid);
			//Reset lists
			//getSearcherManager().reloadLoadedSettings(inAppcatalogid);
			
		}
		catch (Exception ex)
		{
			throw new OpenEditException(ex);
		}

	}
}
