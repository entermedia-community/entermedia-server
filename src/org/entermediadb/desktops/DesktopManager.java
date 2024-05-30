package org.entermediadb.desktops;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.openedit.ModuleManager;
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


	public Desktop connectDesktop(User inUser) {
		// TODO Auto-generated method stub
		
		Desktop desktop = (Desktop) getModuleManager().getBean("desktop");
		desktop.setUser(inUser);
		setDesktop(desktop);
		return desktop;
	}
	
	
}
