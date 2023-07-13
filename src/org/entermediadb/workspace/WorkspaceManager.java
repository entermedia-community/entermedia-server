package org.entermediadb.workspace;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

import org.dom4j.Attribute;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.search.AssetSearcher;
import org.entermediadb.asset.xmldb.CategorySearcher;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.PropertyDetailsArchive;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.modules.translations.LanguageMap;
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
import org.openedit.xml.ElementData;
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
		String pathbase = "/" + catalogid + "/fields/" + searchtype + ".xml";
		if( getPageManager().getPage(path).exists() || getPageManager().getPage(pathbase).exists() )
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
		String file = "/WEB-INF/data/" + catalogid + "/fields/" + tablename + ".xml";

		archive.savePropertyDetails(details, tablename, null, file);
		
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
		if( !appid.endsWith("mediadb"))
		{

			String mid = createModuleFallbacks(appid, module);
			if( !mid.equals("asset") )
			{
				String viewstemplate = "";
				/** DATABASE STUFF **/
				//is Entity?
				if (Boolean.parseBoolean(module.get("isentity"))) {
					String templateentities = "/" + catalogid + "/data/lists/view/entities.xml";
					Page pathentitiesbase = getPageManager().getPage("/" + catalogid + "/data/lists/view/" + module.getId() + ".xml");
					if( !pathentitiesbase.exists())
					{
						String pathentities = "/WEB-INF/data/" + catalogid + "/lists/view/" + module.getId() + ".xml";
						copyXml(catalogid, templateentities, pathentities, module);
					}
					viewstemplate = "/" + catalogid + "/data/views/" + module.getId() + "/";
					Page viewstemplatedefaults = getPageManager().getPage(viewstemplate);
					if (!viewstemplatedefaults.exists()) 
					{
						viewstemplate = "/" + catalogid + "/data/views/defaults/entities/";
					}
					//Copies viewusers, viewgroups and security stuff for this entity.
					Page destination = getPageManager().getPage("/WEB-INF/data/" + catalogid + "/fields/" + module.getId() + "/entitypermissions.xml");
					Page destinationbase = getPageManager().getPage("/" + catalogid + "/fields/" + module.getId() + "/entitypermissions.xml");
					if( !destination.exists() && !destinationbase.exists() )
					{
						String templatepermissionfields = "/" + catalogid + "/configuration/entitypermissiontemplate.xml";
						Page template= getPageManager().getPage(templatepermissionfields);
						getPageManager().copyPage(template, destination);
					}					
					//Add corresponding fields to Asset
					
					
					AssetSearcher searcher = (AssetSearcher) getSearcherManager().getSearcher(catalogid, "asset");
					CategorySearcher cats = (CategorySearcher) getSearcherManager().getSearcher(catalogid, "category");

					PropertyDetailsArchive propertyDetailsArchive = searcher.getPropertyDetailsArchive();

					PropertyDetail detail = searcher.getDetail(mid);
					PropertyDetail catdetail = cats.getDetail(mid);

					if(detail == null || catdetail == null) {

						detail = propertyDetailsArchive.createDetail(mid, mid );
						detail.setDeleted(false);
						Object name = module.getValue("name");
						detail.setDataType("list");
						if(name instanceof String){
							detail.setName((String) name);
						} 
						else if(name instanceof LanguageMap) {
							detail.setName((LanguageMap)name);
						}
						
						propertyDetailsArchive.savePropertyDetail(detail, "asset", null);
						propertyDetailsArchive.savePropertyDetail(detail, "category", null);

												
					}
				
					
					
				}else {
					String template = "/" + catalogid + "/data/lists/view/default.xml";
					String path = "/WEB-INF/data/" + catalogid + "/lists/view/" + module.getId() + ".xml";
					Page pathentitiesbase = getPageManager().getPage("/" + catalogid + "/data/lists/view/" + module.getId() + ".xml");
					if( !pathentitiesbase.exists())
					{
						copyXml(catalogid, template, path, module);		
					}
					viewstemplate = "/" + catalogid + "/data/views/defaults/";
				}
				
				
				Searcher views = getSearcherManager().getSearcher(catalogid, "view");
				Collection valuesdir = getPageManager().getChildrenPaths(viewstemplate, true );
				
				boolean copied = false;
				for (Iterator iterator = valuesdir.iterator(); iterator.hasNext();)
				{
					
					String copypath = (String) iterator.next();
					Page input = getPageManager().getPage(copypath);
					Page destpath = null;
					
					String pathfinal = "/WEB-INF/data/" + catalogid + "/views/" + module.getId() + "/";
					String pathfinalbase = "/" + catalogid + "/data/views/" + module.getId() + "/";
					
					if (input.getName().indexOf(module.getId()) != -1) {
						pathfinal = pathfinal + input.getName();
						pathfinalbase = pathfinalbase + input.getName();
					}
					else {
						pathfinal = pathfinal + module.getId()+ input.getName();
						pathfinalbase = pathfinalbase + module.getId()+ input.getName();
					}
					
					destpath = getPageManager().getPage( pathfinal );
					Page destpathbase = getPageManager().getPage( pathfinalbase );
					
					if (!destpath.exists() && !destpathbase.exists()) {
						getPageManager().copyPage(input, destpath);
						copied = true;
					}
					
				}
				if( copied )
				{
					views.reIndexAll();
				}
				String templte2 = "/" + catalogid + "/data/lists/settingsmenumodule/default.xml";
				String path2 = "/WEB-INF/data/" + catalogid + "/lists/settingsmenumodule/" + module.getId() + ".xml";
				if( !getPageManager().getPage(path2).exists())
				{
					copyXml(catalogid, templte2, path2, module);
					Searcher settingsmenumodule = getSearcherManager().getSearcher(catalogid, "settingsmenumodule");
					settingsmenumodule.reIndexAll();
				}
				String templte3 = "/" + catalogid + "/data/lists/settingsmodulepermissionsdefault.xml";
				String path3 = "/WEB-INF/data/" + catalogid + "/lists/settingsmodulepermissions" + module.getId() + ".xml";
				if( !getPageManager().getPage(path3).exists())
				{
					copyXml(catalogid, templte3, path3, module);
					getSearcherManager().removeFromCache(catalogid, "settingsmenumodule");
				}
			}
			// add settings menu
			createTable(catalogid, module.getId(), module.getId());
		}
		
		createMediaDbModule(catalogid,module);
		
	}

	public String createModuleFallbacks(String appid, Data module)
	{
		String mid = module.getId();
		String basepath = "default";
		Page home = getPageManager().getPage("/" + appid + "/views/modules/" + module.getId() + "/_site.xconf");
		
		//TODO: Can we remove this one day?
		
		Page settings = getPageManager().getPage("/" + appid + "/views/settings/modules/" + module.getId() + "/_site.xconf");
		PageSettings homesettings = home.getPageSettings();
		PageSettings modulesettings = settings.getPageSettings();
		if( mid.equals("asset") || mid.equals("library") || mid.equals("librarycollection") || mid.equals("category"))
		{
			basepath = mid;
			homesettings.removeProperty("fallbackdirectory");
			modulesettings.removeProperty("fallbackdirectory");
		}
		else
		{
			homesettings.setProperty("module", module.getId());
			PageProperty prop = new PageProperty("fallbackdirectory");
			prop.setValue("/" + appid + "/views/modules/" + basepath);
			homesettings.putProperty(prop);
	
			modulesettings.setProperty("module", module.getId());
			prop = new PageProperty("fallbackdirectory");
			prop.setValue("/" + appid + "/views/settings/modules/" + basepath);
			modulesettings.putProperty(prop);
		}		
		getPageManager().getPageSettingsManager().saveSetting(homesettings);
		getPageManager().getPageSettingsManager().saveSetting(modulesettings);
		return mid;
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
		lookup.put("modulename",inModule.getName("en"));
		
		Searcher sectionSearcher = getSearcherManager().getSearcher(inCatalogId, "docsection");
		Data section = (Data)sectionSearcher.searchById("module" + inModule.getId() );
		if( section == null )
		{
			section = sectionSearcher.createNewData();
			section.setId("module" + inModule.getId());
		}
//		Object name = section.getValue("name");
//		if( name == null)
//		{
			LanguageMap names = new LanguageMap();
			names.setText("en", inModule.getName("en"));
			section.setValue("name",names);
			sectionSearcher.saveData(section, null);
//		}
		Searcher endpointSearcher = getSearcherManager().getSearcher(inCatalogId, "endpoint");
		Collection templates = getSearcherManager().getList(inCatalogId, "endpointmoduletemplate");
		for (Iterator iterator = templates.iterator(); iterator.hasNext();)
		{
			Data row = (Data) iterator.next();
			Data endpoint = (Data)endpointSearcher.searchById(section.getId() + row.getId());
			if( endpoint == null )
			{
				endpoint = endpointSearcher.createNewData();
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
		}
		
		//Files
		String settingspath = "/" + mediadb + "/services/module/" + inModule.getId() + "/_site.xconf";
		//if (!getPageManager().getRepository().doesExist(settingspath))
		{
			Page home = getPageManager().getPage(settingspath);
			PageSettings homesettings = home.getPageSettings();
			homesettings.setProperty("module", inModule.getId());
			PageProperty prop = new PageProperty("fallbackdirectory");
			
			//This might be an existing fallback
			String parent = "/" + mediadb + "/services/module/default";
			PageSettings fallback = homesettings.getFallback();
			do
			{
				String path = fallback.getParentPath();
				if( getPageManager().getRepository().doesExist(path) && !path.endsWith("default"))
				{
					//Use this one
					parent = path;
					break;
				}
				fallback = fallback.getFallback();
			}
			while( fallback != null);
			prop.setValue(parent);
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

	public void scanModuleCustomizations(MediaArchive inMediaArchive, Collection inModules)
	{
		Collection skip = Arrays.asList(new String[] {"order","asset","librarycollection","library","category","modulesearch"});
		
		for (Iterator iterator = inModules.iterator(); iterator.hasNext();)
		{
			Data module = (Data) iterator.next();
			if( skip.contains(module.getId()) )
			{
				continue;
			}
			Data customization = inMediaArchive.query("customization").exact("targetid",module.getId()).searchOne();
			if( customization == null)
			{
				//Make em
				customization = inMediaArchive.getSearcher("customization").createNewData();
				customization.setValue("targetid",module.getId());
				customization.setName(module.getName("en"));
				customization.setValue("customizationtype","module");
				customization.setValue("dateupdated",new Date() );
				//This will be used to export and import a bunch of xml files?
				inMediaArchive.saveData("customization",customization);
			}
		}
	}


	public void scanHtmlCustomizations(MediaArchive inMediaArchive, Collection inExisting)
	{
		
	}

	public void exportCustomizations(String inCatalogId, String[] inIds, OutputStream inStream)
	{
		PageZipUtil pageZipUtil = new PageZipUtil(getPageManager());
		ZipOutputStream finalZip = new ZipOutputStream(inStream);

		MediaArchive archive  = (MediaArchive)getSearcherManager().getModuleManager().getBean(inCatalogId,"mediaArchive");

		for (int i = 0; i < inIds.length; i++)
		{
			Data customization = archive.getData("customization", inIds[i]);
			//TODO: Make xml files for each config
			Element root = DocumentHelper.createElement("customization");
			root.attributeValue("targetid",customization.get("targetid"));
			root.attributeValue("customizationtype",customization.get("customizationtype"));
			root.addElement("name").setText(customization.getName("en"));
			try
			{
				if( "module".equals(customization.get("customizationtype")) )
				{
					Data module = archive.getCachedData("module", customization.get("targetid"));
					
					String path = "/WEB-INF/data/" + inCatalogId + "/fields/" + customization.get("targetid") + ".xml";
					if( getPageManager().getRepository().doesExist(path))
					{
						pageZipUtil.zip(path, finalZip);
						path = "/WEB-INF/data/" + inCatalogId + "/fields/" + customization.get("targetid") + "/";
						if( getPageManager().getRepository().doesExist(path))
						{
							pageZipUtil.zip(path, finalZip);
						}
					}
					//Views
					path = "/WEB-INF/data/" + inCatalogId + "/lists/view/" + customization.get("targetid") + ".xml";
					if( getPageManager().getRepository().doesExist(path))
					{
						pageZipUtil.zip(path, finalZip);
					}
					path = "/WEB-INF/data/" + inCatalogId + "/views/" + customization.get("targetid") + "/";
					if( getPageManager().getRepository().doesExist(path))
					{
						pageZipUtil.zip(path, finalZip);
					}
					//Pull in the module data info
					Element xml = saveDataToXml(module);
					root.add(xml);
				}
				String name = customization.getName("en") ;
				pageZipUtil.addTozip(root.asXML(), name + ".xml", finalZip);
				
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try
		{
			finalZip.close();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private Element saveDataToXml(Data inModule)
	{
		ElementData data = new ElementData();
		data.setId(inModule.getId());
		data.setName(inModule.getName());
		data.setSourcePath(inModule.getSourcePath());
		for (Iterator iterator = inModule.keySet().iterator(); iterator.hasNext();)
		{
			String key	= (String) iterator.next();
			data.setValue(key, inModule.getValue(key));
		}
		Element thedata = data.getElement();//.asXML();
		return thedata;
	}
}
