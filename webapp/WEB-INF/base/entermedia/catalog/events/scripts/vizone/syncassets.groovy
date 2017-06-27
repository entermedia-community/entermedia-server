package vizone

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.http.client.HttpClient
import org.apache.http.client.config.CookieSpecs
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.entermediadb.asset.Asset
import org.entermediadb.asset.Category
import org.entermediadb.asset.ChunkySourcePathCreator
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.SourcePathCreator
import org.entermediadb.asset.scanner.MetaDataReader
import org.entermediadb.asset.search.AssetSearcher
import org.entermediadb.email.ElasticPostMail
import org.entermediadb.modules.update.Downloader
import org.openedit.WebPageRequest
import org.openedit.page.Page

public class VizOne{
	
	private static final Log log = LogFactory.getLog(ElasticPostMail.class);
	
	def authString = "EMDEV:3nterMed1a".getBytes().encodeBase64().toString();
	protected ThreadLocal perThreadCache = new ThreadLocal();
	public void testLoadAsset(WebPageRequest inReq){
		//def addr       = "http://vizmtlvamf.media.in.cbcsrc.ca/thirdparty/asset/item?id=50"
		def addr       = "http://vizmtlvamf.media.in.cbcsrc.ca/thirdparty/asset/item?start=1&num=100"
		def conn = addr.toURL().openConnection()
		conn.setRequestProperty( "Authorization", "Basic ${authString}" )
		conn.setRequestProperty("Accept", "application/atom+xml;type=feed");

		String content = conn.content.text;
		log.info(content);
		MediaArchive archive = inReq.getPageValue("mediaarchive");
		AssetSearcher assetsearcher = archive.getAssetSearcher();
		ArrayList assets = new ArrayList();
		if( conn.responseCode == 200 ) {
			def rss = new XmlSlurper().parseText(content  )

			//<core:ardomeIdentity name="id">2101611020017917621</core:ardomeIdentity>
			println rss.title

			rss.entry.each {
				try{
					if("deleted".equals(it.mediastatus.text())){
						return;
					}
					String vizid =it.ardomeIdentity;
					
					Asset asset = assetsearcher.searchByField("vizid", vizid);
					
					def url = it.'**'.find { link-> link.@rel == 'describedby'};
					
					String href= url.@href;
				
					
					
					def conn2 = href.toURL().openConnection()
					conn2.setRequestProperty( "Authorization", "Basic ${authString}" )
					conn2.setRequestProperty("Accept", "application/vnd.vizrt.payload+xml");
					
					String mdata = conn2.content.text;
					def extradata = new XmlSlurper().parseText(mdata  )
					
					
					
					
					
					if(asset == null){
						
						
						asset = assetsearcher.createNewData();

						
						
						
						asset.setValue("vizid", vizid);
						String itContent = it.content.@src;
						asset.setValue("fetchurl", itContent);
					
						
					extradata.field.each{
						String name = it.@name
						def val = it.'value';
						
						String value = it.'value'.text();
						if(name.contains("importFileName")){
							asset.setName(value);
						}
						else if(name.contains("retentionPolicy")){
							asset.setValue("vizoneretention", value);
						}
					}
					
						
						
						asset.setValue("importstatus", "needsdownload");
						SourcePathCreator creator = new ChunkySourcePathCreator();
						
						String sourcepath = creator.createSourcePath(asset, vizid);
						asset.setSourcePath("vizone/${sourcepath}");
						assets.add(asset);

						Downloader dl = new Downloader();
						String path = "/WEB-INF/data/"	+ archive.getCatalogId() + "/originals/" + asset.getSourcePath()			+ "/" + asset.getName();

						Page finalfile = archive.getPageManager().getPage(path);
						File image = new File(finalfile.getContentItem().getAbsolutePath());
						dl.download(itContent, image);
						asset.setPrimaryFile(asset.getName());
						MetaDataReader reader = archive.getModuleManager().getBean("metaDataReader");
						reader.populateAsset(archive, finalfile.getContentItem(), asset);

						asset.setValue("importstatus","imported");
						asset.setValue("fromviz",true);
						
						def tasksearcher = archive.getSearcher("conversiontask");

						archive.fireMediaEvent( "importing","assetsimported", null, asset); //this will save the asset as imported
					}
					
					
					String itTitle = it.title.text();
					asset.setValue("assettitle", itTitle);
					asset.setName(it.derivativefilename.text());

					
					
//					<field name="vpm.importFileName">
//					<value>photo 3x4.JPG</value>
//				  </field>
//				  <field name="asset.title">
//					<value>photo 3x4.JPG</value>
//				  </field>
//				  <field name="asset.retentionPolicy">
//					<value>default</value>
//				  </field>
					
					extradata.field.each{
						String name = it.@name
						def val = it.'value';
						
						String value = it.'value'.text();
						if(name.contains("importFileName")){
							asset.setName(value);
						}
						else if(name.contains("retentionPolicy")){
							asset.setValue("vizoneretention", value);
						}
					}
					
										
					Category cat = archive.getCategorySearcher().createCategoryPath("Vizone");
					asset.addCategory(cat);
					archive.saveAsset(asset,null);
					
					//Always update metadata  and save asset in case VIZ metadata changed?
					
					
					
					
					
					
					

				} catch(Exception e){

					log.info("Skipped " +it.ardomeIdentity + ": " + e.getMessage());



				}
			}


		}

	}







	public HttpClient getClient(String inCatalogId)
	{
		RequestConfig globalConfig = RequestConfig.custom()
				.setCookieSpec(CookieSpecs.DEFAULT)
				.build();
		CloseableHttpClient httpClient = HttpClients.custom()
				.setDefaultRequestConfig(globalConfig)
				.build();

		return httpClient;



	}

	
	
	

}


VizOne vz = new VizOne();
//vz.testModels();

//vz.testMetadata();

//vz.testCreateRecord();

vz.testLoadAsset(context);
//vz.testUpload(context);

//vz.testSearch();


