package data;

import org.dom4j.Attribute
import org.dom4j.Element
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.util.CSVReader
import org.entermediadb.asset.util.ImportFile
import org.entermediadb.asset.util.Row
import org.openedit.Data
import org.openedit.data.*
import org.openedit.util.*
import org.openedit.page.Page
import org.openedit.util.XmlUtil

public void init(){

	MediaArchive mediaarchive = context.getPageValue("mediaarchive");
	catalogid = context.findValue("catalogid");
	SearcherManager searcherManager = context.getPageValue("searcherManager");
	
	
	Page lists = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/dataexport/lists/");
	
	Page target = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/lists/");
	if(lists.exists()){
	mediaarchive.getPageManager().copyPage(lists, target);
	}
	
	Page views = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/dataexport/views/");
	target = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/views/");
	if(views.exists()){
		mediaarchive.getPageManager().copyPage(views, target);
	}
	
	List apps = mediaarchive.getPageManager().getChildrenPaths("/WEB-INF/data/" + catalogid + "/dataexport/application/");
	apps.each{
		   Page page = mediaarchive.getPageManager().getPage(it);
		   target = mediaarchive.getPageManager().getPage("/${page.getName()}/");
		   mediaarchive.getPageManager().copyPage(page, target);	   
	}
	
	
	PropertyDetailsArchive archive = mediaarchive.getPropertyDetailsArchive();
	List searchtypes = archive.getPageManager().getChildrenPaths("/WEB-INF/data/" + catalogid + "/dataexport/");
	
	Page categories = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/dataexport/category.csv");
	
	if(categories.exists()){
		populateData(categories);
	}
	
	
	
	
	
	searchtypes.each{
		
		Page upload = mediaarchive.getPageManager().getPage(it);		
		
		String searchtype = upload.getName();		
		searchtype = searchtype.substring(0, searchtype.length() - 4);
		
			
		if(it.endsWith(".csv") && !it.contains("category")) {
			
			
			try{
					populateData(upload);
			} catch (Exception e)
			{
			
			log.info("Exception thrown importing upload: ${upload} " );
			}
			
			
		}
	}
}



public void populateData(Page upload){
	String searchtype = upload.getName();
	searchtype = searchtype.substring(0, searchtype.length() - 4);
	MediaArchive mediaarchive = context.getPageValue("mediaarchive");
	catalogid = context.findValue("catalogid");
	SearcherManager searcherManager = context.getPageValue("searcherManager");
	
	Searcher searcher = searcherManager.getSearcher(catalogid, searchtype);
	PropertyDetails details = searcher.getPropertyDetails();
	
	
	
	Reader reader = upload.getReader();
	ImportFile file = new ImportFile();
	file.setParser(new CSVReader(reader, ',', '\"'));
	file.read(reader);

	Row trow = null;
	ArrayList data = new ArrayList();
	
	if(searchtype == "settingsgroup")
	{
		importPermissions(file, searcher, data)
	}
	else
	{
		importCsv(file, searcher, data);
	}
	
	
	searcher.saveAllData(data, null);
	data.clear();
	
}

public void importPermissions(ImportFile file, Searcher searcher, Collection data)
{
	Page upload = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + searcher.getCatalogId() + "/dataexport/lists/settingsgroup.xml");
	
	XmlUtil util = new XmlUtil();
	Element root = util.getXml(upload.getReader(),"utf-8");
	for(Iterator iterator = root.elementIterator(); iterator.hasNext();)
	{
		Element row = iterator.next();
		List perms = new ArrayList();
		for(Iterator iterator2 = row.attributes().iterator(); iterator2.hasNext();)
		{
			Attribute attr = iterator2.next();
			if( Boolean.valueOf(attr.getValue() ) )
			{
				perms.add(attr.getQualifiedName() );
			}
		}
		String id = row.attributeValue("id");
		Data existing = sg.searchById(id);
		if( existing == null)
		{
			existing = sg.createNewData();
		}
		existing.setId(id);
		existing.setName(row.attributeValue("name"));
		existing.setValue("permissions", perms);
		//sg.saveData(existing, null);
		data.add(existing);
	}
}


public void importCsv(ImportFile file, Searcher searcher, Collection data)
{	
	PropertyDetails details = searcher.getPropertyDetails();
	
	while( (trow = file.getNextRow()) != null )
	{
		String id = trow.get("id");
		
		PropertyDetail parent = searcher.getDetail("_parent");
		Data newdata = null;
		if(parent == null){
						
		 newdata = searcher.searchById(id);
		} else{
			String parentid = trow.get("_parent");
			if(parentid == null){
				
				 parentid = trow.get("asset");
			}
			
			newdata = searcher.query().match("id", id).match("_parent", parentid).searchOne();
			
		
		}
		
		if(newdata == null){
			newdata = searcher.createNewData();
			newdata.setId(id);
		}
		data.add(newdata);
		
		for (Iterator iterator = file.getHeader().getHeaderNames().iterator(); iterator.hasNext();)
		{
			String header = (String)iterator.next();
			String detailid = PathUtilities.extractId(header,true);
			PropertyDetail detail = details.getDetail(detailid);
			if(detail == null){
				//see if we have a legacy field for this?
				details.each {
					String legacy = it.get("legacy");
					if(legacy != null && legacy.equals(header)){
						detail = it;
					}
				}
			}
			if(detail == null){
				continue; // this should not happen if you run mergemappigns first
			}
								
			
			String value = trow.get(header);
			
			if(detail.isDate()){
				try{
					Date date = DateStorageUtil.getStorageUtil().parseFromStorage(value);
					value = DateStorageUtil.getStorageUtil().formatForStorage(date);
				} catch (Exception e){
					value= null;
				}
			}
			newdata.setProperty(detail.getId(), value);
			
			
		}
		
		if(parent != null && newdata.get("_parent") == null){
			data.remove(newdata);
		}
		
		if(data.size() > 1000){
			searcher.saveAllData(data, null);
			data.clear();
		}
       }
}


init();