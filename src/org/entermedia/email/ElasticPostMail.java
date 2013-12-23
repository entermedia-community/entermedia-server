package org.entermedia.email;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.mail.internet.InternetAddress;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;

import com.openedit.OpenEditException;

//uses ElasticMail to send instead of an SMTP server

public class ElasticPostMail extends PostMail
{

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
			for (InternetAddress str : recipients)
			{
				if (!list.contains(str.getAddress()))
					list.add(str.getAddress());
			}
			String finallist = list.toString().replace("[", "").replace("]", "").replace(",", ";").trim();
			postMethod.setParameter("to", finallist);//email is sent separately to each recipient, ie. treated as BCC, so include BCC list
			postMethod.setParameter("subject", subject);
			if (inHtml != null)
			{
				postMethod.setParameter("body_html", inHtml);
			}
			if (inText != null)
			{
				postMethod.setParameter("body_text", inText);
			}
			int statusCode1 = getHttpClient().executeMethod(postMethod);
			if (statusCode1 == 200)
			{
				//need to save response
				String response = postMethod.getResponseBodyAsString();
				if (inProperties != null)
					inProperties.put(PostMailStatus.ID, response);//TODO - include type safety
			}
		}
		catch (HttpException e)
		{
			e.printStackTrace();
			throw new OpenEditException(e.getMessage(), e);
		}
		catch (IOException e)
		{
			e.printStackTrace();
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

	public ElasticPostMailStatus getMailStatus(String response)
	{
		ElasticPostMailStatus status = null;
		PostMethod postMethod = null;
		try
		{
			String uri = "https://api.elasticemail.com/mailer/status/" + response + "?showstats=true";
			postMethod = new PostMethod(uri);
			int sc = getHttpClient().executeMethod(postMethod);
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
