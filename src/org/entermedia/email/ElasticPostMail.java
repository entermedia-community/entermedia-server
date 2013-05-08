package org.entermedia.email;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.StatusLine;
import org.apache.commons.httpclient.methods.PostMethod;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.openedit.OpenEditException;

//uses ElasticMail to send instead of an SMTP server

public class ElasticPostMail extends PostMail {

	protected HttpClient fieldHttpClient;

	public org.apache.commons.httpclient.HttpClient getHttpClient() {
		if (fieldHttpClient == null) {
			fieldHttpClient = new HttpClient();
		}

		return fieldHttpClient;
	}

	public void setHttpClient(HttpClient inHttpClient) {
		fieldHttpClient = inHttpClient;
	}

	public void postMail(String[] recipients, String subject, String inHtml,
			String inText, String from, List inAttachments) {
		postMail(recipients, subject, inHtml, inText, from, inAttachments, null);
	}
	
	public void postMail(List<Recipient> recipients, List<Recipient> blindrecipients, String subject, 
			String inHtml, String inText, String from, List inAttachments, Map inProperties){
		ArrayList<String> list = new ArrayList<String>();
		for (Recipient recipient:recipients){
			if (recipient.getEmailAddress()!=null && !list.contains(recipient.getEmailAddress()))
				list.add(recipient.getEmailAddress());
		}
		for (Recipient recipient:blindrecipients){
			if (recipient.getEmailAddress()!=null && !list.contains(recipient.getEmailAddress()))
				list.add(recipient.getEmailAddress());
		}
		String [] str = list.toArray(new String[list.size()]);
		postMail(str, subject, inHtml, inText, from, inAttachments, inProperties);
	}
	
	public void postMail(String [] recipients, List<Recipient> blindrecipients, String subject,
			String inHtml, String inText, String from, List inAttachments, Map inProperties){
		ArrayList<String> list = new ArrayList<String>();
		for (String recipient:recipients){
			if (!list.contains(recipient))
				list.add(recipient);
		}
		for (Recipient recipient:blindrecipients){
			if (recipient.getEmailAddress()!=null && !list.contains(recipient.getEmailAddress()))
				list.add(recipient.getEmailAddress());
		}
		String [] str = list.toArray(new String[list.size()]);
		postMail(str, subject, inHtml, inText, from, inAttachments, inProperties);
	}

	public void postMail(String[] recipients, String subject, String inHtml,
			String inText, String from, List inAttachments, Map inProperties) {

		try {
			String fullpath = "https://api.elasticemail.com/mailer/send";

			PostMethod postMethod = new PostMethod(fullpath);
			postMethod.setParameter("username", getSmtpUsername());
			postMethod.setParameter("api_key", getSmtpPassword());
			postMethod.setParameter("from", from);
			postMethod.setParameter("from_name", from);
			//make sure list of recipients is unique since it may combine cc and bcc
			ArrayList<String> list = new ArrayList<String>();
			for (String str:recipients){
				if (!list.contains(str)) list.add(str);
			}
			String finallist = list.toString().replace("[", "").replace("]", "").replace(",",";").trim();
			postMethod.setParameter("to", finallist);//email is sent separately to each recipient, ie. treated as BCC, so include BCC list
			postMethod.setParameter("subject", subject);
			if (inHtml != null) {
				postMethod.setParameter("body_html", inHtml);
			}
			if (inText != null) {
				postMethod.setParameter("body_text", inText);
			}
			int statusCode1 = getHttpClient().executeMethod(postMethod);
			if (statusCode1 == 200) {
				//need to save response
				String response = postMethod.getResponseBodyAsString();
				ElasticPostMailStatus status = getMailStatus(response);
//				System.out.println(status);
			}
		} catch (HttpException e) {
			e.printStackTrace();
			throw new OpenEditException(e.getMessage(),e);
		} catch (IOException e) {
			e.printStackTrace();
			throw new OpenEditException(e.getMessage(),e);
		}

	}
	
	public ElasticPostMailStatus getMailStatus(String response){
		ElasticPostMailStatus status = null;
		try{
			String uri = "https://api.elasticemail.com/mailer/status/"+response+"?showstats=true";
			PostMethod postMethod = new PostMethod(uri);
			int statusCode1 = getHttpClient().executeMethod(postMethod);
			if (statusCode1 == 200) {
				String xml = postMethod.getResponseBodyAsString();
				status = ElasticPostMailStatus.parseXML(xml);
			}
		}catch (Exception e){
			throw new OpenEditException(e.getMessage(),e);
		}
		return status;
	}

}
