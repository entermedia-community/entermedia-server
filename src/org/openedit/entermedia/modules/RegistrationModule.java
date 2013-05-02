package org.openedit.entermedia.modules;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.users.UserSearcher;

import com.openedit.WebPageRequest;
import com.openedit.users.Group;
import com.openedit.users.User;
import com.openedit.users.UserManager;
import com.openedit.users.authenticate.PasswordGenerator;
import com.openedit.util.RequestUtils;

public class RegistrationModule extends BaseMediaModule {

	
	protected UserManager userManager;
	protected SearcherManager fieldSearcherManager;

	public SearcherManager getSearcherManager() {
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager) {
		fieldSearcherManager = inSearcherManager;
	}



	private static final Log log = LogFactory.getLog(RegistrationModule.class);
	
	protected RequestUtils fieldRequestUtils;

	public RequestUtils getRequestUtils() {
		return fieldRequestUtils;
	}

	public void setRequestUtils(RequestUtils inRequestUtils) {
		fieldRequestUtils = inRequestUtils;
	}

	

	static final String USERNAME_PARAMETER = "username";

	public UserManager getUserManager() {
		return userManager;
	}

	public void setUserManager(UserManager inUserManager) {
		userManager = inUserManager;
	}

	public void createGuestUser(WebPageRequest inReq) {
		User user = inReq.getUser();
		if (user == null) {
			Group guest = getUserManager().getGroup("guest");
			if (guest == null) {
				getUserManager().createGroup("guest");
			}

			user = getUserManager().createGuestUser(null, null, "guest");
			inReq.putPageValue("user", user);
			inReq.putSessionValue("user", user);
		}

	}

	public void checkUniqueEmail(WebPageRequest inReq) {

		boolean allowduplicates = Boolean.parseBoolean(inReq
				.findValue("allowduplicateemails"));
		if (allowduplicates) {
			return;
		}
		String email = inReq.getRequestParameter("email.value");

		User user = getUserManager().getUserByEmail(email);

		Map errors = new HashMap();
		if (user != null) {
			errors.put("error-email-in-use", "This email address is in use");
			inReq.putPageValue("errors", errors);
			inReq.putPageValue("emailinuse", true);
			cancelAndForward(inReq);
		}

	}
	
	
	public boolean checkCouponCode(WebPageRequest inReq){
		String catalogid = inReq.findValue("catalogid");
		String couponcode = inReq.getRequestParameter("code.value");
		
		Map errors = new HashMap();
		Searcher prepaidsearcher = getSearcherManager().getSearcher(catalogid, "prepaid");
		Data prepaidcode = (Data) prepaidsearcher.searchById(couponcode);
		if(prepaidcode == null){
			prepaidcode = (Data) inReq.getPageValue("coupon");
		}
		
		if(prepaidcode == null){
			
			log.info("invalid code usage detected: " + couponcode);
			errors.put("error-invalidcode", "This code is invalid");
			inReq.putPageValue("errors", errors);
			cancelAndForward(inReq);
			return false;
		}
		boolean available  = Boolean.parseBoolean(prepaidcode.get("available"));
		if(!available){
			log.info("attempted to use already used code: " + couponcode);
			errors.put("error-codeused", "This code was already used");
			inReq.putPageValue("errors", errors);
			cancelAndForward(inReq);
			return false;
		}
		log.info("processed code successfully" + couponcode);
		
		inReq.putSessionValue("coupon", prepaidcode);
		inReq.putPageValue("coupon", prepaidcode);
		
		return true;
	}

	

	private void cancelAndForward(WebPageRequest inReq) {
		String errorURL = inReq.findValue("errorURL");
		inReq.setHasForwarded(true);
		inReq.setCancelActions(true);
		inReq.forward(errorURL);

	}

	public void registrationReceived(WebPageRequest inReq) throws Exception {
		log.info("starting new registration");
	

		Map errors = new HashMap();
		String email = inReq.getRequestParameter("email.value");
		String password = inReq.getRequestParameter("password.value");
		String password2 = inReq.getRequestParameter("password2.value");
		if (password2 == null) {
			password2 = inReq.getRequestParameter("passwordmatch.value");
		}
		String errorURL = inReq.findValue("errorURL");

		String[] fields = inReq.getRequestParameters("field");

		if (fields == null) {
			if (errorURL != null) {
				errors.put("nodata",
						"No data received by form - please try again.");
				inReq.setHasForwarded(true);
				inReq.putPageValue("errors", errors);
				inReq.setCancelActions(true);
				inReq.forward(errorURL);

			}
			return;
		}

		Group guestgroup = getUserManager().getGroup("guest");
		if (guestgroup == null) {
			guestgroup = getUserManager().createGroup("guest");
		}

		boolean generatepassword = Boolean.parseBoolean(inReq
				.findValue("generatepassword"));
		if (!generatepassword) {
			if (password == null) {
				if (errorURL != null) {
					errors.put("password", "error-no-password");
					inReq.setHasForwarded(true);
					inReq.putPageValue("errors", errors);
					inReq.setCancelActions(true);
					inReq.forward(errorURL);

				}
				return;

			}

			if (!password.equals(password2) || password.length() == 0) {
				if (errorURL != null) {
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
		if (current != null && current.isVirtual()) {
			current.setVirtual(false);

		} else {
			current = getUserManager().createUser(null, password);
		}

		if (password != null && password.length() > 0) {
			current.setPassword(password);
		} else if (generatepassword) {

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

		current.remove("password");
		current.remove("password2");

		handleValidationCodes(inReq,  current);

		
		getUserManager().saveUser(current);
		inReq.putPageValue("saved", "true");
		inReq.putPageValue("newuser", current);
		inReq.putPageValue("password", password);

		Group notifygroup = getUserManager().getGroup("registration");
		if (notifygroup == null) {
			notifygroup = getUserManager().createGroup("registration");
		}
		if (email != null) {
			inReq.setRequestParameter("to", email);

		} else {
			inReq.setRequestParameter("to", "dummy@ijsolutions.ca");
		}
		current.setProperty("creationdate", new Date().toString());

		inReq.setRequestParameter("subject",
				inReq.getPageProperty("subjectprefix") + current.getId());
		inReq.putPageValue("registration", current);
		

		boolean logout = Boolean.parseBoolean(inReq.findValue("autologout"));

		if (logout) {

			inReq.removeSessionValue("user");
		}

		boolean login = Boolean.parseBoolean(inReq.findValue("autologin"));

		if (login) {

			inReq.putSessionValue("user", current);// this logs in the user that
													// just registered.
		}

	}

	protected void handleValidationCodes(WebPageRequest inReq,
			 User inCurrent) {
		boolean usecodes = Boolean.parseBoolean(inReq
				.getPageProperty("usevalidationcodes"));

		String validationcode = inCurrent.get("validationcode");
		if (validationcode == null || validationcode.length() == 0) {
			SecureRandom random = new SecureRandom();
			validationcode =

			new BigInteger(130, random).toString(32);
			inCurrent.setProperty("validationcode", validationcode);

		}
		if (!usecodes) {
			return;

		} else {
			inCurrent.setEnabled(false);
		}

	}

	public void validateCode(WebPageRequest inReq) {
		String userid = inReq.getRequestParameter("id");
		String code = inReq.getRequestParameter("validationcode");
		Boolean autoenable = Boolean.parseBoolean(inReq
				.findValue("autoenable"));
		User target = getUserManager().getUser(userid);
		if (target != null) {
			String validationcode = target.get("validationcode");
			if (code.equals(validationcode)) {
				if (autoenable) {
					target.setEnabled(true);
				}
				target.setProperty("validationcomplete", "true");
				inReq.putPageValue("validated", true);
				getUserManager().saveUser(target);

				boolean logout = Boolean.parseBoolean(inReq
						.findValue("autologout"));

				if (logout) {

					inReq.removeSessionValue("user");
				}

				boolean login = Boolean.parseBoolean(inReq
						.findValue("autologin"));

				if (login) {

					inReq.putSessionValue("user", target);// this logs in the
															// user that just
															// registered.
				}
				inReq.putPageValue("target", target);
				
				
			} else {
				inReq.putPageValue("validated", false);
			}

		}

	}

	

	
	public boolean validateCodes(WebPageRequest inReq) {
		boolean required = Boolean.parseBoolean(inReq
				.getPageProperty("requirevalidationcode"));
		if (!required) {
			log.info("not using validation codes");
			return true;
		}
		Data validationCode = (Data) inReq.getSessionValue("registrationcode");
		if (validationCode != null && validationCode.getName() == null) {
			validationCode.setName(validationCode.getId());
		}
		if (validationCode != null) {
			inReq.putPageValue("registrationcode", validationCode);
			return true;
		}

		String catalogid = inReq.findValue("catalogid");
		String couponcode = inReq.getRequestParameter("registrationcode.value");
		if (couponcode == null) {
			return false;
		}
	
		Map errors = new HashMap();
		Searcher prepaidsearcher = getSearcherManager().getSearcher(catalogid,
				"registrationcode");
		Data code = (Data) prepaidsearcher.searchById(couponcode);
		if (code == null || Boolean.parseBoolean(code.get("disabled"))) {
			log.info("invalid code usage detected: " + couponcode);
			errors.put("error-invalidcode", "This code is invalid");
			inReq.putPageValue("errors", errors);
			cancelAndForward(inReq);
			return false;
		}

		User current = inReq.getUser();
		current.setProperty("registrationcode", couponcode);
		PropertyDetails details = prepaidsearcher.getPropertyDetails();

		for (Iterator iterator = details.iterator(); iterator.hasNext();) {
			PropertyDetail detail = (PropertyDetail) iterator.next();
			String datatype = detail.getSearchType();
			String value = code.get(detail.getId());
			if (value != null) {
				if ("team".equals(datatype) || "group".equals(datatype)) {
					Group group = getUserManager().getGroup(value);
					if (group != null) {
						current.addGroup(group);
					}
				} else {
					if (!"id".equals(detail.getId())) {
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

	protected void handleCoupon(WebPageRequest inReq, String inCatalogId, User current, Data code)
	{
		log.info("detected coupon code: " + code.getId());
		Searcher prepaidsearcher = getSearcherManager().getSearcher(inCatalogId, "prepaid");
		code.setProperty("available", "false");
		
		String collegeid = code.get("college");
		if (collegeid != null) {
			Group group = getUserManager().getGroup(collegeid);
			if (group == null) {
				group = getUserManager().createGroup(collegeid);
				getUserManager().saveGroup(group);

			}
			if (!current.isInGroup(group)) {
				current.addGroup(group);
			}
			current.setProperty("college", collegeid);

		}
		//if there is a class specified for this code, add them to it.
		String classid = inReq.getRequestParameter("class.value");
		if(classid != null){
		
				Group group = getUserManager().getGroup(classid + "_students");
				if (group == null) {
					group = getUserManager().createGroup(classid + "_students");
					getUserManager().saveGroup(group);

				}
				if (!current.isInGroup(group)) {
				current.addGroup(group);
			}
		
		}
		else{
			String codeclass = code.get("class");
			if(!codeclass.contains(" ")){
				Group group = getUserManager().getGroup(codeclass + "_students");
				if (group == null) {
					group = getUserManager().createGroup(codeclass + "_students");
					getUserManager().saveGroup(group);

				}
				if (!current.isInGroup(group)) {
					current.addGroup(group);
				}
			}
		}
		
		
		
		
				
		
		code.setProperty("user", current.getId());
		prepaidsearcher.saveData(code, inReq.getUser());
		inReq.removeSessionValue("coupon");
	}
	

}
