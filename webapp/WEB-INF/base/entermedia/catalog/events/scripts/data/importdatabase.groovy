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

public void init(){

	
	
	
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
		rootdrive = "/WEB-INF/data/" + catalogid + "/dataexport";
	}	

	Page assets = mediaarchive.getPageManager().getPage(rootdrive + "/asset.csv");
	if( !assets.exists() )
	{
		throw new OpenEditException("No asset.csv file in " + assets.getPath());
	}
	
	SearcherManager searcherManager = context.getPageValue("searcherManager");
	
	Page lists = mediaarchive.getPageManager().getPage(rootdrive + "/lists/");

	if(lists.exists()) {
		Page target = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/lists/");
		mediaarchive.getPageManager().copyPage(lists, target);
	}

	Page views = mediaarchive.getPageManager().getPage(rootdrive + "/views/");
	if(views.exists())
	{
		Page target = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/views/");
		mediaarchive.getPageManager().copyPage(views, target);
	}

	List apps = mediaarchive.getPageManager().getChildrenPaths(rootdrive + "/application/");
	apps.each {
		Page page = mediaarchive.getPageManager().getPage(it);
		Page target = mediaarchive.getPageManager().getPage("/" + page.getName() + "/"); //This is the top folder
		mediaarchive.getPageManager().copyPage(page, target);
	}

	PropertyDetailsArchive archive = mediaarchive.getPropertyDetailsArchive();

	List ordereredtypes = new ArrayList();
	ordereredtypes.add("category");
	
	List childrennames = archive.findChildTablesNames();
	List searchtypes = archive.getPageManager().getChildrenPaths(rootdrive + "/" );
	searchtypes.each {
		if( it.endsWith(".csv"))
		{
			String searchtype = PathUtilities.extractPageName(it);
			if(!childrennames.contains(searchtype))
			{
				ordereredtypes.add(searchtype);
			}
		}	
			
	}
	ordereredtypes.addAll(childrennames);
	ordereredtypes.remove("settingsgroup");
		
	/*
	 Page categories = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/dataexport/category.csv");
	 if(categories.exists()){
	 populateData(categories);
	 }
	 */
	boolean deleteold = true;
	
	ordereredtypes.each
	{
		Page upload = mediaarchive.getPageManager().getPage(rootdrive + "/" + it + ".csv");
		prepFields(mediaarchive,it,upload, tempindex);
		
	}
	
	
	
	ordereredtypes.each 
	{
		Page upload = mediaarchive.getPageManager().getPage(rootdrive + "/" + it + ".csv");
		try{
			if( upload.exists() )
			{
				importCsv(mediaarchive,it,upload, tempindex);
			}
		} catch (Exception e) {
			deleteold=false;
			
			log.info("Exception thrown importing upload: ${upload} ", e );
		}
	}
	importPermissions(mediaarchive,rootdrive, tempindex);
	if(deleteold){
		nodeManager.loadIndex(mediaarchive.getCatalogId(), tempindex, deleteold);
	}
	mediaarchive.getSearcherManager().clear();
}


public void importPermissions(MediaArchive mediaarchive, String rootdrive, String tempindex) {
	Page upload = mediaarchive.getPageManager().getPage(rootdrive + "/lists/settingsgroup.xml");
	if( upload.exists() )
	{
		Searcher sg = mediaarchive.getSearcher("settingsgroup");
		sg.setAlternativeIndex(tempindex);
		sg.putMappings();
		
		XmlUtil util = new XmlUtil();
		Element root = util.getXml(upload.getReader(),"utf-8");
		for(Iterator iterator = root.elementIterator(); iterator.hasNext();) {
			Element row = iterator.next();
			String permsstring = row.attributeValue("permissions");
			if( permsstring != null) //upgrade?
			{
				return;
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
		sg.setAlternativeIndex(null);
	}
	
}


public void importCsv(MediaArchive mediaarchive, String searchtype, Page upload, String tempindex) throws Exception{

	
	log.info("Importing " + upload.getPath());
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
	PropertyDetailsArchive archive = mediaarchive.getPropertyDetailsArchive();
	PropertyDetails details = archive.getPropertyDetails(searchtype);
		
	
	Searcher searcher = mediaarchive.getSearcher(searchtype);
	details = searcher.getPropertyDetails();
	searcher.setAlternativeIndex(tempindex);
	searcher.putMappings();
	
	
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
	
	tosave.clear();	
	searcher.setForceBulk(false);
	searcher.setAlternativeIndex(null);
	log.info("Saved " + searchtype + " "  +  tosave.size() );
}



public void prepFields(MediaArchive mediaarchive, String searchtype, Page upload, String tempindex) {

	
	log.info("Importing " + upload.getPath());
	Row trow = null;
	ArrayList tosave = new ArrayList();
	String catalogid = mediaarchive.getCatalogId();
	
	
	
	PropertyDetails olddetails = null;
	PropertyDetailsArchive archive = mediaarchive.getPropertyDetailsArchive();
	PropertyDetails details = archive.getPropertyDetails(searchtype);
	
	
	String filepath = upload.getDirectory() +  "/fields/"  + searchtype + ".xml";
	XmlFile settings = archive.getXmlArchive().loadXmlFile(filepath); // checks time
	if(settings.isExist()){
		String filename = "/WEB-INF/data/" + catalogid + "/fields/" + searchtype + ".xml";
		olddetails = new PropertyDetails(archive,searchtype);
		olddetails.setInputFile(settings);
		archive.setAllDetails(olddetails, searchtype, filename, settings.getRoot());
					
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
		
		
		archive.savePropertyDetails(olddetails, searchtype, null,  filename);
		
		
		
	}
	
	archive.clearCache();
	
	
}




try{
	init();
} finally{
MediaArchive mediaarchive = context.getPageValue("mediaarchive");
mediaarchive.getSearcherManager().resetAlternative();


}