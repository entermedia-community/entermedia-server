package org.openedit.entermedia;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermedia.error.EmailErrorHandler;
import org.entermedia.locks.Lock;
import org.entermedia.locks.LockManager;
import org.openedit.Data;
import org.openedit.data.PropertyDetails;
import org.openedit.data.PropertyDetailsArchive;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.entermedia.creator.CreatorManager;
import org.openedit.entermedia.edit.AssetEditor;
import org.openedit.entermedia.edit.CategoryEditor;
import org.openedit.entermedia.scanner.AssetImporter;
import org.openedit.entermedia.search.AssetSearcher;
import org.openedit.entermedia.search.AssetSecurityArchive;
import org.openedit.entermedia.search.SearchFilterArchive;
import org.openedit.entermedia.util.Replacer;
import org.openedit.event.WebEvent;
import org.openedit.event.WebEventHandler;

import com.openedit.ModuleManager;
import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.page.Page;
import com.openedit.page.Permission;
import com.openedit.page.manage.MimeTypeMap;
import com.openedit.page.manage.PageManager;
import com.openedit.users.User;
import com.openedit.util.PathUtilities;

public class MediaArchive
{
	private static final Log log = LogFactory.getLog(MediaArchive.class);
	protected File BLANKFILE = new File("blank");
	protected EmailErrorHandler fieldEmailErrorHandler;
	protected PageManager fieldPageManager;
	protected WebEventHandler fieldMediaEventHandler;
	protected CreatorManager fieldCreatorManager;

	protected AssetArchive fieldAssetArchive;
	protected AssetArchive fieldMirrorAssetArchive;
	protected Map fieldTaxRates;
	protected AssetSearcher fieldAssetSearcher;
	protected CatalogConverter fieldImportConverter;
	protected CategoryArchive fieldCategoryArchive;
	protected AssetExport fieldAssetExport;
	protected SearcherManager fieldSearcherManager;
	protected OriginalFileManager fieldOriginalFileManager;
	protected SearchFilterArchive fieldSearchFilterArchive;
	protected AssetSecurityArchive fieldAssetSecurityArchive;

	protected CategoryEditor fieldCategoryEditor;
	protected AssetEditor fieldAssetEditor;
	protected AssetImporter fieldAssetImporter;
	
	protected AssetStatsManager fieldAssetStatsManager;
	protected Replacer fieldReplacer;
	protected MimeTypeMap fieldMimeTypeMap;
	protected LockManager fieldLockManager;
	
	public String getMimeTypeIcon(String inFormat)
	{
		String mime = getMimeTypeMap().getMimeType(inFormat);
		if( mime != null)
		{
			mime = mime.replace('/','-');
			if( mime.startsWith("image") )
			{
				return "image-x-generic";
			}
			if( mime.startsWith("video") )
			{
				return "video-x-generic";
			}
			if( mime.startsWith("audio") )
			{
				return "audio-x-generic";
			}
			if( mime.startsWith("text") )
			{
				return "text-x-generic";
			}
		}
		else
		{
			return "missing";
		}
		return mime;
	}
	public MimeTypeMap getMimeTypeMap()
	{
		return fieldMimeTypeMap;
	}

	public void setMimeTypeMap(MimeTypeMap inMimeTypeMap)
	{
		fieldMimeTypeMap = inMimeTypeMap;
	}

	public Replacer getReplacer()
	{
		if( fieldReplacer == null)
		{
			fieldReplacer = new Replacer();
		}
		return fieldReplacer;
	}

	public void setReplacer(Replacer inReplacer)
	{
		fieldReplacer = inReplacer;
	}

	public AssetStatsManager getAssetStatsManager()
	{
		return fieldAssetStatsManager;
	}

	public void setAssetStatsManager(AssetStatsManager inAssetStatsManager)
	{
		fieldAssetStatsManager = inAssetStatsManager;
	}

	public SearchFilterArchive getSearchFilterArchive()
	{
		return fieldSearchFilterArchive;
	}

	public void setSearchFilterArchive(SearchFilterArchive searchFilterArchive)
	{
		fieldSearchFilterArchive = searchFilterArchive;
	}

	protected PropertyDetailsArchive fieldPropertyDetailsArchive;
	
	protected String fieldCatalogId;
	protected String fieldThemePrefix;
	
	protected ModuleManager fieldModuleManager;

	public MediaArchive()
	{
	}

	/**
	 * @param inAsset
	 * @return "/archive/downloads/asx/1073869002award_border/award border.eps";
	 */
	public String asLinkToAsx(Asset inAsset)
	{
		if (inAsset == null)
		{
			return null;
		}
		StringBuffer out = new StringBuffer();
		out.append(getCatalogHome() + "/downloads/asx/");
		out.append(inAsset.getSourcePath() + ".asx");
		out.append("?assetid=" + inAsset.getId());
		return out.toString();
	}

	/**
	 * @param inAsset
	 * @return "/archive/downloads/originals/1073869002award_border/award
	 *         border.eps";
	 */
	public String asLinkToOriginal(Asset inAsset)
	{
		if (inAsset == null)
		{
			return null;
		}
		return asLinkToOriginal(inAsset.getSourcePath(), inAsset.getPrimaryFile());
	}

	//TODO: Remove the inName option since that should be the  same as the originalattachment 
	public String asLinkToOriginal(String inSourcePath, String inPrimaryImageName)
	{
		if (inSourcePath == null)
		{
			return null;
		}
		StringBuffer out = new StringBuffer();
		out.append(getCatalogHome() + "/downloads/originals/");
		out.append(inSourcePath);

		if (inPrimaryImageName == null)
		{
			//Put the sourcepath on there again?
			inPrimaryImageName = PathUtilities.extractFileName(inSourcePath);
		}
		// TODO: Make this less redundant by making changes to Cumulus to
		// use a nicer source path such as 1234MyFile.eps.xconf
		out.append("/");
		out.append(inPrimaryImageName);
		return out.toString();
	}

	/**
	 * Returns a {@link File} representing the original document for the given
	 * asset. This file is not guaranteed to exist; it is simply where the
	 * document <em>ought</em> to be, not necessarily where it actually is.
	 * 
	 * @param inAsset
	 *            The asset
	 * 
	 * @return The location where the original document ought to be, or
	 *         <code>null</code> if that could not be determined
	 */
	public Page getOriginalDocument(Asset inAsset)
	{
		Page path = getOriginalFileManager().getOriginalDocument(inAsset);
		if (path == null)
		{
			return null;
		}
		return path;
	}

	public InputStream getOriginalDocumentStream(Asset inAsset) throws OpenEditException
	{
		return getOriginalFileManager().getOriginalDocumentStream(inAsset);
	}

	public PropertyDetails getAssetPropertyDetails()
	{
		return getPropertyDetailsArchive().getPropertyDetailsCached("asset");
	}

	public Asset getAssetBySourcePath(String inSourcePath)
	{
		return getAssetArchive().getAssetBySourcePath(inSourcePath);
	}
	
	public String asLinkToPreview(String inSourcePath)
	{
		return getCatalogHome() + "/downloads/preview/cache/" + inSourcePath + "/preview.jpg";
	}
	
	public int countSeries(String inAssetID) throws OpenEditException
	{
		Asset asset = (Asset) getAssetSearcher().searchById(inAssetID);

		String count = asset.getProperty("seriescount");

		if (count == null)
		{
			int i = 0;
			String series = asset.getProperty("Series");
			if (series != null)
			{
				SearchQuery searchQuery = getAssetSearcher().createSearchQuery();
				searchQuery.addMatches("Series:" + series);
				try
				{
					HitTracker hits = getAssetSearcher().search(searchQuery);
					i = hits.getTotal();
					asset.setProperty("seriescount", String.valueOf(i));
					AssetArchive assetArchive = getAssetArchive();
					assetArchive.saveAsset(asset);
				}
				catch (Exception e)
				{
					log.info("Error counting series for asset: " + inAssetID);
					return 0;
				}

			}
			return i;
		}
		else
		{
			return Integer.parseInt(count);
		}
	}

	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public String getCatalogHome()
	{
		return "/" + getCatalogId();
	}

	// public HistoryArchive getHistoryArchive()
	// {
	// return fieldHistoryArchive;
	// }
	//
	// public void setHistoryArchive(HistoryArchive inHistoryArchive)
	// {
	// fieldHistoryArchive = inHistoryArchive;
	// }

	public EmailErrorHandler getEmailErrorHandler()
	{
		return fieldEmailErrorHandler;
	}

	public void setEmailErrorHandler(EmailErrorHandler fieldEmailErrorHandler)
	{
		this.fieldEmailErrorHandler = fieldEmailErrorHandler;
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	public String getMediaRenderType(String inFileFormat)
	{
		return getCreatorManager().getRenderTypeByFileFormat(inFileFormat);
	}
	public Data getDefaultAssetTypeForFile(String inFileName)
	{
		String ext = PathUtilities.extractPageType(inFileName,true);
		if( ext == null)
		{
			return null;
		}
		Collection list = getSearcherManager().getList(getCatalogId(),"assettype");
		for (Iterator iterator = list.iterator(); iterator.hasNext();)
		{
			Data type = (Data) iterator.next();
			String exts = type.get("extensions");
			if( exts != null && exts.contains(ext))
			{
				return type;
			}
		}
		return null;
	}

	public String getAttachmentForType(String inAttachmentType, Asset inAsset)
	{
		String value = inAsset.getAttachmentByType(inAttachmentType);
		if( !"original".equals(inAttachmentType) )
		{
			if( value == null )
			{
				String origvalue = inAsset.getAttachmentByType("original");
				if ( origvalue != null )
				{
					String origtype = getMediaRenderType(PathUtilities.extractPageType( origvalue) );
					if( origtype != null)
					{
						
					}
					value = origvalue;
				}
			}
		}
		return value;
	}
	public boolean canConvert(Asset inAsset, String inOutputType, User inUser)
	{
		/*
		 * Note: we removed inUser.hasPermission("convert") checking.
		 * that permission doesn't seem to exist anymore.
		 */
		if (inAsset != null)
		{
			String type = inAsset.getFileFormat();
			if (type == null)
			{
				type = inAsset.getName();
			}
			if(type == null)
			{
				return false;
			}
			if (getCreatorManager().canConvert(type, inOutputType))
			{
				return true;
			}
		}
		return false;
	}
	
	public WebEventHandler getMediaEventHandler()
	{
		return fieldMediaEventHandler;
	}

	public void setMediaEventHandler(WebEventHandler inMediaEventHandler)
	{
		fieldMediaEventHandler = inMediaEventHandler;
	}

	public String getLinkToAssetDetails(String inSourcePath)
	{
		String assetroot = "/" + getCatalogId() + "/assets"; 
		return assetroot + "/" + inSourcePath + ".html";
	}
	
	public String getLinkToAssetViewer(String inSourcePath)
	{
		String viewerRoot = "/" + getCatalogId() + "/mediaviewer/"; 
		return viewerRoot + inSourcePath + ".html";
	}
	
	public boolean isFolderAsset(String inSourcePath)
	{
		String path = "/WEB-INF/data/" + getCatalogId() + "/originals/" + inSourcePath;
		boolean folder = getPageManager().getRepository().getStub(path).isFolder();
		return folder;
	}

	public CreatorManager getCreatorManager()
	{
		if (fieldCreatorManager == null)
		{
			fieldCreatorManager = (CreatorManager) getModuleManager().getBean(getCatalogId(), "creatorManager");
			fieldCreatorManager.setMediaArchive(this);
		}

		return fieldCreatorManager;
	}
	/**The home for the catalog
	 * The 
	 * @return
	 */
	public File getRootDirectory()
	{
		return new File(getPageManager().getRepository().getStub(getCatalogHome()).getAbsolutePath());
	}

	public OriginalFileManager getOriginalFileManager()
	{
		if (fieldOriginalFileManager == null)
		{
			fieldOriginalFileManager = (OriginalFileManager)getModuleManager().getBean(getCatalogId(), "originalFileManager");
			fieldOriginalFileManager.setMediaArchive(this);
		}

		return fieldOriginalFileManager;
	}

	public void setOriginalFileManager(OriginalFileManager inOriginalFileManager)
	{
		fieldOriginalFileManager = inOriginalFileManager;
	}

	public Asset createAsset(String inSourcePath)
	{
		Asset asset = new Asset();
		asset.setCatalogId(getCatalogId());
		String id = getAssetArchive().nextAssetNumber();
		asset.setId(id);
		asset.setSourcePath(inSourcePath);
		String name = PathUtilities.extractFileName(inSourcePath);
		asset.setName(name);
		String ext = PathUtilities.extractPageType(name);
		if( ext != null)
		{
			ext = ext.toLowerCase();
		}
		asset.setProperty("fileformat", ext);

		return asset;
	}

	public CategoryArchive getCategoryArchive()
	{
		if (fieldCategoryArchive == null)
		{
			fieldCategoryArchive = (CategoryArchive) getModuleManager().getBean(getCatalogId(), "categoryArchive");
			fieldCategoryArchive.setCatalogId(getCatalogId());
		}
		return fieldCategoryArchive;
	}

	public void setCategoryArchive(CategoryArchive inCategoryArchive)
	{
		fieldCategoryArchive = inCategoryArchive;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}
	
	public Asset getAsset(String inId)
	{
		Asset asset = (Asset) getAssetSearcher().searchById(inId);
		return asset;
	}

	public String getSourcePathForPage(WebPageRequest inReq)
	{
		String sourcepath = inReq.getRequestParameter("sourcepath");
		if( sourcepath == null)
		{
			Object asset = inReq.getPageValue("asset");
			if( asset != null && asset instanceof Asset)
			{	
				sourcepath = ((Asset)asset).getSourcePath();
			}
			else
			{
				sourcepath = getSourcePathForPage(inReq.getPage());
			}
		}
		return sourcepath;
	}
	
	public String getSourcePathForPage(Page inPage)
	{
		String sourcePath = null;
		String assetrootfolder = inPage.get("assetrootfolder");
		if (assetrootfolder != null && assetrootfolder.length() < inPage.getPath().length())
		{
			sourcePath = inPage.getPath().substring(assetrootfolder.length() + 1);

			String orig = inPage.get("sourcepathhasfilename");
			if (Boolean.parseBoolean(orig))
			{
				// Take off the extra test.eps part
				sourcePath = PathUtilities.extractDirectoryPath(sourcePath);
			}
			if (sourcePath.endsWith("folder") || sourcePath.endsWith("_site.xconf")) //Why is this shere?
			{
				sourcePath = PathUtilities.extractDirectoryPath(sourcePath);
				//sourcePath = sourcePath + "/";
			}
		}
		return sourcePath;
	}

	public void saveAsset(Asset inAsset, User inUser)
	{
		getAssetSearcher().saveData(inAsset, inUser);
	}
	public void saveAssets(Collection inAssets)
	{
		saveAssets(inAssets, (User)null);
	}
	public void saveAssets(Collection inAssets, User inUser)
	{
		getAssetSearcher().saveAllData((Collection<Data>) inAssets, inUser);
	}

	public synchronized ConvertStatus convertCatalog(User inUser, boolean inForce) throws Exception
	{
		ConvertStatus errors = new ConvertStatus();
		errors.setUser(inUser);
		errors.setForcedConvert(inForce);
		errors.add("conversion started on " + getCatalogId() + " full sync=" + inForce);
		getCatalogImportConverter().importAssets(this, errors);
		return errors;
	}

	public CatalogConverter getCatalogImportConverter()
	{
		return fieldImportConverter;
	}

	public void setCatalogImportConverter(CatalogConverter inCatalogImportConverter)
	{
		fieldImportConverter = inCatalogImportConverter;
	}

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	public String getThemePrefix()
	{
		if (fieldThemePrefix == null)
		{
			fieldThemePrefix = getPageManager().getPage(getCatalogHome()).get("themeprefix");
		}
		return fieldThemePrefix;
	}

	public void setThemePrefix(String inThemePrefix)
	{
		fieldThemePrefix = inThemePrefix;
	}

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}

	public PropertyDetailsArchive getPropertyDetailsArchive()
	{
		if (fieldPropertyDetailsArchive == null)
		{
			fieldPropertyDetailsArchive = (PropertyDetailsArchive) getModuleManager().getBean(getCatalogId(), "propertyDetailsArchive");
		}
		return fieldPropertyDetailsArchive;
	}

	public void setPropertyDetailsArchive(PropertyDetailsArchive inPropertyDetailsArchive)
	{
		fieldPropertyDetailsArchive = inPropertyDetailsArchive;
	}
	
	public AssetExport getAssetExport()
	{
		return fieldAssetExport;
	}

	public void setAssetExport(AssetExport assetExport)
	{
		fieldAssetExport = assetExport;
	}

	public AssetArchive getAssetArchive()
	{
		if (fieldAssetArchive == null)
		{
			fieldAssetArchive = (AssetArchive) getModuleManager().getBean(getCatalogId(), "assetArchive");
		}
		return fieldAssetArchive;
	}

	public AssetArchive getMirrorAssetArchive()
	{
		return fieldMirrorAssetArchive;
	}

	public void setMirrorAssetArchive(AssetArchive mirrorAssetArchive)
	{
		fieldMirrorAssetArchive = mirrorAssetArchive;
	}

	public AssetSearcher getAssetSearcher()
	{
		if (fieldAssetSearcher == null)
		{
			fieldAssetSearcher = (AssetSearcher) getSearcherManager().getSearcher(getCatalogId(), "asset");
		}
		return fieldAssetSearcher;
	}

	public void setAssetSearcher(AssetSearcher assetSearcher)
	{
		fieldAssetSearcher = assetSearcher;
	}

	public AssetSecurityArchive getAssetSecurityArchive()
	{
		return fieldAssetSecurityArchive;
	}

	public void setAssetSecurityArchive(AssetSecurityArchive assetSecurityArchive)
	{
		fieldAssetSecurityArchive = assetSecurityArchive;
	}

	public void setConvertManager(CreatorManager creatorManager)
	{
		fieldCreatorManager = creatorManager;
	}

	public Asset getAssetBySourcePath(Page inPage)
	{
		String assetrootfolder = inPage.get("assetrootfolder");
		//log.info(inPage.getPathUrl());
		if( assetrootfolder == null || assetrootfolder.length() >= inPage.getPath().length() )
		{
			return null;
		}
		if( !inPage.getPath().startsWith(assetrootfolder))
		{
			return null;
		}
		String	sourcePath = inPage.getPath().substring(assetrootfolder.length() + 1);
		String stripfilename = inPage.getProperty("sourcepathhasfilename");
		if (Boolean.parseBoolean(stripfilename))
		{
			sourcePath = PathUtilities.extractDirectoryPath(sourcePath); 
		}
		else
		{
			sourcePath = PathUtilities.extractPagePath(sourcePath);
		}
		
		
		Asset asset = getAssetBySourcePath(sourcePath);
		
		int index = sourcePath.length();
		while(index > 0 && asset == null)
		{
			sourcePath = sourcePath.substring(0, index);
			asset = getAssetBySourcePath(sourcePath);
			index = sourcePath.lastIndexOf("/");
		}
		
		return asset;
	}
	
	public void reindexAll() throws OpenEditException 
	{
		getAssetArchive().clearAssets(); // lets hope they are all saved
		// before we delete em
		getAssetSearcher().reIndexAll();
	}
	
	public List getAssetsInCategory(Category inCategory) throws OpenEditException
	{
		if (inCategory == null)
		{
			return null;
		}
		List assets = new ArrayList();
		SearchQuery q = getAssetSearcher().createSearchQuery();
		q.addMatches("category", inCategory.getId());
		
		HitTracker hits = getAssetSearcher().search(q);
		if (hits != null)
		{
			for (Iterator it = hits.iterator(); it.hasNext();)
			{
				Data doc = (Data) it.next();
				String id = doc.get("id");
				Asset asset = getAsset(id);
				if( id == null || asset.getId() == null )
				{
					throw new OpenEditException("ID cant be null");
				}
				if (asset != null)
				{
					assets.add(asset);
				} 
				else
				{
					log.info("Cannot find asset with id " + id);
				}
			}
		}
		return assets;
	}

	public CategoryEditor getCategoryEditor()
	{
		if (fieldCategoryEditor == null)
		{
			fieldCategoryEditor = (CategoryEditor) getModuleManager().getBean(getCatalogId(), "categoryEditor");
			fieldCategoryEditor.setMediaArchive(this);
		}
		return fieldCategoryEditor;
	}

	public void setCategoryEditor(CategoryEditor categoryEditor)
	{
		fieldCategoryEditor = categoryEditor;
	}

	public AssetEditor getAssetEditor() 
	{
		if (fieldAssetEditor == null) 
		{
			fieldAssetEditor = (AssetEditor) getModuleManager().getBean(getCatalogId(), "assetEditor");
			fieldAssetEditor.setMediaArchive(this);
		}
		return fieldAssetEditor;
	}

	public void setAssetEditor(AssetEditor assetEditor) 
	{
		fieldAssetEditor = assetEditor;
	}

	public Category getCategory(String inCategoryId)
	{
		return getCategoryArchive().getCategory(inCategoryId);
	}
	
	public String getLinkToSize(String inSourcePath, String inSize)
	{
		if (inSize == null)
		{
			return null;
		}
		return getCatalogHome() + "/downloads/preview/" + inSize + "/" + inSourcePath + "/thumb.jpg";
	}
	
	public String getLinkToSize(Asset inAsset, String inSize)
	{
		return getLinkToSize(inAsset.getSourcePath(), inSize);
	}

	public void removeGeneratedImages(Asset inAsset)
	{
		String path = "/WEB-INF/data/" + getCatalogId() + "/generated/" + inAsset.getSourcePath();
		Page dir = getPageManager().getPage(path);
		getPageManager().removePage(dir);
		
	}
	
	
	public void removeOriginals(Asset inAsset)
	{
		String path = "/WEB-INF/data/" + getCatalogId() + "/originals/" + inAsset.getSourcePath();
		Page dir = getPageManager().getPage(path);
		getPageManager().removePage(dir);
		
	}
	
	public String toString()
	{
		return getCatalogId();
	}

	public Page findOriginalMediaByType(String inType, Asset inAsset)
	{
		String path = "/WEB-INF/data" + getCatalogHome() + "/originals/" + inAsset.getSourcePath();

		String filename = inAsset.getAttachmentByType(inType);
		if( filename != null)
		{
			path = path + "/" + filename;
			return getPageManager().getPage(path);
		}

		filename = inAsset.getPrimaryFile();
		if( filename != null)
		{
			String found = getMediaRenderType(PathUtilities.extractPageType(filename));
			String fileformat = "";
			if(inAsset.getFileFormat() != null)
			{
				fileformat = getMediaRenderType(inAsset.getFileFormat());
			}
			if( found.equals(inType) || fileformat.equals(inType))
			{
				path = path + "/" + filename;
				return getPageManager().getPage(path);
			}
		}
		String thistype = inAsset.getFileFormat();
		String found = getMediaRenderType(thistype);
		if( found.equals(inType))
		{
			//path = path + "/" + filename;
			Page tryPage = getPageManager().getPage(path);
			if(tryPage.exists())
			{
				return tryPage;
			}
			else
			{
				path = getCatalogHome() + "/users/" + inAsset.getSourcePath();
				tryPage = getPageManager().getPage(path);
				if(tryPage.exists())
				{
					return tryPage;
				}
			}
			
		}
		return null;
	}
	public void fireMediaEvent(String operation, User inUser, Asset asset, List<String> inids)
	{
		WebEvent event = new WebEvent();
		event.setSearchType("asset");
		event.setCatalogId(getCatalogId());
		event.setOperation(operation);
		event.setUser(inUser);
		event.setSource(this);
		event.setSourcePath(asset.getSourcePath()); //TODO: This should not be needed any more
		event.setProperty("sourcepath", asset.getSourcePath());
		if( inids.size() < 10000)
		{
			StringBuffer paths = new StringBuffer();
			for (Iterator iterator = inids.iterator(); iterator.hasNext();)
			{
				String path = (String) iterator.next();
				paths.append(path);
				if( iterator.hasNext())
				{
					paths.append(",");
				}
			}
			event.setProperty("assetids", paths.toString());
		}
		//archive.getWebEventListener()
		getMediaEventHandler().eventFired(event);
		
	}
	public void fireMediaEvent(String operation, User inUser, Asset asset)
	{
			WebEvent event = new WebEvent();
			event.setSearchType("asset");
			event.setCatalogId(getCatalogId());
			event.setOperation(operation);
			event.setUser(inUser);
			event.setSource(this);
			event.setSourcePath(asset.getSourcePath()); //TODO: This should not be needed any more
			event.setProperty("sourcepath", asset.getSourcePath());
			event.setProperty("assetids", asset.getId() );

			//archive.getWebEventListener()
			getMediaEventHandler().eventFired(event);
	}

	public void fireMediaEvent(String operation, String inMetadataType, String inSourcePath,  User inUser)
	{
			WebEvent event = new WebEvent();
			event.setSearchType(inMetadataType);
			
			event.setCatalogId(getCatalogId());
			event.setOperation(operation);
			event.setUser(inUser);
			event.setSource(this);
			event.setProperty("sourcepath", inSourcePath);
			//archive.getWebEventListener()
			getMediaEventHandler().eventFired(event);
	}

	public Category getCategory(WebPageRequest inReq)
	{
		String CATEGORYID = "categoryid";
		Category category = null;
		String categoryId = inReq.getRequestParameter(CATEGORYID);
		if (categoryId == null)
		{
			Page page = inReq.getPage();
			categoryId = page.get(CATEGORYID);
		}
		if (categoryId == null)
		{
			category = (Category) inReq.getPageValue("category");
		}

		if (category == null && categoryId == null)
		{
			// get it from the path?
			String path = inReq.getPath();

			categoryId = PathUtilities.extractPageName(path);
			if (categoryId.endsWith(".draft"))
			{
				categoryId = categoryId.replace(".draft", "");
			}
		}

		// Why the content page? Page page = inPageRequest.getContentPage();
		if (category == null)
		{
			category = getCategoryArchive().getCategory(categoryId);
		}
		if (category == null)
		{
//			if (inReq.getContentPage() == inReq.getPage())
//			{
//				String val = inReq.findValue("showmissingcategories");
//				if (!Boolean.parseBoolean(val))
//				{
//					inReq.redirect("/" + getCatalogId() + "/search/nosuchcategory.html");
//				}
//			}
			log.error("No such category: " + categoryId);
			return null;
		}
		if( category != null)
		{
			inReq.putPageValue("category",category);
		}
		return category;
	}

	public AssetImporter getAssetImporter()
	{
		return fieldAssetImporter;
	}

	public void setAssetImporter(AssetImporter inAssetImporter)
	{
		fieldAssetImporter = inAssetImporter;
	}

	public void loadAssetPermissions(String sourcepath, WebPageRequest inReq)
	{
		String path = "/" + getCatalogId() + "/assets/" + sourcepath + "/_site.xconf";
		
		List names = Arrays.asList(new String[]{"download","forcewatermark","editasset", "viewasset", "view"});
		
		Page page = getPageManager().getPage(path);
		WebPageRequest req = inReq.copy(page);
		for (Iterator iterator = names.iterator(); iterator.hasNext();)
		{
			String pername = (String) iterator.next();
			Permission per = page.getPermission(pername);
			if (per != null)
			{
				boolean value = per.passes(req);
				//log.info(getCatalogId() + " " + pername + " = " + value + " " + per.getPath());
				inReq.putPageValue("can" + per.getName(), Boolean.valueOf(value) );
			}
		}
	}
	
	public void loadAllAssetPermissions(String inSourcepath, WebPageRequest inReq) 
	{
		String path = "/" + getCatalogId() + "/assets/" + inSourcepath + "/_site.xconf";
		
		Page assethome = getPageManager().getPage(path);
		WebPageRequest req = inReq.copy(assethome);
		
		List permissions = assethome.getPermissions();
		if (permissions != null)
		{
			for (Iterator iterator = permissions.iterator(); iterator.hasNext();)
			{
				Permission per = (Permission) iterator.next();
				boolean value = per.passes(req);
				inReq.putPageValue("can" + per.getName(), Boolean.valueOf(value));
			}
		}
	}

	
	public void loadCategoryPermissions(WebPageRequest inReq)
	{
		Page cathome = getPageManager().getPage(getCatalogHome());
		WebPageRequest req = inReq.copy(cathome);
		
		List permissions = cathome.getPermissions();
		if (permissions != null)
		{
			for (Iterator iterator = permissions.iterator(); iterator.hasNext();)
			{
				Permission per = (Permission) iterator.next();
				boolean value = per.passes(inReq);
				inReq.putPageValue("can" + per.getName(), Boolean.valueOf(value));
			}
		}
	}
	
	public boolean isTagSync(String inFileFormat)
	{
		Searcher searcher = getSearcherManager().getSearcher(getCatalogId(), "fileformat");
		Data hit = (Data) searcher.searchById(inFileFormat);
		if(hit == null)
		{
			return false;
		}
		String property = "synctags";
		if(hit.get(property) == null)
		{
			return true;
		}
		return Boolean.parseBoolean(hit.get(property));
	}
	
	public List getFilesIn(String inPath)
	{
		List pages = new ArrayList();
		List files = getPageManager().getChildrenPaths(inPath, false);
		for (Iterator iterator = files.iterator(); iterator.hasNext();)
		{
			String path = (String) iterator.next();
			if( getPageManager().getPage(path).isFolder() )
			{
				continue;
			}
			String filename = PathUtilities.extractFileName(path);
			if(!filename.endsWith("xconf"))
			{
				Page page = getPageManager().getPage(path);
				pages.add(page);
			}
		}
		return pages;
	}

	public void logDownload(String inSourcePath, String inResult, User inUser)
	{
		getAssetStatsManager().logAssetDownload(getCatalogId(), inSourcePath, inResult, inUser);
		
	}

	public String asContentDistributionSiteRoot(WebPageRequest inReq)
	{
		String contentsiteroot = inReq.findValue("contentsiteroot");
		if( contentsiteroot == null)
		{
			contentsiteroot = inReq.getSiteRoot();
		}
		return contentsiteroot;
	}

	public boolean doesAttachmentExist(String outputfile, Asset asset) {
		Page page = getPageManager().getPage("/WEB-INF/" + getCatalogId() + "/generated/" + asset.getSourcePath() + "/" + outputfile);
		return page.exists();
	}

	public String asExportFileName(Asset inAsset, Data inPreset)
	{
		String format = inPreset.get("fileexportformat");
		if( format == null)
		{
			String name = inPreset.get("outputfile");
			if( name == null)
			{
				name = inAsset.getName();
			}
			return name;
		}
		Date now = new Date();
		SimpleDateFormat ymd = new SimpleDateFormat("yyyyMMdd");
		SimpleDateFormat time = new SimpleDateFormat("kkmm");
		
		Map tmp = new HashMap(inPreset.getProperties());
		tmp.put("assetid", inAsset.getId());
		tmp.put("filename", inAsset.getName());
		tmp.put("catalogid", inAsset.getCatalogId());
		tmp.put("sourcepath", inAsset.getSourcePath());
		tmp.put("date", ymd.format(now));
		tmp.put("time", time.format(now));
		
		String result = getReplacer().replace(format, tmp);
		return result;
	}
	public LockManager getLockManager()
	{
		return fieldLockManager;
	}
	public void setLockManager(LockManager inLockManager)
	{
		fieldLockManager = inLockManager;
	}
	
	public Lock lockAssetIfPossible(String inSourcePath, User inUser)
	{
		Lock lock = getLockManager().lockIfPossible(getCatalogId(), getCatalogHome() + "/" + inSourcePath, inUser.getId());
		return lock;
	}
	
	public boolean releaseLock(Lock inLock)
	{
		if( inLock == null)
		{
			throw new OpenEditException("Previous lock was null");
		}
		if( inLock.getId() == null)
		{
			throw new OpenEditException("Previous lock id was null");
		}

		boolean ok = getLockManager().release(getCatalogId(), inLock);
		return ok;
	}
	
	public String formatLength(Object inValue)
	{
		String secondstr = String.valueOf(inValue);
		if(secondstr.length() == 0)
		{
			return "00:00:00";
		}
		int seconds = Integer.parseInt(secondstr);
		if(seconds == 0)
		{
			return "00:00:00";
		}
		
		Calendar cal = new GregorianCalendar();
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.add(Calendar.SECOND, seconds);
		
		StringBuffer length = new StringBuffer();
		if(cal.get(Calendar.HOUR) < 10)
		{
			length.append("0");
		}
		length.append(cal.get(Calendar.HOUR) + ":");
		if(cal.get(Calendar.MINUTE) < 10)
		{
			length.append("0");
		}
		length.append(cal.get(Calendar.MINUTE) + ":");
		if(cal.get(Calendar.SECOND) < 10)
		{
			length.append("0");
		}
		length.append(cal.get(Calendar.SECOND));
		
		return length.toString();
	}
}
