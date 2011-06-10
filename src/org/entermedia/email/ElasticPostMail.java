package org.entermedia.email;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.entermedia.email.PostMail;

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

	public void postMail(String[] recipients, String subject, String inHtml,
			String inText, String from, List inAttachments, Map inProperties) {

		try {
			String fullpath = "https://api.elasticemail.com/mailer/send";

			PostMethod postMethod = new PostMethod(fullpath);
			postMethod.setParameter("username", getSmtpUsername());
			postMethod.setParameter("api_key", getSmtpPassword());
			postMethod.setParameter("from", from);
			postMethod.setParameter("from_name", from);
			StringBuffer recipientlist = new StringBuffer();
			for (String recip : recipients) {
				recipientlist.append(recip);
			
				recipientlist.append(";");

			}
			String finallist = recipientlist.toString();
			
			finallist = finallist.substring(0, finallist.length() -1);
			
			postMethod.setParameter("to", finallist);
			postMethod.setParameter("subject", subject);
			if (inHtml != null) {
				postMethod.setParameter("body_html", inHtml);
			}
			if (inText != null) {
				postMethod.setParameter("body_text", inText);
			}
			int statusCode1 = getHttpClient().executeMethod(postMethod);
			if (statusCode1 == 200) {
				String response = postMethod.getResponseBodyAsString();
			}
		} catch (HttpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
