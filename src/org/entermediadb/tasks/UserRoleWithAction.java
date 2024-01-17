package org.entermediadb.tasks;

import java.util.Date;
import java.util.Map;

import org.openedit.Data;
import org.openedit.util.DateStorageUtil;

public class UserRoleWithAction
{
	Map fieldUserRole;
	Data fieldRoleAction;
	
	public Data getRoleAction()
	{
		return fieldRoleAction;
	}
	public void setRoleAction(Data inRoleAction)
	{
		fieldRoleAction = inRoleAction;
	}
	public Map getUserRole()
	{
		return fieldUserRole;
	}
	public void setUserRole(Map inUserRole)
	{
		fieldUserRole = inUserRole;
	}
	public Date getDate()
	{
		Object obj = getRoleAction().getValue("date");
		Date adate = DateStorageUtil.getStorageUtil().parseFromObject(obj);
		return adate;
	}
	
	public String get(String inKey)
	{
		String val = getRoleAction().get(inKey);
		return val;
	}
	
}
