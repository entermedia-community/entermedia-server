package data;

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import org.apache.commons.io.IOUtils;
import org.entermediadb.asset.MediaArchive
import org.entermediadb.elasticsearch.SearchHitData
import org.entermediadb.elasticsearch.searchers.ElasticListSearcher
import org.openedit.data.PropertyDetails
import org.openedit.data.PropertyDetailsArchive
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.hittracker.HitTracker
import org.openedit.page.Page
import org.openedit.util.DateStorageUtil


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
				Page output = mediaarchive.getPageManager().getPage(rootfolder + "/" + searchtype + ".zip");
				
				OutputStream os = output.getContentItem().getOutputStream();
				ZipOutputStream finalZip = new ZipOutputStream(os);
				ZipEntry ze = new ZipEntry(searchtype + ".json");
				
				finalZip.putNextEntry(ze);
				hits.each{
					SearchHitData hit = it;
					IOUtils.write(hit.toJsonString(), finalZip, "UTF-8");
				}
				finalZip.flush();
				finalZip.closeEntry();
				
				os.close();
				finalZip.close();
				
			}
			
	}

	

}






init();