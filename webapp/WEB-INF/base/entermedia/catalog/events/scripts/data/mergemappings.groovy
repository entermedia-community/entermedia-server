package data;

import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.search.BaseAssetSearcher;
import org.entermediadb.asset.util.CSVReader
import org.entermediadb.asset.util.ImportFile
import org.entermediadb.asset.util.Row
import org.entermediadb.elasticsearch.searchers.BaseElasticSearcher
import org.entermediadb.elasticsearch.searchers.ElasticAssetDataConnector;
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
		if(it.endsWith(".csv")) {
			boolean savedetails = false;
			PropertyDetails olddetails = null;
			Page upload = mediaarchive.getPageManager().getPage(it);
			String searchtype = upload.getName();
			searchtype = searchtype.substring(0, searchtype.length() - 4);
	
			String filepath = "/WEB-INF/data/" + catalogid + "/dataexport/fields/" + searchtype + ".xml";
			XmlFile settings = archive.getXmlArchive().loadXmlFile(filepath); // checks time
			if(settings.isExist()){
				olddetails = new PropertyDetails(archive,searchtype);
				olddetails.setInputFile(settings);
				archive.setAllDetails(olddetails, searchtype, settings.getRoot());
			}
	
			PropertyDetails currentdetails = archive.getPropertyDetails(searchtype);
	
			if(olddetails != null){
				String beanname = olddetails.getBeanName();
	
				if(beanname  && (beanname.contains("xmlFile") || beanname.contains("data"))){
					if(currentdetails != null && !"dataSearcher".equals(currentdetails.getBeanName())){
						currentdetails.setBeanName("dataSearcher");
						savedetails = true;
					}	
				}
			}
			
			Reader reader = upload.getReader();
			ImportFile file = new ImportFile();
			file.setParser(new CSVReader(reader, ',', '\"'));
			file.read(reader);

			for (Iterator iterator = file.getHeader().getHeaderNames().iterator(); iterator.hasNext();)
			{
				String header = (String)iterator.next();
				String detailid = PathUtilities.extractId(header,true);
				PropertyDetail detail = currentdetails.getDetail(detailid);
				
				
				if(detail == null){
					//see if we have a legacy field for this- if so it'll get remapped during the import and we don't want to add it
					currentdetails.each {
						String legacy = it.get("legacy");
						if(legacy != null && legacy.equals(header)){
							detail = it;
							currentdetails.removeDetail(legacy);
						}
					}
				}
				
				
				
				
				if(detail == null && olddetails != null){
					detail = olddetails.getDetail(detailid);
					if(detail != null){
						currentdetails.addDetail(detail);
					}
					savedetails = true;
				}

				if(detail == null){
					detail = new PropertyDetail();
					detail.setId(header);
					detail.setText(header);
					detail.setEditable(true);
					detail.setIndex(true);
					detail.setStored(true);
					currentdetails.addDetail(detail);
					savedetails = true;
				}
				
				if(detail != null){
					
					if(detail.getId().contains(".")){
						String newid = detail.getId().replace('.', '_');
						detail.setProperty("legacy", detail.getId());
						detail.setId(newid);
						savedetails = true;
					}
				}
				

			}
			ArrayList toremove = new ArrayList();
			
			
			
			currentdetails.each {
				String legacy = it.get("legacy");
				if(legacy){
					toremove.add(legacy);
					savedetails=true;
				}
				
			}
			toremove.each{
				currentdetails.removeDetail(it);
			}
			

			if(savedetails){
			
				
				
				mediaarchive.getPropertyDetailsArchive().savePropertyDetails(currentdetails, searchtype, context.getUser());

			}

		}
	}
	
	
	

	
	
	
	
	
}








		init();