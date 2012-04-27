package org.openedit.entermedia.modules;

import java.util.Collection;

import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.scanner.HotFolderManager;

import com.openedit.WebPageRequest;

public class HotFolderModule extends BaseMediaModule
{
	public void loadHotFolders(WebPageRequest inReq)  throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);		
		Collection folders = getHotFolderManager().loadFolders(archive.getCatalogId());
		inReq.putPageValue("folders", folders);
	}
	
	public void saveHotFolders(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		
		getHotFolderManager().saveMounts(archive.getCatalogId());
		
	}
	protected HotFolderManager getHotFolderManager()
	{
		return (HotFolderManager)getModuleManager().getBean("hotFolderManager");
	}

	
}
