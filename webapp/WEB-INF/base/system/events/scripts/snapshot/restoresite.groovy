package snapshot;

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

import org.dom4j.Attribute
import org.dom4j.Element
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.util.CSVReader
import org.entermediadb.asset.util.ImportFile
import org.entermediadb.asset.util.Row
import org.entermediadb.elasticsearch.ElasticNodeManager
import org.entermediadb.workspace.WorkspaceManager
import org.openedit.Data
import org.openedit.OpenEditException
import org.openedit.data.PropertyDetail
import org.openedit.data.PropertyDetails
import org.openedit.data.PropertyDetailsArchive
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.hittracker.HitTracker
import org.openedit.modules.translations.LanguageMap
import org.openedit.page.Page
import org.openedit.page.PageSettings
import org.openedit.page.manage.PageManager
import org.openedit.util.DateStorageUtil
import org.openedit.util.FileUtils
import org.openedit.util.PathUtilities
import org.openedit.util.XmlUtil

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.MappingJsonFactory



public void init() {
	log.info("Initializing restore");
	SearcherManager searcherManager = context.getPageValue("searcherManager");
	Searcher snapshotsearcher = searcherManager.getSearcher("system", "sitesnapshot");
	HitTracker restores = snapshotsearcher.query().match("snapshotstatus","pendingrestore").search();
	if( restores.isEmpty() ) {
		log.info("No pending snapshotstatus  = pendingrestore");
		return;
	}
	//Link files in the FileManager. Keep restores in data/system
	for(Data snapshot:restores)
	{
		snapshot.setValue("snapshotstatus", "restoring"); //Like a lock
		snapshotsearcher.saveData(snapshot);
		Searcher sitesearcher = searcherManager.getSearcher("system", "site");
		Data site = sitesearcher.query().match("id", snapshot.get("site")).searchOne();

		String catalogid =  site.get("catalogid");
		MediaArchive mediaarchive = (MediaArchive)moduleManager.getBean(catalogid,"mediaArchive");

		snapshotsearcher.saveData(snapshot);
		try
		{
			boolean configonly = Boolean.valueOf( snapshot.getValue("configonly") );
			log.info("restoring: " + site.get("rootpath") + " config="  + configonly);
			restore(mediaarchive, site,snapshot,configonly);
			snapshot.setValue("snapshotstatus", "complete");
		}
		catch( Exception ex)
		{
			log.error("Could not restore",ex);
			snapshot.setValue("snapshotstatus", "error");
		}
		finally
		{
			mediaarchive.getSearcherManager().resetAlternative();
		}
		snapshotsearcher.saveData(snapshot);
		mediaarchive.getSearcherManager().clear();

		//Fix apps
		//Fix app paths
		//deploypath
		Searcher appsearcher = mediaarchive.getSearcher("app");
		//Loop over apps
		appsearcher.deleteAll(null);
		Collection paths = mediaarchive.getPageManager().getChildrenPaths(site.get("rootpath"));
		for(String path:paths)
		{
			String name = PathUtilities.extractFileName(path);
			PageSettings settings = mediaarchive.getPageManager().getPageSettingsManager().getPageSettings(path + "/_site.xconf");
			if( settings.exists() && name != "catalog" )
			{
				Data newapp = appsearcher.createNewData();
				newapp.setName(name);
				newapp.setValue("deploypath", path);
				appsearcher.saveData(newapp);
				log.info("Fixed app " + path);

				if( name == "emshare")
				{
					String appid = name;

					Collection all = mediaarchive.getList("module");
					WorkspaceManager manager = mediaarchive.getBean("workspaceManager");
					for (Iterator iterator = all.iterator(); iterator.hasNext();)
					{
						Data module = (Data) iterator.next();
						manager.saveModule(catalogid, appid, module);
					}
				}
			}
		}


		mediaarchive.getCategorySearcher().reIndexAll();
	}


}

public void restore(MediaArchive mediaarchive, Data site, Data inSnap, boolean configonly)
{
	String folder = inSnap.get("folder");

	String catalogid = mediaarchive.getCatalogId();

	ElasticNodeManager nodeManager = mediaarchive.getNodeManager();
	Date date = new Date();
	String tempindex =  nodeManager.toId(mediaarchive.getCatalogId().replaceAll("_", "") +  date.getTime());
	if( !configonly )
	{
		nodeManager.prepareIndex(tempindex);
	}
	String rootfolder = "/WEB-INF/data/exports/" + mediaarchive.getCatalogId() + "/" + folder;

	Collection files = mediaarchive.getPageManager().getChildrenPaths(rootfolder);
	if( files.isEmpty() )
	{
		throw new OpenEditException("No files in " + rootfolder);
	}

	SearcherManager searcherManager = context.getPageValue("searcherManager");

	Page lists = mediaarchive.getPageManager().getPage(rootfolder + "/lists/");

	if(lists.exists()) {
		Page target = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/lists/");
		archiveFolder(mediaarchive.getPageManager(), target, tempindex);
		mediaarchive.getPageManager().copyPage(lists, target);
	}

	Page views = mediaarchive.getPageManager().getPage(rootfolder + "/views/");
	if(views.exists()) {
		Page target = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/views/");
		archiveFolder(mediaarchive.getPageManager(), target, tempindex);
		mediaarchive.getPageManager().copyPage(views, target);
	}

	Page sitefolder = mediaarchive.getPageManager().getPage(rootfolder + "/site");
	if( sitefolder.exists() )
	{
		Page target = mediaarchive.getPageManager().getPage(site.get("rootpath"));
		archiveFolder(mediaarchive.getPageManager(), target, tempindex);
		mediaarchive.getPageManager().copyPage(sitefolder, target);

		//TODO: Go fix the catalogid's and applicationids
		fixXconfs(mediaarchive.getPageManager(),target,catalogid);
	}
	else
	{
		log.info(" site not included " + sitefolder.getPath());
	}

	Page orig = mediaarchive.getPageManager().getPage(rootfolder + "/originals");
	if( orig.exists() )
	{
		Page target = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/originals/");
		mediaarchive.getPageManager().copyPage(orig, target);
	}

	Page gen = mediaarchive.getPageManager().getPage(rootfolder + "/generated");
	if( gen.exists() )
	{
		Page target = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/generated/");
		mediaarchive.getPageManager().copyPage(gen, target);
	}

	mediaarchive.getPageManager().clearCache();
	PropertyDetailsArchive pdarchive = mediaarchive.getPropertyDetailsArchive();
	pdarchive.clearCache();

	/*
	 Page categories = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/dataexport/category.csv");
	 if(categories.exists()){
	 populateData(categories);
	 }
	 */

	Page fields = mediaarchive.getPageManager().getPage(rootfolder + "/fields/");
	if(fields.exists()) {
		Page target = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/fields/");
		archiveFolder(mediaarchive.getPageManager(), target, tempindex);
		mediaarchive.getPageManager().copyPage(fields, target);
	}
	pdarchive.clearCache();

	if( configonly )
	{
		//Reindex all lists tables?
		//Page target = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/lists/");
	}
	else
	{
		List ordereredtypes = new ArrayList();
		ordereredtypes.add("category");

		List childrennames = pdarchive.findChildTablesNames();
		List searchtypes = pdarchive.getPageManager().getChildrenPaths(rootfolder + "/" );
		searchtypes.each {
			if( it.endsWith(".csv")) {
				String searchtype = PathUtilities.extractPageName(it);
				if(!childrennames.contains(searchtype)) {
					ordereredtypes.add(searchtype);
				}
			}
		}
		ordereredtypes.addAll(childrennames);
		ordereredtypes.removeAll("propertydetail");
		ordereredtypes.removeAll("lock");
		ordereredtypes.removeAll("category");
		ordereredtypes.removeAll("user");
		//ordereredtypes.removeAll("userprofile");
		ordereredtypes.removeAll("group");
		ordereredtypes.add(0,"category");


		boolean deleteold = true;
		for( String type in ordereredtypes ) {
			Page upload = mediaarchive.getPageManager().getPage(rootfolder + "/" + type + ".csv");
			try{
				if( upload.exists() )
				{
					mediaarchive.clearCaches();
					importCsv(site, mediaarchive,type,upload, tempindex);
				}
			} catch (Exception e) {
				deleteold=false;

				log.error("Exception thrown importing upload: ${upload} ", e );
				break;
			}
		}
		if(deleteold)
		{
			importPermissions(mediaarchive,rootfolder, tempindex);
			nodeManager.loadIndex(mediaarchive.getCatalogId(), tempindex, deleteold);
		}
		else
		{
			log.info("Import canceled due to CSV import error");
		}

		List jsonfiles = pdarchive.getPageManager().getChildrenPaths(rootfolder + "/json/" );

		orderedtypes = new ArrayList();
		jsonfiles.each {
			if( it.endsWith(".zip")) {
				String searchtype = PathUtilities.extractPageName(it);
				if(!childrennames.contains(searchtype)) {
					ordereredtypes.add(searchtype);
				}
			}
		}
		ordereredtypes.addAll(childrennames);
		ordereredtypes.removeAll("propertydetail");
		ordereredtypes.removeAll("lock");
		ordereredtypes.removeAll("category");
		ordereredtypes.removeAll("user");
		//ordereredtypes.removeAll("userprofile");
		ordereredtypes.removeAll("group");
		ordereredtypes.add(0,"category");


		for( String type in ordereredtypes ) {
			Page upload = mediaarchive.getPageManager().getPage(rootfolder + "/json/" + type + ".zip");
			try{
				if( upload.exists() ) {
					importJson(site, mediaarchive,type,upload, tempindex);
				}
			} catch (Exception e) {
				deleteold=false;

				log.error("Exception thrown importing upload: ${upload} ", e );
				break;
			}
		}



	}
}



public void importJSON() {







}




public void fixXconfs(PageManager pageManager, Page site,String catalogid)
{
	PageSettings settings = pageManager.getPageSettingsManager().getPageSettings(site.getPath() + "/_site.xconf");
	if( settings.exists() )
	{
		settings.setProperty("catalogid", catalogid);
		String appid = PathUtilities.extractPageName(site.getPath());
		settings.setProperty("applicationid", appid);
		pageManager.getPageSettingsManager().saveSetting(settings);
	}
	//Loop over apps
	Collection paths = pageManager.getChildrenPaths(site.getPath());
	for(String path:paths)
	{
		settings = pageManager.getPageSettingsManager().getPageSettings(path + "/_site.xconf");
		if( settings.exists() )
		{
			settings.setProperty("catalogid", catalogid);
			String appid = site.getPath().substring(1) + "/" + PathUtilities.extractPageName(path);
			settings.setProperty("applicationid", appid);
			pageManager.getPageSettingsManager().saveSetting(settings);
		}
	}
	pageManager.clearCache();

}

public void archiveFolder(PageManager inManager, Page inPage, String inIndex)
{
	if( inPage.exists() && "false" != inPage.get("cleanonimport"))
	{
		Page trash = inManager.getPage("/WEB-INF/trash/" + inIndex + inPage.getPath() );
		inManager.movePage(inPage, trash);
	}
}

public void importPermissions(MediaArchive mediaarchive, String rootfolder, String tempindex) {
	Searcher sg = mediaarchive.getSearcher("settingsgroup");
	sg.setAlternativeIndex(tempindex);
	if( !sg.putMappings() ) {
		throw new OpenEditException("Could not import permissions ");
	}
	Page upload = mediaarchive.getPageManager().getPage(rootfolder + "/lists/settingsgroup.xml");
	if( upload.exists() ) {
		XmlUtil util = new XmlUtil();
		Element root = util.getXml(upload.getReader(),"utf-8");
		for(Iterator iterator = root.elementIterator(); iterator.hasNext();) {
			Element row = iterator.next();
			String permsstring = row.attributeValue("permissions");
			if( permsstring != null) //upgrade?
			{
				continue;
			}
			Set perms = new HashSet();
			for(Iterator iterator2 = row.attributes().iterator(); iterator2.hasNext();) {
				Attribute attr = iterator2.next();
				if( Boolean.valueOf(attr.getValue() ) ) {
					perms.add(attr.getQualifiedName() );
				}
			}
			String id = row.attributeValue("id");
			Data existing = sg.searchById(id);
			if( existing == null) {
				existing = sg.createNewData();
			}
			existing.setId(id);
			existing.setName(row.attributeValue("name"));
			existing.setValue("permissions", perms);
			sg.saveData(existing, null);
		}
	}
	sg.reIndexAll();
	sg.setAlternativeIndex(null);
	log.info("Previous application and catalog backed up in /WEB-INF/trash/" + tempindex + "/");
}


public void importCsv(Data site, MediaArchive mediaarchive, String searchtype, Page upload, String tempindex) throws Exception{

	Boolean fastmode = Boolean.parseBoolean(context.findValue("testimportmode")); //maybe store this in the system table/catalog somewhere

	log.info("Importing data " + upload.getPath());
	Row trow = null;
	ArrayList tosave = new ArrayList();
	String catalogid = mediaarchive.getCatalogId();

	//log.info("importing " + upload.getPath());
	Reader reader = upload.getReader();
	ImportFile file = new ImportFile();
	file.setParser(new CSVReader(reader, ',', '\"'));
	file.read(reader);
	//Searcher searcher = mediaarchive.getSearcher(searchtype);


	PropertyDetails olddetails = null;
	PropertyDetailsArchive pdarchive = mediaarchive.getPropertyDetailsArchive();
	PropertyDetails details = pdarchive.getPropertyDetails(searchtype);


	Searcher searcher = mediaarchive.getSearcher(searchtype);
	details = searcher.getPropertyDetails();
	searcher.setAlternativeIndex(tempindex);
	if( !searcher.putMappings() )
	{
		throw new OpenEditException("Could not define dynamic or static fields, check mapping errors");
	}

	int count = 0;
	searcher.setForceBulk(true);
	while( (trow = file.getNextRow()) != null && ((fastmode && count < 1000) || !fastmode))
	{
		count++;
		String id = trow.get("id");
		Data newdata = searcher.createNewData();
		newdata.setId(id);

		for (Iterator iterator = file.getHeader().getHeaderNames().iterator(); iterator.hasNext();)
		{
			String header = (String)iterator.next();
			String detailid = header;//PathUtilities.extractId(header,true);
			String value = trow.get(header);
			if(detailid && detailid.contains(".")){
				continue;
			}
			PropertyDetail detail = details.getDetail(detailid);
			if(detail == null)
			{
				//see if we have a legacy field for this?
				details.each {
					String legacy = it.get("legacy");
					if(legacy != null && legacy.equals(header)){
						detail = it;
					}
				}
			}



			if (header.contains("."))
			{
				def splits = header.split("\\.");
				if (splits.length > 1)
				{
					detail = searcher.getDetail(splits[0]);
					if (detail != null && detail.isMultiLanguage())
					{
						LanguageMap map = null;
						Object values = newdata.getValue(detail.getId());
						if (values instanceof LanguageMap)
						{
							map = (LanguageMap) values;
						}
						if (values instanceof String)
						{
							map = new LanguageMap();
							map.put("en", values);
						}
						if (map == null)
						{
							map = new LanguageMap();
						}
						map.put(splits[1], value);
						newdata.setValue(detail.getId(), map);

					}

				}
				continue;
			}

			if(detail == null)
			{


				continue; // this should not happen if you run mergemappigns first
			}
			if( value == null)
			{
				continue;
			}
			if(detail.isDate()){ //Skip if in the right format
				try{
					Date date = DateStorageUtil.getStorageUtil().parseFromStorage(value);   //????
					newdata.setValue(detail.getId(), date);
				} catch (Exception e)
				{
					log.error("Parse issue " + value)
				}
			}
			else
			{
				if( searchtype.equals("app") && detail.getId().equals("deploypath"))
				{
					int inx= value.indexOf("/");
					if( inx > 1)
					{
						value = site.get("rootpath") + value.substring(inx - 1);
					}
				}
				newdata.setValue(detail.getId(), value);
			}
		}

		tosave.add(newdata);




		if(tosave.size() > 10000){
			searcher.saveAllData(tosave, null);
			tosave.clear();
		}
	}


	searcher.saveAllData(tosave, null);
	searcher.setAlternativeIndex(null);

	FileUtils.safeClose(reader);
	searcher.setForceBulk(false);
	searcher.setAlternativeIndex(null);
	searcher.clearIndex();
	log.info("Saved " + searchtype + " "  +  tosave.size() );
}





public void importJson(Data site, MediaArchive mediaarchive, String searchtype, Page upload, String tempindex) throws Exception{





	Searcher searcher = mediaarchive.getSearcher(searchtype);
	searcher.setAlternativeIndex(tempindex);


	ZipInputStream unzip = new ZipInputStream(upload.getInputStream());
	ZipEntry entry = unzip.getNextEntry();



	MappingJsonFactory f = new MappingJsonFactory();
	JsonParser jp = f.createParser(new InputStreamReader(unzip, "UTF-8"));
	
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
					String id = node.get("id");
					Data data = searcher.searchById(id);
					if(data == null){
						data = searcher.createNewData();
					}

					
					// And now we have random access to everything in the object
					log.info(node);
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



















//
//		Gson gson = new GsonBuilder().create();
//
//		// Read file in stream mode
//		JsonReader reader = new JsonReader(new InputStreamReader(unzip, "UTF-8"));
//
//		JsonToken peak = reader.peek();
//
//		// Read file in stream mode
//		reader.beginObject();
//		 peak = reader.peek();
//		while (reader.hasNext()) {
//			// Read data into object model
//			String data = reader.nextString();
//			log.info(data.toString());
//		}
//				reader.close();
//
//
//
//		searcher.setAlternativeIndex(null);
//
//		FileUtils.safeClose(reader);
//		searcher.setForceBulk(false);
//		searcher.setAlternativeIndex(null);
//		searcher.clearIndex();
//		log.info("Saved " + searchtype + " "  +  tosave.size() );








/* Not needed
 public void prepFields(MediaArchive mediaarchive, Page inFieldXml, String tempindex) 
 {
 if( !inFieldXml.getName().endsWith(".xml"))
 {
 return;
 }
 PropertyDetailsArchive pdarchive = mediaarchive.getPropertyDetailsArchive();
 String searchtype = inFieldXml.getPageName();
 log.info("save fields " + inFieldXml.getPath());
 String catalogid = mediaarchive.getCatalogId();
 PropertyDetails basedetails = pdarchive.getPropertyDetails(searchtype);
 XmlFile newsettings = pdarchive.getXmlArchive().loadXmlFile(inFieldXml.getPath()); 
 String filename = "/WEB-INF/data/" + catalogid + "/fields/" + searchtype + ".xml";
 PropertyDetails newdetails = new PropertyDetails(pdarchive,searchtype);
 newdetails.setInputFile(newsettings);
 pdarchive.setAllDetails(newdetails, searchtype, filename, newsettings.getRoot());
 ArrayList toremove = new ArrayList();
 newdetails.each{
 PropertyDetail olddetail = it;
 PropertyDetail current = basedetails.getDetail(olddetail.getId());
 if(current == null){
 current = basedetails.findCurrentFromLegacy(olddetail.getId());
 }
 //Already in base, we can remove this 
 if(current != null && !("name".equals(current.getId()) || "id".equals(current.getId())))
 {
 toremove.add(olddetail.getId());
 }
 }
 toremove.each{
 newdetails.removeDetail(it);
 }
 pdarchive.savePropertyDetails(newdetails, searchtype, null,  filename);
 pdarchive.clearCache();
 }
 */

init();
