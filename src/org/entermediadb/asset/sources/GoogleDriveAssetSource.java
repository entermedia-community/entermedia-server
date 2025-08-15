package org.entermediadb.asset.sources;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.google.GoogleManager;
import org.entermediadb.google.Results;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.hittracker.HitTracker;
import org.openedit.repository.ContentItem;
import org.openedit.repository.filesystem.FileItem;
import org.openedit.users.User;
import org.openedit.util.PathUtilities;

public class GoogleDriveAssetSource extends BaseAssetSource
{
	private static final Log log = LogFactory.getLog(GoogleDriveAssetSource.class);
	
	protected GoogleManager fieldGoogleManager;
	
	protected int importCount;
	
	public GoogleManager getGoogleManager()
	{
		
	    if (fieldGoogleManager == null) {
	    	fieldGoogleManager = (GoogleManager)getMediaArchive().getModuleManager().getBean(getMediaArchive().getCatalogId(),"googleManager");
	    	fieldGoogleManager.setAssetSource(this);
		
	    }
	    return fieldGoogleManager;  
	    
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
	
	public boolean isHotFolder()
	{
		return true;
	}
	
	public void resetImportCount()
	{
		importCount = 0;
	}
	
	public void addImportCount()
	{
		importCount = importCount +1; 
	}
	
	public int getImportCount() {
		return importCount;
	}

	@Override
	public InputStream getOriginalDocumentStream(Asset inAsset)
	{
		ContentItem item = getOriginalContent(inAsset);
		return item.getInputStream();
	}
	
	
	
	protected ContentItem loadFile(Asset inAsset)
	{
		try {
			File file = getFile(inAsset);
			ContentItem item = new FileItem(file);
			
			String path = "/WEB-INF/data" + getMediaArchive().getCatalogHome() + "/originals/";
			path = path + inAsset.getSourcePath(); //Check archived?
			
			String primaryname = inAsset.getPrimaryFile();
			if(primaryname != null && inAsset.isFolder() )
			{
				path = path + "/" + primaryname;
			}
			item.setPath(path);
			//Download asset
			return getGoogleManager().loadFile(inAsset,  item);
			
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

		//Check it exists and it matches
		ContentItem item = loadFile(inAsset);
		
		return item;
	}

	@Override
	public boolean removeOriginal(User inUser, Asset inAsset)
	{
	
		ContentItem item = loadFile(inAsset);
		getPageManager().getRepository().remove(item);
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
		resetImportCount();
		
		String subfolder = getConfig().get("syncroot");
		if(subfolder == null) {
			subfolder = getName();
		}
		Results r= syncAssets(subfolder);
		return getImportCount();
	}
	
	public Results syncAssets(String inRoot)
	{
		try
		{
			//Load assets from Root
			log.info("Syncing assets from Google Drive Root folder: " + inRoot);
			Results results = getGoogleManager().listDriveFiles(inRoot);
			if (results.getFiles() != null || results.getFolders() != null)
			{
				String folderroot = getConfig().get("subfolder");
				
				processResults(folderroot, results);
				
				getMediaArchive().fireSharedMediaEvent("conversions/runconversions"); // this will save the asset as// imported
			}
			return results;
		}
		catch (Exception ex)
		{
			//throw new OpenEditException(ex);
			log.error("Error syncing assets from Google Drive: " + ex.getMessage(), ex);
			return null;
		}

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

	
	
	protected void processResults(String inCategoryPath, Results inResults) throws Exception
	{
		if (createAssets(inCategoryPath, inResults.getFiles()))
		{
			if (inResults.getFolders() != null)
			{
				for (Iterator iterator = inResults.getFolders().iterator(); iterator.hasNext();)
				{
					JSONObject folder = (JSONObject) iterator.next();
					String id = (String)folder.get("id");
					String foldername = (String)folder.get("name");
					foldername = foldername.trim();
					Results folderresults = getGoogleManager().listDriveFiles(id);
					
					if (folderresults.getFiles() != null)
					{
						Integer assetsfound = folderresults.getFiles().size();
						if (assetsfound > 0) {
							String categorypath = inCategoryPath + "/" + foldername;
							log.info("Found "+assetsfound+" assets at: "+categorypath);
							processResults(categorypath, folderresults);
						}
					}
				}
			}
		}

	}

	protected boolean createAssets( String categoryPath, Collection inFiles) throws Exception
	{
		if (inFiles == null)
		{
			return true;
		}
		Category category = getMediaArchive().createCategoryPath(categoryPath);

		ContentItem item = getMediaArchive().getContent("/WEB-INF/" + getMediaArchive() + "/originals/" + categoryPath);
		File realfile = new File(item.getAbsolutePath());
		realfile.mkdirs();
		long leftkb = realfile.getFreeSpace() / 1000;
		// FileSystemUtils.freeSpaceKb(item.getAbsolutePath());
		String free = getMediaArchive().getCatalogSettingValue("min_free_space");
		if (free == null)
		{
			free = "3000000";
		}

		Map onepage = new HashMap();
		for (Iterator iterator = inFiles.iterator(); iterator.hasNext();)
		{
			JSONObject object = (JSONObject) iterator.next();
			String id = (String)object.get("id");
			onepage.put(id, object);
			String fs = (String)object.get("size");
			if (fs != null)
			{
				leftkb = leftkb - (Long.parseLong(fs) / 1000);
				if (leftkb < Long.parseLong(free))
				{
					log.info("Not enough disk space left to download more " + leftkb + "<" + free);
					return false;
				}
			}

			if (onepage.size() == 25)
			{
				createAssetsIfNeeded(onepage, category);
				onepage.clear();
			}
		}
		createAssetsIfNeeded(onepage, category);
		return true;
	}

	private void createAssetsIfNeeded(Map inOnepage, Category category) throws Exception
	{
		if (inOnepage.isEmpty())
		{
			log.error("Empty map");
			return;
		}
		Collection tosave = new ArrayList();

		HitTracker existingassets = getMediaArchive().getAssetSearcher().query().orgroup("embeddedid", inOnepage.keySet()).search();
		
		log.info(existingassets);
		
		for (Iterator iterator = existingassets.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			Asset existing = (Asset) getMediaArchive().getAssetSearcher().loadData(data);
			log.info("Existing asset " + existing.getName() + " with embeddedid " + existing.get("embeddedid"));
			inOnepage.remove(existing.get("embeddedid"));
			
			//Re-assign Categories
			// existing.clearCategories();
			if (!existing.isInCategory(category))
			{
				
				//Clear old Drive categories from same Hot Folder
				//Category root = getMediaArchive().createCategoryPath("Drive");
				String rootcategorypath = getConfig().get("subfolder");
				Category root = getMediaArchive().getCategorySearcher().loadCategoryByPath(rootcategorypath);
				Collection existingcategories = new ArrayList(existing.getCategories());
				for (Iterator iterator2 = existingcategories.iterator(); iterator2.hasNext();)
				{
					Category drive = (Category) iterator2.next();
					if (root.isAncestorOf(drive))
					{
						existing.removeCategory(drive);
					}
				}
				existing.addCategory(category);
				tosave.add(existing);
				log.info("Asset moved categories " + existing);
			}
		}
		
		if (!tosave.isEmpty())
		{
			getMediaArchive().saveAssets(tosave);
			log.info("Saving Existing Assets " + tosave.size());
			tosave.clear();
		}

		// Only new Assets
		for (Iterator iterator = inOnepage.keySet().iterator(); iterator.hasNext();)
		{
			String googleid = (String) iterator.next();
			JSONObject object = (JSONObject) inOnepage.get(googleid);

			// log.info(object.get("kind"));// "kind": "drive#file",
			
			Asset newasset = (Asset) getMediaArchive().getAssetSearcher().createNewData();
			String filename = (String)object.get("name");
			filename = filename.trim();
			// JsonElement webcontentelem = object.get("webContentLink");

			String sourcepath = category.getCategoryPath() + "/" + filename;
		
			
			String mimetype = (String)object.get("mimeType");
			
			String fileformat = getMediaArchive().getMimeTypeMap().getExtensionForMimeType(mimetype);
			if (fileformat == null)
			{
				fileformat = PathUtilities.extractPageType(filename.toLowerCase());
			}
			newasset.setValue("fileformat", fileformat);
			
			if (fileformat != null)
			{
				String[] gdriveTypes = new String[] { "gddoc", "gdsheet", "gdslide", "gddraw" };
				if (Arrays.asList(gdriveTypes).contains(fileformat))
				{
					sourcepath = sourcepath + "." + fileformat;
					
					if (fileformat.equals("gdsheet") || fileformat.equals("gdslide"))
					{
						newasset.setValue("width", "1000");
						newasset.setValue("height", "500");
					}
					else if (fileformat.equals("gddraw"))
					{
						newasset.setValue("width", "500");
						newasset.setValue("height", "500");
					}
					else
					{
						newasset.setValue("width", "500");
						newasset.setValue("height", "1000");
					}
					
				}
				
			}
			
			newasset.setSourcePath(sourcepath);
			newasset.setFolder(false);
			newasset.setValue("embeddedid", googleid);
			newasset.setValue("embeddedtype", "googledrive");
			newasset.setValue("assetaddeddate", new Date());
			newasset.setValue("retentionpolicy", "deleteoriginal"); // Default
			
			//String rendetype =getMediaArchive().getMediaRenderType(fileformat);
			/*
			if( rendetype != null && rendetype.equals("embedded"))
			{
				newasset.setValue("previewstatus", "mime"); //unknown
				newasset.setValue("importstatus", "complete");
			}
			else
			{
				newasset.setValue("previewstatus", "0"); //unknown
				newasset.setValue("importstatus", "created");
			}
			*/
			newasset.setValue("previewstatus", "0"); //unknown
			newasset.setValue("importstatus", "created");
			
			newasset.setName(filename);
			
			
			//String googledownloadurl = (String)object.get("webContentLink");
			//newasset.setValue("contentlink", googledownloadurl);
			
			String thumbnaillink = null;
			Map exportlinks = (Map)object.get("exportLinks");
			if (exportlinks != null)
			{
				thumbnaillink = (String)exportlinks.get("application/pdf");
			}
			
			if (thumbnaillink == null)
			{
				thumbnaillink = (String)object.get("thumbnailLink");
			}
			
			newasset.setValue("thumbnaillink", thumbnaillink);
			
			

			String weblink  = (String)object.get("webViewLink");
			newasset.setValue("webviewlink", weblink);

				
				// JsonElement thumbnailLink = object.get("thumbnailLink");
			// if (thumbnailLink != null)
			// {
			// newasset.setValue("fetchthumbnailurl", thumbnailLink.getAsString());
			// }
			
			String md5 = (String)object.get("md5Checksum");
			newasset.setValue("md5hex", md5);
			
			newasset.addCategory(category);

			tosave.add(newasset);
			
			addImportCount();
		}
		
		
		if (!tosave.isEmpty())
		{

			getMediaArchive().saveAssets(tosave);

			log.info("Saving New Assets " + tosave.size());
			
			//Download if needed 
			for (Iterator iterator = tosave.iterator(); iterator.hasNext();)
			{
				Asset asset = (Asset) iterator.next();
				loadFile(asset);
			}

			getMediaArchive().fireSharedMediaEvent("importing/assetscreated");  //Kicks off an async saving
		}
	}


}
