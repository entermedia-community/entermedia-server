package asset;


import org.apache.commons.codec.binary.Base64
import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.event.WebEvent

public init(){
	log.info("Starting Pre-Save Event");
	MediaArchive mediaArchive = (MediaArchive)context.getPageValue("mediaarchive");
	Searcher targetsearcher = mediaArchive.getAssetSearcher();
	String assetids = context.getRequestParameter("id");
	Collection hits = mediaarchive.getAssetSearcher().query().orgroup("id",assetids).search();
	
	if(assetids.startsWith("multi")) {
		WebEvent event = context.getPageValue("webevent");
		Data composite = event.getValue("data");
		hits = composite.getInitialSearchResults();
		
	}
	//http://vizmtlvamf.media.in.cbcsrc.ca/vms/#ItemPlace:2101409230000048521
	vizone = mediaArchive.getModuleManager().getBean(mediaArchive.getCatalogId(), "VizOnepublisher");
	String username = "EMDEV";
	String password  = "3nterMed1a";
	
	String enc = username + ":" + password;
	byte[] encodedBytes = Base64.encodeBase64(enc.getBytes());
	String authString = new String(encodedBytes);
	//public Element setMetadata(MediaArchive inArchive, String servername, Asset inAsset, String inAuthString) throws Exception{
		
	hits.each{
		Data hit = it;
		if(hit.vizid){
			Asset asset = targetsearcher.loadData(hit);
			vizone.setMetadata(mediaArchive, "http://vme.mtl.cbc.ca/", asset, authString );
			
		}
		
	}	
	
}

init();