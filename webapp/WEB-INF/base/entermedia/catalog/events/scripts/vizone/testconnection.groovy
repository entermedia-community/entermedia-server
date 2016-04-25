package vizone

import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.methods.PutMethod
import org.entermediadb.asset.MediaArchive
import org.openedit.WebPageRequest
import org.openedit.page.Page

public class VizOne{
	def authString = "EMDEV:3nterMed1a".getBytes().encodeBase64().toString();
	protected ThreadLocal perThreadCache = new ThreadLocal();
	public void testLoadAsset(){
		def addr       = "http://vizmtlvamf.media.in.cbcsrc.ca/thirdparty/asset/item?num=50"
		//def addr       = "http://vizmtlvamf.media.in.cbcsrc.ca/thirdparty/asset/item?num=1"
		def conn = addr.toURL().openConnection()
		conn.setRequestProperty( "Authorization", "Basic ${authString}" )
		conn.setRequestProperty("Accept", "application/atom+xml;type=feed");

		String content = conn.content.text;
		println content;

		if( conn.responseCode == 200 ) {
			def rss = new XmlSlurper().parseText(content  )


			println rss.title
			rss.entry.link.each { println "- ${it.@rel}" }
		}

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
		def addr2 = "http://vizmtlvamf.media.in.cbcsrc.ca/thirdparty/asset/item?start=1&num=20";

		 conn = addr2.toURL().openConnection();
		conn.setRequestProperty( "Authorization", "Basic ${authString}" );
		conn.setRequestProperty("Accept", "Accept: application/atom+xml;type=feed");
		 String results =  conn.content.text;
		if( conn.responseCode == 200 ) {
			def rss = new XmlSlurper().parseText(results  )


			println rss.Url.@template;
			String url = rss.Url.@template;

			//rss.entry.link.each { println "- ${it.@rel}" }
		}
	}
	
	
	
	
	public void testCreateRecord(){
		
				//	curl --insecure --user "$VMEUSER:$VMEPASS" --include --header "Accept: application/opensearchdescription+xml" "https://vmeserver/thirdparty/asset/item?format=opensearch"
				def addr       = "http://vizmtlvamf.media.in.cbcsrc.ca/thirdparty/asset/item"
			
				
				
				String data = "<atom:entry xmlns:atom='http://www.w3.org/2005/Atom'>  <atom:title>Entermedia Test</atom:title><mam:retentionpolicy>oneweek</mam:retentionpolicy></atom:entry>";
				
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
		
		def addr       = "http://vizmtlvamf.media.in.cbcsrc.ca/api/asset/item/2101604190011378421/upload"
		
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
	
	
	public HttpClient getClient(String inCatalogId)
	{
		HttpClient ref = new HttpClient();
		
		
		
		
		
		return ref;
	}
	
	
}


VizOne vz = new VizOne();


vz.testCreateRecord();
//vz.testLoadAsset();
//vz.testUpload(context);

//vz.testSearch();


