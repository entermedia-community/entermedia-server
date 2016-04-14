package org.entermediadb.asset;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.entermediadb.asset.scanner.MetaDataReader;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.FileUtils;
import org.openedit.util.PathUtilities;
import org.openedit.util.Replacer;

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
	public Asset createAssetIfNeeded(ContentItem inContent, final MediaArchive inArchive, User inUser)
	{
		return createAssetIfNeeded(inContent, true, inArchive,  inUser);
	}
	public Asset createAssetIfNeeded(ContentItem inContent, boolean includefilename, final MediaArchive inArchive, User inUser)
	{
		String sourcepath = extractSourcePath(inContent,includefilename, inArchive);
		Asset asset = inArchive.getAssetSearcher().getAssetBySourcePath(sourcepath);
		asset = populateAsset(asset, inContent, inArchive, sourcepath, inUser);
		return asset;
	}
	//Main API
	public Asset createAssetIfNeeded(final MediaArchive inArchive, ContentItem inContent, String inSourcePath, User inUser)
	{
		Asset asset = inArchive.getAssetSearcher().getAssetBySourcePath(inSourcePath);
		asset = populateAsset(asset, inContent, inArchive, inSourcePath, inUser);
		return asset;
	}

	public String extractSourcePath(ContentItem inContent,boolean inIncludeFileName, MediaArchive inArchive)
	{
		String	datadir = "/WEB-INF/data" + inArchive.getCatalogHome() + "/originals/";

		String sourcePath = inContent.getPath().substring(datadir.length());
		if( sourcePath.startsWith("/"))
		{
			sourcePath = sourcePath.substring(1);
		}
		if( !inIncludeFileName )
		{
			return PathUtilities.extractDirectoryPath(sourcePath) + "/";
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
//				else
//				{
//					Category parent = inArchive.getCategory("users");
//					if ( parent != null)
//					{
//						inUser
//					}
//				}
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
			asset = (Asset)inArchive.getAssetSearcher().createNewData();
			if( sourcePath.endsWith("/"))
			{
				asset.setFolder(true);
				asset.setPrimaryFile(inContent.getName());
				sourcePath = sourcePath.substring(0,sourcePath.length() - 1);
			}	
			asset.setSourcePath(sourcePath);
			asset.setProperty("datatype", "original");
			asset.setProperty("editstatus", "1");
			
			asset.setName(inContent.getName());
			String ext = PathUtilities.extractPageType(inContent.getName());
			if( ext != null)
			{
				ext = ext.toLowerCase();
			}
			asset.setProperty("fileformat", ext);
			
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
			asset.setProperty("editstatus", "1");

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
//			This code is not needed. Just user runtime filters for categories			
//			String folderfilter = inArchive.getCatalogSettingValue("categorytreemask");
//			if(folderfilter == null || folderfilter.length() == 0){
//				return;
//				
//			}
//			HashMap properties = new HashMap();
//			for (Iterator iterator = asset.getProperties().keySet().iterator(); iterator.hasNext();)
//			{
//				String key = (String) iterator.next();
//				String value = asset.get(key);
//				properties.put(key, value);
//			}
//			if(inUser != null){
//				properties.put("username", inUser.getUserName());
//			}
//			properties.put("folderpath", folderPath);
//			String categorypath = inArchive.getSearcherManager().getValue(inArchive.getCatalogId(), folderfilter, properties);
			
			//This now is really long, unique, and has a GUID...lets strip off the last folder?
					
			category = inArchive.getCategoryArchive().createCategoryTree(folderPath); //
		}
		else
		{
			category = inArchive.getCategorySearcher().getRootCategory();
		}
		if (inUser != null && category.getId().equals(inUser.getId())) //See if we are in the users home folder
		{
			if (!category.getId().equals(inUser.getShortDescription()))
			{
				category.setName(inUser.getShortDescription());
				category.getParentCategory().setName("Users"); //fixes parent name
				inArchive.getCategorySearcher().saveCategory(category.getParentCategory());
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
		Asset asset = inArchive.getAssetSearcher().getAssetBySourcePath(sourcePath);
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
	
	public String createSourcePath(WebPageRequest inReq, MediaArchive inArchive)
	{
		return createSourcePath( inReq,  inArchive, null);
	}
	public String createSourcePath(WebPageRequest inReq, MediaArchive inArchive, String fileName)
	{
		String sourcepath = inArchive.getCatalogSettingValue("projectassetupload");  //${division.uploadpath}/${user.userName}/${formateddate}
		
		Map vals = new HashMap();
		vals.putAll(inReq.getPageMap());
		String prefix ="";
		String[] fields = inReq.getRequestParameters("field");

		if( fields != null)
		{
			for (int i = 0; i < fields.length; i++)
			{
				String val = inReq.getRequestParameter(prefix + fields[i]+ ".value");
				if( val != null)
				{
					vals.put(fields[i],val);
				}
			}
		}
		String id = inReq.getRequestParameter("id");
		if(id != null)
		{
			vals.put("id",id);
		}
		String library = inReq.getRequestParameter("libraries.value");
		if(library != null)
		{
			vals.put("library", library);
		}

		library = inReq.getRequestParameter("library.value");
		if(library != null)
		{
			vals.put("library", library);
		}

		String division = inReq.getRequestParameter("division.value");
		if(division != null)
		{
			vals.put("division", division);
		}

		if(fileName != null)
		{
			vals.put("filename", fileName);
			String ext = PathUtilities.extractPageType(fileName);
			String render = inArchive.getMediaRenderType(ext);
			vals.put("extension", ext);
			vals.put("rendertype", render);			
		}
		//vals.put("filename", item.getName());
		//vals.put("guid", item.getName());
		String guid = UUID.randomUUID().toString();
		String sguid = guid.substring(0,Math.min(guid.length(), 13));
		vals.put("guid", sguid);
		vals.put("splitguid", sguid.substring(0,2) + "/" + sguid.substring(3).replace("-", ""));
		
		String date  = new SimpleDateFormat("yyyyMM").format(new Date());
		vals.put("formatteddate",date );
		vals.put("user",inReq.getUser());
		Replacer replacer = new Replacer();
		
		replacer.setSearcherManager(inArchive.getSearcherManager());
		replacer.setCatalogId(inArchive.getCatalogId());
		replacer.setAlwaysReplace(true);
		sourcepath = replacer.replace(sourcepath, vals);
		//sourcepath = sourcepath + "/" + item.getName();
		if( sourcepath.endsWith("/"))
		{
			sourcepath = sourcepath.substring(0,sourcepath.length() - 1);
		}
		if( !sourcepath.startsWith("/") )
		{
			sourcepath = sourcepath + "/";
		}
		sourcepath = sourcepath.replace("//", "/"); //in case of missing data

		return sourcepath;
	}

}
