package org.entermediadb.desktops;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.entermediadb.asset.MediaArchive;
import org.openedit.CatalogEnabled;
import org.openedit.ModuleManager;
import org.openedit.users.User;
import org.openedit.util.PathUtilities;

public class DesktopManager implements CatalogEnabled
{
	protected Map<String, Desktop> fieldConnectedClients;
	
	protected ModuleManager fieldModuleManager;

	protected String fieldCatalogId;
	
	
	public String getCatalogId()
	{
		return fieldCatalogId;
	}


	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}


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

	public MediaArchive getMediaArchive()
	{
		return (MediaArchive)getModuleManager().getBean(getCatalogId(),"mediaArchive");
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

	public Desktop loadDesktop(User inUser, String userAgent) 
	{
		if(inUser == null) {
			Desktop desktop = (Desktop) getModuleManager().getBean(getCatalogId(),"desktop");
			return desktop;
		}
		String id = inUser.getId() + userAgent;
		id = PathUtilities.extractId(id);
		Desktop desktop = (Desktop)getConnectedClients().get(id);

		if( desktop == null)
		{
			desktop = (Desktop) getModuleManager().getBean(getCatalogId(),"desktop");
			desktop.setId(id);
			desktop.setUser(inUser);
			
			String version = getMediaArchive().getCatalogSettingValue("desktop_required_api_version");
			if( version != null)
			{
				desktop.setRequiredApiVersion(Integer.parseInt(version));
			}
			
			getConnectedClients().put(id,desktop);
		}
		return desktop;
	}

	
	
}
