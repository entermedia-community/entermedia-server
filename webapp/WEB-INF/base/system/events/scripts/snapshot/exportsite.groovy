package data;

import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.util.CSVWriter
import org.entermediadb.elasticsearch.searchers.ElasticListSearcher
import org.openedit.Data;
import org.openedit.data.PropertyDetail
import org.openedit.data.PropertyDetails
import org.openedit.data.PropertyDetailsArchive
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.hittracker.HitTracker
import org.openedit.page.Page
import org.openedit.util.DateStorageUtil


public void init()
{

	SearcherManager searcherManager = context.getPageValue("searcherManager");
	Searcher snapshotsearcher = searcherManager.getSearcher("system", "sitesnapshot");
	HitTracker exports = snapshotsearcher.query().match("snapshotstatus","pendingexport").search();
	if( exports.isEmpty() )
		{
			log.info("No pending snapshotstatus  = pendingexport");
			return;
		}
	//Link files in the FileManager. Keep exports in data/system
	for(Data snapshot:exports)
	{
		snapshot.setValue("snapshotstatus", "exporting"); //Like a lock
		snapshotsearcher.saveData(snapshot);
		Searcher sitesearcher = searcherManager.getSearcher("system", "site");
		Data site = sitesearcher.query().match("id", snapshot.get("site")).searchOne(); 
		String catalogid =  site.get("catalogid");
		MediaArchive mediaarchive = (MediaArchive)moduleManager.getBean(catalogid,"mediaArchive");
		
		snapshotsearcher.saveData(snapshot);
		export(mediaarchive, site, snapshot);
		snapshot.setValue("snapshotstatus", "complete");
		snapshotsearcher.saveData(snapshot);
		
	}
	

	

	/*  Do this based on the database as a seperate script
	Collection paths = mediaarchive.getPageManager().getChildrenPathsSorted("/WEB-INF/data/" + catalogid + "/dataexport/");
	Collections.reverse(paths);
	int keep = 0;
	for (Iterator iterator = paths.iterator(); iterator.hasNext();)
	{
		String path = (String) iterator.next();
		if( PathUtilities.extractFileName(path).length() == 19)
		{
			keep++;
			if( keep > 100 )
			{
				Page page = mediaarchive.getPageManager().getPage(path);
				mediaarchive.getPageManager().removePage(page);
			}
		}
	}
	*/


}


public void export(MediaArchive mediaarchive,Data inSite, Data inSnap)
{
	String folder = inSnap.get("folder");
	PropertyDetailsArchive archive = mediaarchive.getPropertyDetailsArchive();
	List searchtypes = archive.listSearchTypes();

	String rootfolder = "/WEB-INF/data/exports/" + mediaarchive.getCatalogId() + "/" + folder;
	String catalogid = mediaarchive.getCatalogId();
	log.info("Exporting " + rootfolder);
	searchtypes.each{
		String searchtype = it;
		Searcher searcher = searcherManager.getSearcher(catalogid, searchtype);
		if(searcher instanceof ElasticListSearcher)
		{
			return;
		}
			PropertyDetails details = searcher.getPropertyDetails();
			HitTracker hits = searcher.getAllHits();
			hits.enableBulkOperations();
			if(hits){

				Page output = mediaarchive.getPageManager().getPage(rootfolder + "/" + searchtype + ".csv");

				String realpath = output.getContentItem().getAbsolutePath();
				File outputfile = new File(realpath);
				File parent = outputfile.parentFile;

				parent.mkdirs();
				FileWriter out = new FileWriter(outputfile);
				CSVWriter writer  = new CSVWriter(out);
				HitTracker languages = searcherManager.getList(catalogid, "locale");
				int count = 0;
				int langcount = details.getMultilanguageFieldCount() ;
				langcount = langcount * (languages.size() );

				headers = new String[details.size() + langcount];



				for (Iterator iterator = details.iterator(); iterator.hasNext();) {
					PropertyDetail detail = (PropertyDetail) iterator.next();
					if(detail.isMultiLanguage()){
						languages.each{
							String id = it.id ;
							headers[count] = detail.getId() + "." + id;
							count ++;
						}
					}
					else{
						headers[count] = detail.getId();
						count++;
					}
				}
				writer.writeNext(headers);
				log.info("exporting: " + searchtype + ": " + hits.size() + " records");

				for (Iterator iterator = hits.iterator(); iterator.hasNext();) {
					hit =  iterator.next();
					//tracker = searcher.searchById(hit.get("id"));
					tracker = hit;

					nextrow = new String[details.size() + langcount];//make an extra spot for c
					int fieldcount = 0;
					for (Iterator detailiter = details.iterator(); detailiter.hasNext();)
					{
						PropertyDetail detail = (PropertyDetail) detailiter.next();


						if(detail.isMultiLanguage()){
							languages.each
							{
								String id = it.id ;
								Object vals = tracker.getValue(detail.getId())

								if(vals != null && vals instanceof Map){
									nextrow[fieldcount] = vals.getText(id);
								} else{
									nextrow[fieldcount] = vals;
								}

								fieldcount ++;
							}
						} else{

							String value = tracker.get(detail.getId());
							nextrow[fieldcount] = value;
							fieldcount++;
						}
					}
					writer.writeNext(nextrow);
				}
				writer.close();
		}
	}

	Page fields = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/fields/");
	if (fields.exists()) {
		Page target = mediaarchive.getPageManager().getPage(rootfolder + "/fields/");
		mediaarchive.getPageManager().copyPage(fields, target);
	}

	Page lists = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/lists/");
	if (lists.exists()) {
		Page target = mediaarchive.getPageManager().getPage(rootfolder + "/lists/");
		mediaarchive.getPageManager().copyPage(lists, target);
	}

	Page views = mediaarchive.getPageManager().getPage("/WEB-INF/data/" + catalogid + "/views/");
	if (views.exists()) {
		Page target = mediaarchive.getPageManager().getPage(rootfolder + "/views/");
		mediaarchive.getPageManager().copyPage(views, target);
	}
	String rootpath = inSite.get("rootpath");
	Page site = mediaarchive.getPageManager().getPage(rootpath);
	if (site.exists()) 
	{
		Page target = mediaarchive.getPageManager().getPage(rootfolder + "/site");
		mediaarchive.getPageManager().copyPage(site, target);
	}
	
	
//	Collection apps = mediaarchive.getList("app");
//	for(Data app in apps)
//	{
//		String deploypath = app.get("deploypath");
//		if(deploypath != null)
//		{
//			Page page = mediaarchive.getPageManager().getPage(deploypath);
//			if (page.exists()){
//				Page target = mediaarchive.getPageManager().getPage(rootfolder + "/application/" + deploypath);
//				mediaarchive.getPageManager().copyPage(page, target);
//			}
//		}
//	}

}



init();