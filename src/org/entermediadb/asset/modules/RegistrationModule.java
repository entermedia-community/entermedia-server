package org.entermediadb.asset.modules;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.mail.internet.InternetAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.email.PostMail;
import org.entermediadb.email.TemplateWebEmail;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.profile.UserProfile;
import org.openedit.users.Group;
import org.openedit.users.User;
import org.openedit.users.UserManager;
import org.openedit.users.UserSearcher;
import org.openedit.users.authenticate.PasswordGenerator;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.RequestUtils;

public class RegistrationModule extends BaseMediaModule
{

	protected UserManager userManager;
	protected SearcherManager fieldSearcherManager;
	protected PostMail fieldPostMail;
	
	
	
	public PostMail getPostMail() {
		return fieldPostMail;
	}

	public void setPostMail(PostMail inPostMail) {
		fieldPostMail = inPostMail;
	}

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}

	private static final Log log = LogFactory.getLog(RegistrationModule.class);

	protected RequestUtils fieldRequestUtils;

	public RequestUtils getRequestUtils()
	{
		return fieldRequestUtils;
	}

	public void setRequestUtils(RequestUtils inRequestUtils)
	{
		fieldRequestUtils = inRequestUtils;
	}

	static final String USERNAME_PARAMETER = "username";

	public UserManager getUserManager()
	{
		return userManager;
	}

	public void setUserManager(UserManager inUserManager)
	{
		userManager = inUserManager;
	}

	public void createGuestUser(WebPageRequest inReq)
	{
		User user = inReq.getUser();
		if (user == null)
		{
			Group guest = getUserManager().getGroup("guest");
			if (guest == null)
			{
				getUserManager().createGroup("guest", "Guest");
			}

			user = getUserManager().createGuestUser(null, null, "guest");
			inReq.putPageValue("user", user);
			inReq.putSessionValue("user", user);
		}

	}

	public void checkUniqueEmail(WebPageRequest inReq)
	{

		boolean allowduplicates = Boolean.parseBoolean(inReq.findValue("allowduplicateemails"));
		if (allowduplicates)
		{
			return;
		}
		String email = inReq.getRequestParameter("email.value");
		if(email == null){
			email = inReq.getRequestParameter("email");
		}
		if(email == null){
			return;
		}
		UserSearcher searcher =(UserSearcher) getMediaArchive(inReq).getSearcherManager().getSearcher("system", "user"); 
		Data usert = (Data) searcher.searchByField("email", email);
		if(usert == null){
			return;
		}
		User user = (User) searcher.searchById(usert.getId());
		String id = inReq.getRequestParameter("id");
		User current = getUserManager().getUser(id);
		if (user != null)
		{
			if(id != null){
				if(id.equals(user.getId())){
					return;
				}
			}	
			// errors.put("error-email-in-use", "This email address is in use");
			// inReq.putPageValue("errors", errors);
			inReq.putPageValue("emailinuse", true);
			if(current != null){
				inReq.setRequestParameter("email.value", current.getEmail());
			}
			cancelAndForward(inReq);
		}

	}

	public boolean checkCouponCode(WebPageRequest inReq)
	{
		if (Boolean.parseBoolean(inReq.findValue("requirecode")))
		{

			String catalogid = inReq.findValue("catalogid");
			String couponcode = inReq.getRequestParameter("code.value");

			Map errors = new HashMap();
			Searcher prepaidsearcher = getSearcherManager().getSearcher(catalogid, "prepaid");
			Data prepaidcode = (Data) prepaidsearcher.searchById(couponcode);
			if (prepaidcode == null)
			{
				prepaidcode = (Data) inReq.getPageValue("coupon");
			}

			if (prepaidcode == null)
			{

				log.info("invalid code usage detected: " + couponcode);
				errors.put("error-invalidcode", "This code is invalid");
				inReq.putPageValue("errors", errors);
				cancelAndForward(inReq);
				return false;
			}
			boolean available = Boolean.parseBoolean(prepaidcode.get("available"));
			if (!available)
			{
				log.info("attempted to use already used code: " + couponcode);
				errors.put("error-codeused", "This code was already used");
				inReq.putPageValue("errors", errors);
				cancelAndForward(inReq);
				return false;
			}
			String enddate = prepaidcode.get("expiry");
			if(enddate != null){
				Date now = new Date();
				Date end = DateStorageUtil.getStorageUtil().parseFromStorage(enddate);
				if(end.before(now)){
					errors.put("error-expired", "This code is expired");
					inReq.putPageValue("errors", errors);
					cancelAndForward(inReq);
					return false;
				}
			}
			
			String startdate = prepaidcode.get("startdate");
			if(startdate != null){
				Date now = new Date();
				Date start = DateStorageUtil.getStorageUtil().parseFromStorage(startdate);
				if(now.before(start)){
					errors.put("error-early", "This code is not available yet.");
					inReq.putPageValue("errors", errors);
					cancelAndForward(inReq);
					return false;
				}
			}
			
			
			
			log.info("processed code successfully" + couponcode);

			inReq.putSessionValue("coupon", prepaidcode);
			inReq.putPageValue("coupon", prepaidcode);

			return true;
		}
		else
		{
			return false;
		}
	}

	private void cancelAndForward(WebPageRequest inReq)
	{
		String errorURL = inReq.findValue("errorURL");
		inReq.setHasForwarded(true);
		inReq.setCancelActions(true);
		inReq.forward(errorURL);

	}

	public void registrationReceived(WebPageRequest inReq) throws Exception
	{
		log.info("starting new registration");

		Map errors = new HashMap();
		String email = inReq.getRequestParameter("email.value");
		String password = inReq.getRequestParameter("password.value");
		String password2 = inReq.getRequestParameter("password2.value");
		if (password2 == null)
		{
			password2 = inReq.getRequestParameter("passwordmatch.value");
		}
		String errorURL = inReq.findValue("errorURL");

		String[] fields = inReq.getRequestParameters("field");

		if (fields == null)
		{
			if (errorURL != null)
			{
				errors.put("nodata", "No data received by form - please try again.");
				inReq.setHasForwarded(true);
				inReq.putPageValue("errors", errors);
				inReq.setCancelActions(true);
				inReq.forward(errorURL);

			}
			return;
		}

		Group guestgroup = getUserManager().getGroup("guest");
		if (guestgroup == null)
		{
			guestgroup = getUserManager().createGroup("guest", "Guest");
		}

		boolean generatepassword = Boolean.parseBoolean(inReq.findValue("generatepassword"));
		if (!generatepassword)
		{
			if (password == null)
			{
				if (errorURL != null)
				{
					errors.put("password", "error-no-password");
					inReq.setHasForwarded(true);
					inReq.putPageValue("errors", errors);
					inReq.setCancelActions(true);
					inReq.forward(errorURL);

				}
				return;

			}

			if (!password.equals(password2) || password.length() == 0)
			{
				if (errorURL != null)
				{
					errors.put("password", "error-no-password-match");
					inReq.setHasForwarded(true);
					inReq.putPageValue("errors", errors);
					inReq.setCancelActions(true);
					inReq.forward(errorURL);

				}
				return;

			}
		}

		UserSearcher searcher = (UserSearcher) getSearcherManager().getSearcher("system", "user");

		User current = inReq.getUser();
		if (current != null && current.isVirtual())
		{
			current.setVirtual(false);

		}
		else
		{
			current = getUserManager().createUser(null, password);
		}

		if (password != null && password.length() > 0)
		{
			current.setPassword(password);
		}
		else if (generatepassword)
		{

			current.setPassword(new PasswordGenerator().generate());// Integer.toString((int)(100000
			// +
			// generator.nextDouble()
			// *
			// 899999D));

		}

		current.setEmail(email);

		current.addGroup(guestgroup);

		searcher.updateData(inReq, fields, current);
		searcher.saveData(current, null);

		log.info("user id was" + current.getId());

		current.setValue("password",null);
		current.setValue("password2",null);

		handleValidationCodes(inReq, current);

		boolean enable = Boolean.parseBoolean(inReq.findValue("autoenable"));
		if (enable)
		{
			current.setEnabled(true);
		}
		getUserManager().saveUser(current);
		inReq.putPageValue("saved", "true");
		inReq.putPageValue("newuser", current);
		inReq.putPageValue("password", password);
		// lets create a user profile now too.
		MediaArchive archive = getMediaArchive(inReq);
		Searcher upsearcher = (Searcher) archive.getSearcher("userprofile");
		UserProfile up = (UserProfile) upsearcher.createNewData();
		up.setProperty("settingsgroup", "guest");
		up.setUser(current);
		up.setId(current.getId());
		upsearcher.saveData(up, null);
		inReq.putPageValue("data", up);

		Group notifygroup = getUserManager().getGroup("registration");
		if (notifygroup == null)
		{
			notifygroup = getUserManager().createGroup("registration", "Registration");
		}
		if (email != null)
		{
			inReq.setRequestParameter("to", email);

		}
		else
		{
			inReq.setRequestParameter("to", "dummy@ijsolutions.ca");
		}
		current.setProperty("creationdate", new Date().toString());
		String subject = inReq.findValue("subjectprefix");
		if (subject == null)
		{
			subject = "New Registration Received";
		}
		inReq.setRequestParameter("subject", subject);
		inReq.putPageValue("registration", current);

		boolean logout = Boolean.parseBoolean(inReq.findValue("autologout"));

		if (logout)
		{

			inReq.removeSessionValue("user");
		}

		boolean login = Boolean.parseBoolean(inReq.findValue("autologin"));

		if (login)
		{

			inReq.putSessionValue("user", current);// this logs in the user that
													// just registered.
		}

	}

	public void checkPasswordMatch(WebPageRequest inReq) throws Exception
	{
		Map errors = new HashMap();
		String password = inReq.getRequestParameter("password.value");
		String password2 = inReq.getRequestParameter("password2.value");
		if (password2 == null)
		{
			password2 = inReq.getRequestParameter("passwordmatch.value");
		}
		String errorURL = inReq.findValue("errorURL");
		if (password == null)
		{
			if (errorURL != null)
			{
				errors.put("password", "error-no-password");
				inReq.setHasForwarded(true);
				inReq.putPageValue("errors", errors);
				inReq.setCancelActions(true);
				inReq.forward(errorURL);
			}
			return;
		}
		if (!password.equals(password2) || password.length() == 0)
		{
			if (errorURL != null)
			{
				errors.put("password", "error-no-password-match");
				inReq.setHasForwarded(true);
				inReq.putPageValue("errors", errors);
				inReq.setCancelActions(true);
				inReq.forward(errorURL);
			}
		}
	}

	protected void handleValidationCodes(WebPageRequest inReq, User inCurrent)
	{
		boolean usecodes = Boolean.parseBoolean(inReq.getPageProperty("usevalidationcodes"));

		String validationcode = inCurrent.get("validationcode");
		if (validationcode == null || validationcode.length() == 0)
		{
			SecureRandom random = new SecureRandom();
			validationcode =

			new BigInteger(130, random).toString(32);
			inCurrent.setProperty("validationcode", validationcode);

		}
		if (!usecodes)
		{
			return;

		}
		else
		{
			inCurrent.setEnabled(false);
		}

	}

	public void validateCode(WebPageRequest inReq)
	{
		String userid = inReq.getRequestParameter("id");
		String code = inReq.getRequestParameter("validationcode");
		Boolean autoenable = Boolean.parseBoolean(inReq.findValue("autoenable"));
		User target = getUserManager().getUser(userid);
		if (target != null)
		{
			String validationcode = target.get("validationcode");
			if (code.equals(validationcode))
			{
				if (autoenable)
				{
					target.setEnabled(true);
				}
				target.setProperty("validationcomplete", "true");
				inReq.putPageValue("validated", true);
				getUserManager().saveUser(target);

				boolean logout = Boolean.parseBoolean(inReq.findValue("autologout"));

				if (logout)
				{

					inReq.removeSessionValue("user");
				}

				boolean login = Boolean.parseBoolean(inReq.findValue("autologin"));

				if (login)
				{

					inReq.putSessionValue("user", target);// this logs in the
															// user that just
															// registered.
				}
				inReq.putPageValue("target", target);

			}
			else
			{
				inReq.putPageValue("validated", false);
			}

		}

	}

	public boolean validateCodes(WebPageRequest inReq)
	{
		boolean required = Boolean.parseBoolean(inReq.getPageProperty("requirevalidationcode"));
		if (!required)
		{
			log.info("not using validation codes");
			return true;
		}
		Data validationCode = (Data) inReq.getSessionValue("registrationcode");
		if (validationCode != null && validationCode.getName() == null)
		{
			validationCode.setName(validationCode.getId());
		}
		if (validationCode != null)
		{
			inReq.putPageValue("registrationcode", validationCode);
			return true;
		}

		String catalogid = inReq.findValue("catalogid");
		String couponcode = inReq.getRequestParameter("registrationcode.value");
		if (couponcode == null)
		{
			return false;
		}

		Map errors = new HashMap();
		Searcher prepaidsearcher = getSearcherManager().getSearcher(catalogid, "registrationcode");
		Data code = (Data) prepaidsearcher.searchById(couponcode);
		if (code == null || Boolean.parseBoolean(code.get("disabled")))
		{
			log.info("invalid code usage detected: " + couponcode);
			errors.put("error-invalidcode", "This code is invalid");
			inReq.putPageValue("errors", errors);
			cancelAndForward(inReq);
			return false;
		}

		User current = inReq.getUser();
		current.setProperty("registrationcode", couponcode);
		PropertyDetails details = prepaidsearcher.getPropertyDetails();

		for (Iterator iterator = details.iterator(); iterator.hasNext();)
		{
			PropertyDetail detail = (PropertyDetail) iterator.next();
			String datatype = detail.getSearchType();
			String value = code.get(detail.getId());
			if (value != null)
			{
				if ("team".equals(datatype) || "group".equals(datatype))
				{
					Group group = getUserManager().getGroup(value);
					if (group != null)
					{
						current.addGroup(group);
					}
				}
				else
				{
					if (!"id".equals(detail.getId()))
					{
						current.setProperty(detail.getId(), value);
					}
				}
			}

		}

		log.info("processed code successfully" + couponcode);

		inReq.putSessionValue("registrationcode", code);
		inReq.putPageValue("registrationcode", code);
		cancelAndForward(inReq);
		return true;
	}

	public void handleCoupon(WebPageRequest inReq)
	{
		Data code = (Data) inReq.getSessionValue("coupon");
		if(code == null){
			return;
		}
		MediaArchive archive = getMediaArchive(inReq);
		
		log.info("detected coupon code: " + code.getId());
		User current = inReq.getUser();
		Searcher prepaidsearcher = getSearcherManager().getSearcher(archive.getCatalogId(), "prepaid");
		code.setProperty("available", "false");

		String collegeid = code.get("college");
		if (collegeid != null)
		{
			Group group = getUserManager().getGroup(collegeid);
			if (group == null)
			{
				group = getUserManager().createGroup(collegeid, collegeid);
				getUserManager().saveGroup(group);

			}
			if (!current.isInGroup(group))
			{
				current.addGroup(group);
			}
			current.setProperty("college", collegeid);

		}
		// if there is a class specified for this code, add them to it.
		String classid = inReq.getRequestParameter("class.value");
		if (classid != null)
		{

			Group group = getUserManager().getGroup(classid + "_students");
			if (group == null)
			{
				group = getUserManager().createGroup(classid + "_students", classid + "_students");
				getUserManager().saveGroup(group);

			}
			if (!current.isInGroup(group))
			{
				current.addGroup(group);
			}

		}
		else
		{
			String codeclass = code.get("class");
			if (!codeclass.contains(" "))
			{
				Group group = getUserManager().getGroup(codeclass + "_students");
				if (group == null)
				{
					group = getUserManager().createGroup(codeclass + "_students", codeclass + "_students");
					getUserManager().saveGroup(group);

				}
				if (!current.isInGroup(group))
				{
					current.addGroup(group);
				}
			}
		}

		code.setProperty("user", current.getId());
		prepaidsearcher.saveData(code, inReq.getUser());
		inReq.removeSessionValue("coupon");
	}

	
	public void sendWelcomeMessage(WebPageRequest inReq) throws Exception{
		MediaArchive archive = getMediaArchive(inReq);
		TemplateWebEmail email = getPostMail().getTemplateWebEmail();

		 
		
		String userid = inReq.getRequestParameter("userid");
		UserProfile p = (UserProfile) getSearcherManager().getData(archive.getCatalogId(), "userprofile", userid );
		inReq.putPageValue("target", p);
		User user = p.getUser();
		if(user == null){
			user = getUserManager().getUser(userid);
		}
		inReq.putPageValue("password", getUserManager().decryptPassword(user));
		
		email.loadSettings(inReq);
		InternetAddress recipient = new InternetAddress();
		recipient.setAddress(p.get("email"));
		recipient.setPersonal(p.toString());
		email.setRecipient(recipient);
		
		email.send();
	}
	
	
	
}
