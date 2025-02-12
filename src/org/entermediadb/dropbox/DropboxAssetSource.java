package org.entermediadb.dropbox;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.sources.BaseAssetSource;
import org.entermediadb.google.GoogleManager;
import org.entermediadb.google.Results;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.repository.ContentItem;
import org.openedit.repository.filesystem.FileItem;
import org.openedit.users.User;

public class DropboxAssetSource extends BaseAssetSource
{
	private static final Log log = LogFactory.getLog(DropboxAssetSource.class);
	protected DropboxManager fieldDropboxManager;
	
	
	
	public DropboxManager getDropboxManager()
	{
	    //DropboxManager is not a singleton - one per source
	    if (fieldDropboxManager == null) {
		fieldDropboxManager = (DropboxManager)getMediaArchive().getModuleManager().getBean(getMediaArchive().getCatalogId(),"dropboxManager");
		fieldDropboxManager.setAssetSource(this);
		
	    }
	    return fieldDropboxManager;    	
		
	}
	
	
	
	
	public boolean isHotFolder()
	{
		return true;
	}

	@Override
	public InputStream getOriginalDocumentStream(Asset inAsset)
	{
		ContentItem item = getOriginalContent(inAsset);
		return item.getInputStream();
	}
	
	
	
	protected File download(Asset inAsset, File file)
	{
		throw new OpenEditException("On demand not implemented yet");
	}

	protected void upload(Asset inAsset, File file)
	{
	   //NOT IMPLEMEMTED
	}

	

	
	public ContentItem getOriginalContent(Asset inAsset)
	{
	    	//TODO:  Implement download on demand
	    	String originalpath = "/WEB-INF/data" + getMediaArchive().getCatalogHome() + "/originals";
		String alternative = inAsset.getPath();
		originalpath = originalpath + "/" + alternative;
		String primaryname = inAsset.getPrimaryFile();
		if(primaryname != null && inAsset.isFolder() )
		{
			originalpath = originalpath + "/" + primaryname;
		}
		
		ContentItem page = getPageManager().getRepository().getStub(originalpath);
		return page;

	}

	@Override
	public boolean handles(Asset inAsset)
	{
		String name = getFolderPath();
		if( name != null && inAsset.getSourcePath().startsWith(name))
		{
			return true;
		}
		return false;
	}

	@Override
	public boolean removeOriginal(User inUser, Asset inAsset)
	{
	

		return false;
	}

	@Override
	public Asset addNewAsset(Asset inAsset, List<ContentItem> inTemppages)
	{

		throw new OpenEditException("Not implemented");
	}

	@Override
	public Asset replaceOriginal(Asset inAsset, List<ContentItem> inTemppages)
	{
		throw new OpenEditException("Not implemented");
	}
	
	/**
	 * The move is already done for us
	 */
	@Override
	public Asset assetOrginalSaved(Asset inAsset)
	{
		File file = getFile(inAsset);
		upload(inAsset, file);
		return inAsset;
	}

	@Override
	public void detach()
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void refresh( ) 
	{
		MultiValued currentConfig = (MultiValued) getMediaArchive().getData("hotfolder", getConfig().getId());
		setConfig(currentConfig);
	}

	@Override
	public void saveConfig()
	{
		saveMount();
				
	}

	@Override
	public int importAssets(String inBasepath)
	{
		refresh();
		
		int count = getDropboxManager().syncAssets("");
		return count;
	}

		

	@Override
	public void checkForDeleted()
	{
		//TODO: Do a search for versions that have been deleted and make sure they are marked as such
		
	}


	
	


	protected ContentItem checkLocation(Asset inAsset, ContentItem inUploaded, User inUser)
	{
		ContentItem dest = getOriginalContent(inAsset);
		if(!inUploaded.getPath().equals(dest.getPath()))//move from tmp location to final location
		{
			Map props = new HashMap();
			props.put("absolutepath", dest.getAbsolutePath());
			getMediaArchive().fireMediaEvent("asset","savingoriginal",inAsset.getSourcePath(),props,inUser);
			getMediaArchive().getPageManager().getRepository().move(inUploaded, dest);
			getMediaArchive().fireMediaEvent("asset","savingoriginalcomplete",inAsset.getSourcePath(),props,inUser);
		}
		return dest;
	}



}
