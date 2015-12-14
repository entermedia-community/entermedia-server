package org.entermediadb.asset.members;

import org.openedit.users.UserManager;
import org.openedit.xml.ElementData;

public class Member extends ElementData
{
	protected UserManager fieldUserManager;

	public UserManager getUserManager()
	{
		return fieldUserManager;
	}

	public void setUserManager(UserManager inUserManager)
	{
		fieldUserManager = inUserManager;
	}
	
}
