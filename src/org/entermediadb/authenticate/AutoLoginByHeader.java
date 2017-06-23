package org.entermediadb.authenticate;

import org.openedit.WebPageRequest;
import org.openedit.users.User;
import org.openedit.users.UserManager;

public class AutoLoginByHeader extends BaseAutoLogin implements AutoLoginProvider
{

	protected AutoLoginResult autoLoginFromRequest(WebPageRequest inRequest)
	{
		String username = inRequest.getRequest().getRemoteUser();
		if (username == null)
		{
			return null;
		}
		UserManager userManager = getUserManager(inRequest);
		User user = userManager.getUser(username);

		if (user == null)
		{
			String groupname = inRequest.getPageProperty("autologingroup");
			if (groupname != null)
			{
				user = userManager.createGuestUser(username, null, groupname);
			}
		}
		AutoLoginResult result = new AutoLoginResult();
		result.setUser(user);
		return result;
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
