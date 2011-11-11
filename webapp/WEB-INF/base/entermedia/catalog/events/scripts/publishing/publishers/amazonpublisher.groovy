package publishing.publishers
;

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.entermedia.amazon.S3Repository
import org.entermedia.aspera.AsperaManager
import org.entermedia.aspera.AsperaRepository
import org.openedit.Data
import org.openedit.data.*
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.Publisher
import org.openedit.repository.filesystem.StringItem

import com.openedit.page.Page

public class amazonpublisher extends basepublisher implements Publisher {
	private static final Log log = LogFactory.getLog(amazonpublisher.class);

	public void publish(MediaArchive mediaArchive,Data inOrder, Data inOrderItem, Asset asset) {
		S3Repository repo = (S3Repository)mediaArchive.getModuleManager().getBean("S3Repository");

		String publishdestination = inOrderItem.get("publishdestination");
		if( publishdestination == null) {
			publishdestination = inOrder.get("publishdestination");
		}
		String presetid = inOrderItem.get("presetid");
		if( presetid == null) {
			presetid = inOrder.get("presetid");
		}
		log.info("Publish asset to Amazon ${asset} for preset: ${presetid} on server: ${publishdestination}" );
		Searcher presetsearcher = mediaArchive.getSearcherManager().getSearcher(mediaArchive.getCatalogId(), "convertpreset");
		Page inputpage = findInputPage(mediaArchive,asset,presetid);
		
		if( publishdestination != null && inputpage.exists()) {

			Data destination = mediaArchive.getSearcherManager().getData( mediaArchive.getCatalogId(), "publishdestination", publishdestination);
			repo.setBucket(destination.bucket);
			repo.setAccessKey(destination.accesskey);
			repo.setSecretKey(destination.secretkey);

			//open the file and send it
			
			String exportname= inOrderItem.get("filename");
			
			String url = destination.get("url");
			if( url == null)
			{
				url = "/";
			}
			if( !url.endsWith("/"))
			{
				url = url + "/";
			}
			if( exportname.startsWith("/"))
			{
				exportname = exportname.substring(1);
			}
			StringItem item = new StringItem();
			item.setPath( url + exportname);
			item.setAbsolutePath(inputpage.getContentItem().getAbsolutePath());


			try
			{
				repo.put(item); //copy the file
				log.info("publishished  ${asset.sourcepath} to Amazon s3");
				asset.setProperty("publisheds3", "true");
				asset.setProperty("s3preset", presetid);
				mediaArchive.saveAsset(asset, null);
				inOrderItem.setProperty("status", "complete");
			}
			catch( Exception ex) {
				inOrderItem.setProperty("status", "publisherror");
				inOrderItem.setProperty("errordetails", "Amazon publish failed to server" + ex);
				log.info("Could not publish ${ex} ${asset.sourcepath}");
				ex.printStackTrace();
			}
			Searcher itemsearcher = mediaArchive.getSearcherManager().getSearcher(mediaArchive.getCatalogId(), "orderitem");
			itemsearcher.saveData(inOrderItem, null);
		}
	}

	public void publish(MediaArchive mediaArchive,Asset asset, Data inPublishRequest,  Data destination, Data preset){
		S3Repository repo = (S3Repository)mediaArchive.getModuleManager().getBean("S3Repository");
		log.info("Publish asset to Amazon ${asset} for preset: ${preset} on server: ${destination}" );
		Searcher presetsearcher = mediaArchive.getSearcherManager().getSearcher(mediaArchive.getCatalogId(), "convertpreset");

		repo.setBucket(destination.bucket);
		repo.setAccessKey(destination.accesskey);
		repo.setSecretKey(destination.secretkey);

		//open the file and send it
		String exportname = null;
		Page inputpage = null;
		if( preset.get("type") != "original")
		{
			String input= "/WEB-INF/data/${mediaArchive.catalogId}/generated/${asset.sourcepath}/${preset.outputfile}";
			inputpage= mediaArchive.getPageManager().getPage(input);
		}
		else
		{
			inputpage = mediaArchive.getOriginalDocument(asset);
		}
		
		
		if(!inputpage.exists()){
			return;
			//not ready to be published yet.
		}
		
		exportname = mediaArchive.asExportFileName( asset, preset);
		
		if( !exportname.startsWith("/"))
		{
			exportname = "/" + exportname;
		}
		StringItem item = new StringItem();
		item.setPath( exportname);
		item.setAbsolutePath(inputpage.getContentItem().getAbsolutePath());

		

		//try
		//{
			repo.put(item); //copy the file
			log.info("published  ${item.path} to Amazon s3");
			//asset.setProperty("publisheds3", "true");
			//asset.setProperty("s3preset", preset.id);
			//mediaArchive.saveAsset(asset, null);
			//inPublishRequest.setProperty("status", "complete");
		//Searcher itemsearcher = mediaArchive.getSearcherManager().getSearcher(mediaArchive.getCatalogId(), "publishqueue");
		//itemsearcher.saveData(inPublishRequest, null);
	}
}

