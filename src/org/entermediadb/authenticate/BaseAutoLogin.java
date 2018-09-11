package org.entermediadb.authenticate;

import org.openedit.ModuleManager;
import org.openedit.WebPageRequest;
import org.openedit.data.SearcherManager;
import org.openedit.users.UserManager;

public abstract class BaseAutoLogin implements AutoLoginProvider
{
	protected ModuleManager fieldModuleManager;
	
	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	protected SearcherManager getSearcherManager()
	{
		return (SearcherManager)getModuleManager().getBean( "searcherManager" );
	}

	protected UserManager getUserManager(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		if( catalogid == null)
		{
			catalogid = "system";
		}
		return (UserManager) getModuleManager().getBean(catalogid, "userManager");
	}

}
