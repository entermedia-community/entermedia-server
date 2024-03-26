package org.entermediadb.asset.sources;

import java.io.File;
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
import org.openedit.WebServer;
import org.openedit.data.PropertyDetail;
import org.openedit.data.Searcher;
import org.openedit.page.manage.PageManager;
import org.openedit.repository.ContentItem;
import org.openedit.repository.Repository;
import org.openedit.repository.filesystem.FileRepository;
import org.openedit.repository.filesystem.XmlVersionRepository;
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

	protected PageManager fieldPageManager;

	protected WebServer fieldWebServer;
	
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
		
		String importstatus = (String)asset.get("importstatus");
		if( importstatus == null || !importstatus.equals("needsmetadata"))
		{
			//if it needsmetadata then dont do it now. The upload will run first
			getMediaArchive().getAssetImporter().getAssetUtilities().readMetadata(asset, dest, getMediaArchive());
			asset.setProperty("importstatus", "imported");
		}
		else
		{
			asset.setProperty("importstatus", "needsmetadata");
		}
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
						} 
						else
						{
							String v = col[0];
							asset.setValue(field, v);
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
				
				//Check for _auto
				PropertyDetail detail = getMediaArchive().getAssetSearcher().getPropertyDetails().getDetail(field);
				if( detail != null && detail.isList() )  
				{
					Collection<String> values = asset.getValues(field);
					if( values != null && !values.isEmpty())
					{
						Collection<String> tosave = new ArrayList();
						for (Iterator iterator2 = values.iterator(); iterator2.hasNext();)
						{
							String v = (String) iterator2.next();
							if( "_auto".equals( v ) )
							{
								Searcher s = getMediaArchive().getSearcher(detail.getListId() );
								Data newone = s.createNewData();
								newone.setName(PathUtilities.extractPageName(asset.getName()));
								s.saveData(newone);
								v = newone.getId();
							}
							tosave.add(v);
						}
						asset.setValue(field,tosave);
					}
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
		if(fieldConfig== null)
		{
			return getClass().getName();
		}
		return getConfig().getId();
	}

	protected boolean okToAdd(String inSourcepath)
	{
		if( inSourcepath == null)
		{
			return false;
		}
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
		if( getConfig() == null)
		{
			return null;
		}
		return getConfig().getName();
	}
	@Override
	public boolean isEnabled()
	{	
		String enabled = getConfig().get("enabled");
		return Boolean.parseBoolean(enabled);
	}
	
	protected File getFile(Asset inAsset)
	{
		String sp = inAsset.getPath();
		sp = sp.substring(getFolderPath().length() + 1);
		String abpath = getExternalPath() + "/" + sp;
		String primaryname = inAsset.getPrimaryFile();
		if(primaryname != null && inAsset.isFolder() )
		{
			abpath = abpath + "/" + primaryname;
		}

		return new File(abpath);
	}



	public WebServer getWebServer()
	{
		return fieldWebServer;
	}



	public void setWebServer(WebServer inWebServer)
	{
		fieldWebServer = inWebServer;
	}



	public PageManager getPageManager()
	{
		return fieldPageManager;
	}



	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}



	public void saveMount()
	{
		//remove any old hot folders for this catalog
		getWebServer().reloadMounts();
	
		String originalpath = "/WEB-INF/data/" + getMediaArchive().getCatalogId() + "/originals";
		String toplevelfolder =  getConfig().get("subfolder");
		
		String	type = "mount";
		String fullpath = originalpath + "/" + toplevelfolder;
	
		List configs = new ArrayList(getPageManager().getRepositoryManager().getRepositories());
		for (Iterator iterator = configs.iterator(); iterator.hasNext();)
		{
			Repository config = (Repository) iterator.next();
			if( config.getPath().equals(fullpath))
			{
				getPageManager().getRepositoryManager().removeRepository(config.getPath());
			}
		}
		String external = getConfig().get("externalpath");
		if( external != null )
		{
			//String versioncontrol = folder.get("versioncontrol");
			Repository created = createRepo(type);
			created.setPath(fullpath);
			created.setExternalPath(external);
			created.setFilterIn(getConfig().get("includes"));
			created.setFilterOut(getConfig().get("excludes"));
			//add varliables
			/*
			for (Iterator iterator2 = folder.keySet().iterator(); iterator2.hasNext();) {
				
				String key = (String) iterator2.next();
				created.setProperty(key, (String) folder.get(key)); //
			}
			*/
			configs = getPageManager().getRepositoryManager().getRepositories();
			configs.add(created);
		}	
		//TODO: Make the folder? Thats on loading
		
		getWebServer().saveMounts(configs);
	}
	
	public void refresh( ) 
	{
		
	}

	public boolean isHotFolder()
	{
		return false;
	}


	protected Repository createRepo(String inType)
	{
		Repository repo;
		if("version".equals(inType) )
		{
			repo = new XmlVersionRepository();
			repo.setRepositoryType("versionRepository");
		}
		else
		{
			repo = new FileRepository();
		}
		return repo;
	}

	@Override
	public int removeExtraCategories()
	{
		return -1;
	}
	
}
