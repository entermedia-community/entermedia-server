package data;

import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.util.CSVReader
import org.entermediadb.asset.util.ImportFile
import org.entermediadb.asset.util.Row
import org.openedit.Data
import org.openedit.data.*
import org.openedit.page.Page
import org.openedit.util.DateStorageUtil;
import org.openedit.util.PathUtilities
import org.openedit.xml.XmlFile

public void init(){

	MediaArchive mediaarchive = context.getPageValue("mediaarchive");
	catalogid = context.findValue("catalogid");
	SearcherManager searcherManager = context.getPageValue("searcherManager");
	PropertyDetailsArchive archive = mediaarchive.getPropertyDetailsArchive();
	List searchtypes = archive.getPageManager().getChildrenPaths("/WEB-INF/data/" + catalogid + "/dataexport/");
	searchtypes.each{
		
		Page upload = mediaarchive.getPageManager().getPage(it);		
		
		String searchtype = upload.getName();		
		searchtype = searchtype.substring(0, searchtype.length() - 4);
		
		String filepath = "/WEB-INF/data/" + catalogid + "/dataexport/fields/" + searchtype + ".xml";
			
		if(it.endsWith(".csv")) {
			Searcher searcher = searcherManager.getSearcher(catalogid, searchtype);
			PropertyDetails details = searcher.getPropertyDetails();
			
			
			
			Reader reader = upload.getReader();
			ImportFile file = new ImportFile();
			file.setParser(new CSVReader(reader, ',', '\"'));
			file.read(reader);

			Row trow = null;
			ArrayList data = new ArrayList();
			while( (trow = file.getNextRow()) != null )
			{
			
				if(searchtype == "settingsgroup"){
					continue;//need to deal with this one seperately
				}
				
					
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
			
			
			searcher.saveAllData(data, null);
			data.clear();
			
			
			
			
			
		}
	}
}






init();