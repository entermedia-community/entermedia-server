/*
Copyright (c) 2003 eInnovation Inc. All rights reserved

This library is free software; you can redistribute it and/or modify it under the terms
of the GNU Lesser General Public License as published by the Free Software Foundation;
either version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Lesser General Public License for more details.
 */

package org.entermediadb.email;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.page.manage.PageManager;

public class PostMail
{
	private static final Log log = LogFactory.getLog(PostMail.class);
	protected String fieldSmtpUsername = "noreply";
	protected String fieldSmtpPassword = "whitelistip";
	protected String fieldSmtpServer = "smtp.entermediadb.org";
	protected Integer fieldPort = 2525;
	protected boolean fieldSmtpSecured = true;
	protected boolean fieldEnableTls = false;
	public boolean isEnableTls()
	{
		return fieldEnableTls;
	}

	public void setEnableTls(boolean inEnableTls)
	{
		fieldEnableTls = inEnableTls;
	}

	protected PageManager fieldPageManager;
	protected boolean fieldSslEnabled = false;
	protected ModuleManager fieldModuleManager;

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	public String getSmtpPassword()
	{
		return fieldSmtpPassword;
	}

	public void setSmtpPassword(String inSmtpPassword)
	{
		this.fieldSmtpPassword = inSmtpPassword;
	}

	public boolean isSmtpSecured()
	{
		return fieldSmtpSecured;
	}

	public void setSmtpSecured(boolean inSmtpSecured)
	{
		this.fieldSmtpSecured = inSmtpSecured;
	}

	public String getSmtpUsername()
	{
		return fieldSmtpUsername;
	}

	public void setSmtpUsername(String inSmtpUsername)
	{
		this.fieldSmtpUsername = inSmtpUsername;
	}

	public void postMail(String recipient, String subject, String message, String from) throws MessagingException
	{
		postMail(new String[] { recipient }, subject, message, null, from);
	}

	// returns a new template web email instance preconfigured with spring
	// settings.
	public TemplateWebEmail getTemplateWebEmail()
	{
		TemplateWebEmail email = null;
		if (getModuleManager() != null)
		{
			email = (TemplateWebEmail) getModuleManager().getBean("templateWebEmail");//from spring
		}
		if (email == null)
		{
			email = new TemplateWebEmail();
		}
		email.setPostMail(this);
		email.setPageManager(getPageManager());
		return email;
	}

	public void postMail(String[] recipients, String subject, String inHtml, String inText, String from) throws MessagingException
	{
		InternetAddress fromAddress = new InternetAddress();
		fromAddress.setAddress(from);
		postMail(parseEmails(recipients), subject, inHtml, inText, fromAddress, null, null);
	}
	
	public void postMail(String[] recipients, String subject, String inHtml, String inText,  String from,  String fromname) throws MessagingException
	{
		try
		{
			InternetAddress fromAddress = new InternetAddress();
			fromAddress.setAddress(from);
			if( fromname != null)
			{
				fromAddress.setPersonal(fromname);
			}
			postMail(parseEmails(recipients), subject, inHtml, inText, fromAddress, null, null);
		}
		catch ( Exception ex)
		{
			throw new OpenEditException(ex);
		}
	}

	
	public List<InternetAddress> parseEmails(String[] inEmails)
	{
		List emails = new ArrayList<InternetAddress>();
		for (int i = 0; i < inEmails.length; i++)
		{
			try
			{
			InternetAddress[] inet = InternetAddress.parse(inEmails[i]);
			for (int j = 0; j < inet.length; j++)
			{
				emails.add(inet[i]);
			}
			}
			catch( AddressException ex)
			{
				//ignore
				log.error("Could not process email " + inEmails[i], ex);
			}
		}
		return emails;
	}
	public void postMail(List<InternetAddress> recipients, String subject, String inHtml, String inText, InternetAddress inFrom, List inAttachments, Map inProperties) throws MessagingException
	{
		
		//InternetAddress from = new InternetAddress();
		//from.setAddress(inFrom);
		postMail(recipients, null, subject, inHtml, inText, inFrom, inAttachments, inProperties);
	}

	public void postMail(List<InternetAddress> recipients, List<InternetAddress> blindrecipients, String subject, String inHtml, String inText, InternetAddress from, List inAttachments, Map inProperties) throws MessagingException
	{
		// Set the host smtp address
		Properties props = new Properties();
		// create some properties and get the default Session
		props.put("mail.smtp.host", fieldSmtpServer);
		props.put("mail.smtp.port", String.valueOf(getPort()));
		props.put("mail.smtp.auth", new Boolean(fieldSmtpSecured).toString());
		if (isSslEnabled())
		{
			props.put("mail.smtp.starttls.enable", "true");
			props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		}
		Session session = null;
		if( isEnableTls())
		{
			props.put("mail.smtp.starttls.enable", "true");
			session = Session.getInstance(props,
					new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(getSmtpUsername(), getSmtpPassword());
				}
			  });
		}
		else if (fieldSmtpSecured)
		{
			SmtpAuthenticator auth = new SmtpAuthenticator();
			session = Session.getInstance(props, auth);
		}
		else
		{
			session = Session.getInstance(props);
		}
		// session.setDebug(debug);

		// create a message
		Message msg = new MimeMessage(session);
		MimeMultipart mp = null;
		// msg.setDataHandler(new DataHandler(new ByteArrayDataSource(message,
		// "text/html")));

		if (inAttachments != null && inAttachments.size() == 0)
		{
			inAttachments = null;
		}

		if (inText != null && inHtml != null || inAttachments != null)
		{
			// Create an "Alternative" Multipart message
			mp = new MimeMultipart("mixed");

			if (inText != null)
			{
				BodyPart messageBodyPart = new MimeBodyPart();

				messageBodyPart.setContent(inText, "text/plain; charset=UTF-8");
				mp.addBodyPart(messageBodyPart);
			}
			if (inHtml != null)
			{
				BodyPart messageBodyPart = new MimeBodyPart();
				messageBodyPart.setContent(inHtml, "text/html; charset=UTF-8");
				mp.addBodyPart(messageBodyPart);
			}
			if (inAttachments != null)
			{
				for (Iterator iterator = inAttachments.iterator(); iterator.hasNext();)
				{
					String filename = (String) iterator.next();

					File file = new File(filename);

					if (file.exists() && !file.isDirectory())
					{
						// create the second message part
						MimeBodyPart mbp = new MimeBodyPart();

						FileDataSource fds = new FileDataSource(file);
						mbp.setDataHandler(new DataHandler(fds));

						mbp.setFileName(fds.getName());

						mp.addBodyPart(mbp);
					}
				}
			}

			msg.setContent(mp);

		}
		else if (inHtml != null)
		{
			msg.setContent(inHtml, "text/html; charset=UTF-8");
		}
		else
		{
			msg.setContent(inText, "text/plain; charset=UTF-8");
		}
		// set the from and to address
		msg.setFrom(from);
		//msg.setRecipient(RecipientType.BCC, addressFrom);
		msg.setSentDate(new Date());
		if (recipients == null || recipients.isEmpty() )
		{
			throw new MessagingException("No recipients specified");
		}
		InternetAddress[] addressTo = recipients.toArray(new InternetAddress[recipients.size()]);

		msg.setRecipients(Message.RecipientType.TO, addressTo);

		//add bcc
		if (blindrecipients != null && !blindrecipients.isEmpty())
		{
			InternetAddress[] addressBcc = blindrecipients.toArray(new InternetAddress[blindrecipients.size()]);
			msg.setRecipients(Message.RecipientType.BCC, addressBcc);
		}

		// Optional : You can also set your custom headers in the Email if you
		// Want
		// msg.addHeader("MyHeaderName", "myHeaderValue");
		// Setting the Subject and Content Type
		msg.setSubject(subject);

		// Transport tr = session.getTransport("smtp");
		// tr.connect(serverandport[0], null, null);
		// msg.saveChanges(); // don't forget this
		// tr.sendMessage(msg, msg.getAllRecipients());
		// tr.close();
		// msg.setContent(msg, "text/plain");

		Transport.send(msg);
		log.info("sent email " + subject);
	}

	public int getPort()
	{
		if (fieldPort == null)
		{
			fieldPort = Integer.getInteger("mail.smtp.port");
			if (fieldPort == null)
			{
				fieldPort = new Integer(25);
			}
		}
		return fieldPort;
	}

	public void setPort(int inPort)
	{
		this.fieldPort = new Integer(inPort);
	}

	public void setPort(Integer inPort)
	{
		this.fieldPort = inPort;
	}

	public String getSmtpServer()
	{
		return fieldSmtpServer;
	}

	public void setSmtpServer(String inSmtpServer)
	{
		this.fieldSmtpServer = inSmtpServer;
	}

	public class SmtpAuthenticator extends javax.mail.Authenticator
	{
		public javax.mail.PasswordAuthentication getPasswordAuthentication()
		{
			return new PasswordAuthentication(fieldSmtpUsername, fieldSmtpPassword);
		}
	}

	public boolean isSslEnabled()
	{
		return fieldSslEnabled;
	}

	public void setSslEnabled(boolean inSslEnabled)
	{
		fieldSslEnabled = inSslEnabled;
	}

	public InternetAddress parseEmail(String inValue)
	{
		try
		{
			InternetAddress inet = new InternetAddress(inValue);
			return inet;
		}
		catch (AddressException ex)
		{
			//ignore
			log.error("Could not process email " + inValue, ex);
		}
		return null;
	}

}
