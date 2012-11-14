package org.openedit.entermedia.scanner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.AssetUtilities;
import org.openedit.entermedia.MediaArchive;
import org.openedit.repository.ContentItem;
import org.openedit.util.DateStorageUtil;

import com.openedit.OpenEditException;
import com.openedit.users.User;
import com.openedit.util.FileUtils;
import com.openedit.util.PathProcessor;
import com.openedit.util.PathUtilities;

public class AssetPathProcessor extends PathProcessor
{
	private static final Log log = LogFactory.getLog(AssetPathProcessor.class);

	protected MediaArchive fieldMediaArchive;
    protected Boolean fieldOnWindows;
	protected AssetUtilities fieldAssetUtilities;
	protected long fieldLastCheckedTime;
	protected Collection fieldAttachmentFilters;
	protected FileUtils fieldFileUtils = new FileUtils();
	final List<String> assetsids = new ArrayList<String>();
	final List<Asset> fieldAssetsToSave = new ArrayList<Asset>();

	protected List<Asset> getAssetsToSave()
	{
		return fieldAssetsToSave;
	}
	public long getLastCheckedTime()
	{
		return fieldLastCheckedTime;
	}

	public void setLastCheckedTime(long inLastCheckedTime)
	{
		fieldLastCheckedTime = inLastCheckedTime;
	}

		public Collection getAttachmentFilters()
		{
			return fieldAttachmentFilters;
		}

		public void setAttachmentFilters(Collection inAttachmentFilters)
		{
			fieldAttachmentFilters = inAttachmentFilters;
		}

	public MediaArchive getMediaArchive()
	{
		return fieldMediaArchive;
	}


	protected void saveImportedAssets(User inUser) throws OpenEditException
	{
		if (getAssetsToSave().size() == 0)
		{
			return;
		}
		
		Asset	eventasset = (Asset)getAssetsToSave().get(0);	
		List<String> someids = new ArrayList();

		List existingassets = new ArrayList();
		for (Iterator iter = getAssetsToSave().iterator(); iter.hasNext();)
		{
			Asset asset = (Asset) iter.next();
			if( asset.get("recordmodificationdate") != null )
			{
				existingassets.add(asset);
			}
		}
		
		getMediaArchive().saveAssets(new ArrayList(getAssetsToSave())); //this clears the list

		for (Iterator iter = getAssetsToSave().iterator(); iter.hasNext();)
		{
			Asset asset = (Asset) iter.next();
			someids.add(asset.getId() );
			
			if( existingassets.contains(asset) )
			{
				getMediaArchive().fireMediaEvent("asset/originalmodified",inUser, asset);				
			}
			else
			{
				getMediaArchive().fireMediaEvent("asset/assetcreated",inUser, asset);
			}
		}
		assetsids.addAll(someids);

		getMediaArchive().fireMediaEvent("importing/assetsimported", inUser, eventasset, someids);
		
		getAssetsToSave().clear();
	}

	public void setMediaArchive(MediaArchive inMediaArchive)
	{
		fieldMediaArchive = inMediaArchive;
	}
	@Override
	public boolean acceptFile(ContentItem inItem)
	{
		String path = inItem.getPath();
		if (isOnWindows())
		{
			int absolutepathlimit = 260;
			if (path.length() > absolutepathlimit)
			{
				log.info("Path too long. Couldn't save " + path);
				return false;
			}
		}
		if( !fieldFileUtils.isLegalFilename(path))
		{
			log.info("Path is not web friendly. Couldn't import " + path);
			return false;
		}

		return super.acceptFile(inItem);
	}
	
		public void process(ContentItem inInput, User inUser)
		{
			if (inInput.isFolder())
			{
				if (acceptDir(inInput))
				{
					processAssetFolder( inInput, inUser, 3);
				}
			}
			else
			{
				if (acceptFile(inInput))
				{
					processFile(inInput, inUser);
				}
			}
		}
		protected void processAssetFolder(ContentItem inInput, User inUser, int deepcheck)
		{
			String sourcepath = getAssetUtilities().extractSourcePath(inInput, getMediaArchive());
			Asset asset = getMediaArchive().getAssetArchive().getAssetBySourcePath(sourcepath);
			if( asset != null)
			{
				//check this one primary asset to see if it changed
				if( asset.getPrimaryFile() != null)
				{
					inInput = getPageManager().getRepository().getStub(inInput.getPath() + "/" + asset.getPrimaryFile());
					asset = getAssetUtilities().populateAsset(asset, inInput, getMediaArchive(), sourcepath, inUser);
					if( asset != null)
					{
						getAssetsToSave().add(asset);
						if (getAssetsToSave().size() > 100)
						{
							saveImportedAssets(inUser);
						}
					}
				}
				//dont process sub-folders
			}
			else
			{
				//look deeper for assets
				List paths = getPageManager().getChildrenPaths(inInput.getPath());
				if( paths.size() == 0 )
				{
					return;
				}
				boolean processchildren = true;
				if( createAttachments(paths) )
				{
					ContentItem found = findPrimary(paths);
					if( found == null )
					{
						return; //no good files in here
					}

					//Use the first file that is not a folder
					String soucepath = getAssetUtilities().extractSourcePath(inInput, getMediaArchive());

					asset = getMediaArchive().createAsset(soucepath);
					asset.setFolder(true);
					asset.setProperty("datatype", "original");
					if( inUser != null )
					{
						asset.setProperty("owner", inUser.getUserName());
					}
					asset.setProperty("assetaddeddate",DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
					asset.setProperty("assetviews", "1");
					asset.setProperty("importstatus", "imported");


					String foundprimary = PathUtilities.extractFileName(found.getPath());
					asset.setPrimaryFile(foundprimary);
					getAssetUtilities().readMetadata(asset, found, getMediaArchive());
					getAssetUtilities().populateCategory(asset, inInput, getMediaArchive(), inUser);
					//asset = getAssetUtilities().createAssetIfNeeded(item, getMediaArchive(), inUser);
					//set the primary file
					getAssetsToSave().add(asset);
					if (getAssetsToSave().size() > 100)
					{
						saveImportedAssets(inUser);
					}

					processchildren = false;
				}
				else
				{
					processchildren = true;
				}
				
				deepcheck--;
				if( deepcheck == 0 )
				{
					processchildren = false;
				}
				
				if( processchildren && isRecursive())
				{
					
					for (Iterator iterator = paths.iterator(); iterator.hasNext();)
					{
						String path = (String) iterator.next();
						ContentItem item = getPageManager().getRepository().getStub(path);
						if( item.isFolder() )
						{
							if (acceptDir(item))
							{
								//See if this folders sub folders should be forced to check or not. 
								boolean keepignoringtime = false;
								
								if(  getLastCheckedTime() == 0 || getLastCheckedTime() < item.getLastModified() ) //this folder was edited. 
								{
									deepcheck = 3; //If we are deeper than 3 and still showed a mod stamp then check everything
								}
								
//								if( deep > 2 )
//								{
//									ignoretime = true; //If we are deeper than 3 and still showed a mod stamp then check everything
//								}
								processAssetFolder( item, inUser, deepcheck);
							}
							
						}
						else
						{
							if( getLastCheckedTime() == 0 || getLastCheckedTime() < item.getLastModified() )  //On Windows the folder times stamp matches the most recently modified file
							{
								if (acceptFile(item))
								{
									processFile(item, inUser);
								}
							}
						}
					}
				}
			}
		}
		public Boolean isOnWindows()
		{
			if (fieldOnWindows == null)
			{
				if (System.getProperty("os.name").toUpperCase().contains("WINDOWS"))
				{
					fieldOnWindows = Boolean.TRUE;
				}
				else
				{
					fieldOnWindows = Boolean.FALSE;
				}
				
			}
			return fieldOnWindows;
		}
		
		protected ContentItem findPrimary(List inPaths)
		{
			for (Iterator iterator = inPaths.iterator(); iterator.hasNext();)
			{
				String path = (String) iterator.next();
				ContentItem item = getPageManager().getRepository().getStub(path);
				if( !item.isFolder() && acceptFile(item))
				{
					return item;
				}
			}
			return null;
		}
		public void processFile(ContentItem inContent, User inUser)
		{
			Asset asset = getAssetUtilities().createAssetIfNeeded(inContent, getMediaArchive(), inUser);
			if( asset != null)
			{
				getAssetsToSave().add(asset);
				if (getAssetsToSave().size() > 100)
				{
					saveImportedAssets(inUser);
				}
			}
		}


		public AssetUtilities getAssetUtilities()
		{
				return fieldAssetUtilities;
		}

		public void setAssetUtilities(AssetUtilities inAssetUtilities)
		{
			fieldAssetUtilities = inAssetUtilities;
		}
		protected boolean createAttachments(List inPaths)
		{
			if( fieldAttachmentFilters == null )
			{
				return false;
			}
			for (Iterator iterator = getAttachmentFilters().iterator(); iterator.hasNext();)
			{
				String check = (String) iterator.next();
				for (Iterator iterator2 = inPaths.iterator(); iterator2.hasNext();)
				{
					String path = (String) iterator2.next();
					if( PathUtilities.match(path, check) )
					{
						return true;
					}
				}
			}

			return false;
		}

		public void processAssets(String inStartingPoint, User inUser)
		{
			process(inStartingPoint, inUser);
			saveImportedAssets(inUser);

		}


}
