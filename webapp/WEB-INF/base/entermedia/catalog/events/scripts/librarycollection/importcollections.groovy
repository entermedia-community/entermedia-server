package librarycollection;

import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.util.CSVReader
import org.entermediadb.modules.update.Downloader
import org.openedit.Data
import org.openedit.OpenEditException
import org.openedit.WebPageRequest
import org.openedit.data.Searcher
import org.openedit.page.Page


public void init(){
	System.out.println("import products");
	WebPageRequest req = context;

	MediaArchive archive = req.getPageValue("mediaarchive");
	Searcher searcher = archive.getSearcher("librarycollection");

	List rows = new ArrayList();
	String csvpath = "/${catalogid}/imports/ALF_ALL_ALL.txt";


	Page upload = archive.getPageManager().getPage(csvpath);
	Downloader dl = new Downloader();
	//ftp://pics1.alfred.com/CatData/DailyAll/ALF_ALL_ALL.txt
	String dlname = upload.getContentItem().getAbsolutePath();
	dl.ftpDownload("pics1.alfred.com", "/CatData/DailyAll", "ALF_ALL_ALL.txt", dlname, "", "");
	
	Reader reader = upload.getReader();
	try{
		CSVReader read = new CSVReader(reader, (char)'\t');
		String[] headers = read.readNext();
		String[] line;
		while ((line = read.readNext()) != null){
			String id = line[2];
			Data collection = searcher.searchById(id);
			if(collection == null) {
				collection = searcher.createNewData();
				collection.setValue("library", "products");
				collection.setName(id);
				collection.setId(id);
				//collection.setValue("rootcategory",id);
				rows.add(collection);
			}

			if(rows.size() > 10000){
				searcher.saveAllData(rows, null);
				rows.clear();
			}
		}
		searcher.saveAllData(rows, null);
	} catch (Exception e){
		throw new OpenEditException(e);
	}
}
init();

