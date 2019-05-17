package org.entermediadb.asset.sources;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.google.GoogleManager;
import org.entermediadb.google.Results;
import org.openedit.OpenEditException;
import org.openedit.repository.ContentItem;
import org.openedit.repository.filesystem.FileItem;
import org.openedit.users.User;

public class GoogleDriveAssetSource extends BaseAssetSource
{
	private static final Log log = LogFactory.getLog(GoogleDriveAssetSource.class);
	public GoogleManager getGoogleManager()
	{
		return (GoogleManager)getMediaArchive().getModuleManager().getBean(getMediaArchive().getCatalogId(),"googleManager");
	}
	
	protected String getAccessToken() {
		try
		{
			return getGoogleManager().getUserAccessToken(getConfig(), "hotfolder");
		}
		catch (Exception e)
		{
			throw new OpenEditException (e);
		}
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
			return getGoogleManager().saveFile(getAccessToken(), inAsset);
		} catch (Exception e) {
			throw new OpenEditException(e);
		}
	}

	protected void upload(Asset inAsset, File file)
	{
		getGoogleManager().uploadToDrive(getAccessToken(), inAsset, file);
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
	public boolean removeOriginal(Asset inAsset)
	{
	

		return false;
	}

	@Override
	public Asset addNewAsset(Asset inAsset, List<ContentItem> inTemppages)
	{

		if( inTemppages.size() == 1)
		{
			ContentItem one = inTemppages.iterator().next();
			String path = "/WEB-INF/data" + getMediaArchive().getCatalogHome() + "/originals/";
			path = path + inAsset.getSourcePath();

			File file = getFile(inAsset);

			if(!one.getPath().equals(path))
			{
				//move contents
				FileItem dest = new FileItem(file);
				getMediaArchive().getPageManager().getRepository().move(one, dest);
			}
			upload(inAsset, file);
		}
		else
		{
			throw new OpenEditException("Dont support folder uploading");
		}
		return inAsset;
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
	public void saveConfig()
	{
		saveMount();
				
	}

	@Override
	public int importAssets(String inBasepath)
	{
		String subfolder = getConfig().get("subfolder");
		if(subfolder == null) {
			subfolder = getName();
		}
		Results r= getGoogleManager().syncAssets(getAccessToken(), subfolder, false);
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
