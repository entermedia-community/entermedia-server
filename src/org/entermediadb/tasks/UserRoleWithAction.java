package org.entermediadb.tasks;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.openedit.Data;
import org.openedit.util.DateStorageUtil;

public class UserRoleWithAction
{
	Map fieldUserRole;
	Data fieldRoleAction;
	
	protected Data getRoleAction()
	{
		return fieldRoleAction;
	}
	protected void setRoleAction(Data inRoleAction)
	{
		fieldRoleAction = inRoleAction;
	}
	protected Map getUserRole()
	{
		return fieldUserRole;
	}
	protected void setUserRole(Map inUserRole)
	{
		fieldUserRole = inUserRole;
	}
	public Date getDate()
	{
		Object obj = getRoleAction().getValue("date");
		Date adate = DateStorageUtil.getStorageUtil().parseFromObject(obj);
		return adate;
	}
	
}
