package org.entermediadb.asset.publish.publishers;


import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.publishing.BasePublisher;
import org.entermediadb.asset.publishing.PublishResult;
import org.entermediadb.asset.publishing.Publisher;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.page.Page;
import org.openedit.users.User;
import org.openedit.users.UserManager;
import org.openedit.util.XmlUtil;


public class vizonepublisher extends BasePublisher implements Publisher
{

	
	private static final Log log = LogFactory.getLog(vizonepublisher.class);
	protected XmlUtil fieldXmlUtil;

	
	
	public PublishResult publish(MediaArchive inMediaArchive, Asset inAsset, Data inPublishRequest, Data inDestination, Data inPreset)
	{
	
		try
		{
			PublishResult result = checkOnConversion(inMediaArchive,inPublishRequest,inAsset,inPreset);
			if( result != null)
			{
				return result;
			}
			
			result = new PublishResult();

			Page inputpage = findInputPage(inMediaArchive,inAsset,inPreset);
			String servername = inDestination.get("server");
			String username = inDestination.get("username");
			String url = inDestination.get("url");
			
			log.info("Publishing ${asset} to ftp server ${servername}, with username ${username}.");
			
		String password = inDestination.get("password");
			//get password and login
			if(password == null)
			{
				UserManager userManager = inMediaArchive.getUserManager();
				User user = userManager.getUser(username);
				password = userManager.decryptPassword(user);
			}
				
			byte[] encodedBytes = Base64.encodeBase64("${username}:${password}".getBytes());
			String authString = new String(encodedBytes);
			
			String vizid = inAsset.get("vizid");
			
			if(vizid == null){
				Element results = createAsset(inDestination, inAsset);
				String id = results.element("id").getText();
				inAsset.setProperty("vizid", vizid);
			}
			
			
			result.setComplete(true);
			log.info("publishished  ${asset} to FTP server ${servername}");
			return result;
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}
		
		
		
	}

	
	
	
//	public void testLoadAsset(){
//		def addr       = "http://vizmtlvamf.media.in.cbcsrc.ca/thirdparty/asset/item/2101604190011378421"
//		//def addr       = "http://vizmtlvamf.media.in.cbcsrc.ca/thirdparty/asset/item?num=1"
//		def conn = addr.toURL().openConnection()
//		conn.setRequestProperty( "Authorization", "Basic ${authString}" )
//		conn.setRequestProperty("Accept", "application/atom+xml;type=feed");
//
//		String content = conn.content.text;
//		//println content;
//
//		if( conn.responseCode == 200 ) {
//			def rss = new XmlSlurper().parseText(content  )
//
//
//			println rss.title
//			rss.entry.link.each { println "- ${it.@rel}" }
//		}
//
//	}


//	public void testSearch(){
//
//		//	curl --insecure --user "$VMEUSER:$VMEPASS" --include --header "Accept: application/opensearchdescription+xml" "https://vmeserver/thirdparty/asset/item?format=opensearch"
//		def addr       = "http://vizmtlvamf.media.in.cbcsrc.ca/thirdparty/asset/item?format=opensearch"
//		def conn = addr.toURL().openConnection();
//		conn.setRequestProperty( "Authorization", "Basic ${authString}" );
//		conn.setRequestProperty("Accept", "Accept: application/opensearchdescription+xml");
//
//		String content = conn.content.text;
//		//println content;
//
//		if( conn.responseCode == 200 ) {
//			def rss = new XmlSlurper().parseText(content  )
//
//
//			println rss.Url.@template;
//			String url = rss.Url.@template;
//
//			//rss.entry.link.each { println "- ${it.@rel}" }
//		}
//		//	Accept: application/atom+xml;type=feed" "https://vmeserver/thirdparty/asset/item?start=1&num=20&sort=-search.modificationDate&q=breakthrough
//		def addr2 = "http://vizmtlvamf.media.in.cbcsrc.ca/thirdparty/asset/item?start=1&num=20";
//
//		 conn = addr2.toURL().openConnection();
//		conn.setRequestProperty( "Authorization", "Basic ${authString}" );
//		conn.setRequestProperty("Accept", "Accept: application/atom+xml;type=feed");
//		 String results =  conn.content.text;
//		if( conn.responseCode == 200 ) {
//			def rss = new XmlSlurper().parseText(results  )
//
//
//			println rss.Url.@template;
//			String url = rss.Url.@template;
//
//			//rss.entry.link.each { println "- ${it.@rel}" }
//		}
//	}
	
	
	
	
	public Element createAsset(Data inDestination, Asset inAsset) throws Exception{
		
				//	curl --insecure --user "$VMEUSER:$VMEPASS" --include --header "Accept: application/opensearchdescription+xml" "https://vmeserver/thirdparty/asset/item?format=opensearch"
				
		
				String servername = inDestination.get("server");
				String addr       = servername + "thirdparty/asset/item";
							
				String data = "<atom:entry xmlns:atom='http://www.w3.org/2005/Atom'>  <atom:title>Entermedia Test</atom:title></atom:entry>";
			
				
				
				PostMethod method = new PostMethod(addr);
				
				//method.setRequestEntity(new ByteArrayEntity(data.bytes));
				method.setRequestBody(data);
				
				
				method.setRequestHeader( "Authorization", "Basic ${authString}" );
				method.setRequestHeader( "Expect", "" );
				method.setRequestHeader("Content-Type", "application/atom+xml;type=entry");
				
				int status = getClient("test").executeMethod(method);
				
				Element response = getXmlUtil().getXml(method.getResponseBodyAsStream(), "UTF-8");
				return response;
			
				//	Accept: application/atom+xml;type=feed" "https://vmeserver/thirdparty/asset/item?start=1&num=20&sort=-search.modificationDate&q=breakthrough
	}
	
	
	
	public void testUpload(WebPageRequest inReq){
		try
		{
			MediaArchive archive = (MediaArchive) inReq.getPageValue("mediaarchive");
			
			String  addr       = "http://vizmtlvamf.media.in.cbcsrc.ca/api/asset/item/2101604190011378421/upload";
			
			PutMethod method = new PutMethod(addr);
			method.setRequestHeader( "Authorization", "Basic ${authString}" );
			method.setRequestHeader( "Expect", "" );
			
			String response = method.getResponseBodyAsString();
			
			int status = getClient("test").executeMethod(method);
			String addr2 = method.getResponseHeader("Location").getValue();
			
			
			
			
			PutMethod upload = new PutMethod(addr2);
			upload.setRequestHeader( "Authorization", "Basic ${authString}" );
			upload.setRequestHeader( "Expect", "" );
			Page page = archive.getPageManager().getPage("/EMS1.png");
			upload.setRequestBody(page.getInputStream());
			
			 status = getClient("test").executeMethod(upload);
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}
		
		
		
		
	
		
		
	}
	
	
	public HttpClient getClient(String inCatalogId)
	{
		HttpClient ref = new HttpClient();
		
		
		
		
		
		return ref;
	}
	
	public XmlUtil getXmlUtil()
	{
		if (fieldXmlUtil == null)
		{
			fieldXmlUtil = new XmlUtil();
		}
		return fieldXmlUtil;
	}


	public void setXmlUtil(XmlUtil inXmlUtil)
	{
		fieldXmlUtil = inXmlUtil;
	}

	
}
