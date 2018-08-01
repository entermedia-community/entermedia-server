package org.entermediadb.asset.sources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.importer.FolderMonitor;
import org.openedit.Data;
import org.openedit.data.PropertyDetail;
import org.openedit.data.Searcher;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;

public abstract class BaseAssetSource implements AssetSource
{
	protected List fieldImportLogs;
	protected Searcher fieldFolderSearcher;
	
	public Searcher getFolderSearcher()
	{
		return getMediaArchive().getSearcher("hotfolder");
	}

	

	public List getImportLogs()
	{
		if (fieldImportLogs == null)
		{
			fieldImportLogs = new ArrayList();
		}
		return fieldImportLogs;
	}

	public void setImportLogs(List inImportLogs)
	{
		fieldImportLogs = inImportLogs;
	}

	protected MediaArchive fieldMediaArchive;
	protected Data fieldConfig;
	protected FolderMonitor fieldFolderMonitor;
	
	public Data getConfig()
	{
		return fieldConfig;
	}

	public void setConfig(Data inConfig)
	{
		fieldConfig = inConfig;
	}

	public MediaArchive getMediaArchive()
	{
		return fieldMediaArchive;
	}

	public void setMediaArchive(MediaArchive inMediaArchive)
	{
		fieldMediaArchive = inMediaArchive;
	}

	protected String getExternalPath()
	{
		return getConfig().get("externalpath");
	}

	protected String getFolderPath()
	{
		return getConfig().get("subfolder");
	}



	protected String getSourcePath(Asset inAsset)
	{
		String alternative = inAsset.get("archivesourcepath");
		if( alternative == null)
		{
			alternative = inAsset.getSourcePath();
		}
		return alternative;
	}



	public FolderMonitor getFolderMonitor()
	{
		return fieldFolderMonitor;
	}



	public void setFolderMonitor(FolderMonitor inFolderMonitor)
	{
		fieldFolderMonitor = inFolderMonitor;
	}

	public Asset createAsset(Asset inAsset, ContentItem inUploaded, Map inMetadata, String inSourcepath, boolean inCreateCategories, User inUser)
	{
		ContentItem dest = checkLocation(inAsset, inUploaded, inUser);
		String sourcepath = inAsset.getSourcePath();
		if( dest.exists() )
		{
			inAsset = getMediaArchive().getAssetBySourcePath(inAsset.getSourcePath());
		}
		Asset asset = getMediaArchive().getAssetImporter().getAssetUtilities().populateAsset(inAsset, dest, getMediaArchive(), inCreateCategories, sourcepath, inUser);
		getMediaArchive().getAssetImporter().getAssetUtilities().readMetadata(asset, dest, getMediaArchive());
		asset.setProperty("importstatus", "imported");
		for (Iterator iterator = inMetadata.keySet().iterator(); iterator.hasNext();)
		{
			String field  = (String)iterator.next();
			Object val = inMetadata.get(field);
			if( field.equals("keywords"))
			{
				//combine lists
				String[] col = (String[])val;
				for (int i = 0; i < col.length; i++)
				{
					asset.addKeyword(col[i]);					
				}
			}
			else if( field.equals("categories"))
			{
				for (Iterator citerator = ((Collection)val).iterator(); citerator.hasNext();)
				{
					Category cat = (Category) citerator.next();
					asset.addCategory(cat);
				}				
			}
			else 
			{
				if( val instanceof String[] )
				{
					String[] col = (String[])val;
					if( col.length == 0)
					{
						continue;
					}
					if( col.length == 1)
					{
						if( col[0] == null || col[0].isEmpty())
						{
							continue; //dont blank out the value
						}
						PropertyDetail detail = getMediaArchive().getAssetSearcher().getPropertyDetails().getDetail(field);
						if(detail != null && detail.isDate()){
							String targetval = col[0];
							String format = "yyyy-MM-dd";
							if (targetval.matches("[0-9]{2}/[0-9]{2}/[0-9]{4}"))  //TODO: clean this up
							{
								format = "MM/dd/yyyy";
							}
							Date date = DateStorageUtil.getStorageUtil().parse(targetval, format);
							asset.setValue(field, date);
						}else{
							asset.setValue(field, col[0]);
						}
					}
					else
					{
						asset.setValue(field, Arrays.asList((String[])col) );
					}
				}
				else
				{
					asset.setValue(field, val);
				}
			}
		}
		
		if( asset.get("editstatus") == null )
		{
			asset.setProperty("editstatus","1");
		}
		//asset.setProperty("importstatus", "uploading");
		if( asset.get("importstatus") == null )
		{
			asset.setProperty("importstatus", "imported");
		}
		if( asset.get("previewstatus") == null )
		{
			asset.setProperty("previewstatus", "0");
		}
		
		if( asset.get("assettype") == null)
		{
			Data type = getMediaArchive().getDefaultAssetTypeForFile(asset.getName());
			if( type != null)
			{
				asset.setProperty("assettype", type.getId());
			}
		}
		asset.setFolder(dest.isFolder());

		return asset;
	}

	protected abstract ContentItem checkLocation(Asset inAsset, ContentItem inUploaded, User inUser);

	public void assetUploaded(Asset inAsset)
	{
		//Do nothing
	}
	
}
