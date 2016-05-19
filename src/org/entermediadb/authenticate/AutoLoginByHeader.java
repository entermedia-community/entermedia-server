package org.entermediadb.authenticate;

import org.openedit.WebPageRequest;
import org.openedit.page.PageRequestKeys;
import org.openedit.users.User;
import org.openedit.users.UserManager;

public class AutoLoginByHeader extends BaseAutoLogin implements AutoLoginProvider
{

	protected void autoLoginFromRequest(WebPageRequest inRequest)
	{
		String username = inRequest.getRequest().getRemoteUser();
		if (username == null)
		{
			return;
		}
		if (inRequest.getUser() != null)
		{
			return;
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
		if (user != null)
		{
			inRequest.putProtectedPageValue(PageRequestKeys.USER, user);
		}
	}
	
	@Override
	public boolean autoLogin(WebPageRequest inReq)
	{
		if (Boolean.parseBoolean(inReq.getContentProperty("oe.usernameinheader")))
		{
			autoLoginFromRequest(inReq);
			return true;
		}

		// TODO Auto-generated method stub
		return false;
	}

}
