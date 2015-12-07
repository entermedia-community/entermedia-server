package org.entermedia.email;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.mail.internet.InternetAddress;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.openedit.OpenEditException;
import com.openedit.page.Page;


//uses ElasticMail to send instead of an SMTP server

public class ElasticPostMail extends PostMail
{

	private static final Log log = LogFactory.getLog(ElasticPostMail.class);
	
	protected HttpClient fieldHttpClient;

	public org.apache.commons.httpclient.HttpClient getHttpClient()
	{
		if (fieldHttpClient == null)
		{
			fieldHttpClient = new HttpClient();
		}
		return fieldHttpClient;
	}

	public void setHttpClient(HttpClient inHttpClient)
	{
		fieldHttpClient = inHttpClient;
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
		PostMethod postMethod = null;
		try
		{
			String fullpath = "https://api.elasticemail.com/mailer/send";
			postMethod = new PostMethod(fullpath);
			postMethod.setParameter("username", getSmtpUsername());
			postMethod.setParameter("api_key", getSmtpPassword());
			postMethod.setParameter("from", from);
			postMethod.setParameter("from_name", from);
			//make sure list of recipients is unique since it may combine cc and bcc
			ArrayList<String> list = new ArrayList<String>();
			for (InternetAddress str : recipients){
				if (!list.contains(str.getAddress())){
					list.add(str.getAddress());
				}
			}
			String finallist = list.toString().replace("[", "").replace("]", "").replace(",", ";").trim();
			postMethod.setParameter("to", finallist);//email is sent separately to each recipient, ie. treated as BCC, so include BCC list
			postMethod.setParameter("subject", subject);
			if (inHtml != null){
				postMethod.setParameter("body_html", inHtml);
			}
			if (inText != null){
				postMethod.setParameter("body_text", inText);
			}
			boolean hasAttachments = false;
			if (inAttachments!=null && !inAttachments.isEmpty()){
				String attachments = uploadAttachments(inAttachments);
				if (attachments!=null) {
					postMethod.setParameter("attachments", attachments);
					hasAttachments = true;
				} else {
					log.info("Error uploading attachments, aborting");
					return;
				}
			}
			HttpClient client = getHttpClient();
			//should check if attachments are being sent
			//if so, increase the socket timeout
//			int connectionTimeout = 5000; // 5 seconds default
//			int socketTimeout = 10000;//10 seconds default
//			if (hasAttachments){
//				socketTimeout = 60000; //60 seconds
//			}
//			client.getParams().setSoTimeout(socketTimeout);
//			client.getParams().setConnectionManagerTimeout(connectionTimeout);
			int statusCode1 = client.executeMethod(postMethod);
			if (statusCode1 == 200)
			{
				//need to save response
				String response = postMethod.getResponseBodyAsString();
				if (inProperties != null){
					inProperties.put(PostMailStatus.ID, response);
				}
			}
		}
		catch (HttpException e)
		{
			throw new OpenEditException(e.getMessage(), e);
		}
		catch (IOException e)
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
		PostMethod postMethod = null;
		try{
			postMethod = new PostMethod("https://api.elasticemail.com/attachments/upload");
			//multipart submit - include string parts and file parts
			List<Part> parts = new ArrayList<Part>();
			//string parts
			parts.add(new StringPart("username", getSmtpUsername()));
			parts.add(new StringPart("api_key", getSmtpPassword()));
			parts.add(new StringPart("file", page.getName()));
			//file parts
			parts.add(new FilePart(page.getName(),new File(page.getContentItem().getAbsolutePath())));
			
			Part[] arrayOfparts = parts.toArray(new Part[]{});
			postMethod.setRequestEntity(new MultipartRequestEntity(arrayOfparts, postMethod.getParams()));
			
			HttpClient client = getHttpClient();
//			int connectionTimeout = 5000; // 5 seconds default
//			int socketTimeout = 60000; //60 seconds
//			client.getParams().setSoTimeout(socketTimeout);
//			client.getParams().setConnectionManagerTimeout(connectionTimeout);
			int sc = client.executeMethod(postMethod);
			String response = postMethod.getResponseBodyAsString();
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
		PostMethod postMethod = null;
		try
		{
			String uri = "https://api.elasticemail.com/mailer/status/" + response + "?showstats=true";
			postMethod = new PostMethod(uri);
			
			HttpClient client = getHttpClient();
//			int connectionTimeout = 5000; // 5 seconds default
//			int socketTimeout = 10000; //10 seconds
//			client.getParams().setSoTimeout(socketTimeout);
//			client.getParams().setConnectionManagerTimeout(connectionTimeout);
			int sc = client.executeMethod(postMethod);
			if (sc == 200)
			{
				String xml = postMethod.getResponseBodyAsString();
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
