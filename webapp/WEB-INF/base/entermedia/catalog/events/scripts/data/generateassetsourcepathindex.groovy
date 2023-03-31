package data;

import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.util.CSVWriter
import org.openedit.Data
import org.openedit.WebPageRequest
import org.openedit.data.PropertyDetail
import org.openedit.data.PropertyDetails
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.hittracker.HitTracker
import org.openedit.page.Page
import org.openedit.util.DateStorageUtil
import org.openedit.util.Replacer

public void init(){

	
	
	try{
		
	
	MediaArchive mediaarchive = context.getPageValue("mediaarchive");

	SearcherManager searcherManager = context.getPageValue("searcherManager");
	String searchtype = context.findValue("searchtype");
	String catalogid = context.findValue("catalogid");
	Searcher searcher = mediaarchive.getSearcher(searchtype);
	boolean friendly = Boolean.parseBoolean(context.findValue("friendly"));
	String view = context.findValue("view");
	
	Collection details = null;
	if(view != null){
		details = searcher.getDetailsForView(view);
		log.info("export custom view detauls: "+ details);
	}
	
	if(details == null){
		details = searcher.getPropertyDetails();
	}

	HitTracker hits = searcher.getAllHits();
	
	hits.enableBulkOperations();
	String exportpath = context.findValue("exportpath");

	HashMap map = new HashMap();
	String date = DateStorageUtil.getStorageUtil().formatDateObj(new Date(), "yyyy-MM-dd-HH-mm-ss");
	map.put("date",date );
	Replacer replacer = new Replacer();
	exportpath = replacer.replace(exportpath, map);

	Page output = mediaarchive.getPageManager().getPage(exportpath);

	String realpath = output.getContentItem().getAbsolutePath();
	File outputfile = new File(realpath);
	File parent = outputfile.parentFile;
	
	parent.mkdirs();
	FileWriter out = new FileWriter(outputfile);
	CSVWriter writer  = new CSVWriter(out);
	
	
	log.info("export path: "+ outputfile);
	
	int count = 0;
	
	
	
	headers = new String[details.size()];
	for (Iterator iterator = details.iterator(); iterator.hasNext();)
	{
		PropertyDetail detail = (PropertyDetail) iterator.next();
		
		if(friendly){
			String headername = detail.getText(context);
			if(headername == null) {
				headername = detail.getId();
			}
			headers[count] = headername;
		} else{
		if(detail.isMultiLanguage()){
					languages.each{
						String id = it.id ;
						headers[count] = detail.getId() + "." + id;
					
					}
				}
				else{
					headers[count] = detail.getId();
					
				}
		}		
		count++;
	}
	int rowcount = 0;
	writer.writeNext(headers);

	log.info("about to start exporting: " + hits.size() + " records");
	context.putPageValue("records", hits);
	context.putPageValue("date", date);
	for (Iterator iterator = hits.iterator(); iterator.hasNext();) {
		hit =  iterator.next();
		//tracker = searcher.searchById(hit.get("id"));
		tracker = hit;

		nextrow = new String[details.size()];//make an extra spot for c
		int fieldcount = 0;
		for (Iterator detailiter = details.iterator(); detailiter.hasNext();)
		{
			PropertyDetail detail = (PropertyDetail) detailiter.next();
			String value = tracker.get(detail.getId());
			//do special logic here

			if(detail.isList() && friendly){
				//Join?
				//#set($label = $searcherManager.getValue($catalogid, $detail.render, $type.properties))
				if(detail.render){
					value = searcherManager.getValue(catalogid, detail.render, tracker.properties);
				} else{

					Data remote  = searcherManager.getData( detail.getListCatalogId(),detail.getListId(), value);

					if(remote != null){
						value= remote.getName();
					}
				}
			}
			nextrow[fieldcount] = value;

			fieldcount++;

		}

		writer.writeNext(nextrow);
	}
	writer.close();
	
	log.info("Compelte - sending notifications : ${notify}");
	
	} 
	
	catch(Exception e){
	
		e.printStackTrace();
	}

}


init();