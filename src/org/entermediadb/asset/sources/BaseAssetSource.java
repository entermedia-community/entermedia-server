package org.entermediadb.asset.sources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.importer.FolderMonitor;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.data.PropertyDetail;
import org.openedit.data.Searcher;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.EmStringUtils;
import org.openedit.util.PathUtilities;

public abstract class BaseAssetSource implements AssetSource
{
	private static final Log log = LogFactory.getLog(BaseAssetSource.class);

	protected List fieldImportLogs;
	protected Searcher fieldFolderSearcher;
	protected Collection fieldExcludes;
	public Collection getExcludes()
	{
		if (fieldExcludes == null)
		{
			String value = getConfig().get("excludes");
			if( value == null)
			{
				fieldExcludes = Collections.EMPTY_LIST;
			}
			else
			{
				fieldExcludes = EmStringUtils.split(value);
			}
		}
		return fieldExcludes;
	}



	public void setExcludes(Collection inExcludes)
	{
		fieldExcludes = inExcludes;
	}



	public Collection getIncludes()
	{
		if (fieldIncludes == null)
		{
			String value = getConfig().get("includes");
			if( value == null)
			{
				fieldIncludes = Collections.EMPTY_LIST;
			}
			else
			{
				fieldIncludes = EmStringUtils.split(value);
			}
		}

		return fieldIncludes;
	}



	public void setIncludes(Collection inIncludes)
	{
		fieldIncludes = inIncludes;
	}

	protected Collection fieldIncludes;
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
	protected MultiValued fieldConfig;
	protected FolderMonitor fieldFolderMonitor;
	
	public MultiValued getConfig()
	{
		return fieldConfig;
	}

	public void setConfig(MultiValued inConfig)
	{
		fieldConfig = inConfig;
		fieldExcludes = null;
		fieldIncludes = null;
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
		log.info("Destination:" + dest);
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

	@Override
	public String getId()
	{
		return getConfig().getId();
	}

	protected boolean okToAdd(String inSourcepath)
	{
		for (Iterator iterator = getExcludes().iterator(); iterator.hasNext();)
		{
			String key = (String) iterator.next();
			if( PathUtilities.match(inSourcepath, key) )
			{
				return false;
			}
		}
		if( !getIncludes().isEmpty() )
		{
			for (Iterator iterator = getIncludes().iterator(); iterator.hasNext();)
			{
				String key = (String) iterator.next();
				if( PathUtilities.match(inSourcepath, key) )
				{
					return true;
				}
			}
			return false;
		}	
		return true;
	}

	@Override
	public String getName()
	{
		return getConfig().getName();
	}
	@Override
	public boolean isEnabled()
	{	
		String enabled = getConfig().get("enabled");
		return Boolean.parseBoolean(enabled);
	}
	
}
