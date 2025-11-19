import java.text.SimpleDateFormat

import org.openedit.data.PropertyDetail
import org.openedit.data.PropertyDetails
import org.openedit.data.PropertyDetailsArchive
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.util.CSVWriter

import com.openedit.hittracker.HitTracker
import com.openedit.page.Page
import com.openedit.util.PathUtilities

public void init(){

	MediaArchive mediaarchive = context.getPageValue("mediaarchive");
	SearcherManager searcherManager = context.getPageValue("searcherManager");
	PropertyDetailsArchive archive = mediaarchive.getPropertyDetailsArchive();
	List searchtypes = archive.listSearchTypes();

	//String folder = DateStorageUtil.getStorageUtil().formatDateObj"yyyy-MM-dd-HH-mm-ss" "yyyy-MM-dd-HH-mm-ss");
	SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
	format.setLenient(true);
	String folder = format.format( new Date() );
	
	String rootfolder = "/WEB-INF/data/" + mediaarchive.getCatalogId() + "/dataexport/" + folder;
	String catalogid = mediaarchive.getCatalogId();

	searchtypes.each{
		String searchtype = it;
		//catalogid = context.findValue("catalogid");
		Searcher searcher = searcherManager.getSearcher(catalogid, searchtype);
		String classname = searcher.getClass().getName();
		if(classname.contains("XmlSearcher") || classname.contains("XmlFolderSearcher") || classname.contains("ListSearcher"))
		{
			return;
		}
		//boolean friendly = Boolean.parseBoolean(context.findValue("friendly"));
		PropertyDetails details = searcher.getPropertyDetails();
		HitTracker hits = searcher.getAllHits();

		if(hits){

			Page output = mediaarchive.getPageManager().getPage(rootfolder + "/" + searchtype + ".csv");

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
			if( keep > 10 )
			{
				Page page = mediaarchive.getPageManager().getPage(path);
				mediaarchive.getPageManager().removePage(page);
			}
		}
	}
}

init();