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
	Searcher searcher = archive.getSearcher("entityproduct");

	List rows = new ArrayList();
	String csvpath = "/${catalogid}/imports/ALF2.txt";


	Page upload = archive.getPageManager().getPage(csvpath);
	Downloader dl = new Downloader();
	//ftp://pics1.alfred.com/CatData/DailyAll/ALF_ALL_ALL.txt
	String dlname = upload.getContentItem().getAbsolutePath();
	//dl.ftpDownload("pics1.alfred.com", "/CatData/DailyAll", "ALF_ALL_ALL.txt", dlname, "", "");
	
	Reader reader = upload.getReader();
	try{
		Integer li = 1;
		CSVReader read = new CSVReader(reader, (char)'\t', true);
		String[] headers = read.readNext();
		String[] line;
		while ((line = read.readNext()) != null){
			String id = line[2];
			//log.info("Line:"+li+" Id " + id);
			Data product = searcher.searchById(id);
			if(product == null) {
				product = searcher.createNewData();
				
				
				product.setName(id);
				product.setId(id);
				//collection.setValue("rootcategory",id);
			}
			product.setValue("library", "products");
			if(line.length >=37) {
				product.setValue("projectdescription", line[36]);
				product.setValue("title", line[9]);
				product.setValue("subtitle", line[10]);
				product.setValue("genre", line[16]);
				product.setValue("level", line[21]);
				product.setValue("series", line[20]);
				product.setValue("keywords", line[26]);
			} else {
				log.info("CSV Import, incomplete row line " + li +" id: "+id);
			}
			
			rows.add(product);
			li++;
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

