package org.entermediadb.asset.scanner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.AssetUtilities;
import org.entermediadb.asset.MediaArchive;
import org.openedit.OpenEditException;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.FileUtils;
import org.openedit.util.PathProcessor;
import org.openedit.util.PathUtilities;

public class AssetPathProcessor extends PathProcessor
{
	private static final Log log = LogFactory.getLog(AssetPathProcessor.class);

	protected int fieldSaveSize = 1000;
	protected MediaArchive fieldMediaArchive;
    protected Boolean fieldOnWindows;
    protected boolean fieldModificationCheck = false;
    protected boolean fieldShowLogs;
    protected AssetImporter fieldAssetImporter;
    
	public AssetImporter getAssetImporter()
	{
		return fieldAssetImporter;
	}


	public void setAssetImporter(AssetImporter inAssetImporter)
	{
		fieldAssetImporter = inAssetImporter;
	}


	public boolean isShowLogs()
	{
		return fieldShowLogs;
	}


	public void setShowLogs(boolean inShowLogs)
	{
		fieldShowLogs = inShowLogs;
	}


	public boolean isModificationCheck()
	{
		return fieldModificationCheck;
	}


	public void setModificationCheck(boolean inModificationCheck)
	{
		fieldModificationCheck = inModificationCheck;
	}

	protected AssetUtilities fieldAssetUtilities;
	protected Collection fieldAttachmentFilters;
	protected FileUtils fieldFileUtils = new FileUtils();
	final List<String> assetsids = new ArrayList<String>();
	final List<Asset> fieldAssetsToSave = new ArrayList<Asset>();
	protected List<Asset> getAssetsToSave()
	{
		return fieldAssetsToSave;
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


	public void saveImportedAssets(User inUser) throws OpenEditException
	{
		if (getAssetsToSave().size() == 0)
		{
			return;
		}
		
		//Asset	eventasset = (Asset)getAssetsToSave().get(0);	
		//List<String> someids = new ArrayList();

		List existingassets = new ArrayList();
		for (Iterator iter = getAssetsToSave().iterator(); iter.hasNext();)
		{
			Asset asset = (Asset) iter.next();
			if( asset.getId() != null )
			{
				existingassets.add(asset);
			}
		}
		
		getMediaArchive().saveAssets(new ArrayList(getAssetsToSave())); 

		for (Iterator iter = getAssetsToSave().iterator(); iter.hasNext();)
		{
			Asset asset = (Asset) iter.next();
			assetsids.add(asset.getId() );
			
			if( existingassets.contains(asset) )
			{
				getMediaArchive().fireMediaEvent("originalmodified",inUser, asset);				
			}
			else
			{
				getMediaArchive().fireMediaEvent("assetcreated",inUser, asset);
			}
		}
		//assetsids.addAll(someids);

		//archive.firePathEvent("importing/assetsuploaded",inReq.getUser(),getAssetsToSave());
		//archive.fireMediaEvent("asset/assetcreated",inReq.getUser(),sample,listids); //This does not do much
		getMediaArchive().firePathEvent("importing/assetscreated",inUser,getAssetsToSave());
		if( isShowLogs() )
		{
			getAssetImporter().fireHotFolderEvent(getMediaArchive(), "update", "saved", String.valueOf( getAssetsToSave().size()), null);
		}
		getAssetsToSave().clear();
	}

	public void setMediaArchive(MediaArchive inMediaArchive)
	{
		fieldMediaArchive = inMediaArchive;
	}
	@Override
	public boolean acceptFile(ContentItem inItem)
	{
		String path = inItem.getAbsolutePath();
		if (isOnWindows())
		{
			int absolutepathlimit = 260;
			if (path.length() > absolutepathlimit)
			{
				log.info("Path too long. Couldn't save " + path);
				return false;
			}
		}
		if( !fieldFileUtils.isLegalFilename(inItem.getPath()))
		{
			if(log.isDebugEnabled())
				log.debug("Path is not web friendly.  Will have archivepath set." + inItem.getPath());
			//return true;
		}

		return super.acceptFile(inItem);
	}

		public void process(ContentItem inInput, User inUser)
		{
			if (inInput.isFolder())
			{
				if (acceptDir(inInput))
				{
					processAssetFolder( inInput, inUser);
				}
			}
			else if (acceptFile(inInput))
			{
				processFile(inInput, inUser);
			}
		}
		protected void processAssetFolder(ContentItem inInput, User inUser)
		{
			String sourcepath = getAssetUtilities().extractSourcePath(inInput, true, getMediaArchive());
			Asset asset = getMediaArchive().getAssetSearcher().getAssetBySourcePath(sourcepath);
			if( asset != null)
			{
				//check this one primary asset to see if it changed
				if( asset.getPrimaryFile() != null) //Attachments only
				{
					inInput = getPageManager().getRepository().getStub(inInput.getPath() + "/" + asset.getPrimaryFile());
					asset = getAssetUtilities().populateAsset(asset, inInput, getMediaArchive(), sourcepath, inUser);
					if( asset != null)
					{
						getAssetsToSave().add(asset);
						if (getAssetsToSave().size() > fieldSaveSize)
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
					if( isShowLogs() )
					{
						getAssetImporter().fireHotFolderEvent(getMediaArchive(), "update", "optimization", 
								"Empty folder: " + inInput.getAbsolutePath(), null);
					}
					return;
				}
				if( paths.size() > 3000 )
				{
					getAssetImporter().fireHotFolderEvent(getMediaArchive(), "update", "slowdown", paths.size()  + " files in one folder:" + inInput.getPath(), null);
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
					asset = getMediaArchive().createAsset(sourcepath);
					asset.setFolder(true);
					asset.setProperty("datatype", "original");
					if( inUser != null )
					{
						asset.setProperty("owner", inUser.getUserName());
					}
					asset.setProperty("assetaddeddate",DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
					asset.setProperty("assetviews", "1");
					asset.setProperty("importstatus", "needsmetadata");

					String foundprimary = PathUtilities.extractFileName(found.getPath());
					asset.setPrimaryFile(foundprimary);
					//getAssetUtilities().readMetadata(asset, found, getMediaArchive());
					getAssetUtilities().populateCategory(asset, inInput, getMediaArchive(), inUser);
					//asset = getAssetUtilities().createAssetIfNeeded(item, getMediaArchive(), inUser);
					//set the primary file
					getAssetsToSave().add(asset);
					if (getAssetsToSave().size() > fieldSaveSize)
					{
						saveImportedAssets(inUser);
					}

					processchildren = false;
				}
				else
				{
					processchildren = true;
				}
				
				if( processchildren && isRecursive())
				{
					Set	knownssourcepaths = loadGeneratedFolders(inInput, sourcepath);
					int processedfiles = 0;
					int acceptfolder = 0;
					int rejectfolder = 0;
					int skipfile = 0;
					int rejectfile = 0;
					for (Iterator iterator = paths.iterator(); iterator.hasNext();)
					{
						String path = (String) iterator.next();
						ContentItem item = getPageManager().getRepository().getStub(path);
						if( item.isFolder() )
						{
							if (acceptDir(item))
							{
								acceptfolder++;
//								if( deep > 2 )
//								{
//									ignoretime = true; //If we are deeper than 3 and still showed a mod stamp then check everything
//								}
								try 
								{
									processAssetFolder( item, inUser);
								}
								catch (StackOverflowError e) 
								{

									e.printStackTrace();
									
									throw new OpenEditException("Error processing due to stack overflow: " + item.getName() + " : "  + item.getAbsolutePath());
									
								}
							}
							else
							{
								rejectfolder++;
							}
							
						}
						else
						{
							if (acceptFile(item))
							{
								if( isModificationCheck() ) //Only set to true when importing a specific folder/file
								{
									processFile(item, inUser);
									processedfiles++;
								}
								else if( !knownssourcepaths.isEmpty())
								{
									String nwwsourcepath = getAssetUtilities().extractSourcePath(item, true, getMediaArchive());

									if( !knownssourcepaths.contains(nwwsourcepath) )
									{
										processFile(item, inUser);
										processedfiles++;
									}
									else
									{
										skipfile++;
									}
								}
								else
								{
									processFile(item, inUser);
									processedfiles++;
								}
							}
							else
							{
								rejectfile++;
							}
						}
					}
					if( isShowLogs() )
					{
						getAssetImporter().fireHotFolderEvent(getMediaArchive(), "update", "optimization", 
								"processedfiles:" + processedfiles +
								" acceptfolder:" + acceptfolder + 
								" rejectfolder:"+ rejectfolder + 
								" skipfile:"+ skipfile + 
								" rejectfile:" + rejectfile + 
								" path:" + inInput.getPath(), null);
						log.info("processAssetFolder folder:" + inInput.getPath());
					}
					
				}
			}
		}
		protected Set loadGeneratedFolders(ContentItem inInput, String inSourcepath)
		{
			Set set = new HashSet();
			String filepath = "/WEB-INF/data/" + getMediaArchive().getCatalogId() + "/generated/";
			Collection generatedfolders = getPageManager().getChildrenPaths(filepath  + inSourcepath); 

			for (Iterator iterator = generatedfolders.iterator(); iterator.hasNext();)
			{
				String path = (String) iterator.next();
				String sourcepath = path.substring(filepath.length());
				set.add(sourcepath);
			}
			
			return set;
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
			ContentItem first = null;
			for (Iterator iterator = inPaths.iterator(); iterator.hasNext();)
			{
				String path = (String) iterator.next();
				
				ContentItem item = getPageManager().getRepository().getStub(path);
				String format = PathUtilities.extractPageType(path);
				if( !item.isFolder() && acceptFile(item) )
				{
					if(first == null && format != null && !"txt".equals(format) && !"xml".equals(format)){
						first = item;
					}
					if("indd".equals(format)){
						return item;
					}
				}
			}
			
			return first;
		}
		public void processFile(ContentItem inContent, User inUser)
		{
			Asset asset = createAssetIfNeeded(inContent, getMediaArchive(), inUser);
			if( asset != null)
			{
				getAssetsToSave().add(asset);
				if (getAssetsToSave().size() > fieldSaveSize)
				{
					saveImportedAssets(inUser);
				}
			}
		}


		protected Asset createAssetIfNeeded(ContentItem inContent, MediaArchive inMediaArchive, User inUser)
		{
			Asset asset = getAssetUtilities().createAssetIfNeeded(inContent, inMediaArchive, inUser);
			return asset;
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
			ContentItem item = getMediaArchive().getPageManager().getRepository().getStub(inStartingPoint);
			String sourcepath = getAssetUtilities().extractSourcePath(item, true, getMediaArchive());
			String[] folderlist = sourcepath.split("/");
			String pathtocheck = "";
			for (int i = 0; i < folderlist.length; i++)
			{
				String nextfolder = folderlist[i];
				if(i > 0){
					pathtocheck = pathtocheck + "/" + nextfolder;
				} else{
					pathtocheck =folderlist[0];
				}
				//TODO: This is super slow....Cache it for top level assets?
				Asset asset = getMediaArchive().getAssetSearcher().getAssetBySourcePath(pathtocheck);
				if(asset != null)
				{
					if( isShowLogs() )
					{
						log.error("Found top level asset " + inStartingPoint + " " + "checked: " + pathtocheck);
						getAssetImporter().fireHotFolderEvent(getMediaArchive(), "init", "error", 
								"Found top level asset " + pathtocheck , null);
					}
					return;
				}
			}
			
			process(inStartingPoint, inUser);
			saveImportedAssets(inUser);

		}


}
