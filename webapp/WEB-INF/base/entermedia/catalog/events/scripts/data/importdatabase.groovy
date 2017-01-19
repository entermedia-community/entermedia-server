package data;

import org.dom4j.Attribute
import org.dom4j.Element
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.util.CSVReader
import org.entermediadb.asset.util.ImportFile
import org.entermediadb.asset.util.Row
import org.entermediadb.elasticsearch.ElasticNodeManager
import org.openedit.Data
import org.openedit.OpenEditException
import org.openedit.data.*
import org.openedit.modules.translations.LanguageMap
import org.openedit.page.Page
import org.openedit.util.*
import org.openedit.xml.XmlFile

public void init() 
{
	MediaArchive mediaarchive = context.getPageValue("mediaarchive");
	String catalogid = context.findValue("catalogid");

	ElasticNodeManager nodeManager = mediaarchive.getNodeManager();
	Date date = new Date();
	String tempindex =  nodeManager.toId(mediaarchive.getCatalogId().replaceAll("_", "") +  date.getTime());
	nodeManager.prepareIndex(tempindex);

	String rootdrive = null;

	Collection paths = mediaarchive.getPageManager().getChildrenPathsSorted("/WEB-INF/data/" + catalogid + "/dataexport/");
	if( paths.isEmpty() ) {
		log.info("No import folders found " + catalogid);
		return;
	}
	Collections.reverse(paths);
	rootdrive = (String)paths.iterator().next();
	if( PathUtilities.extractPageName(rootdrive).size() != 19) 
	{
		log.error("Root drive invalid: " + rootdrive);
		rootdrive = "/WEB-INF/data/" + catalogid + "/dataexport";
	}

	Page assets = mediaarchive.getPageManager().getPage(rootdrive + "/asset.csv");
	if( !assets.exists() ) {
		throw new OpenEditException("No asset.csv file in " + assets.getPath());
	}

	SearcherManager searcherManager = context.getPageValue("searcherManager");

	Page lists = mediaarchive.getPageManager().getPage(rootdrive + "/lists/");

	if(lists.exists()) {
		Page target = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/lists/");
		archiveFolder(target, tempindex);
		mediaarchive.getPageManager().copyPage(lists, target);
	}

	Page views = mediaarchive.getPageManager().getPage(rootdrive + "/views/");
	if(views.exists()) {
		Page target = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/views/");
		archiveFolder(target, tempindex);
		mediaarchive.getPageManager().copyPage(views, target);
	}

	List apps = mediaarchive.getPageManager().getChildrenPaths(rootdrive + "/application/");
	apps.each {
		Page page = mediaarchive.getPageManager().getPage(it);
		Page target = mediaarchive.getPageManager().getPage("/" + page.getName() + "/"); //This is the top folder
		archiveFolder(target, tempindex);
		mediaarchive.getPageManager().copyPage(page, target);
	}

	PropertyDetailsArchive pdarchive = mediaarchive.getPropertyDetailsArchive();
	pdarchive.clearCache();

	List ordereredtypes = new ArrayList();
	ordereredtypes.add("category");

	List childrennames = pdarchive.findChildTablesNames();
	List searchtypes = pdarchive.getPageManager().getChildrenPaths(rootdrive + "/" );
	searchtypes.each {
		if( it.endsWith(".csv")) {
			String searchtype = PathUtilities.extractPageName(it);
			if(!childrennames.contains(searchtype)) {
				ordereredtypes.add(searchtype);
			}
		}
	}
	ordereredtypes.addAll(childrennames);
	ordereredtypes.remove("settingsgroup");
	ordereredtypes.remove("propertydetail");
	ordereredtypes.remove("lock");
	ordereredtypes.remove("category");
	ordereredtypes.add(0,"category");

	/*
	 Page categories = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/dataexport/category.csv");
	 if(categories.exists()){
	 populateData(categories);
	 }
	 */
	Page target = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/fields/");
	archiveFolder(target, tempindex);
	
	boolean deleteold = true;
	ordereredtypes.each {
		Page upload = mediaarchive.getPageManager().getPage(rootdrive + "/" + it + ".csv");
		prepFields(mediaarchive,it,upload, tempindex); //Does this throw an exception?

	}
	pdarchive.clearCache();

	for( String type in ordereredtypes ) {
		Page upload = mediaarchive.getPageManager().getPage(rootdrive + "/" + type + ".csv");
		try{
			if( upload.exists() ) {
				importCsv(mediaarchive,type,upload, tempindex);
			}
		} catch (Exception e) {
			deleteold=false;

			log.error("Exception thrown importing upload: ${upload} ", e );
			break;
		}
	}
	
	if(deleteold) 
	{
		importPermissions(mediaarchive,rootdrive, tempindex);
		nodeManager.loadIndex(mediaarchive.getCatalogId(), tempindex, deleteold);
	}
	else {
		log.info("Import canceled");
	}
	mediaarchive.getSearcherManager().clear();
}

public void archiveFolder(Page inPage, String inIndex) 
{
	if( inPage.exists() && "false" == inPage.get("cleanonimport"))
	{
		MediaArchive mediaarchive = context.getPageValue("mediaarchive");
		Page trash = mediaarchive.getPageManager().getPage("/WEB-INF/trash/" + inIndex + "/" + inPage.getPath() );
		log.info("Archiving " + trash.getPath());
		mediaarchive.getPageManager().movePage(inPage, trash);
	}	
}

public void importPermissions(MediaArchive mediaarchive, String rootdrive, String tempindex) {
	Searcher sg = mediaarchive.getSearcher("settingsgroup");
	sg.setAlternativeIndex(tempindex);
	if( !sg.putMappings() ) {
		throw new OpenEditException("Could not import permissions ");
	}
	Page upload = mediaarchive.getPageManager().getPage(rootdrive + "/lists/settingsgroup.xml");
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

}


public void importCsv(MediaArchive mediaarchive, String searchtype, Page upload, String tempindex) throws Exception{


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



public void prepFields(MediaArchive mediaarchive, String searchtype, Page upload, String tempindex) {


	log.info("putMapping for " + upload.getPath());
	Row trow = null;
	ArrayList tosave = new ArrayList();
	String catalogid = mediaarchive.getCatalogId();



	PropertyDetails olddetails = null;
	PropertyDetailsArchive pdarchive = mediaarchive.getPropertyDetailsArchive();
	PropertyDetails details = pdarchive.getPropertyDetails(searchtype);


	String filepath = upload.getDirectory() +  "/fields/"  + searchtype + ".xml";
	XmlFile settings = pdarchive.getXmlArchive().loadXmlFile(filepath); // checks time
	if(settings.isExist()){
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



	}
}




try{
	init();
} finally{
	MediaArchive mediaarchive = context.getPageValue("mediaarchive");
	mediaarchive.getSearcherManager().resetAlternative();


}