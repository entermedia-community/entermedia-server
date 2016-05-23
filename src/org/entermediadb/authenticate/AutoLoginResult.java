package org.entermediadb.authenticate;

import org.openedit.users.User;

public class AutoLoginResult
{
	protected User fieldUser;

	public User getUser()
	{
		return fieldUser;
	}

	public void setUser(User inUser)
	{
		fieldUser = inUser;
	}
	
}
