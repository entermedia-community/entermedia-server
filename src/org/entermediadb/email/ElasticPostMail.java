package org.entermediadb.email;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.mail.internet.InternetAddress;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.openedit.OpenEditException;
import org.openedit.page.Page;
import org.openedit.util.HttpRequestBuilder;


//uses ElasticMail to send instead of an SMTP server

public class ElasticPostMail extends PostMail
{

	private static final Log log = LogFactory.getLog(ElasticPostMail.class);
	
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

	//Combine BCC and CC
	public void postMail(List<InternetAddress> recipients, List<InternetAddress> blindrecipients, String subject, String inHtml, String inText, String from, List inAttachments, Map inProperties)
	{
		ArrayList<InternetAddress> list = new ArrayList<InternetAddress>();
		list.addAll(recipients);
		list.addAll(blindrecipients);
		postMail(list, subject, inHtml, inText, from, inAttachments, inProperties);
	}

	public void postMail(List<InternetAddress> recipients, String subject, String inHtml, String inText, String from, List inAttachments, Map inProperties)
	{
		HttpRequestBuilder builder = new HttpRequestBuilder();

		HttpPost postMethod = null;
		try
		{
			String fullpath = "https://api.elasticemail.com/mailer/send";
			postMethod = new HttpPost(fullpath);
			builder.addPart("username", getSmtpUsername());
			builder.addPart("api_key", getSmtpPassword());
			builder.addPart("from", from);
			builder.addPart("from_name", from);
			//make sure list of recipients is unique since it may combine cc and bcc
			ArrayList<String> list = new ArrayList<String>();
			for (InternetAddress str : recipients){
				if (!list.contains(str.getAddress())){
					list.add(str.getAddress());
				}
			}
			String finallist = list.toString().replace("[", "").replace("]", "").replace(",", ";").trim();
			builder.addPart("to", finallist);//email is sent separately to each recipient, ie. treated as BCC, so include BCC list
			builder.addPart("subject", subject);
			if (inHtml != null){
				builder.addPart("body_html", inHtml);
			}
			if (inText != null){
				builder.addPart("body_text", inText);
			}
			boolean hasAttachments = false;
			if (inAttachments!=null && !inAttachments.isEmpty()){
				String attachments = uploadAttachments(inAttachments);
				if (attachments!=null) {
					builder.addPart("attachments", attachments);
					hasAttachments = true;
				} else {
					log.info("Error uploading attachments, aborting");
					return;
				}
			}
			HttpClient client = getClient();
			//should check if attachments are being sent
			//if so, increase the socket timeout
//			int connectionTimeout = 5000; // 5 seconds default
//			int socketTimeout = 10000;//10 seconds default
//			if (hasAttachments){
//				socketTimeout = 60000; //60 seconds
//			}
//			client.getParams().setSoTimeout(socketTimeout);
//			client.getParams().setConnectionManagerTimeout(connectionTimeout);
			StatusLine line  = client.execute(postMethod).getStatusLine();
			int statuscode = line.getStatusCode();
			if (statuscode == 200)
			{
				//need to save response
				String response = IOUtils.toString(postMethod.getEntity().getContent());
				if (inProperties != null){
					inProperties.put(PostMailStatus.ID, response);
				}
			}
		}
		catch (Exception e)
		{
			throw new OpenEditException(e.getMessage(), e);
		}
		
		finally
		{
			if (postMethod != null)
			{
				try
				{
					postMethod.releaseConnection();
				}
				catch (Exception e)
				{
				}//not handled
			}
		}
	}
	
	protected String uploadAttachments(List<String> inAttachments){
		StringBuilder buf = new StringBuilder();
		for(String attachment:inAttachments){
			Page page = getPageManager().getPage(attachment);
			if (page.exists()){
				String response = uploadAttachment(page);
				if (response != null){
					buf.append(response.trim());
					continue;
				} else {
					log.info("attachment "+page.getName()+" could not be uploaded, aborting");
				}
			} else {
				log.info("attachment "+page.getName()+" does not exist, aborting");
			}
			return null;
		}
		return buf.toString();
	}
	
	protected String uploadAttachment(Page page){
	
		
		HttpRequestBuilder builder = new HttpRequestBuilder();

		
		HttpPost postMethod = null;
		try{
			postMethod = new HttpPost("https://api.elasticemail.com/attachments/upload");
			//multipart submit - include string parts and file parts
			
			//string parts
			builder.addPart("username", getSmtpUsername());
			builder.addPart("api_key", getSmtpPassword());
			//file parts
			builder.addPart(page.getName(),new File(page.getContentItem().getAbsolutePath()));
			
			HttpClient client = getClient();
//			int connectionTimeout = 5000; // 5 seconds default
//			int socketTimeout = 60000; //60 seconds
//			client.getParams().setSoTimeout(socketTimeout);
//			client.getParams().setConnectionManagerTimeout(connectionTimeout);
			postMethod.setEntity(builder.build());
			
			int sc = client.execute(postMethod).getStatusLine().getStatusCode();
			
			String response = IOUtils.toString(postMethod.getEntity().getContent());
			log.info("uploadAttachment "+page.getName()+", response from server: "+response);
			if (sc == 200 && response!=null && response.trim().endsWith(";") && !response.toLowerCase().contains("error")){
				return response;
			} else {
				log.info("error uploading attachments, "+response+", sc="+sc);
			}
		}catch (Exception e){
			throw new OpenEditException(e.getMessage(),e);
		} finally {
			if (postMethod!=null){
				try{
					postMethod.releaseConnection();
				}catch (Exception e){}
			}
		}
		return null;
	}
	
	public ElasticPostMailStatus getMailStatus(String response)
	{
		ElasticPostMailStatus status = null;
		HttpPost postMethod = null;
		try
		{
			String uri = "https://api.elasticemail.com/mailer/status/" + response + "?showstats=true";
			postMethod = new HttpPost(uri);
			
			HttpClient client = getClient();
//			int connectionTimeout = 5000; // 5 seconds default
//			int socketTimeout = 10000; //10 seconds
//			client.getParams().setSoTimeout(socketTimeout);
//			client.getParams().setConnectionManagerTimeout(connectionTimeout);
			int sc = client.execute(postMethod).getStatusLine().getStatusCode();
			if (sc == 200)
			{
				String xml = IOUtils.toString(postMethod.getEntity().getContent());
				status = ElasticPostMailStatus.parseXML(xml);
			}
		}
		catch (Exception e)
		{
			throw new OpenEditException(e.getMessage(), e);
		}
		finally
		{
			if (postMethod != null)
			{
				try
				{
					postMethod.releaseConnection();
				}
				catch (Exception e)
				{
				}//not handled
			}
		}
		return status;
	}
}
