package org.entermediadb.desktops;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.openedit.ModuleManager;
import org.openedit.users.User;
import org.openedit.util.PathUtilities;

public class DesktopManager
{
	protected Map<String, Desktop> fieldConnectedClients;
	
	protected ModuleManager fieldModuleManager;

	public ModuleManager getModuleManager() {
		return fieldModuleManager;
	}


	public void setModuleManager(ModuleManager fieldModuleManager) {
		this.fieldModuleManager = fieldModuleManager;
	}


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
		Desktop oldesktop = (Desktop)getConnectedClients().get(inDesktop.getUserId());
		if( oldesktop != null)
		{
			oldesktop.replacedWithNewDesktop(inDesktop);
		}
		
		getConnectedClients().put(inDesktop.getUserId(),inDesktop);
	}

	public void removeDesktop(String inUserId)
	{
		getConnectedClients().remove(inUserId);
	}

	public void removeDesktop(Desktop inDesktop)
	{
		getConnectedClients().remove(inDesktop.getUserId());
	}
	public Collection getUsers()
	{
		return getConnectedClients().keySet();
	}

	public Desktop loadDesktop(User inUser, String computername) 
	{
		String id = inUser.getId() + computername;
		id = PathUtilities.extractId(id);
		Desktop desktop = (Desktop)getConnectedClients().get(id);

		if( desktop == null)
		{
			desktop = (Desktop) getModuleManager().getBean("desktop");
			desktop.setId(id);
			desktop.setComputerName(computername);
			desktop.setUser(inUser);
			getConnectedClients().put(id,desktop);
		}
		return desktop;
	}

	
	
}
