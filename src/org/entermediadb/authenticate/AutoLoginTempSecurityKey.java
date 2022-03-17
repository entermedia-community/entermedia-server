package org.entermediadb.authenticate;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.users.UserProfileManager;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.users.User;
import org.openedit.users.UserManager;

public class AutoLoginTempSecurityKey extends BaseAutoLogin implements AutoLoginProvider
{
	private static final Log log = LogFactory.getLog(AutoLoginTempSecurityKey.class);

	protected AutoLoginResult autoLoginFromRequest(WebPageRequest inRequest)
	{
		String code = inRequest.getRequestParameter("templogincode");
		if( code == null)
		{
			return null;
		}
		//Search for the code
		UserManager userManager = getUserManager(inRequest);
		String userid = inRequest.getRequestParameter("accountname");
		User user = userManager.getUser(userid);
		
		if (user == null)
		{
			log.error("User not found " + userid);
		}
		
		Searcher searcher = getSearcherManager().getSearcher("system", "templogincode");
		
		Calendar cal  = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -1); //24 hours
		Date newerthan = cal.getTime();
		Data found = searcher.query().exact("user",userid).exact("securitycode",code).after("date",newerthan).searchOne();
		
		String securitycode = found.get("securitycode");  //Double checking
		if( code.equals(securitycode))
		{
			saveCookieForUser(inRequest,user); //For next time
			AutoLoginResult result = new AutoLoginResult();
			result.setUser(user);
			return result;
			
		}
		
		return null;
	}
	
	protected User createNewUserInRole(String catalogid, String inUserName, String inRole)
	{
		if( catalogid == null)
		{
			catalogid = "system";
		}
		Searcher usersearcher = getSearcherManager().getSearcher(catalogid, "user");
		User newuser = (User)usersearcher.createNewData();
		newuser.setUserName(inUserName);
		newuser.setEnabled(true);
		usersearcher.saveData(newuser);
		UserProfileManager profilemanager = (UserProfileManager)getModuleManager().getBean(catalogid,"userProfileManager");
		profilemanager.setRoleOnUser(catalogid,newuser,inRole);
		return newuser;
		
	}

	@Override
	public AutoLoginResult autoLogin(WebPageRequest inReq)
	{
		if (Boolean.parseBoolean(inReq.getContentProperty("oe.usernameinheader")))
		{
			return autoLoginFromRequest(inReq);
		}

		return null;
	}

}
