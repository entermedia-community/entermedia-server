package org.entermediadb.desktops;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.openedit.ModuleManager;
import org.openedit.WebPageRequest;
import org.openedit.users.User;

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
		Desktop desktop = (Desktop)getConnectedClients().get(inUser.getId() + computername);

		if( desktop == null)
		{
			desktop = (Desktop) getModuleManager().getBean("desktop");
			desktop.setComputerName(computername);
			desktop.setUser(inUser);
			getConnectedClients().put(inUser.getId() + computername,desktop);
		}
		return desktop;
	}

	
	
}
