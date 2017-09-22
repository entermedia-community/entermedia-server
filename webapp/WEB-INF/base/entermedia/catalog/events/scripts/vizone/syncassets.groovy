package vizone

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.entermediadb.asset.Asset
import org.entermediadb.asset.Category
import org.entermediadb.asset.ChunkySourcePathCreator
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.SourcePathCreator
import org.entermediadb.asset.scanner.MetaDataReader
import org.entermediadb.asset.search.AssetSearcher
import org.entermediadb.modules.update.Downloader
import org.openedit.WebPageRequest
import org.openedit.hittracker.HitTracker
import org.openedit.page.Page

public class VizOne{

	private static final Log log = LogFactory.getLog(VizOne.class);

	def authString = "EMDEV:3nterMed1a".getBytes().encodeBase64().toString();
	protected ThreadLocal perThreadCache = new ThreadLocal();
	public void downloadNew(WebPageRequest inReq){

	
		def addr       = "http://vizmtlvamf.media.in.cbcsrc.ca/thirdparty/asset/item?start=1&num=10000"
		def conn = addr.toURL().openConnection()
		conn.setRequestProperty( "Authorization", "Basic ${authString}" )
		conn.setRequestProperty("Accept", "application/atom+xml;type=feed");

		String content = conn.content.text;
		//log.info(content);
		MediaArchive archive = inReq.getPageValue("mediaarchive");
		def viz = archive.getModuleManager().getBean(archive.getCatalogId(), "VizOnepublisher");
		
		AssetSearcher assetsearcher = archive.getAssetSearcher();
		ArrayList assets = new ArrayList();
		if( conn.responseCode == 200 ) {
			def rss = new XmlSlurper().parseText(content  )

			//<core:ardomeIdentity name="id">2101611020017917621</core:ardomeIdentity>
			println rss.title

			rss.entry.each {
				try{
					

					
					String vizid =it.ardomeIdentity;

					Asset asset = assetsearcher.searchByField("vizid", vizid);

					
					if("deleted".equals(it.mediastatus.text()) && asset.getValue("fromviz") == true){
						if(asset){
						archive.deleteAsset(asset, true);
						}
						return;
					}
					
					
					
					
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
						
						if("remove".equals(asset.getValue("vizoneretention")))
						{
							return;
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
						asset.setValue("assetaddeddate", new Date());
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
					
					if("remove".equals(asset.getValue("vizoneretention")) && asset.getValue("fromviz") == true)
					{
						archive.deleteAsset(asset, true);
					}
					else{
					
					
						
					viz.updateAsset(archive,"http://vizmtlvamf.media.in.cbcsrc.ca/" , asset,authString);
					

					Category cat = archive.getCategorySearcher().searchById("AVyh5_tsmQeu4rFCDJ4S");
					asset.clearCategories();
					asset.addCategory(cat);
					
					

					archive.saveAsset(asset,null);

					//Always update metadata  and save asset in case VIZ metadata changed?

					}






				} catch(Exception e){

					log.info("Skipped " +it.ardomeIdentity + ": " + e.getMessage());



				}
			}


		}

	}




	public void validate(WebPageRequest inReq){
		//def addr       = "http://vizmtlvamf.media.in.cbcsrc.ca/thirdparty/asset/item?id=50"



		MediaArchive archive = inReq.getPageValue("mediaarchive");
		AssetSearcher assetsearcher = archive.getAssetSearcher();
		ArrayList todelete = new ArrayList();
		HitTracker assets = assetsearcher.query().match("vizid", "*").match("fromviz", "true").search();
		assets.each{
			def addr       = "http://vizmtlvamf.media.in.cbcsrc.ca/thirdparty/asset/item/${it.vizid}";

			def conn = addr.toURL().openConnection()
			conn.setRequestProperty( "Authorization", "Basic ${authString}" )
			conn.setRequestProperty("Accept", "application/atom+xml;type=entry");


			if( conn.responseCode == 200 ) {
				Asset asset = archive.getAsset(it.id);
				String content = conn.content.text;
			//	log.info(content);
				def rss = new XmlSlurper().parseText(content  );

				def url = rss.'**'.find { link-> link.@rel == 'describedby'};

				String href= url.@href;



				try{def conn2 = href.toURL().openConnection()
					conn2.setRequestProperty( "Authorization", "Basic ${authString}" )
					conn2.setRequestProperty("Accept", "application/vnd.vizrt.payload+xml");

					String mdata = conn2.content.text;
					def extradata = new XmlSlurper().parseText(mdata  )
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
				} catch(Exception e){
					//NO metadata available
				}


				assetsearcher.saveData(asset);



			} else if (conn.responseCode == 404){
				log.info("Will delete ${it.id} Got a 404" );

				Asset asset = archive.getAsset(it.id);
				archive.deleteAsset(asset, true);

			}



			else{

				log.info("Will delete ${it.id} - ");
				Asset asset = archive.getAsset(it.id);
				archive.deleteAsset(asset, true);
			}


		}

	

	}





}


VizOne vz = new VizOne();


vz.downloadNew(context);
vz.validate(context);



