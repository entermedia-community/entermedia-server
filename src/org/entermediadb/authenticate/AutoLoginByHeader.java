package org.entermediadb.authenticate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.users.UserProfileManager;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.users.Group;
import org.openedit.users.User;
import org.openedit.users.UserManager;

public class AutoLoginByHeader extends BaseAutoLogin implements AutoLoginProvider
{
	private static final Log log = LogFactory.getLog(AutoLoginByHeader.class);

	protected AutoLoginResult autoLoginFromRequest(WebPageRequest inRequest)
	{
		String username = inRequest.getRequest().getRemoteUser();
		if (username == null)
		{
			return null;
		}
		UserManager userManager = getUserManager(inRequest);
		User user = userManager.getUser(username);
		String catalogid = inRequest.findValue("catalogid");

		if (user == null)
		{
			String role = inRequest.getContentProperty("autologinrole");
			String groupname = inRequest.getContentProperty("autologingroup");
			if (role != null)
			{
				user = createNewUserInRole(catalogid, username,role);
				if( groupname != null)
				{
					Group group = userManager.getGroup(groupname);
					if (group == null)
					{
						log.error("No such auto login group " + groupname);
					} else {
						user.addGroup(group);
					}
				}
			}
			if (user == null && groupname != null)
			{
				user = userManager.createGuestUser(username, null, groupname);
			}
		}
		
		if (user == null)
		{
			log.error("No auto login group or role configured");
		}
		
		AutoLoginResult result = new AutoLoginResult();
		result.setUser(user);
		return result;
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
