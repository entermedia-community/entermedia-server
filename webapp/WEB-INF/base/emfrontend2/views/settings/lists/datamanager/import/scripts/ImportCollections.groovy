import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.util.CSVReader
import org.entermediadb.asset.util.ImportFile
import org.openedit.data.PropertyDetail
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.modules.translations.LanguageMap
import org.openedit.page.Page


MediaArchive mediaarchive = context.getPageValue("mediaarchive");
String importpath = context.findValue("importpath");
Page upload = mediaarchive.getPageManager().getPage(importpath);
Reader reader = upload.getReader();
ImportFile file = new ImportFile();
file.setParser(new CSVReader(reader, ',', '\"'));
file.read(reader);
Searcher searcher = mediaarchive.getSearcher("librarycollectionasset");
ArrayList rows = new ArrayList();
while( (trow = file.getNextRow()) != null ) {

	String id = trow.get("id");
	String searchtype = trow.get("searchtype");
	if(id && searchtype){
		Searcher remote = mediaarchive.getSearcher(searchtype);
		PropertyDetail detail = remote.getDetail(id);  //target detail from the actual table
		if(detail == null){
			detail = new PropertyDetail();
			detail.setId(id);
			detail.setSearchType(searchtype);
		}
		boolean changed = false;
		searcher.getPropertyDetails().each{
			String detailid = it.id ;  //name, editable, analyzer etc...
			if("id".equals(detailid)){
				return; //skip the ID column - it's nonsense
			}
			String value = trow.get(detailid);
			if(value != null && value.length() == 0){
				value = null;
			}
			if(detailid.contains("name.")){
				String[] split = detailid.split(".");
				String langcode = split[1];
				LanguageMap map = detail.getValue("name");
				map.setValue(langcode, value);
			} else{
			
			String oldval = detail.get(detailid);
			if(value != null && oldval != null && !value.equals(oldval) ){
				changed = true;
				detail.setValue(detailid, value);
			}			
		}
		HitTracker locales = mediaarchive.getSearcherManager().getList(mediaarchive.getCatalogId(), "locale");
		LanguageMap map = detail.getValue("name");
		locales.each{
			String langval = trow.get("name." + it.id);
			if(langval != null)				 
				map.setText(it.id, langval);
				changed=true; 
			}
		}
			
			
			
		if(changed){
			remote.getPropertyDetailsArchive().updatePropertyDetail(detail, searchtype, null);
		}
		
		

	}
}


