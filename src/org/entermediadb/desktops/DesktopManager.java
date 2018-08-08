package org.entermediadb.desktops;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class DesktopManager
{
	protected Map<String, Collection> fieldConnectedClients;

	public Map<String, Collection> getConnectedClients()
	{
		if (fieldConnectedClients == null)
		{
			fieldConnectedClients = new HashMap();
		}

		return fieldConnectedClients;
	}

	
	public void addDesktop(Desktop inDesktop)
	{
		Collection found = getConnectedClients().get(inDesktop.getUserId());
		if( found == null)
		{
			found = new ArrayList();
			getConnectedClients().put(inDesktop.getUserId(),found);
		}
		found.add(inDesktop);
	}


	public void removeDesktop(Desktop inDesktop)
	{
		Collection found = getConnectedClients().get(inDesktop.getUserId());
		if( found == null)
		{
			found = new ArrayList();
			getConnectedClients().put(inDesktop.getUserId(),found);
		}
		found.remove(inDesktop);
		
	}
	public Collection getDesktops(String inUserId)
	{
		Collection found = getConnectedClients().get(inUserId);
		
		return found; 
	}
	public Collection getUsers()
	{
		return getConnectedClients().keySet();
	}
}
