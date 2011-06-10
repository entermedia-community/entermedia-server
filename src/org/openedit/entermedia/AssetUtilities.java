package org.openedit.entermedia;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.openedit.entermedia.scanner.MetaDataReader;
import org.openedit.repository.ContentItem;
import org.openedit.repository.filesystem.FileItem;
import org.openedit.util.DateStorageUtil;

import com.openedit.OpenEditException;
import com.openedit.page.Page;
import com.openedit.users.User;
import com.openedit.util.FileUtils;
import com.openedit.util.PathUtilities;

public class AssetUtilities
{
	protected MetaDataReader fieldMetaDataReader;
	protected DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");// TODO: use it8l
	
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
		/**
		String absolutepath = dest.getContentItem().getAbsolutePath();
		File itemFile = new File(absolutepath);
		getAssetUtilities().getMetaDataReader().populateAsset(archive,itemFile, asset);
		archive.saveAsset(asset, inUser);
		 */
		

		boolean newasset = true;
		if (asset != null)
		{
			// Incremental conversion
			// Asset Modification Date">2005-03-04 08:28:57
			String existingdate = asset.getProperty("assetmodificationdate");
			if( existingdate != null)
			{
				long filemmod = inContent.getLastModified();
				Date saveddate = DateStorageUtil.getStorageUtil().parseFromStorage(existingdate);
				
				if (saveddate !=  null && filemmod == saveddate.getTime())
				{
					inArchive.getAssetArchive().clearAsset(asset);
					newasset = false;
				}
			}
		}
		if (asset == null)
		{
			asset = inArchive.createAsset(sourcePath);
			asset.setFolder(inContent.isFolder());
			asset.setProperty("datatype", "original");
			asset.setProperty("owner", inUser.getUserName());
			asset.setProperty("assetaddeddate",DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
			asset.setProperty("assetviews", "1");
			asset.setProperty("importstatus", "imported");
			//asset.setProperty("primaryfile", name);
		}
		if (newasset)
		{
			inArchive.removeGeneratedImages(asset); //Just in case?
			readMetadata(asset, inContent, inArchive);
			// TODO: clear out old cached thumbnails and conversions

			// Makes a mirror of the inputfilepath within the assets
			// directory
			populateCategory(asset, inContent, inArchive, inUser);
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
		if (category.getId().equals(inUser.getId())) //See if we are in the users home folder
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
		if (inContent instanceof FileItem)
		{
			File input = ((FileItem) inContent).getFile();
			getMetaDataReader().populateAsset(inArchive, input, asset);
		}
	}
	
	public void readMetadata(Asset asset, File inFile, final MediaArchive inArchive)
	{
		getMetaDataReader().populateAsset(inArchive, inFile, asset);
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
