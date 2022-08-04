/*
 * Created on Sep 26, 2003
 *
/*
Copyright (c) 2003 eInnovation Inc. All rights reserved

This library is free software; you can redistribute it and/or modify it under the terms
of the GNU Lesser General Public License as published by the Free Software Foundation;
either version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Lesser General Public License for more details.
*/
package org.entermediadb.asset.modules;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.mail.MessagingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.email.PostMail;
import org.entermediadb.email.TemplateWebEmail;
import org.entermediadb.email.WebEmail;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.modules.BaseModule;
import org.openedit.page.Page;

/**
 * @author Matt Avery, mavery@einnovation.com
 */
public class SendMailModule extends BaseModule
{
	private static final Log log = LogFactory.getLog(SendMailModule.class);
	public static final String EMAIL_SETTINGS = "emailsettings";
	private PostMail fieldPostMail;
	
	public PostMail getPostMail() {
		return fieldPostMail;
	}

	public void setPostMail(PostMail postMail) {
		this.fieldPostMail = postMail;
	}

	public SendMailModule()
	{
	}
	public void sendEmailtoProject(WebPageRequest inContext)throws OpenEditException
	{
		MediaArchive archive = (MediaArchive)inContext.getPageValue("mediaarchive");
		TemplateWebEmail webmail = (TemplateWebEmail)inContext.getPageValue(EMAIL_SETTINGS);
		if( webmail == null)
		{

			
			
//			webmail = new TemplateWebEmail();
			webmail = (TemplateWebEmail) getModuleManager().getBean("templateWebEmail");//from spring
			webmail.setPostMail(getPostMail());
			webmail.setPageManager(getPageManager());
			
			Map<String, String> formfields = new HashMap<String, String>();
			//Get email To address from collection
			String collectionid = inContext.getRequestParameter("collectionid");
			if(collectionid != null) {
				Data collection = archive.getData("librarycollection", collectionid);
				String emailto = (String) collection.getValue("contactemail");
				if(emailto == null) {
					//get it from request or content? or set default to something
				}
				if(emailto != null) {
					webmail.setTo(emailto);
				}
				webmail.setSubject("Contacted from: " + collection.getName());
				//formfields.put("project", collection.getName());
				inContext.putPageValue("project", collection.getName());
			}
			String fields[] = inContext.getRequestParameters("field");
			
			if (fields != null)
			{
				for (int i = 0; i < fields.length; i++)
				{
					String field = fields[i];
					String value = (String) inContext.getRequestParameter(field + ".value");
					if (value != null) {
						formfields.put(field, value);
					}
				}
				inContext.putPageValue("formfields", formfields);
			}
			webmail.loadSettings(inContext);
			inContext.putPageValue(EMAIL_SETTINGS, webmail);
		}
		sendEmail(inContext, webmail);
		log.info("Mail Sent to " + Arrays.asList(webmail.getTo()) );
	}
	
	public void sendEmail(WebPageRequest inContext)throws OpenEditException
	{
		TemplateWebEmail webmail = (TemplateWebEmail)inContext.getPageValue(EMAIL_SETTINGS);
		if( webmail == null)
		{
//			webmail = new TemplateWebEmail();
			webmail = (TemplateWebEmail) getModuleManager().getBean("templateWebEmail");//from spring
			webmail.setPostMail(getPostMail());
			webmail.setPageManager(getPageManager());
			webmail.loadSettings(inContext);
			inContext.putPageValue(EMAIL_SETTINGS,webmail);
		}
		log.info("Sending email");
		sendEmail(inContext, webmail);
		log.info("Mail Sent to " + Arrays.asList(webmail.getTo()) );
	}
	
	public void sendFormEmail(WebPageRequest inReq) throws Exception
	{
		//check for subject in the email
		String subject = inReq.getRequestParameter("subject");
		if( subject != null)
		{
			TemplateWebEmail webmail = (TemplateWebEmail)inReq.getPageValue(EMAIL_SETTINGS);
			if( webmail == null)
			{
//				webmail = new TemplateWebEmail();
				webmail = (TemplateWebEmail) getModuleManager().getBean("templateWebEmail");//from spring
				webmail.setPostMail(getPostMail());
				webmail.setPageManager(getPageManager());
				webmail.loadSettings(inReq);
				webmail.loadBodyFromForm(inReq);
				inReq.putPageValue(EMAIL_SETTINGS,webmail);
			}
			sendEmail(inReq, webmail);
			//Use this page we are on and replace the <input to be <input and textarea
		}
	}
	
	protected void sendEmail(WebPageRequest inReq, WebEmail inWebmail) throws OpenEditException
	{
		if (inWebmail.getPostMail() == null)
		{
			inWebmail.setPostMail(getPostMail());
		}
		Page page = inReq.getPage();
		//use send to send them
		try
		{
			inWebmail.send();
		}
		catch (MessagingException e)
		{
			log.info("failed to send email" +e);
			log.error(e);
			String errorpage = page.get("error_page");
			if (errorpage == null || errorpage.length() <= 0)
				errorpage = inReq.getRequestParameter( "error_page" );
			if ( errorpage != null)
			{	
				inReq.redirect( errorpage );
				return;
			}

		}
		String successpage = inReq.findValue("thankspage");
		if( successpage == null)
		{
			successpage = page.get("success_page");
		}
		if (successpage == null || successpage.length() <= 0)
			successpage = inReq.getRequestParameter( "success_page" );
		if ( successpage != null)
		{	
			inReq.redirect( successpage );
		}
	}
}
