package org.openedit.entermedia.members;

import org.openedit.xml.ElementData;

import com.openedit.users.UserManager;

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
