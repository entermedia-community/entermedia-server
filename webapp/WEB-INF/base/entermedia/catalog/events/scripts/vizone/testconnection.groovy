package vizone

import java.lang.ClassValue.Identity;

import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.methods.PutMethod
import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.search.AssetSearcher
import org.openedit.WebPageRequest
import org.openedit.page.Page

public class VizOne{
	def authString = "EMDEV:3nterMed1a".getBytes().encodeBase64().toString();
	protected ThreadLocal perThreadCache = new ThreadLocal();
	public void testLoadAsset(WebPageRequest inReq){
		//def addr       = "http://vizmtlvamf.media.in.cbcsrc.ca/thirdparty/asset/item?id=50"
		def addr       = "http://vizmtlvamf.media.in.cbcsrc.ca/thirdparty/asset/item?start=2&num=500"
		def conn = addr.toURL().openConnection()
		conn.setRequestProperty( "Authorization", "Basic ${authString}" )
		conn.setRequestProperty("Accept", "application/atom+xml;type=feed");

		String content = conn.content.text;
		println content;
		MediaArchive archive = inReq.getPageValue("mediaarchive");
		AssetSearcher assetsearcher = archive.getAssetSearcher();
		ArrayList assets = new ArrayList();
		if( conn.responseCode == 200 ) {
			def rss = new XmlSlurper().parseText(content  )

			//<core:ardomeIdentity name="id">2101611020017917621</core:ardomeIdentity>
			println rss.title

			rss.entry.each {

				String vizid =it.ardomeIdentity;

				Asset asset = assetsearcher.searchByField("vizid", vizid);
				if(asset == null){
					asset = assetsearcher.createNewData();
					asset.setValue("vizid", vizid);
					String itContent = it.content.@src;
					asset.setValue("fetchurl", itContent);
					String itTitle = it.title.text();
					asset.setValue("assettitle", itTitle);
					asset.setName(it.derivativefilename.text());
					asset.setValue("importstatus", "needsdownload");
					asset.setSourcePath("vizone/${vizid}");
					assets.add(asset);
				}
			}


		}
		assetsearcher.saveAllData(assets,null);

	}





	public void testSearch(){

		//	curl --insecure --user "$VMEUSER:$VMEPASS" --include --header "Accept: application/opensearchdescription+xml" "https://vmeserver/thirdparty/asset/item?format=opensearch"
		def addr       = "http://vizmtlvamf.media.in.cbcsrc.ca/thirdparty/asset/item?format=opensearch"
		def conn = addr.toURL().openConnection();
		conn.setRequestProperty( "Authorization", "Basic ${authString}" );
		conn.setRequestProperty("Accept", "Accept: application/opensearchdescription+xml");

		String content = conn.content.text;
		//println content;

		if( conn.responseCode == 200 ) {
			def rss = new XmlSlurper().parseText(content  )


			println rss.Url.@template;
			String url = rss.Url.@template;

			//rss.entry.link.each { println "- ${it.@rel}" }
		}
		//	Accept: application/atom+xml;type=feed" "https://vmeserver/thirdparty/asset/item?start=1&num=20&sort=-search.modificationDate&q=breakthrough
		def addr2 = "http://vizmtlvamf.media.in.cbcsrc.ca/thirdparty/asset/item?start=1&num=1000";

		conn = addr2.toURL().openConnection();
		conn.setRequestProperty( "Authorization", "Basic ${authString}" );
		conn.setRequestProperty("Accept", "Accept: application/atom+xml;type=feed");
		String results =  conn.content.text;
		if( conn.responseCode == 200 ) {
			def rss = new XmlSlurper().parseText(results  )


			println rss.Url.@template;
			String url = rss.Url.@template;
			println
			//<core:ardomeIdentity name="id">2101611020017917621</core:ardomeIdentity>
			//rss.entry.link.each { println "- ${it.@rel}" }
		}
	}




	public void testCreateRecord(){

		//	curl --insecure --user "$VMEUSER:$VMEPASS" --include --header "Accept: application/opensearchdescription+xml" "https://vmeserver/thirdparty/asset/item?format=opensearch"
		def addr       = "http://vizmtlvamf.media.in.cbcsrc.ca/thirdparty/asset/item"



		String data = "<atom:entry xmlns:atom='http://www.w3.org/2005/Atom'>  <atom:title>Entermedia Test</atom:title></atom:entry>";

		PostMethod method = new PostMethod(addr);

		//method.setRequestEntity(new ByteArrayEntity(data.bytes));
		method.setRequestBody(data);


		method.setRequestHeader( "Authorization", "Basic ${authString}" );
		method.setRequestHeader( "Expect", "" );
		method.setRequestHeader("Content-Type", "application/atom+xml;type=entry");

		int status = getClient("test").executeMethod(method);

		//FilePart part = new FilePart(type + count, name, new File( file.getAbsolutePath() ));
		//	parts.add(part);
		//	count++;
		//}
		String response = method.getResponseBodyAsString();
		println status;

		//	Accept: application/atom+xml;type=feed" "https://vmeserver/thirdparty/asset/item?start=1&num=20&sort=-search.modificationDate&q=breakthrough
	}



	public void testUpload(WebPageRequest inReq){
		MediaArchive archive = inReq.getPageValue("mediaarchive");

		def addr       = "http://vizmtlvamf.media.in.cbcsrc.ca/api/asset/item/2101605090012023821/upload"

		PutMethod method = new PutMethod(addr);
		method.setRequestHeader( "Authorization", "Basic ${authString}" );
		method.setRequestHeader( "Expect", "" );

		String response = method.getResponseBodyAsString();

		int status = getClient("test").executeMethod(method);
		String location = method.getResponseHeader("Location");
		String addr2 = location.replace("Location:", "").trim();



		PutMethod upload = new PutMethod(addr2);
		upload.setRequestHeader( "Authorization", "Basic ${authString}" );
		upload.setRequestHeader( "Expect", "" );
		Page page = archive.getPageManager().getPage("/EMS1.png");
		upload.setRequestBody(page.getInputStream());

		status = getClient("test").executeMethod(upload);



		println status;


	}



	public void testMetadata(){

		//	curl --insecure --user "$VMEUSER:$VMEPASS" --include --header "Accept: application/opensearchdescription+xml" "https://vmeserver/thirdparty/asset/item?format=opensearch"

		//	##<atom:link rel="models" type="application/atom+xml;type=feed" href="http://vizmtlvamf.media.in.cbcsrc.ca/api/metadata/form?qType=ASSET.Item&amp;qId=2101605090012023821"/>




		def addr       = "http://vizmtlvamf.media.in.cbcsrc.ca/api/asset/item/2101605090012023821/metadata"



		String data = "<payload xmlns='http://www.vizrt.com/types' model=\"http://vizmtlvamf.media.in.cbcsrc.ca/api/metadata/form/vpm-item/r1\">  <field name='asset.title'>    <value>Metadata breakthrough</value>  </field>   <field name='asset.retentionPolicy'>    <value>oneweek</value>  </field>     </payload>";

		PutMethod method = new PutMethod(addr);

		//method.setRequestEntity(new ByteArrayEntity(data.bytes));
		method.setRequestBody(data);


		method.setRequestHeader( "Authorization", "Basic ${authString}" );
		method.setRequestHeader( "Expect", "" );
		method.setRequestHeader("Content-Type", "application/vnd.vizrt.payload+xml");

		int status = getClient("test").executeMethod(method);

		//FilePart part = new FilePart(type + count, name, new File( file.getAbsolutePath() ));
		//	parts.add(part);
		//	count++;
		//}
		String response = method.getResponseBodyAsString();
		println status;

		//	Accept: application/atom+xml;type=feed" "https://vmeserver/thirdparty/asset/item?start=1&num=20&sort=-search.modificationDate&q=breakthrough
	}


	public void testModels(){

		//	curl --insecure --user "$VMEUSER:$VMEPASS" --include --header "Accept: application/opensearchdescription+xml" "https://vmeserver/thirdparty/asset/item?format=opensearch"

		//	##<atom:link rel="models" type="application/atom+xml;type=feed" href="http://vizmtlvamf.media.in.cbcsrc.ca/api/metadata/form?qType=ASSET.Item&amp;qId=2101605090012023821"/>




		def addr       = "http://vizmtlvamf.media.in.cbcsrc.ca/api/metadata/form?qType=ASSET.Item&amp;qId=2101605090012023821"



		//	String data = "<payload xmlns='http://www.vizrt.com/types' model='https://vmeserver/api/metadata/form/example/r1'>  <field name='asset.title'>    <value>Metadata breakthrough</value>  </field>  <field name='asset.retentionPolicy'>    <value>oneweek</value>  </field>    </payload>";

		GetMethod method = new GetMethod(addr);

		//method.setRequestEntity(new ByteArrayEntity(data.bytes));
		//method.setRequestBody(data);


		method.setRequestHeader( "Authorization", "Basic ${authString}" );
		method.setRequestHeader( "Expect", "" );
		method.setRequestHeader("Content-Type", "application/atom+xml;type=feed");

		int status = getClient("test").executeMethod(method);

		//FilePart part = new FilePart(type + count, name, new File( file.getAbsolutePath() ));
		//	parts.add(part);
		//	count++;
		//}
		String response = method.getResponseBodyAsString();
		println status;

		//<atom:link rel="self" type="application/vnd.vizrt.model+xml" href="http://vizmtlvamf.media.in.cbcsrc.ca/api/metadata/form/vpm-asset/r1?qId=2101605090012023821"/>


		addr       = "http://vizmtlvamf.media.in.cbcsrc.ca/api/metadata/form/vpm-asset/r1?qId=2101605090012023821"



		//	String data = "<payload xmlns='http://www.vizrt.com/types' model='https://vmeserver/api/metadata/form/example/r1'>  <field name='asset.title'>    <value>Metadata breakthrough</value>  </field>     </payload>";

		method = new GetMethod(addr);

		//method.setRequestEntity(new ByteArrayEntity(data.bytes));
		//method.setRequestBody(data);


		method.setRequestHeader( "Authorization", "Basic ${authString}" );
		method.setRequestHeader( "Expect", "" );
		method.setRequestHeader("Content-Type", "application/vnd.vizrt.model+xml");

		status = getClient("test").executeMethod(method);


		String response2 = method.getResponseBodyAsString();
		println status;


		//	Accept: application/atom+xml;type=feed" "https://vmeserver/thirdparty/asset/item?start=1&num=20&sort=-search.modificationDate&q=breakthrough
	}






	public HttpClient getClient(String inCatalogId)
	{
		HttpClient ref = new HttpClient();





		return ref;
	}


}


VizOne vz = new VizOne();
vz.testModels();

vz.testMetadata();

//vz.testCreateRecord();

vz.testLoadAsset(context);
//vz.testUpload(context);

vz.testSearch();


