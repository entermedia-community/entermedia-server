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
	public DropboxManager getDropboxManager()
	{
		return (DropboxManager)getMediaArchive().getModuleManager().getBean(getMediaArchive().getCatalogId(),"dropboxManager");
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
		try {
			return getDropboxManager().saveFile(inAsset);
		} catch (Exception e) {
			throw new OpenEditException(e);
		}
	}

	protected void upload(Asset inAsset, File file)
	{
	   //NOT IMPLEMEMTED
	}

	

	@Override
	public ContentItem getOriginalContent(Asset inAsset)
	{
		return getOriginalContent(inAsset,true);
	}
	public ContentItem getOriginalContent(Asset inAsset, boolean downloadifNeeded)
	{
		
		
		
		
		File file = getFile(inAsset);
		FileItem item = new FileItem(file);
		
		String path = "/WEB-INF/data" + getMediaArchive().getCatalogHome() + "/originals/";
		path = path + inAsset.getSourcePath(); //Check archived?
		
		String primaryname = inAsset.getPrimaryFile();
		if(primaryname != null && inAsset.isFolder() )
		{
			path = path + "/" + primaryname;
		}
		item.setPath(path);
		if(downloadifNeeded)
		{
			//Check it exists and it matches
			long size = inAsset.getLong("filesize");
			if( item.getLength() != size)
			{
				download(inAsset, file);
			}
		}
		
		return item;
	}

	@Override
	public boolean handles(Asset inAsset)
	{
		String name = getFolderPath();
		if( inAsset.getSourcePath().startsWith(name))
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
		String subfolder = getConfig().get("subfolder");
		if(subfolder == null) {
			subfolder = getName();
		}
		Results r= getDropboxManager().syncAssets(subfolder, true);
		return r.getFiles().size();
	}

		

	@Override
	public void checkForDeleted()
	{
		//TODO: Do a search for versions that have been deleted and make sure they are marked as such
		
	}


	
	public void assetUploaded(Asset inAsset)
	{
		//Upload
		File file = getFile(inAsset);
		upload(inAsset, file);
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
