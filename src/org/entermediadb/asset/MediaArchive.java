package org.entermediadb.asset;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.mail.internet.InternetAddress;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.convert.TranscodeTools;
import org.entermediadb.asset.edit.AssetEditor;
import org.entermediadb.asset.edit.CategoryEditor;
import org.entermediadb.asset.orders.OrderManager;
import org.entermediadb.asset.scanner.AssetImporter;
import org.entermediadb.asset.scanner.PresetCreator;
import org.entermediadb.asset.search.AssetSearcher;
import org.entermediadb.asset.search.AssetSecurityArchive;
import org.entermediadb.asset.sources.AssetSourceManager;
import org.entermediadb.asset.xmldb.CategorySearcher;
import org.entermediadb.email.TemplateWebEmail;
import org.entermediadb.error.EmailErrorHandler;
import org.entermediadb.events.PathEventManager;
import org.entermediadb.projects.ProjectManager;
import org.entermediadb.users.PermissionManager;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.cache.CacheManager;
import org.openedit.data.BaseData;
import org.openedit.data.PropertyDetails;
import org.openedit.data.PropertyDetailsArchive;
import org.openedit.data.QueryBuilder;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.event.EventManager;
import org.openedit.event.WebEvent;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.ListHitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.locks.Lock;
import org.openedit.locks.LockManager;
import org.openedit.node.NodeManager;
import org.openedit.page.Page;
import org.openedit.page.PageSettings;
import org.openedit.page.Permission;
import org.openedit.page.manage.MimeTypeMap;
import org.openedit.page.manage.PageManager;
import org.openedit.profile.UserProfile;
import org.openedit.repository.ContentItem;
import org.openedit.users.Group;
import org.openedit.users.User;
import org.openedit.users.UserManager;
import org.openedit.util.PathProcessor;
import org.openedit.util.PathUtilities;
import org.openedit.util.Replacer;
import org.openedit.util.RequestUtils;
import org.openedit.util.URLUtilities;

public class MediaArchive implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(MediaArchive.class);
	protected File BLANKFILE = new File("blank");
	protected EmailErrorHandler fieldEmailErrorHandler;
	protected PageManager fieldPageManager;

	protected EventManager fieldEventManager;

	//	protected EventManager fieldMediaEventHandler;
	//	protected EventManager fieldLoggingEventHandler;

	public EventManager getEventManager()
	{
		return fieldEventManager;
	}

	public void setEventManager(EventManager inEventManager)
	{
		fieldEventManager = inEventManager;
	}

	protected TranscodeTools fieldTranscodeTools;
	protected AssetArchive fieldAssetArchive;
	protected AssetArchive fieldMirrorAssetArchive;
	//	protected Map fieldTaxRates;
	protected AssetSearcher fieldAssetSearcher;
	protected CatalogConverter fieldImportConverter;
	protected CategoryArchive fieldCategoryArchive;
	protected AssetExport fieldAssetExport;
	protected SearcherManager fieldSearcherManager;
	protected AssetSourceManager fieldAssetManager;
	protected AssetSecurityArchive fieldAssetSecurityArchive;

	protected CategoryEditor fieldCategoryEditor;
	protected AssetEditor fieldAssetEditor;
	protected AssetImporter fieldAssetImporter;

	protected AssetStatsManager fieldAssetStatsManager;
	protected Replacer fieldReplacer;
	protected MimeTypeMap fieldMimeTypeMap;
	protected LockManager fieldLockManager;
	protected Map<String, Data> fieldLibraries;
	protected PresetCreator fieldPresetManager;
	protected CacheManager fieldCacheManager;
	protected OrderManager fieldOrderManager;
	protected UserManager fieldUserManager;

	public CacheManager getCacheManager()
	{
		return fieldCacheManager;
	}

	public void setCacheManager(CacheManager inCacheManager)
	{
		fieldCacheManager = inCacheManager;
	}

	public PresetCreator getPresetManager()
	{
		if (fieldPresetManager == null)
		{
			fieldPresetManager = new PresetCreator();
			fieldPresetManager.setCacheManager(getCacheManager());
		}
		return fieldPresetManager;
	}

	public void setPresetManager(PresetCreator inPresetManager)
	{
		fieldPresetManager = inPresetManager;
	}

	public String getMimeTypeIcon(String inFormat)
	{
		String mime = getMimeTypeMap().getMimeType(inFormat);
		if (mime != null)
		{
			mime = mime.replace('/', '-');
			if (mime.startsWith("image"))
			{
				return "image-x-generic";
			}
			if (mime.startsWith("video"))
			{
				return "video-x-generic";
			}
			if (mime.startsWith("audio"))
			{
				return "audio-x-generic";
			}
			if (mime.startsWith("text"))
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
		if (fieldReplacer == null)
		{
			fieldReplacer = new Replacer();
			fieldReplacer.setCatalogId(getCatalogId());
			fieldReplacer.setSearcherManager(getSearcherManager());
			fieldReplacer.setAlwaysReplace(true);
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
	public String asLinkToOriginal(Data inAsset)
	{
		if (inAsset == null)
		{
			return null;
		}
		return asLinkToOriginal(inAsset.getSourcePath(), inAsset.get("primaryfile"));
	}

	//TODO: Remove the inName option since that should be the  same as the originalattachment 
	public String asLinkToOriginal(String inSourcePath, String inPrimaryImageName)
	{
		if (inSourcePath == null)
		{
			return null;
		}
		StringBuffer out = new StringBuffer();
		out.append(inSourcePath);

		if (inPrimaryImageName == null)
		{
			//Put the sourcepath on there again?
			inPrimaryImageName = PathUtilities.extractFileName(inSourcePath);
		}
		inPrimaryImageName = URLUtilities.encode(inPrimaryImageName);
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
		ContentItem item = getAssetManager().getOriginalContent(inAsset);
		if (item == null)
		{
			return null;
		}
		Page page = new Page() //SPEED UP
		{
			public boolean isHtml()
			{
				return false;
			}
		};
		page.setName(item.getName());
		//Is this needed?
		String tmppath = getCatalogHome() + "/originals";
		PageSettings settings = getPageManager().getPageSettingsManager().getPageSettings(tmppath);
		page.setPageSettings(settings);
		page.setContentItem(item);

		return page;
	}

	public ContentItem getOriginalContent(Asset inAsset)
	{

		return getAssetManager().getOriginalContent(inAsset);
	}

	public InputStream getOriginalDocumentStream(Asset inAsset) throws OpenEditException
	{
		return getAssetManager().getOriginalDocumentStream(inAsset);
	}

	public PropertyDetails getAssetPropertyDetails()
	{
		return getPropertyDetailsArchive().getPropertyDetailsCached("asset");
	}

	//cached
	public Asset getAssetBySourcePath(String inSourcePath)
	{
		return (Asset) getAssetSearcher().query().or().exact("sourcepath", inSourcePath).exact("archivesourcepath", inSourcePath).searchOne();
	}

	/**
	 * @deprecated see asLinkToPreview
	 * @param inSourcePath
	 * @return
	 */
	public String asLinkToPreview(String inSourcePath)
	{
		return getCatalogHome() + "/downloads/preview/cache/" + inSourcePath + "/preview.jpg";
	}

	public int countSeries(String inAssetID) throws OpenEditException
	{
		Asset asset = (Asset) getAssetSearcher().searchById(inAssetID);

		String count = asset.get("seriescount");

		if (count == null)
		{
			int i = 0;
			String series = asset.get("Series");
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

	public String getMediaDbId()
	{
		String mediadb = getCatalogSettingValue("mediadbappid");
		if ((mediadb == null || mediadb.isEmpty()) && getCatalogId().endsWith("catalog"))
		{
			mediadb = getCatalogId().substring(0, getCatalogId().length() - 7) + "mediadb";
		}
		return mediadb;
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
		if (inFileFormat == null)
		{
			return "none";
		}
		return getTranscodeTools().getRenderTypeByFileFormat(inFileFormat);
	}

	public String getMediaRenderType(Data inAsset)
	{
		if (inAsset == null)
		{
			return "none";
		}
		String format = inAsset.get("fileformat");
		return getMediaRenderType(format);
	}

	public String getMediaPlayerType(Data inAsset)
	{
		if (inAsset == null)
		{
			return null;
		}

		if (inAsset.get("embeddedurl") != null)
		{
			return "embedded";
		}
		String format = inAsset.get("fileformat");
		String finalformat = getTranscodeTools().getRenderTypeByFileFormat(format);
		if (finalformat == null)
		{
			finalformat = "none";
		}
		return finalformat;
	}

	public Data getDefaultAssetTypeForFile(String inFileName)
	{
		String ext = PathUtilities.extractPageType(inFileName, true);
		if (ext == null)
		{
			return null;
		}
		Collection list = getSearcherManager().getList(getCatalogId(), "assettype");
		for (Iterator iterator = list.iterator(); iterator.hasNext();)
		{
			Data type = (Data) iterator.next();
			String exts = type.get("extensions");
			if (exts != null && exts.contains(ext))
			{
				return type;
			}
		}
		return null;
	}

	public String getAttachmentForType(String inAttachmentType, Asset inAsset)
	{
		String value = inAsset.getAttachmentByType(inAttachmentType);
		if (!"original".equals(inAttachmentType))
		{
			if (value == null)
			{
				String origvalue = inAsset.getAttachmentByType("original");
				if (origvalue != null)
				{
					String origtype = getMediaRenderType(PathUtilities.extractPageType(origvalue));
					if (origtype != null)
					{

					}
					value = origvalue;
				}
			}
		}
		return value;
	}
	//	public boolean canConvert(Asset inAsset, String inOutputType, User inUser)
	//	{
	//		/*
	//		 * Note: we removed inUser.hasPermission("convert") checking.
	//		 * that permission doesn't seem to exist anymore.
	//		 */
	//		if (inAsset != null)
	//		{
	//			String type = inAsset.getFileFormat();
	//			if (type == null)
	//			{
	//				type = inAsset.getName();
	//			}
	//			if(type == null)
	//			{
	//				return false;
	//			}
	//			if (getMediaCreator().canConvert(type, inOutputType))
	//			{
	//				return true;
	//			}
	//		}
	//		return false;
	//	}

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

	public TranscodeTools getTranscodeTools()
	{
		if (fieldTranscodeTools == null)
		{
			fieldTranscodeTools = (TranscodeTools) getModuleManager().getBean(getCatalogId(), "transcodeTools");
			fieldTranscodeTools.setMediaArchive(this);
		}

		return fieldTranscodeTools;
	}

	/**
	 * The home for the catalog The
	 * 
	 * @return
	 */
	public File getRootDirectory()
	{
		return new File(getPageManager().getRepository().getStub(getCatalogHome()).getAbsolutePath());
	}

	public AssetSourceManager getAssetManager()
	{
		if (fieldAssetManager == null)
		{
			fieldAssetManager = (AssetSourceManager) getModuleManager().getBean(getCatalogId(), "assetSourceManager");
			fieldAssetManager.setMediaArchive(this);
		}

		return fieldAssetManager;
	}

	public void setAssetManager(AssetSourceManager inAssetManager)
	{
		fieldAssetManager = inAssetManager;
	}

	//Only use on old style sourcepaths
	public Asset createAsset(String inId, String inSourcePath)
	{
		Asset asset = new Asset(this);
		//asset.setCatalogId(getCatalogId());
		if (inId == null)
		{
			inId = getAssetSearcher().nextAssetNumber();
		}
		asset.setId(inId);
		asset.setSourcePath(inSourcePath);
		String name = PathUtilities.extractFileName(inSourcePath);
		asset.setName(name);
		String ext = PathUtilities.extractPageType(name);
		if (ext != null)
		{
			ext = ext.toLowerCase();
		}
		asset.setProperty("fileformat", ext);

		return asset;
	}

	public Asset createAsset(String inSourcePath)
	{
		return createAsset(null, inSourcePath);
	}

	public CategorySearcher getCategorySearcher()
	{
		CategorySearcher searcher = (CategorySearcher) getSearcher("category");
		return searcher;
	}

	/**
	 * @deprecated use getCategorySearcher()
	 * @return
	 */
	public CategoryArchive getCategoryArchive()
	{
		if (fieldCategoryArchive == null)
		{
			//CategorySearcher searcher = (CategorySearcher)getSearcher("category");
			fieldCategoryArchive = (CategoryArchive) getModuleManager().getBean(getCatalogId(), "categoryArchive");
			//fieldCategoryArchive.setCatalogId(getCatalogId());
		}
		return fieldCategoryArchive;
	}

	public Category createCategoryPath(String inPath)
	{
		//Break down each name and load the category
		return getCategorySearcher().createCategoryPath(inPath);
	}

	public void setCategoryArchive(CategoryArchive inCategoryArchive)
	{
		fieldCategoryArchive = inCategoryArchive;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}

	public Asset getAsset(String assetid, WebPageRequest inReq)
	{
		Asset asset = null;
		
		if (assetid.startsWith("multiedit"))
		{
			CompositeAsset compositeasset = (CompositeAsset) inReq.getSessionValue(assetid);
			String hitssessionid = assetid.substring("multiedit".length() + 1);
			HitTracker hits = (HitTracker) inReq.getSessionValue(hitssessionid);
			if (compositeasset!= null && !compositeasset.getSelectedResults().hasChanged(hits)) 
			{
				asset = compositeasset;
			}

			if (asset == null)
			{
				if (hits == null)
				{
					log.error("Could not find " + hitssessionid);
					return null;
				}
				CompositeAsset composite = new CompositeAsset(this, hits);
				composite.setId(assetid);
				asset = composite;
				inReq.putSessionValue(assetid, asset);
			}
		}
		else
		{
			asset = getAsset(assetid);
		}
		return asset;

	}

	public Asset getAsset(String inId)
	{
		Asset asset = (Asset) getAssetSearcher().searchById(inId);
		return asset;
	}
	public Asset getCachedAsset(String inId)
	{
		if( inId == null)
		{
			return null;
		}
		Asset asset = (Asset)getCacheManager().get("assetcache", inId);
		if( asset == null && inId != null)
		{
			asset = (Asset) getAssetSearcher().searchById(inId);
			if( asset != null)
			{
				getCacheManager().put("assetcache", inId, asset);
			}
		}
		return asset;
	}

	public String getSourcePathForPage(WebPageRequest inReq)
	{
		String sourcepath = inReq.getRequestParameter("sourcepath");
		if (sourcepath == null)
		{
			Object asset = inReq.getPageValue("asset");
			if (asset != null && asset instanceof Asset)
			{
				sourcepath = ((Asset) asset).getSourcePath();
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

	public void saveAsset(CompositeAsset inAssets, User inUser)
	{
		inAssets.saveChanges();
		//		for (Iterator iterator = inAsset.iterator(); iterator.hasNext();)
		//		{
		//			Asset asset = (Asset) iterator.next();
		//			getAssetSearcher().saveData(asset, inUser);			
		//		}
	}

	public void saveAsset(Asset inAsset, User inUser)
	{
		if (inAsset instanceof CompositeAsset)
		{
			saveAsset((CompositeAsset) inAsset, inUser);
		}
		else
		{
			getAssetSearcher().saveData(inAsset, inUser);
		}
	}

	public void saveAsset(Asset inAsset)
	{
		getAssetSearcher().saveData(inAsset, null);
	}

	public void saveAssets(Collection inAssets)
	{
		saveAssets(inAssets, (User) null);
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
			fieldAssetArchive = (AssetArchive) getModuleManager().getBean(getCatalogId(), "assetDataArchive");
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

	public void setConvertManager(TranscodeTools creatorManager)
	{
		fieldTranscodeTools = creatorManager;
	}

	public Asset getAssetBySourcePath(Page inPage)
	{
		String assetrootfolder = inPage.get("assetrootfolder");
		//log.info(inPage.getPathUrl());
		if (assetrootfolder == null || assetrootfolder.length() >= inPage.getPath().length())
		{
			return null;
		}
		if (!inPage.getPath().startsWith(assetrootfolder))
		{
			return null;
		}
		String sourcePath = inPage.getPath().substring(assetrootfolder.length() + 1);
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
		while (index > 0 && asset == null)
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
				if (id == null || asset.getId() == null)
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
		if (inCategoryId == null)
		{
			return null;
		}
		return getCategorySearcher().getCategory(inCategoryId);
	}

	public String getLinkToSize(String inSourcePath, String inSize)
	{
		if (inSize == null)
		{
			return null;
		}
		return getCatalogHome() + "/downloads/preview/" + inSize + "/" + inSourcePath + "/thumb.jpg";
	}

	public String getLinkToSize(Data inAsset, String inSize)
	{
		if (inAsset == null)
		{
			return null;
		}
		return getLinkToSize(inAsset.getSourcePath(), inSize);
	}

	public void removeGeneratedImages(Asset inAsset)
	{
		removeGeneratedImages(inAsset, false);
	}

	public void removeGeneratedImages(Asset inAsset, final boolean everything)
	{
		//		if(everything){
		//			removeGeneratedImages(inAsset);
		//			return;
		//		}

		String path = "/WEB-INF/data/" + getCatalogId() + "/generated/" + inAsset.getSourcePath();
		if (inAsset.isFolder() && !path.endsWith("/"))
		{
			path = path + "/";

		}
//TODO: refine this to only do if no child folders
//		if (everything)
//		{
//			Page folder = getPageManager().getPage(path);
//			getPageManager().removePage(folder);
//			return;
//		}

		PathProcessor processor = new PathProcessor()
		{
			public void processFile(ContentItem inContent, User inUser)
			{
				//getPageManager().removePage(page);
				if (inContent.getName().startsWith("customthumb"))
				{
					return;
				}
				if (!everything && inContent.getName().equals("image1024x768.jpg"))
				{
					return;
				}
				//				if( inContent.getName().equals("document.pdf"))
				//				{
				//					return;
				//				}
				String type = PathUtilities.extractPageType(inContent.getPath());
				String fileformat = getMediaRenderType(type);
				if ("image".equals(fileformat))
				{
					Page page = getPageManager().getPage(inContent.getPath());
					getPageManager().removePage(page);
				}

			}
		};
		processor.setRecursive(true); //Should this be tr
		processor.setRootPath(path);
		processor.setPageManager(getPageManager());
		processor.process();

	}

	public void removeGeneratedPages(Asset inAsset, final String prefix)
	{

		String path = "/WEB-INF/data/" + getCatalogId() + "/generated/" + inAsset.getSourcePath();
		if (inAsset.isFolder() && !path.endsWith("/"))
		{
			path = path + "/";

		}
		log.info(inAsset);

		PathProcessor processor = new PathProcessor()
		{
			public void processFile(ContentItem inContent, User inUser)
			{
				log.info(inContent.getName());
				if (inContent.getName().contains(prefix) && inContent.getName().contains("page"))
				{

					Page page = getPageManager().getPage(inContent.getPath());
					getPageManager().removePage(page);
				}

			}
		};
		processor.setRecursive(true);
		processor.setRootPath(path);
		processor.setPageManager(getPageManager());
		processor.process();

	}

	public void removeOriginals(Asset inAsset)
	{
		ContentItem item = getOriginalContent(inAsset);
		getPageManager().getRepository().remove(item);

	}

	public void deleteAsset(Asset inAsset, boolean deletefiles)
	{
		if (deletefiles)
		{

			removeGeneratedImages(inAsset, deletefiles);
			removeOriginals(inAsset);
		}
		getAssetSearcher().delete(inAsset, null);
	}

	public String toString()
	{
		return getCatalogId();
	}

	public Page findOriginalMediaByType(String inType, Asset inAsset)
	{
		String path = "/WEB-INF/data" + getCatalogHome() + "/originals/" + inAsset.getSourcePath();

		String filename = inAsset.getAttachmentByType(inType);
		if (filename != null)
		{
			path = path + "/" + filename;
			return getPageManager().getPage(path);
		}

		filename = inAsset.getPrimaryFile();
		if (filename != null)
		{
			String found = getMediaRenderType(PathUtilities.extractPageType(filename));
			String fileformat = "";
			if (inAsset.getFileFormat() != null)
			{
				fileformat = getMediaRenderType(inAsset.getFileFormat());
			}
			if ((found != null && found.equals(inType)) || fileformat.equals(inType))
			{
				path = path + "/" + filename;
				return getPageManager().getPage(path);
			}
		}
		String thistype = inAsset.getFileFormat();
		String found = getMediaRenderType(thistype);
		if (found != null && found.equals(inType))
		{
			Page tryPage = getPageManager().getPage(path);
			if (tryPage.exists())
			{
				return tryPage;
			}
			else
			{
				path = getCatalogHome() + "/users/" + inAsset.getSourcePath();
				tryPage = getPageManager().getPage(path);
				if (tryPage.exists())
				{
					return tryPage;
				}
			}

		}
		return null;
	}

	public void firePathEvent(String operation, User inUser, Collection inData)
	{
		String runpath = "/" + getCatalogId() + "/events/" + operation + ".html";
		PathEventManager manager = (PathEventManager) getModuleManager().getBean(getCatalogId(), "pathEventManager");
		WebPageRequest request = manager.getRequestUtils().createPageRequest(runpath, inUser);

		request.setRequestParameter("catalogid", getCatalogId());
		request.putPageValue("hits", inData);
		manager.runPathEvent(runpath, request);
	}

	/*
	 * public void fireMediaEvent(String operation, User inUser, Asset asset,
	 * List<String> inids) { WebEvent event = new WebEvent();
	 * event.setSearchType("asset"); event.setCatalogId(getCatalogId());
	 * event.setOperation(operation); event.setUser(inUser);
	 * event.setSource(this); event.setSourcePath(asset.getSourcePath());
	 * //TODO: This should not be needed any more
	 * event.setProperty("sourcepath", asset.getSourcePath()); if( inids.size()
	 * < 10000) { StringBuffer paths = new StringBuffer();
	 * 
	 * for (Iterator iterator = inids.iterator(); iterator.hasNext();) { String
	 * path = (String) iterator.next(); paths.append(path); if(
	 * iterator.hasNext()) { paths.append(","); } }
	 * event.setProperty("assetids", paths.toString()); }
	 * event.setValue("dataids", inids); //archive.getWebEventListener()
	 * getMediaEventHandler().eventFired(event); }
	 */
	public void fireMediaEvent(String operation, User inUser, CompositeAsset inAsset)
	{
		for (Iterator iterator = inAsset.iterator(); iterator.hasNext();)
		{
			Asset asset = (Asset) iterator.next();
			fireMediaEvent(operation, inUser, asset);
		}
	}

	public void fireMediaEvent(String operation, User inUser, Asset asset)
	{
		if (operation.contains("/"))
		{
			log.error("Calling old style event");
		}
		fireMediaEvent("asset", operation, inUser, asset);
	}

	public void fireMediaEvent(String inSearchType, String operation, User inUser, Asset asset)
	{
		if (asset instanceof CompositeAsset)
		{
			fireMediaEvent(operation, inUser, (CompositeAsset) asset);
		}
		else
		{
			WebEvent event = new WebEvent();
			event.setSearchType(inSearchType);
			event.setCatalogId(getCatalogId());
			event.setOperation(operation);
			event.setUser(inUser);
			event.setSource(this);
			event.setSourcePath(asset.getSourcePath()); //TODO: This should not be needed any more
			event.setProperty("sourcepath", asset.getSourcePath());
			event.setProperty("assetids", asset.getId());
			event.setProperty("dataid", asset.getId()); //Needed?
			event.setValue("asset", asset);
			//archive.getWebEventListener()
			getEventManager().fireEvent(event);
		}
	}

	public void fireMediaEvent(String operation, User inUser)
	{
		WebEvent event = new WebEvent();
		event.setSearchType("asset");
		event.setCatalogId(getCatalogId());
		event.setOperation(operation);
		event.setUser(inUser);
		event.setSource(this);
		//event.setSourcePath("/"); //TODO: This should not be needed any more
		getEventManager().fireEvent(event);
	}

	public void fireMediaEvent(String inMetadataType, String operation, String inId, User inUser)
	{
		WebEvent event = new WebEvent();
		event.setSearchType(inMetadataType);

		event.setCatalogId(getCatalogId());
		event.setOperation(operation);
		event.setUser(inUser);
		event.setSource(this);
		//event.setProperty("sourcepath", inSourcePath);
		event.setProperty("targetid", inId);
		//archive.getWebEventListener()
		getEventManager().fireEvent(event);
	}

	//conversionfailed  conversiontask assetsourcepath, params[id=102], admin
	public void fireMediaEvent(String inMetadataType, String operation, String inSourcePath, Map inParams, User inUser)
	{
		WebEvent event = new WebEvent();
		event.setProperties(inParams);
		event.setSearchType(inMetadataType);

		event.setCatalogId(getCatalogId());
		event.setOperation(operation);
		event.setUser(inUser);
		event.setSource(this);

		event.setProperty("sourcepath", inSourcePath);
		//archive.getWebEventListener()
		getEventManager().fireEvent(event);
	}

	public void fireGeneralEvent(User inUser, String inSearchType, String inAction, Map inParams)
	{
		WebEvent event = new WebEvent();
		event.setProperties(inParams);
		event.setSearchType(inSearchType);

		event.setCatalogId(getCatalogId());
		event.setOperation(inAction);
		event.setUser(inUser);
		event.setSource(this);

		//archive.getWebEventListener()
		getEventManager().fireEvent(event);
	}

	public Category getCategory(WebPageRequest inReq)
	{
		Category category = null;
		String categoryId = inReq.getRequestParameter("selectedcategory");
		String CATEGORYID = "categoryid";
		if (categoryId == null)
		{
			categoryId = inReq.getRequestParameter(CATEGORYID);
		}
		if (categoryId == null)
		{
			categoryId = inReq.getRequestParameter("nodeID");
		}
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
			category = getCategorySearcher().getCategory(categoryId);
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
		if (category != null)
		{
			inReq.putPageValue("category", category);
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

	/**
	 * Do not use this any more. Instead use xconf settings
	 * <action name="AssetControlModule.canViewAsset" />
	 * 
	 * @deprecated
	 * @param sourcepath
	 * @param inReq
	 */
	public void loadAssetPermissions(String sourcepath, WebPageRequest inReq)
	{
		Asset asset = (Asset) inReq.getPageValue("asset");
		if (asset == null)
		{
			asset = getAssetBySourcePath(sourcepath);
			inReq.putPageValue("asset", asset);
		}
		if (asset == null)
		{
			asset = findAsset(sourcepath);
		}
		if(asset == null) {
			return; //This doesn't work in collections!
		}

		List<String> types = Arrays.asList(new String[] { "edit", "view", "forcewatermark" });
		
		for (Iterator iterator = types.iterator(); iterator.hasNext();)
		{
			String type = (String) iterator.next();
			Boolean cando = getAssetSecurityArchive().canDo(this, inReq.getUser(), inReq.getUserProfile(), type, asset);
			inReq.putPageValue("can" + type + "asset", cando);
		}
	}

	public Asset findAsset(String inSourcepath)
	{
		Asset asset = getAssetBySourcePath(inSourcepath);
		if (asset == null && inSourcepath.contains("/"))
		{
			inSourcepath = inSourcepath.substring(0, inSourcepath.lastIndexOf("/"));
			asset = getAssetBySourcePath(inSourcepath);
			if (asset == null)
			{
				return findAsset(inSourcepath);
			}
			else
			{
				return asset;
			}
		}
		return null;
	}

	/*
	 * public void loadAllAssetPermissions(String inSourcepath, WebPageRequest
	 * inReq) { String path = "/" + getCatalogId() + "/assets/" + inSourcepath +
	 * "/_site.xconf";
	 * 
	 * Page assethome = getPageManager().getPage(path); WebPageRequest req =
	 * inReq.copy(assethome);
	 * 
	 * List permissions = assethome.getPermissions(); if (permissions != null) {
	 * for (Iterator iterator = permissions.iterator(); iterator.hasNext();) {
	 * Permission per = (Permission) iterator.next(); boolean value =
	 * per.passes(req); inReq.putPageValue("can" + per.getName(),
	 * Boolean.valueOf(value)); } } }
	 */
	public Data getCatalogSetting(String inId)
	{
		Data setting = getSearcherManager().getData(getCatalogId(), "catalogsettings", inId);

		return setting;
	}

	public String getCatalogSettingValue(String inId)
	{
		String value = (String) getCacheManager().get("catalogsettings", inId);
		if (value == CacheManager.NULLVALUE)
		{
			return null;
		}
		if (value != null)
		{
			return value;
		}
		Data setting = getCatalogSetting(inId);
		//log.info("Loading " + inId);
		if (setting == null)
		{
			value = CacheManager.NULLVALUE;
		}
		else
		{
			value = setting.get("value");
			if (value == null)
			{
				log.info("Null value " + getCatalogId() + " " + inId);
				value = CacheManager.NULLVALUE;
			}
		}
		getCacheManager().put("catalogsettings", inId, value);
		if (value == CacheManager.NULLVALUE)
		{
			return null;
		}
		return value;
	}

	public void setCatalogSettingValue(String inId, String inValue)
	{
		Searcher search = getSearcher("catalogsettings");
		Data setting = (Data) search.searchById(inId);
		if (setting == null)
		{
			setting = search.createNewData();
			setting.setId(inId);
		}
		setting.setProperty("value", inValue);
		search.saveData(setting, null);
		getCacheManager().remove("catalogsettings", inId);
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
		if (inFileFormat == null)
		{
			return false;
		}
		Searcher searcher = getSearcherManager().getSearcher(getCatalogId(), "fileformat");
		Data hit = (Data) searcher.searchById(inFileFormat);
		if (hit == null)
		{
			return false;
		}
		String property = "synctags";
		if (hit.get(property) == null)
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
			if (getPageManager().getPage(path).isFolder())
			{
				continue;
			}
			String filename = PathUtilities.extractFileName(path);
			if (!filename.endsWith("xconf"))
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
		if (contentsiteroot == null)
		{
			contentsiteroot = inReq.getSiteRoot();
		}
		return contentsiteroot;
	}

	public boolean doesAttachmentExist(Data asset, Data inPreset, int inPageNumber)
	{
		String outputfile = inPreset.get("generatedoutputfile");
		if (inPageNumber > 1)
		{
			String name = PathUtilities.extractPageName(outputfile);
			String ext = PathUtilities.extractPageType(outputfile);
			outputfile = name + "page" + inPageNumber + "." + ext;
		}
		ContentItem page = getPageManager().getRepository().get("/WEB-INF/data/" + getCatalogId() + "/generated/" + asset.getSourcePath() + "/" + outputfile);
		return page.getLength() > 1;

	}

	public boolean doesAttachmentExist(String outputfile, Data asset)
	{
		ContentItem page = getPageManager().getRepository().get("/WEB-INF/data/" + getCatalogId() + "/generated/" + asset.getSourcePath() + "/" + outputfile);
		return page.getLength() > 1;
	}

	public String asExportFileName(Data inAsset, Data inPreset)
	{
		return asExportFileName(null, inAsset, inPreset);
	}

	public String asExportFileName(User inUser, Data inAsset, Data inPreset)
	{
		if(inAsset == null || inPreset == null) {
			return null;
		}
		String format = inPreset.get("fileexportformat");
		if (format == null)
		{
			String name = inPreset.get("generatedoutputfile");
			if (name == null)
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

		String shortname = PathUtilities.extractPageName(inAsset.getName());
		tmp.put("shortfilename", shortname);

		tmp.put("catalogid", getCatalogId());
		tmp.put("sourcepath", inAsset.getSourcePath());
		tmp.put("date", ymd.format(now));
		tmp.put("time", time.format(now));
		tmp.put("asset", inAsset);
		tmp.put("preset", inPreset);
		if (inUser != null)
		{
			tmp.put("user", inUser);
			tmp.put("username", inUser.getUserName());
		}

		String result = getReplacer().replace(format, tmp);
		return result;
	}

	public LockManager getLockManager()
	{
		if (fieldLockManager == null)
		{
			fieldLockManager = (LockManager) getModuleManager().getBean(getCatalogId(), "lockManager");
		}
		return fieldLockManager;
	}

	public void setLockManager(LockManager inLockManager)
	{
		fieldLockManager = inLockManager;
	}

	public Lock lock(String inPath, String inOwner)
	{
		return getLockManager().lock(inPath, inOwner);
	}

	public boolean releaseLock(Lock inLock)
	{
		if (inLock == null)
		{
			//throw new OpenEditException("Previous lock was null");
			return false;
		}
		if (inLock.getId() == null)
		{
			throw new OpenEditException("Previous lock id was null");
		}

		boolean ok = getLockManager().release(inLock);
		return ok;
	}

	public String formatLength(Object inValue)
	{
		String secondstr = String.valueOf(inValue);
		if (secondstr.length() == 0)
		{
			return "00:00:00";
		}
		int seconds = Integer.parseInt(secondstr);
		if (seconds == 0)
		{
			return "00:00:00";
		}

		Calendar cal = new GregorianCalendar();
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.add(Calendar.SECOND, seconds);

		StringBuffer length = new StringBuffer();
		if (cal.get(Calendar.HOUR) < 10)
		{
			length.append("0");
		}
		length.append(cal.get(Calendar.HOUR) + ":");
		if (cal.get(Calendar.MINUTE) < 10)
		{
			length.append("0");
		}
		length.append(cal.get(Calendar.MINUTE) + ":");
		if (cal.get(Calendar.SECOND) < 10)
		{
			length.append("0");
		}
		length.append(cal.get(Calendar.SECOND));

		return length.toString();
	}

	public String formatMinutesAndSeconds(String inSeconds)
	{
		if (inSeconds == null || inSeconds.trim().length() == 0)
			return ":00";
		StringBuilder sb = new StringBuilder();
		int allSeconds = 0;
		try
		{
			float secs = Float.parseFloat(inSeconds);
			allSeconds = new Float(secs).intValue();
		}
		catch (NumberFormatException e)
		{
		} //not handled
		int minutes = allSeconds > 60 ? allSeconds / 60 : 0;
		int seconds = allSeconds % 60;
		String min = minutes > 0 ? String.valueOf(minutes) : "";
		String sec = seconds >= 10 ? String.valueOf(seconds) : seconds > 0 ? "0" + String.valueOf(seconds) : "00";
		sb.append(min + ":" + sec);
		return sb.toString();
	}

	public String formatSeconds(String inSeconds, String inMask)
	{

		Long millis = Long.parseLong(inSeconds) * 1000;

		return DurationFormatUtils.formatDuration(millis, inMask);

	}

	public Searcher getSearcher(String inSearchType)
	{
		return getSearcherManager().getSearcher(getCatalogId(), inSearchType);
	}

	public Data getData(String inSearchType, String inId)
	{
		if (inId == null)
		{
			return null;
		}
		Searcher searcher = getSearcher(inSearchType);
		Data hit = (Data) searcher.searchById(inId);
		hit = searcher.loadData(hit); //not needed?
		return hit;

	}

	public Data getCachedData(String inSearchType, String inId)
	{
		if (inId == null)
		{
			return null;
		}
		Searcher searcher = getSearcher(inSearchType);
		Data hit = (Data)getCacheManager().get("data" + inSearchType, inId);
		if( hit == null)
		{
			hit = (Data) searcher.searchById(inId);
			hit = searcher.loadData(hit); //not needed?
			getCacheManager().put("data" + inSearchType, inId,hit);
		}
		return hit;
	}
	
	public HitTracker getList(String inSearchType)
	{
		return getSearcherManager().getList(getCatalogId(), inSearchType);
	}

	public Collection getCatalogSettingValues(String inKey)
	{
		String value = getCatalogSettingValue(inKey);
		if (value == null)
		{
			return null;
		}
		if( value.contains("|"))
		{
			String[] vals = value.split("\\|+");
			Collection presets = Arrays.asList(vals);
			return presets;			
		}
		else
		{
			String[] vals = value.split("\\s+");
			Collection presets = Arrays.asList(vals);
			return presets;
		}	
	}

	//force runs now instead of on a delay in the scheduler
	public boolean fireSharedMediaEvent(String inName)
	{
		PathEventManager manager = (PathEventManager) getModuleManager().getBean(getCatalogId(), "pathEventManager");
		return manager.runSharedPathEvent(getCatalogHome() + "/events/" + inName + ".html");
	}

	//What is this for?
	public HitTracker getTracker(int total)
	{
		List all = new ArrayList(total);
		for (int i = 0; i < total; i++)
		{
			all.add(new Integer(i + 1));
		}
		HitTracker tracker = new ListHitTracker(all);
		return tracker;
	}

	public Data getLibrary(String inId)
	{
		Data library = (Data) getCacheManager().get("library_lookup", inId);
		if (library == null)
		{
			library = getSearcherManager().getData(getCatalogId(), "library", inId);
			if (library == null)
			{
				library = BaseData.NULL;
			}
			getCacheManager().put("library_lookup", inId, library);
		}
		if (library == BaseData.NULL)
		{
			return null;
		}
		return library;
	}


	//Look for previews that should be marked as complete now
	public void conversionCompleted(Asset asset)
	{
		if (asset == null)
		{
			return; //asset deleted
		}
		getPresetManager().conversionCompleted(this, asset);
	}

	public User getUser(String inId)
	{
		User user = getUserManager().getUserSearcher().getUser(inId,true);
		return user;
	}

	public UserManager getUserManager()
	{
		if (fieldUserManager == null)
		{
			fieldUserManager = (UserManager) getModuleManager().getBean(getCatalogId(), "userManager");
		}
		return fieldUserManager;
	}

	public void clearCaches()
	{
		getCacheManager().clearAll();
		getPresetManager().clearCaches();
		CacheManager shared = (CacheManager) getModuleManager().getBean("cacheManager");
		if( shared != null)
		{
			shared.clearAll();
		}
	}

	public ContentItem getContent(String inPath)
	{
		return getPageManager().getRepository().getStub(inPath);
	}

	public Collection<ContentItem> listGeneratedFiles(Data inHit)
	{
		String sourcepath = inHit.getSourcePath();
		String path = "/WEB-INF/data/" + getCatalogId() + "/generated/" + sourcepath + "/";
		Collection<ContentItem> children = new ArrayList();
		findItems(path, children);
		return children;
	}

	protected void findItems(String path, Collection<ContentItem> children)
	{
		Collection paths = getPageManager().getChildrenPaths(path);
		if (paths.isEmpty())
		{
			return;
		}

		for (Iterator iterator = paths.iterator(); iterator.hasNext();)
		{
			String cpath = (String) iterator.next();
			ContentItem item = getContent(cpath);
			if( item.isFolder())
			{
				findItems(item.getPath(), children);
			}
			else
			{
				children.add(item);
			}
		}
	}

	public OrderManager getOrderManager()
	{
		if (fieldOrderManager == null)
		{
			fieldOrderManager = (OrderManager) getModuleManager().getBean(getCatalogId(), "orderManager");
		}

		return fieldOrderManager;
	}

	public NodeManager getNodeManager()
	{
		return (NodeManager) getModuleManager().getBean(getCatalogId(), "nodeManager");
	}

	public ProjectManager getProjectManager()
	{
		ProjectManager manager = (ProjectManager) getModuleManager().getBean(getCatalogId(), "projectManager");
		return manager;
	}

	public void clearAll()
	{
		getCacheManager().clearAll();
		getPageManager().clearCache();
		getPageManager().getPageSettingsManager().clearCache();
		getSearcherManager().getPropertyDetailsArchive(getCatalogId()).clearCache();
		getSearcherManager().clear();
		getNodeManager().clear();
		getPresetManager().clearCaches();
		getPropertyDetailsArchive().clearCache();
		getCategorySearcher().clearIndex();
		getCategoryArchive().clearCategories();

	}

//	public Collection<Data> listHiddenCollections()
//	{
//		Searcher search = getSearcher("librarycollection");
//		Collection visibility = (Collection) getCacheManager().get("hiddencollection", search.getIndexId()); //Expires after 5 min
//		if (visibility == null)
//		{
//			visibility = getSearcher("librarycollection").query().exact("visibility", "3").search();
//			getCacheManager().put("hiddencollection", search.getIndexId(), visibility);
//		}
//		return visibility;
//	}

	/**
	 * @deprecated use view category view permissions
	 * 
	 */
	public Collection<Data> listPublicCollections()
	{
		Searcher search = getSearcher("librarycollection");
		Collection visibility = (Collection) getCacheManager().get("publiccollection", search.getIndexId()); //Expires after 5 min
		if (visibility == null)
		{
			visibility = getSearcher("librarycollection").query().orgroup("visibility", "1|2").search();
			getCacheManager().put("publiccollection", search.getIndexId(), visibility);
		}
		return visibility;
	}
	/**
	 * @deprecated use view category
	 * 
	 */
	public Collection<Category> listPublicCategories()
	{
		Searcher search = getSearcher("librarycollection");
		Collection<Category> categories = (Collection) getCacheManager().get("publiccollectioncategories", search.getIndexId()); //Expires after 5 min
		if (categories == null)
		{
			categories = new ArrayList();

			Collection publiccollections = listPublicCollections();
			for (Iterator iterator = publiccollections.iterator(); iterator.hasNext();)
			{
				Data librarycollection = (Data) iterator.next();
				String categoryid = librarycollection.get("rootcategory");
				if (categoryid != null)
				{
					Category child = getCategory(categoryid);
					if (child != null)
					{
						categories.add(child);
					}
				}
			}
			getCacheManager().put("publiccollectioncategories", search.getIndexId(), categories);
		}
		return categories;

	}
/*
 * 	public Collection<Category> listHiddenCategories()

	{
		Searcher search = getSearcher("librarycollection");
		Collection<Category> categories = (Collection) getCacheManager().get("hiddencollectioncategories", search.getIndexId()); //Expires after 5 min
		if (categories == null)
		{
			categories = new ArrayList();
			Collection visibility = listHiddenCollections();
			for (Iterator iterator = visibility.iterator(); iterator.hasNext();)
			{
				Data librarycollection = (Data) iterator.next();
				String categoryid = librarycollection.get("rootcategory");
				if (categoryid != null)
				{
					Category child = getCategory(categoryid);
					if (child != null)
					{
						categories.add(child);
					}
				}
			}
			getCacheManager().put("hiddencollectioncategories", search.getIndexId(), categories);
		}
		return categories;

	}
   */
	/*
	public Collection<Category> listHiddenCategories(Collection<Category> inViewCategories)
	{
		Collection<Category> all = listHiddenCategories();
		if (inViewCategories == null || inViewCategories.isEmpty())
		{
			return all;
		}
		Set allowedset = new HashSet(inViewCategories.size());
		for (Iterator iterator2 = inViewCategories.iterator(); iterator2.hasNext();)
		{
			Category allowed = (Category) iterator2.next();
			allowedset.add(allowed.getId());
		}

		Collection<Category> filtered = new ArrayList<Category>(all.size());

		for (Iterator iterator = all.iterator(); iterator.hasNext();)
		{
			Category hidden = (Category) iterator.next();
			if (!allowedset.contains(hidden.getId()))
			{
				filtered.add(hidden);
			}
		}

		return filtered;
	}
	*/
	public QueryBuilder query(String inSearchType)
	{
		return getSearcher(inSearchType).query();
	}

	public Collection getBadges(MultiValued inRow)
	{
		Collection badges = inRow.getValues("badge");
		//		HitTracker chats = getSearcher("chatterbox").query().match("channel", "asset"+ inRow.getId()).match("type","message").search();
		//		if(chats.size() > 0) {	
		//			badges.add("haschats");
		//		}
		if (badges != null && !badges.isEmpty())
		{
			String id = inRow.get("badge"); //text version of the ids
			List b = (List) getCacheManager().get("badges", id); //Expires after 5 min, sort it?
			if (b == null)
			{
				b = new ArrayList<Data>();
				for (Iterator iterator = badges.iterator(); iterator.hasNext();)
				{
					String badgeid = (String) iterator.next();
					Data badge = getData("badge", badgeid);
					if (badge == null)
					{
						log.info("badge not defined" + badgeid);
					}
					else
					{

						b.add(badge);
					}
				}
				Collections.sort(b);
				getCacheManager().put("badges", id, b);
			}
			return b;
		}

		return null;
	}

	public TemplateWebEmail createSystemEmail(User inSendTo, String inTemplatePath)
	{
		TemplateWebEmail webmail = (TemplateWebEmail) getModuleManager().getBean("templateWebEmail");//from spring

		String fromemail = getCatalogSettingValue("system_from_email");
		String fromemailname = getCatalogSettingValue("system_from_email_name");

		webmail.setFrom(fromemail);
		webmail.setFromName(fromemailname);

		webmail.setMailTemplatePath(inTemplatePath);

		try
		{
			InternetAddress to = new InternetAddress(inSendTo.getEmail(), inSendTo.getShortDescription());
			webmail.setRecipient(to);
		}
		catch (UnsupportedEncodingException e)
		{
			throw new OpenEditException(e);
		}
		return webmail;
	}

	//Made by Thomas
	public TemplateWebEmail createSystemEmail(String email, String inTemplatePath)
	{
		TemplateWebEmail webmail = (TemplateWebEmail) getModuleManager().getBean("templateWebEmail");//from spring

		String fromemail = getCatalogSettingValue("system_from_email");
		String fromemailname = getCatalogSettingValue("system_from_email_name");

		webmail.setFrom(fromemail);
		webmail.setFromName(fromemailname);

		webmail.setMailTemplatePath(inTemplatePath);

		try
		{
			InternetAddress to = new InternetAddress(email, "");
			webmail.setRecipient(to);
		}
		catch (UnsupportedEncodingException e)
		{
			throw new OpenEditException(e);
		}
		return webmail;
	}

	public int getRealImageWidth(Data inHit)
	{
		String orientation = inHit.get("imageorientation");
		String width = null;
		if (orientation == null || orientation.equals("1") || orientation.equals("3"))
		{
			width = inHit.get("width");
		}
		else
		{
			width = inHit.get("height");
		}
		if (width == null)
		{
			return 0;
		}
		return Integer.parseInt(width);
	}

	public int getRealImageHeight(Data inHit)
	{
		String orientation = inHit.get("imageorientation");
		String height = null;
		if (orientation == null || orientation.equals("1") || orientation.equals("3"))
		{
			height = inHit.get("height");
		}
		else
		{
			height = inHit.get("width");
		}
		if (height == null)
		{
			return 0;
		}
		return Integer.parseInt(height);

	}

	public Object getBean(String inId)
	{
		return getModuleManager().getBean(getCatalogId(), inId);
	}

	public String asLinkToProfile(String assetid)
	{
		if( assetid != null)
		{
			Asset asset = getCachedAsset(assetid);
			if( asset != null)
			{
				return asLinkToPreview(asset, null, "image200x200.jpg");
			}
		}
		return null;
	}	

	
	public String asLinkToPreview(Data inAsset, String inGeneratedName) {
		
		return asLinkToPreview(inAsset, null, inGeneratedName);
	}	
	
	
	public String asLinkToDownload(Data inAsset, String inCollectionId, Data inPreset) {
		
		
		if (inAsset == null)
		{
			return null;
		}
		String cdnprefix = getCatalogSettingValue("cdn_prefix");
		String finalroot = null;
		if (cdnprefix == null)
		{
			RequestUtils rutil = (RequestUtils) getModuleManager().getBean("requestUtils");
			cdnprefix = rutil.getSiteRoot();
			if (cdnprefix.contains("localhost"))
			{
				cdnprefix = "";
			}
			//			//TODO: Look up the home variable?
			//			Searcher searcher = getSearcherManager().getSearcher(getCatalogId(), "catalogsettings");
			//			Data prefix = (Data)searcher.searchById("cdn_prefix");
			//			if( prefix == null)
			//			{
			//				prefix = searcher.createNewData();
			//				prefix.setId("cdn_prefix");
			//			}
			//			prefix.setValue("value", cdnprefix);
			//			searcher.saveData(prefix);
			//			getCacheManager().clear("catalogsettings");
		}
		
		String downloadroot = null;
		if(inCollectionId != null) {
			downloadroot = "/services/module/librarycollection/downloads/createpreset";
		} else {
			downloadroot = "/services/module/asset/downloads/createpreset";

		}
		
		
		
		String sourcepath = inAsset.getSourcePath();
		String generatedfilename = inPreset.get("generatedoutputfile") + "/" + inAsset.getName() + "-" + inPreset.get("generatedoutputfile");
		if(inCollectionId != null) {
		finalroot = cdnprefix + "/" + getMediaDbId() + downloadroot +"/" + inCollectionId +"/" + sourcepath + "/" + generatedfilename;
		}
		else {
			finalroot = cdnprefix + "/" + getMediaDbId() + downloadroot + "/" + sourcepath + "/" + generatedfilename;

		}
		finalroot = URLUtilities.urlEscape(finalroot);
		return finalroot;
	}
	
	public String asLinkToPreview(Data inAsset, String inCollectionId, String inGeneratedName)
	{
		return asLinkToPreview(inAsset,inCollectionId,inGeneratedName,false);
	}
	
	public String asLinkToPreview(Data inAsset, String inCollectionId, String inGeneratedName, boolean isExternalLink)
	{
		if (inAsset == null)
		{
			return null;
		}
		String finalroot = null;
		
		String cdnprefix = "";
		if (isExternalLink)
		{
			cdnprefix = getCatalogSettingValue("cdn_prefix");
			if (cdnprefix == null)
			{
				RequestUtils rutil = (RequestUtils) getModuleManager().getBean("requestUtils");
				cdnprefix = rutil.getSiteRoot();
				//				if (cdnprefix.contains("localhost")) //Prefix no longer used for internal checks
				//				{
				//					cdnprefix = "";
				//				}
			}
		}
		String sourcepath = inAsset.getSourcePath();

		String downloadroot = null;
		if (inCollectionId != null)
		{
			downloadroot = "/services/module/librarycollection/downloads/";
		}
		else
		{
			downloadroot = "/services/module/asset/downloads/";
		}

		if (inGeneratedName.contains("."))
		{
			if (inCollectionId != null)
			{
				finalroot = cdnprefix + "/" + getMediaDbId() + downloadroot + "preset/" + inCollectionId + "/" + sourcepath + "/" + inGeneratedName;
			}
			else
			{
				finalroot = cdnprefix + "/" + getMediaDbId() + downloadroot + "preset/" + sourcepath + "/" + inGeneratedName;
			}
		}
		else
		{
			finalroot = cdnprefix + "/" + getMediaDbId() + downloadroot + "preview/" + inGeneratedName + "/" + sourcepath + "/thumb.jpg";

		}
		finalroot = URLUtilities.urlEscape(finalroot);

		return finalroot;

	}

	public String asLinkToDownload(Data inAsset, Data inPreset)
	{

		//	<li><a href="$cdnprefix$home/$mediadbappid/services/module/asset/downloads/createpreset/${asset.sourcepath}/${result.generatedoutputfile}/${asset.name}-${result.generatedoutputfile}">$result.name</a></li>

		if (inAsset == null)
		{
			return null;
		}
		String cdnprefix = getCatalogSettingValue("cdn_prefix");
		String finalroot = null;
		if (cdnprefix == null)
		{
			RequestUtils rutil = (RequestUtils) getModuleManager().getBean("requestUtils");
			cdnprefix = rutil.getSiteRoot();
			if (cdnprefix.contains("localhost"))
			{
				cdnprefix = "";
			}
			//			//TODO: Look up the home variable?
			//			Searcher searcher = getSearcherManager().getSearcher(getCatalogId(), "catalogsettings");
			//			Data prefix = (Data)searcher.searchById("cdn_prefix");
			//			if( prefix == null)
			//			{
			//				prefix = searcher.createNewData();
			//				prefix.setId("cdn_prefix");
			//			}
			//			prefix.setValue("value", cdnprefix);
			//			searcher.saveData(prefix);
			//			getCacheManager().clear("catalogsettings");
		}
		String sourcepath = inAsset.getSourcePath();
		String generatedfilename = inPreset.get("generatedoutputfile") + "/" + inAsset.getName() + "-" + inPreset.get("generatedoutputfile");

		finalroot = cdnprefix + "/" + getMediaDbId() + "/services/module/asset/downloads/createpreset/" + sourcepath + "/" + generatedfilename;
		finalroot = URLUtilities.urlEscape(finalroot);
		return finalroot;

	}
	
	
	
	
	

	public boolean isCatalogSettingTrue(String string)
	{
		String catalogSettingValue = getCatalogSettingValue(string);
		return Boolean.parseBoolean(catalogSettingValue);
	}

	public void saveData(String inString, Data inSelectedgoal)
	{
		getSearcher(inString).saveData(inSelectedgoal);
	}

	public void updateAndSave(String searchtype, String dataid, String key, String value)
	{
		Data target = getData(searchtype, dataid);
		if (target != null)
		{
			target.setValue(key, value);
			saveData(searchtype, target);
		}

	}

	public PermissionManager getPermissionManager()
	{
		PermissionManager manager = (PermissionManager) getModuleManager().getBean(getCatalogId(), "permissionManager");
		return manager;
	}
	
	public File getFileForPath(String inPath) {
		return new File(getPageManager().getPage(inPath).getContentItem().getAbsolutePath());
	}

	public void saveData(String inString, Collection inTosave)
	{
		getSearcher(inString).saveAllData(inTosave,null);		
	}

	public Group getGroup(String inGid)
	{
		return getUserManager().getGroup(inGid);
	}

}