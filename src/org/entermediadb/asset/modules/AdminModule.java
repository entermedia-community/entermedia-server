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

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Category;
import org.entermediadb.authenticate.AutoLoginProvider;
import org.entermediadb.authenticate.AutoLoginResult;
import org.entermediadb.authenticate.BaseAutoLogin;
import org.entermediadb.users.AllowViewing;
import org.entermediadb.users.PasswordHelper;
import org.entermediadb.users.PermissionManager;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.SearcherManager;
import org.openedit.page.Page;
import org.openedit.page.PageRequestKeys;
import org.openedit.page.PageStreamer;
import org.openedit.page.Permission;
import org.openedit.page.manage.PageManager;
import org.openedit.profile.UserProfile;
import org.openedit.users.Group;
import org.openedit.users.Permissions;
import org.openedit.users.User;
import org.openedit.users.UserManager;
import org.openedit.users.authenticate.AuthenticationRequest;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.StringEncryption;
import org.openedit.util.URLUtilities;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;

/**
 * This module allows the user to view and administer the site.
 * 
 * @author Eric Galluzzo
 * @author Matt Avery, mavery@einnovation.com
 */
public class AdminModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(AdminModule.class);

	protected String fieldImagesRoot; // used by the imagepicker
	protected String fieldRootFTPURL;
	protected static final String UNAME = "username";
	protected static final String EMAIL = "to";
	protected StringEncryption fieldCookieEncryption;
	protected SendMailModule sendMailModule;
	protected List fieldWelcomeFiles;
	protected List fieldAutoLoginProviders;
	
	public List getAutoLoginProviders()
	{
		return fieldAutoLoginProviders;
	}

	public void setAutoLoginProviders(List inAutoLoginProviders)
	{
		fieldAutoLoginProviders = inAutoLoginProviders;
	}

	public List getWelcomeFiles()
	{
		return fieldWelcomeFiles;
	}

	public void setWelcomeFiles(List inWelcomeFiles)
	{
		fieldWelcomeFiles = inWelcomeFiles;
	}

	public AdminModule()
	{
		super();
	}

	public SearcherManager getSearcherManager()
	{
		return (SearcherManager) getBeanLoader().getBean("searcherManager");
	}

	/**
	 * Sets the root FTP URL.
	 * 
	 * @param rootFTPURL
	 *            The root FTP URL to set
	 */
	public void setRootFTPURL(String rootFTPURL)
	{
		if ((rootFTPURL != null) && rootFTPURL.endsWith("/"))
		{
			fieldRootFTPURL = rootFTPURL.substring(0, rootFTPURL.length() - 1);
		}
		else
		{
			fieldRootFTPURL = rootFTPURL;
		}
	}

	/**
	 * Returns the root FTP URL.
	 * 
	 * @return A string, which will not end in a slash, or <code>null</code> if
	 *         FTP support has not been configured.
	 */
	public String getRootFTPURL()
	{
		return fieldRootFTPURL;
	}

	//TODO: Use Spring
	protected PasswordHelper getPasswordHelper(WebPageRequest inReq) throws OpenEditException
	{
		PasswordHelper passwordHelper = (PasswordHelper) inReq.getSessionValue("passwordHelper");
		if (passwordHelper == null)
		{
			passwordHelper = new PasswordHelper();
			passwordHelper.setSendMailModule(sendMailModule);
			inReq.putSessionValue("passwordHelper", passwordHelper);
		}

		return passwordHelper;
	}
	
	
	
	
	public void emailUserLoginCode(WebPageRequest inReq) throws Exception
	{
		String emailaddress = inReq.getRequestParameter(EMAIL);
		if( emailaddress == null)
		{
			emailaddress = inReq.getRequestParameter("email"); //Move to using this
		}
		String u = inReq.getRequestParameter(UNAME);
		if (emailaddress == null && u == null)
		{
			inReq.putPageValue("commandSucceeded", "missingparam");
			// log.error("Invalid information");
			return;
		}

		User foundUser = null;
		String username = null;
		String firstName = "";
		String lastName = "";
		
		Category userCategory = (Category)inReq.getPageValue("userCategory"); 
		
		PasswordHelper passwordHelper = getPasswordHelper(inReq);

		if (emailaddress != null && emailaddress.length() > 0)
		{
			foundUser = (User) getUserManager(inReq).getUserByEmail(emailaddress);
		}
		if (foundUser != null)
		{
			emailaddress = foundUser.getEmail();
			firstName = foundUser.getFirstName();
			lastName = foundUser.getLastName();
			username = foundUser.getId();
			
			String userCode = getUserManager(inReq).createNewTempLoginKey(username,emailaddress,firstName,lastName,false);
			
			
			
			if(userCategory != null)
			{
				foundUser.setValue("logincategoryid",userCategory.getId());
				getUserManager(inReq).saveUser(foundUser);
			}
			
			if(foundUser.isEnabled() )
			{
				
				passwordHelper.emailPasswordReminder(inReq, getPageManager(), userCode, emailaddress);
			}
		}
		else
		{
			//Show error on page. Notify admin
			inReq.putPageValue("emailaddress", emailaddress);
			String userapproveremail = (String)inReq.getPageValue("userapproveremail");
			if (userapproveremail != null) {
				passwordHelper.emailAdminAboutNewUser(inReq, getPageManager(), emailaddress, userapproveremail);
			}
			else {
				firstName = inReq.getRequestParameter("firstName");
				lastName = inReq.getRequestParameter("lastName");
				String userCode = getUserManager(inReq).createNewTempLoginKey(null,emailaddress,firstName,lastName,false);
				String subject = inReq.getRequestParameter("subject");
				inReq.putPageValue("subject", subject);
				passwordHelper.emailPasswordReminder(inReq, getPageManager(), userCode, emailaddress);
				
			}
		}
		if(inReq.getPageValue("error") != null) {
			log.info("Error sending Email. " + inReq.getPageValue("error"));
			inReq.putPageValue("commandSucceeded", "error");
			
		}
		else
		{
			inReq.putPageValue("commandSucceeded", "ok");
		}
	}
	
	
	

	public void emailPasswordReminder(WebPageRequest inReq) throws Exception
	{
		String emailaddress = inReq.getRequestParameter(EMAIL);
		if( emailaddress == null)
		{
			emailaddress = inReq.getRequestParameter("email"); //Move to using this
		}
		String u = inReq.getRequestParameter(UNAME);
		if (emailaddress == null && u == null)
		{
			inReq.putPageValue("commandSucceeded", "missingparam");
			// log.error("Invalid information");
			return;
		}

		User foundUser = null;
		String username = null;
		String firstName = "";
		String lastName = "";
		// if the user provided an email instead of a username, lookup username
		if (emailaddress != null && emailaddress.length() > 0)
		{
			foundUser = (User) getUserManager(inReq).getUserByEmail(emailaddress);
		}
		if (foundUser == null)
		{
			// If the user provided a valid username
			username = inReq.getRequestParameter(UNAME);
			if (username != null)
			{
				foundUser = (User) getUserManager(inReq).getUser(username);
			}
		}
		if( foundUser != null)
		{
			emailaddress = foundUser.getEmail();
			firstName = foundUser.getFirstName();
			lastName = foundUser.getLastName();
			username = foundUser.getId();
		}
		
		Boolean allowguestregistration =  Boolean.parseBoolean( inReq.findPathValue("allowguestregistration"));
		if (foundUser == null && !allowguestregistration) {
			
			inReq.putPageValue("commandSucceeded", "nouser");
			return;
		}
		//Different email template for desktopapp
		String launchersource = inReq.getRequestParameter("launchersource");
		inReq.putPageValue("launchersource", launchersource);

		try
		{
			//firstName = inReq.getRequestParameter("firstName");
			//lastName = inReq.getRequestParameter("lastName");
			if (foundUser == null && firstName == null && lastName == null)
			{
				inReq.putPageValue("commandSucceeded", "nouser");
				return;
			}
			
			 
			
			String tempsecuritykey = getUserManager(inReq).createNewTempLoginKey(username,emailaddress,firstName,lastName,false);
			
			PasswordHelper passwordHelper = getPasswordHelper(inReq);
			String key = null;
			if( foundUser != null )
			{
				key = getUserManager(inReq).getStringEncryption().getTempEnterMediaKey(foundUser); //Optional
			}
			
			if(foundUser != null &&  foundUser.isEnabled() )
			{
				/*
				if(userCategory != null)
				{
					foundUser.setValue("logincategoryid",userCategory.getId());
					getUserManager(inReq).saveUser(foundUser);
				}
				*/
				passwordHelper.emailPasswordReminder(inReq, getPageManager(), tempsecuritykey, key, emailaddress);
			}
			else
			{
				//Show error on page. Notify admin
				inReq.putPageValue("emailaddress", emailaddress);
				Category userCategory = (Category)inReq.getPageValue("userCategory");
				String  userapproveremail = null;
				if( userCategory != null)
				{
					userapproveremail = (String)userCategory.findValue("categoryadminemail");
				}
				if( userapproveremail == null)
				{
					userapproveremail = getMediaArchive(inReq).getCatalogSettingValue("userapproveremail");
				}
				if (userapproveremail != null) {
					inReq.putPageValue("userapproveremail", userapproveremail);
					passwordHelper.emailAdminAboutNewUser(inReq, getPageManager(), emailaddress, userapproveremail);
				}
				else {
					inReq.putPageValue("commandSucceeded", "nouser");
					return;
				}
			}
			
			if(inReq.getPageValue("error") != null) {
				log.info("Error sending Email. " + inReq.getPageValue("error"));
			}
			inReq.putPageValue("commandSucceeded", "ok");
			//inReq.putPageValue("founduserid", foundUser.getUserName());
			
		}
		catch (OpenEditException oex)
		{
			inReq.putPageValue("commandSucceeded", "encrypterror");
			log.error(oex.getMessage(), oex);
			log.info("Unable to append encrypted timestamp. Autologin URL does not have an expiry.");
		}
		
		
	}
		
	/**
	 * @deprecated use 		String passenc = getUserManager(inReq).getStringEncryption().getPasswordMd5(foundUser.getPassword());
	 * @param inReq
	 */
	public void getKey(WebPageRequest inReq) {
		User foundUser = inReq.getUser();
		if (foundUser == null ) {
			return;
		}
		String passenc = getUserManager(inReq).getStringEncryption().getPasswordMd5(foundUser.getPassword());
		passenc = foundUser.getUserName() + "md542" + passenc;
		try
		{
			String expiry = inReq.getPageProperty("temporary_password_expiry");
			if (expiry == null || expiry.isEmpty())
			{
				log.info("Temporary password expiry is not enabled.");
			}
			else
			{
					String tsenc = getUserManager(inReq).getStringEncryption().encrypt(String.valueOf(new Date().getTime()));
					if (tsenc != null && !tsenc.isEmpty())
					{
						if (tsenc.startsWith("DES:"))
							tsenc = tsenc.substring("DES:".length());//kloog: remove DES: prefix since appended to URL
						passenc += StringEncryption.TIMESTAMP + tsenc;
					}
					else
					{
						log.info("Unable to append encrypted timestamp. Autologin URL does not have an expiry.");
					}
			}
			inReq.putPageValue("userKey", passenc);
		}
		catch (OpenEditException oex)
		{
			log.error(oex.getMessage(), oex);
			log.info("Unable to append encrypted timestamp. Autologin URL does not have an expiry.");
		}
	}

	public void loadPermissions(WebPageRequest inReq) throws Exception
	{
		String catid = inReq.findPathValue("catalogid");
		if (catid == null)
		{
			catid = "system";
		}
		PermissionManager manager = (PermissionManager) getModuleManager().getBean(catid, "permissionManager");
		String limited = null;
		if (inReq.getCurrentAction() != null)
		{
			limited = inReq.getCurrentAction().getChildValue("permissions");
		}
		manager.loadPermissions(inReq, inReq.getContentPage(), limited);
	}
	
	public void loadPermissionFinder(WebPageRequest inReq) throws Exception
	{
		UserProfile profile = inReq.getUserProfile();
		if( profile != null)
		{
			Permissions permissions = profile.getPermissions();
			inReq.putPageValue("permissions", permissions);
			
			//$permissions.can("viewsettings") $permissions.can("asset","upload")
		}
	}
	
	//We will see if we use this or not. Actions may want to handle it themself
	public void permissionRedirect(WebPageRequest inReq) throws OpenEditException
	{
		String name = inReq.findValue("permission");
		String value = (String) inReq.getPageValue("can" + name);
		if (!Boolean.parseBoolean(value))
		{
			String login = inReq.findValue("redirectpath");
			if (login != null)
			{
				inReq.redirect(login);
			}
		}
	}

	/**
	 * 
	 * @deprecated Use Admin.loadPermissions then check for the "canedit" page
	 *             property
	 * @throws Exception
	 */

	public void allowEditing(WebPageRequest inReq) throws Exception
	{
		//		if( inReq.getPageValue("canedit") == null)
		//		{
		createUserSession(inReq);
		boolean value = false;
		if (inReq.getUser() != null)
		{
			Permission filter = inReq.getPage().getPermission("edit");
			value = ((filter == null) || filter.passes(inReq));
		}
		inReq.setEditable(value);
		//		}
	}

	public void allowViewing(WebPageRequest inReq) throws OpenEditException
	{
		createUserSession(inReq);

		AllowViewing command = new AllowViewing();
		command.setPageManager(getPageManager());
		command.configure(inReq);
		command.execute(inReq);
	}

	public void checkForDuplicateByEmail(WebPageRequest inReq) throws Exception
	{
		String email = inReq.getRequiredParameter("email");

		User user = getUserManager(inReq).getUserByEmail(email);
		if (user != null)
		{
			String page = inReq.getCurrentAction().getConfig().getChildValue("redirectpage");
			if (page == null)
			{
				inReq.redirect(page);
			}
			else
			{
				inReq.putPageValue("oe-exception", "Account already exists with address " + email);
			}
		}
	}

	/*
	 * public void loginByEmail( WebPageRequest inReq ) throws Exception {
	 * String account = inReq.getRequestParameter("email");
	 * 
	 * if ( account != null ) { User user = getUserManager(inReq).getUserByEmail(
	 * account ); loginAndRedirect(user,inReq); } else { String referrer =
	 * inReq.getRequest().getHeader("REFERER"); if ( referrer != null ) { //this
	 * is the original page someone might have been on
	 * inReq.putSessionValue("originalEntryPage",referrer ); } } }
	 */
	public void login(WebPageRequest inReq) throws Exception
	{
		String entermediakey = inReq.getRequestParameter("entermedia.key");
		if( entermediakey == null)
		{
			entermediakey = inReq.getRequestParameter("entermediakey");
		}
		String account = inReq.getRequestParameter("accountname");
		if( account == null)
		{
			account = inReq.getRequestParameter("id");
		}
		String email = inReq.getRequestParameter("email");
		
		String password = inReq.getRequestParameter("password");

		if(Boolean.parseBoolean(inReq.findValue("forcelowercaseusername"))) {
			if(account != null) {
				account = account.toLowerCase();
			}
		}
		String sendTo = (String) inReq.getSessionValue("fullOriginalEntryPage");
		
		if(sendTo == null){
			
			sendTo = inReq.getRequestParameter("loginokpage");
			
			if( sendTo == null)
			{
				sendTo = inReq.getRequest().getHeader("REFERER");
			}
			if (sendTo != null && !sendTo.contains("authentication") && sendTo.startsWith(inReq.getSiteRoot()) && (sendTo.endsWith("html") || sendTo.endsWith("jpg")) )
			{ //the original page someone might have been on
				inReq.putSessionValue("fullOriginalEntryPage", sendTo);
			}
		}
			
		if (entermediakey == null && account == null && email == null && inReq.getRequest() != null)
		{
			log.info("Missing parameters " + entermediakey + " and " + account + " email:" + email );
			return;
		}
		else
		{
			if( entermediakey != null)
			{
				if (entermediakey.indexOf("md5") != -1) {
					account = entermediakey.substring(0, entermediakey.indexOf("md5"));
				}
			}
			UserManager userManager = getUserManager(inReq);
			User user = null;
			if( account == null)
			{
				if( email == null)
				{
					log.info("No user id or email found " + account);
					inReq.putPageValue("oe-exception", "No user id or email found");
					inReq.putPageValue("commandSucceeded", "nouser");
					return;
				}
				user = userManager.getUserByEmail(email);
			}
			else
			{
				user = userManager.getUser(account);
				if( user == null)
				{
					if( !"system".equals( userManager.getUserSearcher().getCatalogId() ) )
					{
						log.error("Catalog has customized searchtypes table for user and group database. "
								+ "Make sure users exist in: WEB-INF/data/" + userManager.getUserSearcher().getCatalogId() + "/users");
					}
				}
				if( user == null && account.contains("@"))
				{
					user = userManager.getUserByEmail(account);
				}
			}
			String templogincode = inReq.getRequestParameter("templogincode");
			if (user == null && templogincode != null) // Allow guest user?
			{
				String allow = inReq.getPage().get("allowguestregistration");
				if( allow == null)
				{
					log.error("allowguestregistration must be set to login with temp codes");
				}
				else
				{
					String groupid = inReq.getPage().get("autologingroup");
					user = userManager.checkForNewUser(email,templogincode, groupid);
				}

//				if (groupname != null)
//				{
//					//we dont want to save the real password since it might be NT based
//					String tmppassword = new PasswordGenerator().generate();
//					user = userManager.createGuestUser(account, tmppassword, groupname);
//					log.info("Username not found. Creating guest user.");
//				}
			}
			if( password == null)
			{
				password = entermediakey;
			}
			
			if (password == null && templogincode == null)
			{
				inReq.putPageValue("oe-exception", "Password cannot be blank " + account);
				log.info(" Password cannot be blank ");
				inReq.putPageValue("commandSucceeded", "nopassword");
				return;
			}
			if (user == null )
			{
				String server = inReq.getPage().get("authenticationserver");
				if( server != null)
				{	
					log.info("Checking server for user " + server);
					String groupid = inReq.getPage().get("autologingroup");
					user = userManager.createGuestUser(account, null, groupid);
					user.setEnabled(true);
				}
				else
				{
					inReq.putPageValue("oe-exception", "Invalid Login");
					inReq.putPageValue("commandSucceeded", "nouser");
					log.info("No user found " + account);
					
					return;
				}
			}
			AuthenticationRequest aReq = userManager.createAuthenticationRequest(inReq, password, user);

			aReq.putProperty("templogincode", templogincode);
			
			if (loginAndRedirect(aReq, inReq))
			{
				user.setVirtual(false);
				userManager.saveUser(user);
				inReq.putPageValue("commandSucceeded", "ok");
			}
			else
			{
				inReq.putPageValue("oe-exception", "Invalid Login");
				inReq.putPageValue("commandSucceeded", "invalidlogin");
			}
		}
	}

	
	//TODO: Remove this, its duplicates login
	public boolean authenticate(WebPageRequest inReq) throws Exception
	{
		String account = inReq.getRequestParameter("id");
		String email = inReq.getRequestParameter("email");
		
		if(Boolean.parseBoolean(inReq.findValue("forcelowercaseusername"))) {
			account = account.toLowerCase();
		}
		String password = inReq.getRequestParameter("password");
		UserManager userManager = getUserManager(inReq);
		User user = null;
		if( account != null )
		{
			user = userManager.getUser(account);
		}
		else if( email != null)
		{
			userManager.getUserByEmail(email);
		}
		Boolean ok = false;
		if (user != null)
		{
			AuthenticationRequest aReq = userManager.createAuthenticationRequest(inReq, password, user);
			
			String templogincode = inReq.getRequestParameter("templogincode");
			aReq.putProperty("templogincode", templogincode);
			
			if (userManager.authenticate(aReq))
			{
				ok = true;
				String md5 = getCookieEncryption().getPasswordMd5(user.getPassword());
				String value = user.getUserName() + "md542" + md5;
				inReq.putPageValue("entermediakey", value); //TODO: Remove this, its slow
				inReq.putSessionValue(aReq.getCatalogId() + "user", user);
				inReq.putPageValue("user", user);
				inReq.putPageValue("commandSucceeded", "ok");
			}
			else
			{
				inReq.putPageValue("oe-exception", "Invalid Login");
				inReq.putPageValue("commandSucceeded", "invalidlogin");
			}
		}
		else
		{
			log.info("No such user" + account);
			inReq.putPageValue("commandSucceeded", "nouser");
//			String catalogid =user.get("catalogid");
//			inReq.putSessionValue(catalogid + "user", null);
		}

		inReq.putPageValue("id", account);
		inReq.putPageValue("authenticated", ok);
		return ok;

	}

	/**
	 * @param inUser
	 * @param inReq
	 */
	protected boolean loginAndRedirect(AuthenticationRequest inAReq, WebPageRequest inReq) throws Exception
	{
		User inUser = inAReq.getUser();
		boolean userok = false;
		String maxcounts = inReq.findValue("maxfailedloginattemps");

		int maxattemps = 5;
		if (maxcounts != null)
		{
			try
			{
				maxattemps = Integer.parseInt(maxcounts);
			}
			catch (Exception e)
			{

			}
		}
		boolean disable = Boolean.parseBoolean(inReq.getContentProperty("autodisableusers"));
		UserManager userManager = getUserManager(inReq);
		if (inUser != null)
		{
			// Save our logged-in user in the session,
			// because we use it again later.
			if (inAReq.getPassword() != null || inUser.getPassword() != null)
			{
				if (inUser.isEnabled())
				{
					userok = userManager.authenticate(inAReq); //<---- This is it!!!! we login
				}
				else
				{
					inReq.putSessionValue("oe-exception", "User has been disabled");
					inReq.putPageValue("oe-exception", "User has been disabled");
					inReq.putPageValue("disabled", true);
					inReq.putPageValue("invaliduser", inUser);
					getUserManager(inReq).fireUserEvent(inUser, "disabled");
					log.info("User disabled");
					
					String categoryid = inReq.getRequestParameter("categoryid");
					if(categoryid != null) 
					{
						Data category = getMediaArchive(inReq).getCachedData("category", categoryid);
						if(category != null)
						{
							inReq.putPageValue("category", category);
						}
					}
					
					return false;
				}
			}
		}

		if (userok)
		{
			if (disable)
			{
				//This resets the "failed attemps" to 0.
				inUser.setProperty("failedlogincount", "0");
				userManager.saveUser(inUser);

			}

			//			// check validation
			//			String lastTime= inUser.getLastLoginTime(); 
			//			if(lastTime != null){
			//				int duration= Integer.parseInt(inReq.getPageProperty("active-duration"));
			//				if(duration >-1){
			//					//Date lastDateTime = DateStorageUtil.getStorageUtil().parseFromStorage(lastTime);
			//					double eslapsedPeriod =DateStorageUtil.getStorageUtil().compareStorateDateWithCurrentTime(lastTime);
			//					if( eslapsedPeriod > duration){
			//						inReq.putPageValue("inactive", true);
			//						inReq.putSessionValue("inactive", true);
			//						inReq.putPageValue("inactiveuser", inUser);
			//						inReq.putSessionValue("active-duration", String.valueOf(duration));
			//						return false;
			//					}
			//					
			//				}
			//			}
			
			//LastLogin set
			String time = DateStorageUtil.getStorageUtil().formatForStorage(new Date());
			inUser.setProperty("lastlogin", String.valueOf(time));

			inReq.removeSessionValue("userprofile");
			//inReq.putSessionValue("user", inUser);
			
			//TODO: Move this into the manager
			String catalogid = userManager.getUserSearcher().getCatalogId();
			inUser.setProperty("catalogid", catalogid);
			inReq.putSessionValue(catalogid + "user", inUser);
			
			String realcatalog = inReq.findPathValue("catalogid");
			inReq.putSessionValue(realcatalog + "user", inUser);
			
			createUserSession(inReq);
			// user is now logged in
			log.info("User logged in " + inReq.getUser());
			
			String sendTo = (String) inReq.getSessionValue("fullOriginalEntryPage");
			if (sendTo == null)
			{
				String appid = inReq.findValue("applicationid");
				if (appid != null)
				{
					sendTo = "/" + appid + "/index.html";
				}
				else
				{
					sendTo = "/index.html";
				}
			}
			savePasswordAsCookie(inUser, inReq);
			String cancelredirect = inReq.findValue("cancelredirect");
			if (!Boolean.parseBoolean(cancelredirect))
			{
				sendTo = sendTo.replace("oemaxlevel=", "canceloemaxlevel=");
				inReq.redirect(sendTo);
			}
			return true;
		}
		else
		{
			if (disable)
			{
				String failedLoginCount = inUser.get("failedlogincount");
				int fails = 0;
				if (failedLoginCount != null)
				{
					fails = Integer.parseInt(failedLoginCount);
				}
				fails++;
				inUser.setProperty("failedlogincount", String.valueOf(fails));
				if (fails >= maxattemps)
				{
					{
						User user = inReq.getUser();
						if (user != null)
						{
							String md5 = getCookieEncryption().getPasswordMd5(user.getPassword());
							String value = user.getUserName() + "md542" + md5;
							inReq.putPageValue("entermediakey", value);
						}
					}
					inUser.setEnabled(false);
				}
				userManager.saveUser(inUser);

			}

			//	inReq.putSessionValue("oe-exception", "Invalid Logon");
			inReq.putPageValue("oe-exception", "Invalid Logon");
			return false;
		}

	}
	public void savePasswordAsCookie(WebPageRequest inReq)
	{
		User user = getUserSearcher(inReq).getUser(inReq.getUserName());
		//Latest one
		savePasswordAsCookie(user, inReq);
		String catalogid = getUserManager(inReq).getUserSearcher().getCatalogId();
		inReq.putSessionValue(catalogid + "user", user);
		inReq.putPageValue( "user", user);
		
	}
	public void savePasswordAsCookie(User user, WebPageRequest inReq) throws OpenEditException
	{
		if (user.isVirtual())
		{
			log.debug("User is virtual. Not saving cookie");
			return;
		}
		BaseAutoLogin autologin = (BaseAutoLogin)getModuleManager().getBean(inReq.findPathValue("catalogid"),"autoLoginWithCookie");
		autologin.saveCookieForUser(inReq, user);
	}

	public void loadEnterMediaKey(WebPageRequest inReq)
	{
		String account = inReq.getRequestParameter("id");
		if(Boolean.parseBoolean(inReq.findValue("forcelowercaseusername"))) {
			account = account.toLowerCase();
		}
		String password = inReq.getRequestParameter("password");
		UserManager userManager = getUserManager(inReq);
		User user = userManager.getUser(account);
		Boolean ok = false;
		if (user != null)
		{
			AuthenticationRequest aReq = userManager.createAuthenticationRequest(inReq, password, user);
			if (userManager.authenticate(aReq))
			{
				String md5 = getCookieEncryption().getPasswordMd5(user.getPassword());
				String value = user.getUserName() + "md542" + md5;
				inReq.putPageValue("entermediakey", value);
				inReq.putPageValue("user", user);
			}
		}
	}

	public void logout(WebPageRequest inReq) throws OpenEditException
	{
		UserManager usermanager = getUserManager(inReq);
		String catalogid = usermanager.getUserSearcher().getCatalogId();
		User user = (User) inReq.getSessionValue(catalogid + "user");
		if(user == null) {
			user = (User) inReq.getPageValue("user");
		}
		if (user == null)
		{
			//this user is already logged out
			return;
		}
		usermanager.logout(user);
		inReq.removeSessionValue(catalogid + "user");

		Enumeration enumeration = inReq.getSession().getAttributeNames();
		List toremove = new ArrayList();
		while (enumeration.hasMoreElements())
		{
			String id = (String) enumeration.nextElement();
			toremove.add(id);
		}
		for (Iterator iter = toremove.iterator(); iter.hasNext();)
		{
			String id = (String) iter.next();
			inReq.removeSessionValue(id);
			// inReq.removeSessionValue("editMode"); //legacy
			// inReq.removeSessionValue("username"); //legacy
			// inReq.removeSessionValue("user");
		}

		inReq.removePageValue("user");
		inReq.removePageValue("userprofile");
		getCookieEncryption().removeCookie(inReq,AutoLoginProvider.ENTERMEDIAKEY);
		getCookieEncryption().removeCookie(inReq,"entermedia.keyopenedit");
		getCookieEncryption().removeCookie(inReq,"JSESSIONID"); //Added this to try and logout of all the sub-domains
		

		String referrer = inReq.getRequestParameter("editingPath");
		if (referrer != null && !referrer.startsWith("http"))
		{
			Page epath = getPageManager().getPage(referrer);
			if (referrer.indexOf("/openedit") >= 0 || !epath.isHtml() || !epath.exists())
			{
				referrer = null;
			}
		}
		if (referrer != null)
		{
			inReq.redirect(referrer);
		}
	}

	public void autoLogin(WebPageRequest inReq) throws OpenEditException
	{
		createUserSession(inReq);
		String forceAuto = inReq.findValue("forceautologin");
		
		if (inReq.getUser() != null && !Boolean.parseBoolean(forceAuto))
		{
			return;
		}
		for (Iterator iterator = getAutoLoginProviders().iterator(); iterator.hasNext();)
		{
			AutoLoginProvider login = (AutoLoginProvider) iterator.next();
			AutoLoginResult result = login.autoLogin(inReq);
			if( result != null)
			{
				UserManager userManager = getUserManager(inReq);
				String catalogid = userManager.getUserSearcher().getCatalogId();
				inReq.putSessionValue(catalogid + "user", result.getUser());
				inReq.putPageValue( "user", result.getUser());
				//userManager.fireUserEvent(result.getUser(), "autologin");
				return;
			}
		}
	}

	
	public User createUserSession(WebPageRequest inReq)
	{

		User user = (User)inReq.getPageValue("user");
		if( user == null)
		{
			String catalogid = inReq.findPathValue("catalogid");
			user = (User) inReq.getSessionValue(catalogid + "user");
			inReq.putPageValue( "user", user);
		}

		return user;
		
	}

	protected void quickLogin(WebPageRequest inReq) throws OpenEditException
	{
		if (inReq.getUser() == null)
		{
			String username = inReq.getRequestParameter("accountname");
			if(Boolean.parseBoolean(inReq.findValue("forcelowercaseusername"))) {
				username = username.toLowerCase();
			}
			String password = inReq.getRequestParameter("password");
			if (password == null)
			{
				password = inReq.getRequestParameter("code");
				password = getUserManager(inReq).getStringEncryption().decrypt(password);
			}
			if (password == null)
			{
				return;
			}
			User user = getUserManager(inReq).getUser(username);
			if (user == null)
			{
				return;
			}
			if (!getUserManager(inReq).authenticate(user, password))
			{
				throw new OpenEditException("Did not authenticate: " + username);
			}
			else
			{
				inReq.setUser(user);
			}
		}
	}

	



	public void forwardToSecureSocketsLayer(WebPageRequest inReq)
	{
		String useSecure = inReq.getPage().get("useshttps");

		if (Boolean.parseBoolean(useSecure) && inReq.getRequest() != null)
		{
			String host = inReq.getPage().get("hostname");
			if (host == null)
			{
				host = inReq.getPage().get("hostName");
			}
			if (host == null)
			{
				return;
			}
			if (host.contains("localhost"))
			{
				return;
			}
			if (host != null && !inReq.getRequest().isSecure())
			{
				String path = "https://" + host + inReq.getPathUrl();
				log.info("Forward to address " + path);
				inReq.redirect(path);
			}
		}
	}

	/**
	 * This is deprecated because it can't handle directory redirects and is
	 * buggy and hard to read. Use redirectHost and redirectInternal instead.
	 * 
	 * @param inReq
	 * @throws OpenEditException
	 */
	public void redirect(WebPageRequest inReq) throws OpenEditException
	{
		String path = inReq.getCurrentAction().getChildValue("redirectpath");
		if( path != null)
		{
			path = inReq.getContentPage().getPageSettings().replaceProperty(path);
		}
		if (path == null)
		{
			path = inReq.findValue("redirectpath");
		}
		if (path == null)
		{
			path = inReq.getPage().get("redirectpath");
		}		
		if (path != null && inReq.getRequest() != null)
		{
			if (path.endsWith("/"))
			{
				path = path.substring(0, path.length() - 1);
			}
			else if (!inReq.getPath().equals(path))
			{
				inReq.redirect(path);
			}
		}
	}

	/**
	 * This is used to direct to a different host. I can be used in conjunction
	 * with redirectInternal(). <path-action name="Admin.redirectHost">
	 * <redirecthost>http://xyz.com/</redirecthost> </path-action>
	 * 
	 * @param inReq
	 * @throws OpenEditException
	 */
	public void redirectHost(WebPageRequest inReq) throws OpenEditException
	{
		if (inReq.getRequest() == null)
		{
			return;
		}
		String host = inReq.findValue("redirecthost");
		URLUtilities utils = (URLUtilities) inReq.getPageValue(PageRequestKeys.URL_UTILITIES);
		if (utils == null)
		{
			utils = new URLUtilities(inReq.getRequest(), inReq.getResponse());
		}
		String ending = utils.requestPathWithArgumentsNoContext();
		String server = utils.buildRoot();
		if (server != null && server.endsWith("/"))
		{
			server = server.substring(0, server.length() - 1);
		}
		if (host.endsWith("/"))
		{
			host = host.substring(0, host.length() - 1);
		}

		if (!host.equals(server))
		{
			String redirectPath = host + ending;
			//log.info("Redirecting " + host + " AND " + server);
			inReq.redirectPermanently(redirectPath);
		}
		else
		{
			return;
		}
	}

	/**
	 * This is used to redirect between pages on the same server. It may be used
	 * in conjunction with redirectHost(). This should be used instead of the
	 * old Admin.redirect <path-action name="Admin.redirectInternal"> <!--This
	 * is an example of directory substitution-->
	 * <redirectpath>/newpath/</redirectpath>
	 * <redirectroot>/oldpath/</redirectroot> </path-action> <path-action
	 * name="Admin.redirectInternal"> <!--This is an example of absolute
	 * substitution--> <redirectpath>/newpath/index.html</redirectpath>
	 * <redirectroot>*</redirectroot> </path-action>
	 * 
	 * @param inReq
	 * @throws OpenEditException
	 */
	public void redirectInternal(WebPageRequest inReq) throws OpenEditException
	{
		String path = inReq.getCurrentAction().get("redirectpath");
		path = inReq.getPage().getPageSettings().replaceProperty(path);
		String rootdir = inReq.getCurrentAction().get("redirectroot");
		rootdir = inReq.getPage().getPageSettings().replaceProperty(rootdir);
		URLUtilities utils = (URLUtilities) inReq.getPageValue(PageRequestKeys.URL_UTILITIES);
//		String server = utils.buildRoot();
//		if (server.endsWith("/"))
//		{
//			server = server.substring(0, server.length() - 1);
//		}
		String ending = utils.requestPathWithArgumentsNoContext();
		String redirectPath;

		if (rootdir == null || rootdir.equals("*"))
		{
			redirectPath = path;
		}
		else
		{
			redirectPath = ending.replace(rootdir, path);
		}

		if (!redirectPath.equals(ending))
		{
			inReq.redirectPermanently(redirectPath);
		}
		else
		{
			return;
		}
	}

	public void redirectToOriginal(WebPageRequest inReq)
	{
		String orig = inReq.findValue("origURL");
		String editPath = inReq.getRequestParameter("editPath");
//		if (orig == null)
//		{
//			orig = inReq.getRequestParameter("origURL");
//		}	
			
//		if(orig == null)
//		{
//			orig = inReq.getReferringPage();
//		}
		
		if (orig != null )
		{
			if( orig.startsWith("http") )
			{
				log.error("Orig starts with " + orig);
				inReq.redirect("/index.html");
				return;
			}
			
			if (orig.indexOf("?") == -1 && editPath != null)
			{
				inReq.redirect(orig + "?path=" + editPath + "&cache=false");
			}
			else
			{
				inReq.redirect(orig);
			}
		}
		else
		{
			//log.error("No origURL specified");
		}
	}

	public SendMailModule getSendMailModule()
	{
		return sendMailModule;
	}

	public void setSendMailModule(SendMailModule sendMailModule)
	{
		this.sendMailModule = sendMailModule;
	}

	public void toogleAdminToolbar(WebPageRequest inReq)
	{
		User user = inReq.getUser();
		if (user != null)
		{
			boolean bol = user.getBoolean("openadmintoolbar");
			if (bol)
			{
				user.setValue("openadmintoolbar", false);
			}
			else
			{
				user.setValue("openadmintoolbar", true);
			}
			if (!user.isVirtual())
			{
				getUserManager(inReq).saveUser(user);
			}
		}
		redirectToOriginal(inReq);
	}

	public StringEncryption getCookieEncryption()
	{
		//		if (fieldCookieEncryption == null)
		//		{
		//			fieldCookieEncryption = new StringEncryption();
		////			String KEY = "SomeWeirdReallyLongStringYUITYGFNERDF343dfdGDFGSDGGD";
		////			fieldCookieEncryption.setEncryptionKey(KEY);
		//		}
		return fieldCookieEncryption;
	}

	public void setCookieEncryption(StringEncryption inCookieEncryption)
	{
		fieldCookieEncryption = inCookieEncryption;
	}

	public void toggleProperty(WebPageRequest inReq) throws Exception
	{
		User user = inReq.getUser();
		if (user != null)
		{
			String id = inReq.getRequestParameter("propertyid");
			if (id == null)
			{
				id = inReq.getRequestParameter("id");
			}
			if (id != null)
			{
				boolean has = user.hasProperty(id);
				if (has)
				{
					user.setValue(id,null);
				}
				else
				{
					user.setValue(id, String.valueOf(has));
				}
				getUserManager(inReq).saveUser(user);
			}
		}
	}

	public String getTheme(WebPageRequest inReq) throws Exception
	{
		String theme = inReq.findValue("themeprefix");
		inReq.putPageValue("themeprefix", theme);
		return theme;
	}

	protected Page findWelcomePage(Page inDirectory) throws OpenEditException
	{
		String dir = inDirectory.getPath();
		if (!dir.endsWith("/"))
		{
			dir = dir + "/";
		}
		for (Iterator iterator = getWelcomeFiles().iterator(); iterator.hasNext();)
		{
			String index = (String) iterator.next();
			if (getPageManager().getRepository().doesExist(dir + index))
			{
				return getPageManager().getPage(dir + index, true);
			}
		}
		return getPageManager().getPage(dir + "index.html", true);
	}

	public void checkExist(WebPageRequest inReq) throws Exception
	{
		check404(inReq);
	}

	public void check404(WebPageRequest inReq) throws Exception
	{
		boolean exist = inReq.getPage().exists();
		if (exist)
		{
			return;
		}

		PageStreamer streamer = inReq.getPageStreamer();
		if (streamer != null)
		{
			streamer.getWebPageRequest().putPageValue("pathNotFound", inReq.getPath());
		}
		String isVirtual = inReq.getPage().get("virtual");
		if (Boolean.parseBoolean(isVirtual))
		{
			return;
		}

		URLUtilities utils = (URLUtilities) inReq.getPageValue(PageRequestKeys.URL_UTILITIES);

		if (utils != null)
		{
			//redirecting only works relative to a webapp
			if (streamer != null)
			{
				streamer.getWebPageRequest().putPageValue("forcedDestinationPath", utils.requestPathWithArgumentsNoContext());
			}
		}
		PageManager pageManager = getPageManager();
		if (inReq.getPage().isHtml() && inReq.isEditable())
		{
			Page wizard = pageManager.getPage("/system/nopagefound.html");
			if (wizard.exists())
			{
				inReq.getPageStreamer().include(wizard);
				inReq.setHasRedirected(true);
				return;
			}
		}
		if (!inReq.getPage().isHtml())
		{
			HttpServletResponse response = inReq.getResponse();
			if (response != null)
			{
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				inReq.setHasRedirected(true);
				return;
			}
		}

		if (inReq.getContentPage().getPath().equals(inReq.getPath()))
		{
			//log.info( "Could not use  add page wizard. 404 error on: " + inReq.getPath() );
			Page p404 = pageManager.getPage("/error404.html");
			if (p404.exists())
			{
				HttpServletResponse response = inReq.getResponse();
				if (response != null)
				{
					response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				}
				inReq.putProtectedPageValue("content", p404);
				//inReq.forward(p404.getPath());
				return;
			}
			else
			{
				log.error("Could not report full 404 error on: " + inReq.getPath() + ". Make sure the 404 error page exists " + p404.getPath());
				//other users will get the standard file not found error
				HttpServletResponse response = inReq.getResponse();
				if (response != null)
				{
					response.sendError(HttpServletResponse.SC_NOT_FOUND);
					inReq.setHasRedirected(true);
				}
			}
		}
		else
		{
			inReq.getWriter().write("404 on " + inReq.getPath());
			inReq.getWriter().flush();
			inReq.setHasRedirected(true);

		}
	}

	public void createGuestUser(WebPageRequest inReq)
	{
		User user = inReq.getUser();
		if (user == null)
		{
			Group guest = getGroupSearcher(inReq).getGroup("guest");
			if (guest == null)
			{
				getUserManager(inReq).createGroup("guest", "Guest");
			}

			user = getUserManager(inReq).createGuestUser(null, null, "guest");
			String catalogid = getUserManager(inReq).getUserSearcher().getCatalogId();
			user.setVirtual(true);
			user.setProperty("catalogid", catalogid);
			user.setEnabled(false);
			//getUserManager(inReq).saveUser(user);
			inReq.putSessionValue(catalogid + "user", user);
			inReq.putPageValue("user", user);

		}

	}
	
	public void createTempUser(WebPageRequest inReq)
	{
		String email = inReq.getRequestParameter(EMAIL);
		if (email != null) {
			
			User user =  getUserManager(inReq).createTempUserFromEmail(email);
			inReq.putSessionValue(inReq.findPathValue("catalogid") + "user", user);
			inReq.putPageValue("user", user);
		}

	}
	

	public void switchToUser(WebPageRequest inReq)
	{
		User user = inReq.getUser();
		if (!user.isInGroup("administrators"))
		{
			log.info("Only administrators can switch users" );
			return;
		}

		String userid = inReq.getRequestParameter("userid");
		User target = getUserManager(inReq).getUser(userid);
		
		if (target != null)
		{
			clearSession(inReq);		
			inReq.putSessionValue("realuser", user);
			String catid = inReq.findPathValue("catalogid");
			inReq.putSessionValue(catid + "user", target);
			inReq.putSessionValue("system" + "user", target);
			inReq.putPageValue( "user", target);
			String appid = inReq.findValue("applicationid");
			inReq.redirect("/" + appid + "/");
		}

	}

	public void revertToUser(WebPageRequest inReq)
	{

		User olduser = (User) inReq.getSessionValue("realuser");
		if (olduser != null)
		{
			clearSession(inReq);		

			inReq.putSessionValue("realuser", null);
			inReq.putSessionValue(olduser.get("catalogid") + "user", olduser);
			createUserSession(inReq);
		}

	} 
	
	public void clearSession(WebPageRequest inReq){
		
		Enumeration keys = inReq.getSession().getAttributeNames();
		while (keys.hasMoreElements())
		{
			  String key = (String)keys.nextElement();

			inReq.putSessionValue(key, null);
		}
	}

	public void allowAccesControl(WebPageRequest inReq)
	{
		HttpServletResponse req = inReq.getResponse();
		if( req != null)
		{
			String domain = inReq.getSiteRoot();
			req.setHeader("Access-Control-Allow-Origin",domain);
			req.setHeader("Access-Control-Allow-Credentials","true");
		}	
	}	
	
	public void generateKey(WebPageRequest inReq) {
		
		String account = inReq.getRequestParameter("accountname");
		String password = inReq.getRequestParameter("password");

		if(Boolean.parseBoolean(inReq.findValue("forcelowercaseusername"))) {
			if(account != null) {
				account = account.toLowerCase();
			}
		}
		UserManager userManager = getUserManager(inReq);

		User user = userManager.getUser(account);
		if(user.get("googlesecretkey") != null) {
			inReq.putPageValue("error", "Sorry, your code has already been generated.  IF you need it reset contact your administrator");
			return;
		}
		
		AuthenticationRequest aReq = getUserManager(inReq).createAuthenticationRequest(inReq, password, user);
		if (userManager.getAuthenticator().authenticate(aReq))
		{
			
			GoogleAuthenticator gAuth = new GoogleAuthenticator();
			final GoogleAuthenticatorKey key = gAuth.createCredentials();
			String secret = key.getKey();
			user.setValue("googlesecretkey", secret);
			getSearcherManager().getSearcher(aReq.getCatalogId(), "user").saveData(user);
			inReq.putPageValue("googlesecret", secret);
			String sitename = getMediaArchive(inReq).getCatalogSettingValue("sitename");
//			if(sitename == null) {
//				sitename = inReq.findValue("siteroot");
//			}
//			if(sitename == null) {
//				sitename = inReq.getSiteRoot();
//			}
			if(sitename == null) 
			{
				sitename="Entermedia";
			}
			
			String qr = GoogleAuthenticatorQRGenerator.getOtpAuthURL(sitename, user.getEmail(), key);
			inReq.putPageValue("qrcode", qr);
		}
		else 
		{
			inReq.putPageValue("invalid", "Sorry, couldn't login.  Please try again.");
		}
	}
	
	public void resetKey(WebPageRequest inReq) {
		String foruser = inReq.getRequestParameter("username");
		UserManager userManager = getUserManager(inReq);
		User user = userManager.getUser(foruser);
		if (user != null) {
			user.setValue("googlesecretkey", "");
		}
		userManager.saveUser(user);
		return;

	}
	public void loadTemporaryKey(WebPageRequest inReq)
	{
		StringEncryption encoder = getUserManager(inReq).getStringEncryption();

		String foruser = inReq.getRequestParameter("username");
		if( foruser != null)
		{
			Object canUpload = inReq.getPageValue("caneditusersgroups");
			if( !Boolean.parseBoolean(String.valueOf(canUpload)))
			{
				throw new OpenEditException("No permissions to view keys of other users");
			}
		}
		if( foruser == null)
		{
			foruser = inReq.getUserName();
		}
		User user = getUserManager(inReq).getUser(foruser);
		String entermediakey = "";
		if( user != null)
		{
			entermediakey = encoder.getTempEnterMediaKey(user);
		}

		inReq.putPageValue("tempentermediakey",entermediakey);
	}

	public static String VALID_METHODS = "DELETE, HEAD, GET, OPTIONS, POST, PUT";
	public static String VALID_HEADERS = "x-csrf-token,x-file-name,x-file-size,x-requested-with,cache-control,access-control-allow-credentials,"
	                                   + "authorization,origin,content-type,accept,x-email,x-token,x-tokentype,access-control-allow-headers,access-control-allow-method,"
	                                   + "access-control-allow-origin";

	public void allowCorsHeaders(WebPageRequest inReq)
	{
		HttpServletResponse request = inReq.getResponse();
		HttpServletRequest httpRequest = (HttpServletRequest) inReq.getRequest();
		if( httpRequest != null)
		{
			String method = inReq.getRequest().getMethod();
			if(method == null) {
				return;
			}
			boolean isoptions = method.equals("OPTIONS");
			//see https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS
			String origin = httpRequest.getHeader("Origin");
			
			
			boolean forcealloworigin = Boolean.parseBoolean(inReq.findValue("forcealloworigin"));
			if (origin != null && ( forcealloworigin || isoptions || inReq.getUser() != null) )
			{
				request.setHeader("Access-Control-Allow-Origin",origin);
			}
			else
			{
				request.setHeader("Access-Control-Allow-Origin","*");  //This is not useful
			}
			request.setHeader("Access-Control-Allow-Credentials","true");
			request.setHeader("Access-Control-Allow-Methods",VALID_METHODS);
			request.setHeader("Access-Control-Allow-Headers", VALID_HEADERS);
          	request.setHeader("Access-Control-Max-Age", "3600");
			if( isoptions )
			{
				inReq.setHasRedirected(true);
				inReq.setCancelActions(true);
				request.setStatus(200);
			}
			
			
			
			//Allow be in a Frame only from specific domains. 
			//Also Review Nginx conf, it may be overwrithing this setting.
			if(Boolean.parseBoolean(inReq.findValue("allowframes"))) {
				String frameancestors = (String) inReq.findValue("allowframesfrom");
				if (frameancestors != null) {
					//frameancestors expected: frame-ancestors 'self' https://domain.com https://domain2.com
					request.setHeader("Content-Security-Policy", frameancestors);
				}
			}
          	
		}	
		
	}
	 

	
}
