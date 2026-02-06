package org.entermediadb.workspace;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Attribute;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Requests;
import org.entermediadb.ai.Schema;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.elasticsearch.ElasticNodeManager;
import org.entermediadb.elasticsearch.SearchHitData;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.PropertyDetailsArchive;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.HitTracker;
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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;

public class WorkspaceManager
{
	private static final Log log = LogFactory.getLog(WorkspaceManager.class);

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

	public MediaArchive getMediaArchive(String inCatalogId) {
		MediaArchive archive  = (MediaArchive)getSearcherManager().getModuleManager().getBean(inCatalogId,"mediaArchive");
		return archive;
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

	public String createTable(String catalogid, String tablename, String inPrefix) 
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
		//Create a new one
		PropertyDetails details = new PropertyDetails(archive,searchtype);
		//details.setDetails(defaultdetails.getDetails()); //Entities have everything in the folder
		if( details.getBeanName() == null )
		{
			details.setBeanName("dataSearcher");
		}
		String file = "/WEB-INF/data/" + catalogid + "/fields/" + tablename + ".xml";

		archive.savePropertyDetails(details, tablename, null, file);
		
		
		//Now using ElasticViewSearcher to merge views
		/*
		for (Iterator iterator = details.iterator(); iterator.hasNext();) {
			PropertyDetail detail = (PropertyDetail) iterator.next();
			archive.savePropertyDetail(detail, searchtype, null);

			
		}
		 */

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
	public void saveModule(String catalogid, String appid, Data module) 
	{
		saveModule(catalogid,appid,module,false);
	}
	public void saveModule(String catalogid, String appid, Data module, boolean verify) 
	{
		if(module == null )
		{
			throw new OpenEditException("Invalid module id");
		}
		String mid = createModuleFallbacks(appid, module);
		if( !mid.equals("asset") )
		{
			String viewstemplate = "";
			/** DATABASE STUFF **/
			//is Entity?
			if (Boolean.parseBoolean(module.get("isentity"))) {
				String templateentities = "/" + catalogid + "/data/lists/view/entities.xml";
//					Page pathentitiesbase = getPageManager().getPage("/" + catalogid + "/data/lists/view/" + module.getId() + ".xml");
//					if( !pathentitiesbase.exists())
//					{
//						String pathentities = "/WEB-INF/data/" + catalogid + "/lists/view/" + module.getId() + ".xml";
//						copyXml(catalogid, templateentities, pathentities, module);
//					}
//					viewstemplate = "/" + catalogid + "/data/views/" + module.getId() + "/";
//					Page viewstemplatedefaults = getPageManager().getPage(viewstemplate);
//					if (!viewstemplatedefaults.exists()) 
//					{
//						viewstemplate = "/" + catalogid + "/data/views/defaults/";
//					}
				Page destinationbase = getPageManager().getPage("/" + catalogid + "/fields/" + module.getId() + "/baseentity.xml");
				//if( !destinationbase.exists() )
				//{
					String templatepermissionfields = "/" + catalogid + "/configuration/baseentitytemplate.xml";
					Page template= getPageManager().getPage(templatepermissionfields);
					if(template.exists()) {
						Page destination = getPageManager().getPage("/WEB-INF/data/" + catalogid + "/fields/" + module.getId() + "/baseentity.xml");
						getPageManager().copyPage(template, destination); //Always update these
					}
				//}					
			}
			String templte2 = "/" + catalogid + "/data/lists/settingsmenumodule/default.xml";
			String path2 = "/WEB-INF/data/" + catalogid + "/lists/settingsmenumodule/" + module.getId() + ".xml";
			if( !getPageManager().getPage(path2).exists())
			{
				copyXml(catalogid, templte2, path2, module);
				Searcher settingsmenumodule = getSearcherManager().getSearcher(catalogid, "settingsmenumodule");
				settingsmenumodule.reIndexAll();
			}
			else if( verify )
			{
				//Merge
				
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
		getSearcherManager().getPropertyDetailsArchive(catalogid).clearCache();
		getMediaArchive(catalogid).getPermissionManager().queuePermissionCheck((MultiValued)module);
		getMediaArchive(catalogid).saveData("module", module);
	}

	
	
	public String createModuleFallbacks(String appid, Data module)
	{
		return createModuleFallbacks(appid, module, false);
	}
	
	public String createModuleFallbacks(String appid, Data module, boolean force)
	{
		String mid = module.getId();
		Page modulehome = getPageManager().getPage("/" + appid + "/views/modules/" + module.getId() + "/_site.xconf");
		
		//TODO: Can we remove this one day?
		
		Page settings = getPageManager().getPage("/" + appid + "/views/settings/modules/" + module.getId() + "/_site.xconf");
		PageSettings homesettings = modulehome.getPageSettings();
		PageSettings modulesettings = settings.getPageSettings();

		Page parentfallback = getPageManager().getPage(modulehome.getPageSettings().getFallback().getPath());

		
		if(parentfallback.exists() && parentfallback.getDirectoryName().equals(module.getId()) && !force) //mid.equals("asset") || mid.equals("library") || mid.equals("librarycollection") || mid.equals("category"))
		{
			homesettings.removeProperty("fallbackdirectory");
			modulesettings.removeProperty("fallbackdirectory");
		}
		else
		{
			homesettings.setProperty("module", module.getId());
			PageProperty prop = new PageProperty("fallbackdirectory");
			prop.setValue("../default");
			homesettings.putProperty(prop);
	
			modulesettings.setProperty("module", module.getId());
			prop = new PageProperty("fallbackdirectory");
			prop.setValue("../default");
			modulesettings.putProperty(prop);
		}		
		getPageManager().getPageSettingsManager().saveSetting(homesettings);
		getPageManager().getPageSettingsManager().saveSetting(modulesettings);
		return mid;
	}

	public void createMediaDbModule(String inCatalogId, Data inModule)
	{
		//Data setup
		Data setting = getSearcherManager().getData(inCatalogId, "catalogsettings", "mediadbappid");
		String mediadb = setting.get("value");
		if( mediadb == null)
		{
			throw new OpenEditException("Must set the mediadbappid id");
		}
		
		Replacer replacer = new Replacer();
		Map lookup = new HashMap();
		lookup.put("mediadbappid",mediadb);
		lookup.put("moduleid",inModule.getId());
		lookup.put("module",inModule);
		lookup.put("modulename",inModule.getName("en"));
		
		Searcher sectionSearcher = getSearcherManager().getSearcher(inCatalogId, "docsection");
		Data section = (Data)sectionSearcher.searchById("module" + inModule.getId() );

		LanguageMap names = new LanguageMap();
		names.setText("en", inModule.getName("en"));

		if( section == null )
		{
			section = sectionSearcher.createNewData();
			section.setId("module" + inModule.getId());
			section.setValue("name",names);
			sectionSearcher.saveData(section, null);
			
		}	
		if(	!inModule.getName().equals( section.getName() ) )
		{
			section.setValue("name",names);
			sectionSearcher.saveData(section, null);
		}
		Searcher endpointSearcher = getSearcherManager().getSearcher(inCatalogId, "endpoint");
		Collection templates = getSearcherManager().getList(inCatalogId, "endpointmoduletemplate");
		
		//TODO: Use a new smart merge Searcher
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

	public void createMediaDbAiFunctionEndPoints(String inCatalogId)
	{
		Searcher endpointSearcher = getSearcherManager().getSearcher(inCatalogId, "endpoint");
		Searcher functionsSearcher = getSearcherManager().getSearcher(inCatalogId, "aifunction");
		
		MediaArchive archive = getMediaArchive(inCatalogId);
		Data section = archive.getCachedData("docsection", "aifunctions");
		if( section == null )
		{
			section = archive.getSearcher("docsection").createNewData();
			section.setId("aifunctions");
			section.setName("AI Functions");
			archive.saveData("docsection",section);
		}
		
		HitTracker<Data> moduleids =  archive.getList("module");
		Data entity = archive.query("modulesearch")
				.put("searchtypes", moduleids.collectValues("id"))
				.exact("entityembeddingstatus", "embedded")
				.searchOne();
		Data entitymodule = archive.getCachedData("module",entity.get("entitysourcetype"));
		
		JSONObject request = new JSONObject();
		
		request.put("channel", "testchannel");
		request.put("message", "What is this all about?");
		
		String siteid = PathUtilities.extractDirectoryPath(inCatalogId);
		request.put("chatapplicationid",siteid + "/find");
		request.put("entityname",entity.getName());
		request.put("entityid",entity.getId());
		request.put("entitymoduleid", entitymodule.getId());
		
		Collection tosave = new ArrayList();
		String mediadbhome = "/" + archive.getCatalogSettingValue("mediadbappid");

		Collection all = functionsSearcher.query().all().search();
		for (Iterator iterator = all.iterator(); iterator.hasNext();)
		{
			Data function = (Data) iterator.next();
			Data endpoint = endpointSearcher.createNewData();
			endpoint.setName(function.getName());
			endpoint.setId(function.getId());
			endpoint.setValue("url", mediadbhome + "/services/ai/" +function.getId());
			
			if( function.get("samplemesage") != null)
			{
				request.put("message", function.get("samplemesage"));
			}
			
			endpoint.setValue( "samplerequest", request.toJSONString() );
			endpoint.setValue( "httpmethod","POST");
			endpoint.setProperty( "docsection",section.getId() );
			tosave.add(endpoint);

		}
		endpointSearcher.saveAllData(tosave, null);
		/*
		  <endpoint id="search" name="Search for ${modulename}" url="/${mediadbappid}/services/module/${moduleid}/search" httpmethod="POST"> 
		    <samplerequest>
		    	<![CDATA[{
		    	    "page": "1", 
		    	    "hitsperpage":"20",
		            "query": 
		            {
		            	"terms":[{
			            	"field": "id",
							"operator": "matches",
							"value": "*"
						}]
			         }
			        } 
			      ]]></samplerequest>
		  </endpoint>  
		*/
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
		Collection skip = Arrays.asList(new String[] {"order","asset","librarycollection","library","category","modulesearch","faceprofilegroup", "group", "user", "settingsgroup" });

		Set tables = new HashSet();

		for (Iterator iterator = inModules.iterator(); iterator.hasNext();)
		{
			Data module = (Data) iterator.next();
			if( skip.contains(module.getId()) )
			{
				continue;
			}
			MultiValued customization = (MultiValued)inMediaArchive.query("customization").exact("targetid",module.getId()).searchOne();
			if( customization == null)
			{
				//Make em
				customization = (MultiValued)inMediaArchive.getSearcher("customization").createNewData();
				customization.setValue("targetid",module.getId());
				customization.setName(module.getName("en"));
				customization.setValue("customizationtype","module");
				customization.setValue("dateupdated",new Date() );
				//This will be used to export and import a bunch of xml files?
				inMediaArchive.saveData("customization",customization);
			}
			tables.add(module.getId());
			PropertyDetails details = inMediaArchive.getPropertyDetailsArchive().getPropertyDetails(module.getId());
			for (Iterator iterator2 = details.iterator(); iterator2.hasNext();)
			{
				PropertyDetail detail = (PropertyDetail) iterator2.next();
				if( detail.isList())
				{
					if( !tables.contains(detail.getListId()) && !skip.contains(detail.getListId()) )
					{
						tables.add(detail.getListId());
						customization = (MultiValued)inMediaArchive.query("customization").exact("targetid",detail.getListId()).searchOne();
						if( customization == null)
						{
							//Make em
							customization = (MultiValued)inMediaArchive.getSearcher("customization").createNewData();
							customization.setValue("targetid",detail.getListId());
							customization.addValue("moduleids",module.getId());
							
							customization.setName(detail.getName("en"));
							customization.setValue("customizationtype","table");
							customization.setValue("dateupdated",new Date() );
							//This will be used to export and import a bunch of xml files?
							inMediaArchive.saveData("customization",customization);
						}
					}
				}
				
			}
		}
		Collection menu = inMediaArchive.query("appsection").all().search();
		if( !menu.isEmpty() )
		{
			Data customization = inMediaArchive.query("customization").exact("targetid","appsection").searchOne();
			if( customization == null)
			{
				//Make em
				customization = inMediaArchive.getSearcher("customization").createNewData();
				customization.setValue("targetid","appsection");
				customization.setName("App Section Menu");
				customization.setValue("customizationtype","table");
				customization.setValue("dateupdated",new Date() );
				//This will be used to export and import a bunch of xml files?
				inMediaArchive.saveData("customization",customization);
			}
		}

		
	}


	public void scanHtmlCustomizations(MediaArchive inMediaArchive, Collection inExisting)
	{
		
	}

	public void exportCustomizations(String inCatalogId, String[] inIds, OutputStream inStream) throws Exception
	{
		PageZipUtil pageZipUtil = new PageZipUtil(getPageManager());
		ZipOutputStream finalZip = new ZipOutputStream(inStream);

		MediaArchive archive  = (MediaArchive)getSearcherManager().getModuleManager().getBean(inCatalogId,"mediaArchive");

		for (int i = 0; i < inIds.length; i++)
		{
			Data customization = archive.getData("customization", inIds[i]);
			//TODO: Make xml files for each config
			Element root = DocumentHelper.createElement("customization");
			String searchtype = customization.get("targetid");
			root.addAttribute("targetid",searchtype);
			root.addAttribute("customizationtype",customization.get("customizationtype"));
			root.addElement("name").setText(customization.getName("en"));
				if( "module".equals(customization.get("customizationtype")) )
				{
					Data module = archive.getCachedData("module", searchtype);
					
					String path = "/WEB-INF/data/" + inCatalogId + "/fields/" + searchtype + ".xml";
					if( getPageManager().getRepository().doesExist(path))
					{
						pageZipUtil.zip(path, finalZip);
						path = "/WEB-INF/data/" + inCatalogId + "/fields/" + searchtype + "/";
						if( getPageManager().getRepository().doesExist(path))
						{
							pageZipUtil.zip(path, finalZip);
						}
					}
					
					//Views
					path = "/WEB-INF/data/" + inCatalogId + "/lists/view/" + searchtype + ".xml";
					if( getPageManager().getRepository().doesExist(path))
					{
						pageZipUtil.zip(path, finalZip);
					}
					path = "/WEB-INF/data/" + inCatalogId + "/views/" + searchtype + "/";
					if( getPageManager().getRepository().doesExist(path))
					{
						pageZipUtil.zip(path, finalZip);
					}
					//Pull in the module data info
					Element xml = saveDataToXml(module);
					root.add(xml);
				}
				else if( "table".equals(customization.get("customizationtype")) )
				{
					String path = "/WEB-INF/data/" + inCatalogId + "/fields/" + searchtype+ ".xml";
					if( getPageManager().getRepository().doesExist(path))
					{
						pageZipUtil.zip(path, finalZip);
						path = "/WEB-INF/data/" + inCatalogId + "/fields/" + searchtype + "/";
						if( getPageManager().getRepository().doesExist(path))
						{
							pageZipUtil.zip(path, finalZip);
						}
					}
					exportData(archive,searchtype,finalZip);
					String xml = "/WEB-INF/data/" + inCatalogId + "/lists/" + searchtype+ ".xml";
					if( getPageManager().getRepository().doesExist(xml))
					{
						pageZipUtil.zip(xml, finalZip);
					}
				}
				
				pageZipUtil.addTozip(root.asXML(), "/customizations/" + searchtype + ".xml", finalZip);
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

	private void exportData(MediaArchive mediaarchive, String inSearchtype, ZipOutputStream inFinalZip) throws Exception
	{
		Searcher searcher = mediaarchive.getSearcher(inSearchtype);
		PropertyDetails details = searcher.getPropertyDetails();
		HitTracker hits = searcher.getAllHits();
		hits.enableBulkOperations();
		if(hits.size() > 0){
			ZipEntry ze = new ZipEntry("/customizations/" + inSearchtype + ".json");

			inFinalZip.putNextEntry(ze);
			IOUtils.write("{ \"" + inSearchtype + "\": [", inFinalZip, "UTF-8");
			int size = hits.size();
			int count = 0;
			for (Iterator iterator = hits.iterator(); iterator.hasNext();)
			{
				Data it = (Data) iterator.next();
				count++;
				SearchHitData hit = (SearchHitData)it;
				IOUtils.write(hit.toJsonString(), inFinalZip, "UTF-8");
				if(size != count) {
				IOUtils.write(",", inFinalZip, "UTF-8");
				}
			}
			IOUtils.write("]}", inFinalZip, "UTF-8");
			
			inFinalZip.flush();
			inFinalZip.closeEntry();
		}
	}
	
	public void importData(MediaArchive mediaarchive, String searchtype , Page inDataFile) throws Exception
	{
		ElasticNodeManager manager = (ElasticNodeManager)mediaarchive.getNodeManager();
		
		BulkProcessor processor = manager.getBulkProcessor();

		String indexid = manager.toId(mediaarchive.getCatalogId());

		
		try{

			MappingJsonFactory f = new MappingJsonFactory();
			JsonParser jp = f.createParser(inDataFile.getReader());

			JsonToken current;

			current = jp.nextToken();
			if (current != JsonToken.START_OBJECT) {
				System.out.println("Error: root should be object: quiting.");
				return;
			}

			while (jp.nextToken() != JsonToken.END_OBJECT) {
				String fieldName = jp.getCurrentName();
				// move from field name to field value
				current = jp.nextToken();
				if (fieldName.equals(searchtype)) {
					if (current == JsonToken.START_ARRAY) {
						// For each of the records in the array
						while (jp.nextToken() != JsonToken.END_ARRAY) {
							// read the record into a tree model,
							// this moves the parsing position to the end of it
							JsonNode node = jp.readValueAsTree();
							IndexRequest req = Requests.indexRequest(indexid).type(searchtype);
							JsonNode source = node.get("_source");
							if (source == null)
							{
								source = node;
							}
							String json  = source.toString();
							
							//log.info("JSON: "+json);
							req.source(json);
							JsonNode id = node.get("_id");
							if( id == null) {
								id = node.get("id");
							}
							if( id == null)
							{
								log.info("No ID found " + searchtype + " node:" + node);
							}
							else
							{
								req.id(id.asText());
							}	
							processor.add(req);

						}
					} else {
						System.out.println("Error: records should be an array: skipping.");
						jp.skipChildren();
					}
				} else {
					System.out.println("Unprocessed property: " + fieldName);
					jp.skipChildren();
				}
			}
		}
		finally
		{

			manager.flushBulk();

				//This is in memory only flush
				//RefreshResponse actionGet = getClient().admin().indices().prepareRefresh(catid).execute().actionGet();
		}
	}
	protected XmlUtil fieldXmlUtil;
	public XmlUtil getXmlUtil()
	{
		if (fieldXmlUtil == null)
		{
			fieldXmlUtil = new XmlUtil();
		}
		return fieldXmlUtil;
	}



	public void importCustomizations(MediaArchive mediaArchive, List inFiles) throws Exception
	{
		//Unzip the upload

		
		for (Iterator iterator = inFiles.iterator(); iterator.hasNext();)
		{
			Page file = (Page) iterator.next();
			if( file.getPath().contains("/customizations/") && file.getName().endsWith("xml"))
			{
				//Import customization
				Element element = getXmlUtil().getXml(file.getReader(), "utf-8");
				
				String type = element.attributeValue("customizationtype");
				if( "module".equals(type) )
				{
					ElementData data = new ElementData(element.element("element"));
					String targetid = data.get("id");
					Data module = mediaArchive.getCachedData("module", targetid);
					if( module == null)
					{
						module = data;
					}
					mediaArchive.saveData("module", module);
				}
				continue;
			}
			if( file.getPath().contains("/customizations/") && file.getName().endsWith("json"))
			{
				String searchtype = file.getPageName();
				importData(mediaArchive,searchtype,file);
				continue;
			}
			int fieldindex = file.getPath().indexOf("/fields/"); 
			if(fieldindex > -1)
			{
				//Copy all the views etc files
				String path = "/WEB-INF/data/" + mediaArchive.getCatalogId();
				path = path + file.getPath().substring(fieldindex);
				Page target = getPageManager().getPage(path);
				getPageManager().copyPage(file, target);
				continue;
			}
			int listindex = file.getPath().indexOf("/lists/"); 
			if(listindex > -1)
			{
				//Copy all the xml files
				String path = "/WEB-INF/data/" + mediaArchive.getCatalogId();
				path = path + file.getPath().substring(listindex);
				Page target = getPageManager().getPage(path);
				getPageManager().copyPage(file, target);
				continue;
			}
//			if(file.getPath().contains("/view/"))
//			{
//				//Views
//				String path = "/WEB-INF/data/" + mediaArchive.getCatalogId() + "/lists/view/";
//				Page target = getPageManager().getPage(path);
//				getPageManager().copyPage(file, target);
//			}
			if(file.getPath().contains("/views/"))
			{
				String path = "/WEB-INF/data/" + mediaArchive.getCatalogId() + "/views/" + file.getDirectoryName() + "/" + file.getName();
				Page target = getPageManager().getPage(path);
				getPageManager().copyPage(file, target);
			}
			
		}
		//Reindex
		//mediaArchive.reindexAll();
		Collection tables = getSearcherManager().reloadLoadedSettings(mediaArchive.getCatalogId());
		//Scan changes?
		mediaArchive.clearAll();
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
