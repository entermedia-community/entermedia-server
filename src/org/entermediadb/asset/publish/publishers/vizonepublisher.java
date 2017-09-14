package org.entermediadb.asset.publish.publishers;


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.dom4j.Element;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.publishing.BasePublisher;
import org.entermediadb.asset.publishing.PublishResult;
import org.entermediadb.asset.publishing.Publisher;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.data.PropertyDetail;
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
			String enc = username + ":" + password;
			byte[] encodedBytes = Base64.encodeBase64(enc.getBytes());
			String authString = new String(encodedBytes);
			
			String vizid = inAsset.get("vizid");
			
			if(vizid == null){
				Element results = createAsset(inMediaArchive, inDestination, inAsset, authString);
				String id = results.element("id").getText();
				String[] splits = id.split(":");
				
				inAsset.setProperty("vizid", splits[4]);
				String identifier = results.element("identifier").getText();
				inAsset.setValue("vizidentifier", identifier);
				
				inMediaArchive.saveAsset(inAsset, null);
			}
			
			uploadAsset(inMediaArchive, result, inAsset, inDestination, inPreset, authString);
			//http://vizmtlvamf.media.in.cbcsrc.ca/api/asset/item/2101604250011569821/metadata
			setMetadata(inMediaArchive,  inDestination.get("url"), inAsset, authString);
			
			result.setComplete(true);
			log.info("publishished  ${asset} to FTP server ${servername}");
			return result;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new OpenEditException(e);
		}
		
		
		
	}

	
	
	public void updateAsset(MediaArchive inArchive, String servername, Asset inAsset, String inAuthString) throws Exception{
		
		//	curl --insecure --user "$VMEUSER:$VMEPASS" --include --header "Accept: application/opensearchdescription+xml" "https://vmeserver/thirdparty/asset/item?format=opensearch"
		

		
		String addr       = servername + "api/asset/item/" + inAsset.get("vizid") + "/metadata";


		
		
		
		//Change URL - 
		//String data = "<payload xmlns='http://www.vizrt.com/types' model=\"http://vizmtlvamf.media.in.cbcsrc.ca/api/metadata/form/vpm-item/r1\"><field name='asset.title'><value>${title}</value></field> <field name='asset.owner'><value>Img</value></field>  <field name='asset.retentionPolicy'>    <value>${policy}</value>  </field>     </payload>";

		HttpGet get = new HttpGet(addr);
		
		get.setHeader("Content-Type", "application/vnd.vizrt.payload+xml");
		get.setHeader("Authorization", "Basic " + inAuthString);
		get.setHeader("Expect", "" );

		HttpResponse response = getClient().execute(get);
		StatusLine sl = response.getStatusLine();           
		int status = sl.getStatusCode();
		if( status>=400)
		{
			throw new OpenEditException("error from server " + status + "  " + sl.getReasonPhrase());
		}
		
		Element elem = getXmlUtil().getXml(response.getEntity().getContent(), "UTF-8");
		
		for (Iterator iterator = elem.elementIterator("field"); iterator.hasNext();)
		{
			Element field = (Element) iterator.next();
			String name = field.attributeValue("name");
			Element value = field.element("value");
			String current = value.getText();
			
			PropertyDetail detail = inArchive.getAssetSearcher().getPropertyDetails().getDetailByProperty("vizonefield", name);
			if(detail != null){
				inAsset.setValue(detail.getId(),current);
			}
			
		}
		
		
		
		
	
		//	Accept: application/atom+xml;type=feed" "https://vmeserver/thirdparty/asset/item?start=1&num=20&sort=-search.modificationDate&q=breakthrough
}
	
	
	
	
	
	
	
	public Element setMetadata(MediaArchive inArchive, String servername, Asset inAsset, String inAuthString) throws Exception{
		
		//	curl --insecure --user "$VMEUSER:$VMEPASS" --include --header "Accept: application/opensearchdescription+xml" "https://vmeserver/thirdparty/asset/item?format=opensearch"
		

		
		String addr       = servername + "api/asset/item/" + inAsset.get("vizid") + "/metadata";


		
		
		
		//Change URL - 
		//String data = "<payload xmlns='http://www.vizrt.com/types' model=\"http://vizmtlvamf.media.in.cbcsrc.ca/api/metadata/form/vpm-item/r1\"><field name='asset.title'><value>${title}</value></field> <field name='asset.owner'><value>Img</value></field>  <field name='asset.retentionPolicy'>    <value>${policy}</value>  </field>     </payload>";

		HttpGet get = new HttpGet(addr);
		
		get.setHeader("Content-Type", "application/vnd.vizrt.payload+xml");
		get.setHeader("Authorization", "Basic " + inAuthString);
		get.setHeader("Expect", "" );

		HttpResponse response = getClient().execute(get);
		StatusLine sl = response.getStatusLine();           
		int status = sl.getStatusCode();
		if( status>=400)
		{
			throw new OpenEditException("error from server " + status + "  " + sl.getReasonPhrase());
		}
		
		Element elem = getXmlUtil().getXml(response.getEntity().getContent(), "UTF-8");
		
		for (Iterator iterator = elem.elementIterator("field"); iterator.hasNext();)
		{
			Element field = (Element) iterator.next();
			String name = field.attributeValue("name");
			Element value = field.element("value");
			String current = value.getText();
			
			PropertyDetail detail = inArchive.getAssetSearcher().getPropertyDetails().getDetailByProperty("vizonefield", name);
			if(detail != null){
				String assetvalue = inAsset.get(detail.getId());
				value.setText(assetvalue);
			}
			
		}
		
		
		
		HttpPut method = new HttpPut(addr);
		method.setHeader("Content-Type", "application/vnd.vizrt.payload+xml");
		method.setHeader("Authorization", "Basic " + inAuthString);
		method.setHeader("Expect", "" );

		StringEntity params = new StringEntity(elem.asXML());
		method.setEntity(params);
		
		HttpResponse response2 = getClient().execute(method);
		 sl = response2.getStatusLine();           
		 status = sl.getStatusCode();
		if( status>=400)
		{
			throw new OpenEditException("error from server " + status + "  " + sl.getReasonPhrase());
		}
		Element response3 = getXmlUtil().getXml(response2.getEntity().getContent(), "UTF-8");
		//String xml = response.asXML();
		return response3;
	
		//	Accept: application/atom+xml;type=feed" "https://vmeserver/thirdparty/asset/item?start=1&num=20&sort=-search.modificationDate&q=breakthrough
}
	
	
	
	
	public Element createAsset(MediaArchive inArchive, Data inDestination, Asset inAsset, String inAuthString) throws Exception{
		
				//	curl --insecure --user "$VMEUSER:$VMEPASS" --include --header "Accept: application/opensearchdescription+xml" "https://vmeserver/thirdparty/asset/item?format=opensearch"
				
		
				String servername = inDestination.get("url");
				String addr       = servername + "thirdparty/asset/item";
//				urn:vme:default:dictionary:retentionpolicy:oneweek

				String data = "<atom:entry xmlns:atom='http://www.w3.org/2005/Atom'>  <atom:title>Title: ${title}</atom:title>"
						+ "<identifier>${name}</identifier>"
					

						+ "</atom:entry>";
				Map metadata = inAsset.getProperties();
				Calendar now = new GregorianCalendar();
				now.add(now.DAY_OF_YEAR, 7);
				Date oneweek = now.getTime();
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
				String date = format.format(oneweek);
				
				
				metadata.put("${date}",date );
				
				data = inArchive.getReplacer().replace(data, metadata);
				
				
				
				
				HttpPost method = new HttpPost(addr);
				
				//method.setEntity(new ByteArrayEntity(data.bytes));
				StringEntity params = new StringEntity(data);
//				method.setEntity(params);

				//HttpEntity entity = new ByteArrayEntity(data.getBytes("UTF-8"));
				//StringEntity xmlEntity = new StringEntity(data, "application/atom+xml;type=entry","UTF-8");
			
				
				
				
		        method.setEntity(params);
				method.setHeader( "Authorization", "Basic " + inAuthString);
				method.setHeader( "Expect", "" );
				method.setHeader("Content-Type", "application/atom+xml;type=entry");
				
				HttpResponse response2 = getClient().execute(method);
				StatusLine sl = response2.getStatusLine();           
				int status = sl.getStatusCode();
				if( status>=400)
				{
					throw new OpenEditException("error from server " + status + "  " + sl.getReasonPhrase());
				}
				
				
				Element response = getXmlUtil().getXml(response2.getEntity().getContent(), "UTF-8");
				String xml = response.asXML();
				return response;
			
				//	Accept: application/atom+xml;type=feed" "https://vmeserver/thirdparty/asset/item?start=1&num=20&sort=-search.modificationDate&q=breakthrough
	}
	
	
	
	public void uploadAsset(MediaArchive archive, PublishResult inResult , Asset inAsset,Data inDestination, Data inPreset, String inAuthString){
		try
		{
			Page inputpage = findInputPage(archive,inAsset,inPreset);

			String servername = inDestination.get("url");
			String addr       = servername + "api/asset/item/" + inAsset.get("vizid") + "/upload";
						
			
			
			HttpPut method = new HttpPut(addr);
			method.setHeader( "Authorization", "Basic " + inAuthString );
			method.setHeader( "Expect", "" );
			
			
			HttpResponse response2 =  getClient().execute(method);
			
			String addr2 = response2.getFirstHeader("Location").getValue();
			
			
			
			HttpPut upload = new HttpPut(addr2);
			upload.setHeader( "Authorization", "Basic " + inAuthString );
			upload.setHeader( "Expect", "" );
			
			FileEntity entity = new FileEntity(new File(inputpage.getContentItem().getAbsolutePath()));
			upload.setEntity(entity);
			

			HttpResponse response3 = getClient().execute(upload);
			
			StatusLine sl = response3.getStatusLine();           
			int status = sl.getStatusCode();
			if( status>=400)
			{
				throw new OpenEditException("error from server " + status + "  " + sl.getReasonPhrase());
			}
			
			
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}
		
		
		
		
	
		
		
	}
	
	
	public HttpClient getClient()
	{
		  
			
		  RequestConfig globalConfig = RequestConfig.custom()
	                .setCookieSpec(CookieSpecs.DEFAULT)
	                .build();
	        CloseableHttpClient httpClient = HttpClients.custom()
	                .setDefaultRequestConfig(globalConfig)
	                .build();
		
		return httpClient;
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
