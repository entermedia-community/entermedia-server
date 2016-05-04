package data;

import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.util.CSVWriter
import org.openedit.data.PropertyDetail
import org.openedit.data.PropertyDetailsArchive
import org.openedit.data.SearcherManager
import org.openedit.hittracker.HitTracker
import org.openedit.page.Page


public void init(){

	MediaArchive mediaarchive = context.getPageValue("mediaarchive");
	SearcherManager searcherManager = context.getPageValue("searcherManager");
	PropertyDetailsArchive archive = mediaarchive.getPropertyDetailsArchive();
	List searchtypes = archive.listSearchTypes();
	searchtypes.each{
		String searchtype = it;




		catalogid = context.findValue("catalogid");
		searcher = searcherManager.getSearcher(catalogid, searchtype);
		boolean friendly = Boolean.parseBoolean(context.findValue("friendly"));
		details = searcher.getPropertyDetails();
		HitTracker hits = searcher.getAllHits();

		if(hits){

			Page output = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/dataexport/" + searchtype + ".csv");

			String realpath = output.getContentItem().getAbsolutePath();
			File outputfile = new File(realpath);
			File parent = outputfile.parentFile;

			parent.mkdirs();
			FileWriter out = new FileWriter(outputfile);
			CSVWriter writer  = new CSVWriter(out);
			int count = 0;
			headers = new String[details.size()];
			for (Iterator iterator = details.iterator(); iterator.hasNext();) {
				PropertyDetail detail = (PropertyDetail) iterator.next();
				headers[count] = detail.getId();
				count++;
			}
			writer.writeNext(headers);
			log.info("about to start: " + hits.size() + "records");

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


					nextrow[fieldcount] = value;

					fieldcount++;

				}

				writer.writeNext(nextrow);



			}


			writer.close();
			
		}
	}
	
	Page fields = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/fields/");
	if (fields.exists()) {
	Page target = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/dataexport/fields/");
	mediaarchive.getPageManager().copyPage(fields, target);
	}
	
	Page lists = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/lists/");
	if (lists.exists()) {
	target = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/dataexport/lists/");
	mediaarchive.getPageManager().copyPage(lists, target);
	}
	
	Page views = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/views/");
	if (views.exists()) {
	target = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/dataexport/views/");
   mediaarchive.getPageManager().copyPage(views, target);
	}
	
	
   String applicationid  = context.findValue("applicationid");
   if(applicationid != null){
	   Page page = mediaarchive.getPageManager().getPage("/${applicationid}/");
	   if (page.exists()){
		   target = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/dataexport/application/${applicationid}/");
		   mediaarchive.getPageManager().copyPage(page, target);
		   
		   
	   }
	   
   }
   
	
}






init();