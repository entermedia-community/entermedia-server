package org.entermediadb.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.openedit.Data;

public class UserRoleWithActions
{
	Map fieldUserRole;
	protected Map getUserRole()
	{
		return fieldUserRole;
	}
	protected void setUserRole(Map inUserRole)
	{
		fieldUserRole = inUserRole;
	}
	protected Collection getUserActions()
	{
		return fieldUserActions;
	}
	protected void setUserActions(Collection inUserActions)
	{
		fieldUserActions = inUserActions;
	}
	Collection fieldUserActions = new ArrayList();
	public void addRoleAction(Data inRoleAction)
	{
		fieldUserActions.add(inRoleAction);
	}
	
	//find by week
}
