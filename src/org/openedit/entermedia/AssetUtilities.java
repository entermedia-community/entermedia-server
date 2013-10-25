package org.openedit.entermedia;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.openedit.Data;
import org.openedit.entermedia.scanner.MetaDataReader;
import org.openedit.repository.ContentItem;
import org.openedit.repository.filesystem.FileItem;
import org.openedit.util.DateStorageUtil;

import com.openedit.OpenEditException;
import com.openedit.users.User;
import com.openedit.util.FileUtils;
import com.openedit.util.PathUtilities;

public class AssetUtilities
{
	protected MetaDataReader fieldMetaDataReader;
	protected DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");// TODO: use it8l
	protected boolean fieldIncludeCategories = true;
	
	public boolean isIncludeCategories()
	{
		return fieldIncludeCategories;
	}

	public void setIncludeCategories(boolean inIncludeCategories)
	{
		fieldIncludeCategories = inIncludeCategories;
	}

	public MetaDataReader getMetaDataReader()
	{
		return fieldMetaDataReader;
	}

	public void setMetaDataReader(MetaDataReader inMetaDataReader)
	{
		fieldMetaDataReader = inMetaDataReader;
	}
	//Main API
	public Asset createAssetIfNeeded(ContentItem inContent, final MediaArchive inArchive, User inUser)
	{
		String sourcepath = extractSourcePath(inContent,inArchive);
		Asset asset = inArchive.getAssetArchive().getAssetBySourcePath(sourcepath);
		asset = populateAsset(asset, inContent, inArchive, sourcepath, inUser);
		return asset;
	}

	public String extractSourcePath(ContentItem inContent,MediaArchive inArchive)
	{
		String	datadir = "/WEB-INF/data" + inArchive.getCatalogHome() + "/originals/";

		String sourcePath = inContent.getPath().substring(datadir.length());
		if( sourcePath.startsWith("/"))
		{
			sourcePath = sourcePath.substring(1);
		}
		return sourcePath;
	}
	public Asset populateAsset(Asset asset, ContentItem inContent, final MediaArchive inArchive, String sourcePath, User inUser)
	{
		return populateAsset(asset, inContent, inArchive, isIncludeCategories(), sourcePath, inUser);
	}
	public Asset populateAsset(Asset asset, ContentItem inContent, final MediaArchive inArchive, boolean inCludeCategories, String sourcePath, User inUser)
	{
		/**
		String absolutepath = dest.getContentItem().getAbsolutePath();
		File itemFile = new File(absolutepath);
		getAssetUtilities().getMetaDataReader().populateAsset(archive,itemFile, asset);
		archive.saveAsset(asset, inUser);
		 */
		boolean importedasset = true;
		if (asset != null)
		{
			// Incremental conversion
			// Asset Modification Date">2005-03-04 08:28:57
			String editstatus = asset.get("editstatus");
			if( "7".equals( editstatus) ) //Not deleted anymore
			{
				//restore
				asset.setProperty("importstatus", "reimported");
				asset.setProperty("editstatus", "1"); //pending
				asset.setProperty("pushstatus", "resend");
				readMetadata(asset, inContent, inArchive); 				//should we re-load metadata?
				if( inCludeCategories )
				{
					populateCategory(asset, inContent, inArchive, inUser);
				}
				return asset;
			}
			
			String existingdate = asset.getProperty("assetmodificationdate");
			if( existingdate != null)
			{
				long filemmod = inContent.getLastModified();
				Date saveddate = DateStorageUtil.getStorageUtil().parseFromStorage(existingdate);
				//We need to ignore milliseconds since our parsed date will not have them
				if (saveddate !=  null)
				{
					long oldtime = saveddate.getTime();
					filemmod = filemmod/1000;
					oldtime = oldtime/1000;
					if (filemmod == oldtime)
					{
						inArchive.getAssetArchive().clearAsset(asset);
						//saveasset = false;
						return null;
					}
				}
			}
			
		}
		else
		{
			asset = inArchive.createAsset(sourcePath);
			asset.setFolder(inContent.isFolder());
			asset.setProperty("datatype", "original");
			if( inUser != null ) 
			{
				asset.setProperty("owner", inUser.getUserName());
			}
			asset.setProperty("assetaddeddate",DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
			asset.setProperty("assetviews", "1");
			Data assettype = inArchive.getDefaultAssetTypeForFile(asset.getName());
			if( assettype != null)
			{
				asset.setProperty("assettype",assettype.getId());
			}
		}
		if (importedasset)
		{
			asset.setProperty("importstatus", "imported");
			asset.setProperty("pushstatus", "resend");

			readMetadata(asset, inContent, inArchive);
			// TODO: clear out old cached thumbnails and conversions
			// directory
			if( inCludeCategories )
			{
				populateCategory(asset, inContent, inArchive, inUser);
			}
			return asset;
		}
		return null;
	}
	
	public void populateCategory(Asset inAsset, ContentItem inContent, final MediaArchive inArchive, User inUser)
	{
		String	datadir = "/WEB-INF/data" + inArchive.getCatalogHome() + "/originals/";
		String dir = PathUtilities.extractDirectoryPath(inContent.getPath());
		populateCategory(inAsset, inArchive, datadir, dir, inUser);
	}

	public void populateCategory(Asset asset, final MediaArchive inArchive, String datadir, String dir, User inUser)
	{
		Category category = null;
		if (dir.length() > datadir.length())
		{
			String folderPath = dir.substring(datadir.length());
			category = inArchive.getCategoryArchive().createCategoryTree(folderPath);
		}
		else
		{
			category = inArchive.getCategoryArchive().getRootCategory();
		}
		if (inUser != null && category.getId().equals(inUser.getId())) //See if we are in the users home folder
		{
			if (!category.getId().equals(inUser.getShortDescription()))
			{
				category.setName(inUser.getShortDescription());
				category.getParentCategory().setName("Users"); //fixes parent name
				inArchive.getCategoryArchive().saveAll();
			}
		}

		asset.addCategory(category);
	}

	public void readMetadata(Asset asset, ContentItem inContent, final MediaArchive inArchive)
	{
		getMetaDataReader().populateAsset(inArchive, inContent, asset);
	}
		
	public boolean deleteAsset(ContentItem inContent, final MediaArchive inArchive)
	{
		Asset asset = getAsset(inContent, inArchive);
		if (asset != null)
		{
			inArchive.getAssetSearcher().delete(asset, null);
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public Asset getAsset(ContentItem inContent, final MediaArchive inArchive)
	{
		String datadir = "/WEB-INF/data" + inArchive.getCatalogHome() + "/originals/";
		String sourcePath = inContent.getPath().substring(datadir.length());
		if( sourcePath.startsWith("/"))
		{
			sourcePath = sourcePath.substring(1);
		}
		Asset asset = inArchive.getAssetArchive().getAssetBySourcePath(sourcePath);
		return asset;
	}

	public void moveAsset(Asset inAsset, String inNewPath, MediaArchive inArchive)
	{
		String oldSourcePath = inAsset.getSourcePath();
		
		String sourcePath = inNewPath;
		if (inNewPath.startsWith("/"))
		{
			sourcePath = inNewPath.substring(getDataDir(inArchive).length());
		}
		
		inAsset.setSourcePath(sourcePath);
		
		File oldFile = null;
		File newFile = null;
		if (inAsset.isFolder())
		{
			oldFile = new File(inArchive.getRootDirectory(), "/assets/" + oldSourcePath);
			newFile = new File(inArchive.getRootDirectory(), "assets/" + sourcePath);

		}
		else
		{
			oldFile = new File(inArchive.getRootDirectory(), "assets/" + oldSourcePath + ".xconf");
			newFile = new File(inArchive.getRootDirectory(), "assets/" + sourcePath + ".xconf");
		}
		try
		{
			new FileUtils().move(oldFile, newFile);
		} catch (IOException e)
		{
			throw new OpenEditException(e);
		}
	}
	
	public String getDataDir(MediaArchive inArchive)
	{
		return "/WEB-INF/data" + inArchive.getCatalogHome() + "/originals/";
	}

}
