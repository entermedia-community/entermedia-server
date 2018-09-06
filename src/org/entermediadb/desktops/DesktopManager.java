package org.entermediadb.desktops;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.openedit.users.User;

public class DesktopManager
{
	protected Map<String, Desktop> fieldConnectedClients;

	public Map<String, Desktop> getConnectedClients()
	{
		if (fieldConnectedClients == null)
		{
			fieldConnectedClients = new HashMap();
		}

		return fieldConnectedClients;
	}

	
	public void setDesktop(Desktop inDesktop)
	{
		getConnectedClients().put(inDesktop.getUserId(),inDesktop);
	}


	public void removeDesktop(Desktop inDesktop)
	{
		getConnectedClients().remove(inDesktop.getUserId());
	}
	public Collection getUsers()
	{
		return getConnectedClients().keySet();
	}
	public Desktop getDesktop(User inUser)
	{
		if( inUser == null)
		{
			return null;
		}
		return getDesktop(inUser.getId());
	}

	public Desktop getDesktop(String inUserId)
	{
		Desktop desktop = getConnectedClients().get(inUserId);
		return desktop;
	}
}
