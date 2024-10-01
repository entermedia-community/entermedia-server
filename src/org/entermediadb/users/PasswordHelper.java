/*
 * Created on Jun 14, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.entermediadb.users;

import java.io.Serializable;

import org.entermediadb.asset.modules.SendMailModule;
import org.entermediadb.email.TemplateWebEmail;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.page.manage.PageManager;

/**
 * @author mcgaha_b
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class PasswordHelper implements Serializable {
	
	protected String fieldTo;
	protected String fieldPassword;
	
	protected SendMailModule sendMailModule;

	public SendMailModule getSendMailModule() {
		return sendMailModule;
	}

	public void setSendMailModule(SendMailModule sendMailModule) {
		this.sendMailModule = sendMailModule;
	}

	public void emailPasswordReminder(WebPageRequest inContext, PageManager inManager, String inUserCode, String inEmail) 
	{
		emailPasswordReminder(inContext, inManager, inUserCode, null, inEmail);
	}
	public void emailPasswordReminder(WebPageRequest inContext, PageManager inManager, String inUserCode, String inEnterMediaKey, String inEmail) 
	{
	
		//TO
		inContext.setRequestParameter("to", inEmail);
		String subject = inContext.findValue("subject");
		if( subject == null)
		{
			subject = "Forgotten Password";
		}
		inContext.setRequestParameter("subject",subject);
		
		TemplateWebEmail email = sendMailModule.getPostMail().getTemplateWebEmail();
		email.setPageManager(inManager);
		try {
			email.loadSettings(inContext);
		} catch (OpenEditException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		inContext.putPageValue(SendMailModule.EMAIL_SETTINGS,email);
		
		inContext.putPageValue("templogincode", inUserCode);
		inContext.putPageValue("entermediakey", inEnterMediaKey);
			
		if (inEmail != null){
			inContext.putPageValue("mail", inEmail);
		}
		inContext.putPageValue("commandSucceeded", "didnotexecute");
		if (inEmail != null){
			try
			{
				sendMailModule.sendEmail( inContext );
				inContext.putPageValue("commandSucceeded", "true");
			}
			catch (OpenEditException e)
			{
				inContext.putPageValue("commandSucceeded", "false");
				inContext.putPageValue("error", e.getLocalizedMessage());
			}
		}
	}

	/**
	 * @param inContext
	 * @param username
	 * @param password
	 * @param email
	 */
	public void emailAdminAboutNewUser(WebPageRequest inContext, PageManager inManager, String emailaddress, String inEmail) 
	{
		//TO
		inContext.setRequestParameter("to", inEmail);
		
		TemplateWebEmail email = sendMailModule.getPostMail().getTemplateWebEmail();
		email.setPageManager(inManager);
		try {
			email.loadSettings(inContext);
		} catch (OpenEditException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String subject = emailaddress + " Approval";
		email.setSubject(subject);
		email.setMailTemplatePath(inContext.findValue("emaillayoutadmin"));
		inContext.putPageValue(SendMailModule.EMAIL_SETTINGS,email);
		inContext.putPageValue("categorylogo", inContext.getPageValue("categorylogo"));
		
		if (inEmail != null){
			inContext.putPageValue("mail", inEmail);
		}
		if (inEmail != null)
		{
			try
			{
				sendMailModule.sendEmail( inContext );
			}
			catch (OpenEditException e)
			{
				inContext.putPageValue("error", e.getLocalizedMessage());
			}
		}
	}

}

