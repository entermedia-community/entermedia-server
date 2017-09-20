package asset;

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.data.Searcher
import org.apache.commons.codec.binary.Base64

public void init() {
	String dataid = context.getRequestParameter("dataid");
	MediaArchive mediaArchive = (MediaArchive)context.getPageValue("mediaarchive");
	
	Asset deleting = mediaArchive.getAsset(dataid);
	if(Boolean.valueOf( deleting.getValue("duplicate") ) )
	{
		//Search for guid
		String md5 = deleting.getValue("md5hex");
		Collection hits = mediaArchive.query("asset").match("md5hex", md5).search();
		if( hits.size() == 2)
		{
			for(Data asset in hits)
			{
				if( !dataid.equals( asset.getId()  ) )
				{
					asset = mediaArchive.getAssetSearcher().loadData(asset);
					asset.setValue("duplicate","false");
					mediaArchive.saveAsset(asset);
				}
			}
		}
	
	}
	
}

init();






public checkViz(){
	log.info("Starting Pre-Deletion Event");
	MediaArchive mediaArchive = (MediaArchive)context.getPageValue("mediaarchive");
	Searcher targetsearcher = mediaArchive.getAssetSearcher();
	String assetids = context.getRequestParameter("assetids");
	
	Collection hits = mediaarchive.getAssetSearcher().query().orgroup("id",assetids).search();
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
			asset.setValue("vizoneretention", "remove");
			targetsearcher.saveData(asset);
			vizone.setMetadata(mediaArchive, "http://vme.mtl.cbc.ca/", asset, authString );
			
		}
		
	}
	
}

checkViz();

