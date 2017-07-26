package snapshot;

import org.dom4j.Attribute
import org.dom4j.Element
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.util.CSVReader
import org.entermediadb.asset.util.ImportFile
import org.entermediadb.asset.util.Row
import org.entermediadb.elasticsearch.ElasticNodeManager
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
import org.openedit.xml.XmlFile

public void init() 
{
	log.info("Initializing restore");
	SearcherManager searcherManager = context.getPageValue("searcherManager");
	Searcher snapshotsearcher = searcherManager.getSearcher("system", "sitesnapshot");
	HitTracker restores = snapshotsearcher.query().match("snapshotstatus","pendingrestore").search();
	if( restores.isEmpty() )
	{
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
	ordereredtypes.removeAll("settingsgroup");
	ordereredtypes.removeAll("propertydetail");
	ordereredtypes.removeAll("lock");
	ordereredtypes.removeAll("category");
	ordereredtypes.removeAll("user");
	ordereredtypes.removeAll("userprofile");
	ordereredtypes.removeAll("group");
	ordereredtypes.add(0,"category");

	/*
	 Page categories = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/dataexport/category.csv");
	 if(categories.exists()){
	 populateData(categories);
	 }
	 */
	Page target = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/fields/");
	archiveFolder(mediaarchive.getPageManager(), target, tempindex);  //Dele all existing fields
	
	boolean deleteold = true;
	ordereredtypes.each {
		Page upload = mediaarchive.getPageManager().getPage(rootfolder + "/fields/" + it + ".xml");
		prepFields(mediaarchive,it,upload, tempindex); //only move fields over that have data we care about

	}
	pdarchive.clearCache();
	if( configonly )
	{
		//Reindex all lists tables?
		//Page target = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/lists/");
	}
	else
	{
		for( String type in ordereredtypes ) {
			Page upload = mediaarchive.getPageManager().getPage(rootfolder + "/" + type + ".csv");
			try{
				if( upload.exists() ) {
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
		else {
			log.info("Import canceled");
		}
	}
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


	searcher.setForceBulk(true);
	while( (trow = file.getNextRow()) != null )
	{

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
	log.info("Saved " + searchtype + " "  +  tosave.size() );
}



public void prepFields(MediaArchive mediaarchive, String searchtype, Page upload, String tempindex) 
{
	if( !upload.exists())
	{
		return;
	}
	log.info("save fields " + upload.getPath());
	Row trow = null;
	ArrayList tosave = new ArrayList();
	String catalogid = mediaarchive.getCatalogId();

	PropertyDetails olddetails = null;

	String filepath = upload.getPath();
	XmlFile settings = pdarchive.getXmlArchive().loadXmlFile(filepath); // checks time
	if(settings.isExist())
	{
		PropertyDetailsArchive pdarchive = mediaarchive.getPropertyDetailsArchive();
		pdarchive.clearCache();
		
		PropertyDetails details = pdarchive.getPropertyDetails(searchtype);

		String filename = "/WEB-INF/data/" + catalogid + "/fields/" + searchtype + ".xml";
		olddetails = new PropertyDetails(pdarchive,searchtype);
		olddetails.setInputFile(settings);
		pdarchive.setAllDetails(olddetails, searchtype, filename, settings.getRoot());

		ArrayList toremove = new ArrayList();


		olddetails.each{
			PropertyDetail olddetail = it;

			PropertyDetail current = details.getDetail(olddetail.getId());
			if(current == null){
				current = details.findCurrentFromLegacy(olddetail.getId());
			}
			if(current != null && !("name".equals(current.getId()) || "id".equals(current.getId()))){

				toremove.add(olddetail.getId());


			}
		}

		toremove.each{
			olddetails.removeDetail(it);
		}
		pdarchive.savePropertyDetails(olddetails, searchtype, null,  filename);
		pdarchive.clearCache();
	}
	else
	{
		Page inputed = mediaarchive.getPageManager().getPage(filepath);
		if( inputed.exists() )
		{
			String dest = "/WEB-INF/data/" + catalogid + "/fields/" + searchtype + ".xml";
			Page target = mediaarchive.getPageManager().getPage(dest);
			mediaarchive.getPageManager().copyPage(inputed, target);
		}
	}
}

init();
