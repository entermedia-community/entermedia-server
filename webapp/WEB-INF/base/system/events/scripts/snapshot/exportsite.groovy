package snapshot;


import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import org.apache.commons.io.IOUtils
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse
import org.elasticsearch.cluster.metadata.MappingMetaData
import org.elasticsearch.common.collect.ImmutableOpenMap
import org.entermediadb.asset.MediaArchive
import org.entermediadb.elasticsearch.SearchHitData
import org.entermediadb.elasticsearch.searchers.ElasticListSearcher
import org.openedit.Data;
import org.openedit.data.NonExportable
import org.openedit.data.PropertyDetails
import org.openedit.data.PropertyDetailsArchive
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.hittracker.HitTracker
import org.openedit.page.Page


public void init() {

	SearcherManager searcherManager = context.getPageValue("searcherManager");
	Searcher snapshotsearcher = searcherManager.getSearcher("system", "sitesnapshot");
	HitTracker exports = snapshotsearcher.query().match("snapshotstatus","pendingexport").search();
	if( exports.isEmpty() ) {
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
		boolean configonly = Boolean.valueOf( snapshot.getValue("configonly") );
		export(mediaarchive, site, snapshot, configonly);
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


public void export(MediaArchive mediaarchive,Data inSite, Data inSnap, boolean configonly)
{
	String folder = inSnap.get("folder");
	PropertyDetailsArchive archive = mediaarchive.getPropertyDetailsArchive();
	List searchtypes = archive.listSearchTypes();

	String rootfolder = "/WEB-INF/data/exports/" + mediaarchive.getCatalogId() + "/" + folder;
	String catalogid = mediaarchive.getCatalogId();
	log.info("Exporting " + rootfolder);
	if( !configonly)
	{
		exportDatabase(mediaarchive, searchtypes, rootfolder);
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

public void exportDatabase(MediaArchive mediaarchive, List searchtypes, String rootfolder)
{
	String catalogid = mediaarchive.getCatalogId();
	String elasticid;
	
	
	String cat = mediaarchive.getCatalogId().replace("/", "_");
	String indexid = mediaarchive.getNodeManager().getIndexNameFromAliasName(cat);
	
//	GetMappingsRequest req = new GetMappingsRequest().indices(indexid);
//	GetMappingsResponse resp = mediaarchive.getNodeManager().getClient().admin().indices().getMappings(req).actionGet();
//
	
	GetMappingsResponse getMappingsResponse = mediaarchive.getNodeManager().getClient().admin().indices().getMappings(new GetMappingsRequest().indices(indexid)).actionGet();
	ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> indexToMappings = getMappingsResponse.getMappings();
	
	
	searchtypes.each{
		String searchtype = it;
	
		Searcher searcher = searcherManager.getSearcher(catalogid, searchtype);
		if(searcher instanceof ElasticListSearcher)
		{
			return;
		}
		if(searcher instanceof NonExportable)
		{
			return;
		}
	
			PropertyDetails details = searcher.getPropertyDetails();
		HitTracker hits = searcher.getAllHits();
		hits.enableBulkOperations();
		if(hits.size() > 0){
			
			Page output = mediaarchive.getPageManager().getPage(rootfolder + "/json/" + searchtype + ".zip");

			OutputStream os = output.getContentItem().getOutputStream();
			ZipOutputStream finalZip = new ZipOutputStream(os);
			ZipEntry ze = new ZipEntry(searchtype + ".json");

			finalZip.putNextEntry(ze);
			IOUtils.write("{ \"${searchtype}\": [", finalZip, "UTF-8");
			
			hits.each{
				SearchHitData hit = it;
				IOUtils.write(hit.toJsonString(), finalZip, "UTF-8");
				if(it != hits.last()) {
				IOUtils.write(",", finalZip, "UTF-8");
				}
				
			}
			IOUtils.write("]}", finalZip, "UTF-8");
			
			finalZip.flush();
			finalZip.closeEntry();
			finalZip.close();
			
			os.close();

			
			MappingMetaData actualMapping = indexToMappings.get(indexid).get(searchtype);
			
			
			if(actualMapping) {
			String json = actualMapping.source().string();
			Page mappings = mediaarchive.getPageManager().getPage(rootfolder + "/json/${searchtype}-mapping.json");
			mediaarchive.getPageManager().saveContent(mappings, null, json, "Saved mapping");
			} else {
				log.info("no mapping found for ${searchtype}");
			}
			
		}

	}
	



	
	
	
	

}

init();