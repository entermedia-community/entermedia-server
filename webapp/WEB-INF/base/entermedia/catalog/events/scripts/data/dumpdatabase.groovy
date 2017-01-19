package data;

import javax.management.InstanceOfQueryExp;

import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.util.CSVWriter
import org.entermediadb.elasticsearch.searchers.ElasticListSearcher;
import org.openedit.data.PropertyDetail
import org.openedit.data.PropertyDetails
import org.openedit.data.PropertyDetailsArchive
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.hittracker.HitTracker
import org.openedit.page.Page
import org.openedit.util.DateStorageUtil
import org.openedit.util.PathUtilities


public void init(){

	MediaArchive mediaarchive = context.getPageValue("mediaarchive");
	SearcherManager searcherManager = context.getPageValue("searcherManager");
	PropertyDetailsArchive archive = mediaarchive.getPropertyDetailsArchive();
	List searchtypes = archive.listSearchTypes();

	String folder = DateStorageUtil.getStorageUtil().formatDateObj(new Date(), "yyyy-MM-dd-HH-mm-ss");
	String rootfolder = "/WEB-INF/data/" + mediaarchive.getCatalogId() + "/dataexport/" + folder;
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
				log.info("about to start: " + searchtype + " " + hits.size() + "records");

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


	String applicationid  = context.findValue("applicationid");
	if(applicationid != null){
		Page page = mediaarchive.getPageManager().getPage("/${applicationid}/");
		if (page.exists()){
			Page target = mediaarchive.getPageManager().getPage(rootfolder + "/application/${applicationid}/");
			mediaarchive.getPageManager().copyPage(page, target);


		}

	}

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


}






init();