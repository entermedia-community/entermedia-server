import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.util.CSVReader
import org.entermediadb.asset.util.ImportFile
import org.entermediadb.projects.ProjectManager
import org.openedit.Data
import org.openedit.page.Page


MediaArchive mediaarchive = context.getPageValue("mediaarchive");
String importpath = context.findValue("importpath");
Page upload = mediaarchive.getPageManager().getPage(importpath);
Reader reader = upload.getReader();
ImportFile file = new ImportFile();
file.setParser(new CSVReader(reader, ',', '\"'));
file.read(reader);
ArrayList rows = new ArrayList();
ProjectManager pm = mediaarchive.getProjectManager();
while( (trow = file.getNextRow()) != null ) {

	String id = trow.get("id");
	String searchtype = trow.get("libraries");
	String[] splits = searchtype.split("|");
	Asset asset = mediaarchive.getAsset(id);
	if(asset){
		splits.each{
			Data library = mediaarchive.getData("library", it);
			if(library){
				String categoryid = library.categoryid;
				if(categoryid != null){
					Category cat = mediaarchive.getCategory(categoryid);
					asset.addCategory(cat);
					mediaarchive.saveAsset(asset);
					
				}
				
			}
		}
	}
	
	
}


