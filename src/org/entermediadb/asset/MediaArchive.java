package org.entermediadb.asset;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
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
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.internet.InternetAddress;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.asset.convert.ConversionManager;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.entermediadb.asset.convert.TranscodeTools;
import org.entermediadb.asset.edit.AssetEditor;
import org.entermediadb.asset.edit.CategoryEditor;
import org.entermediadb.asset.facedetect.FaceProfileManager;
import org.entermediadb.asset.orders.OrderManager;
import org.entermediadb.asset.scanner.AssetImporter;
import org.entermediadb.asset.scanner.MetaDataReader;
import org.entermediadb.asset.scanner.PresetCreator;
import org.entermediadb.asset.search.AssetSearcher;
import org.entermediadb.asset.search.AssetSecurityArchive;
import org.entermediadb.asset.sources.AssetSourceManager;
import org.entermediadb.asset.xmldb.CategorySearcher;
import org.entermediadb.email.PostMail;
import org.entermediadb.email.TemplateWebEmail;
import org.entermediadb.error.EmailErrorHandler;
import org.entermediadb.events.PathEventManager;
import org.entermediadb.find.EntityManager;
import org.entermediadb.find.FolderManager;
import org.entermediadb.projects.ProjectManager;
import org.entermediadb.users.PermissionManager;
import org.entermediadb.users.UserProfileManager;
import org.entermediadb.websocket.usernotify.UserNotifyManager;
import org.openedit.BaseWebPageRequest;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.cache.CacheManager;
import org.openedit.data.BaseCompositeData;
import org.openedit.data.BaseData;
import org.openedit.data.CompositeData;
import org.openedit.data.DataWithSearcher;
import org.openedit.data.PropertyDetail;
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
import org.openedit.util.DateStorageUtil;
import org.openedit.util.ExecutorManager;
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
	protected ExecutorManager fieldExecutorManager;

	public CacheManager getCacheManager()
	{
		if( fieldCacheManager == null)
		{
			fieldCacheManager = (CacheManager)getModuleManager().getBean(getCatalogId(), "cacheManager",true);
			//slog.info(getCatalogId() + "/fieldCacheManager" + fieldCacheManager.hashCode());
		}
		return fieldCacheManager;
	}

	public void setCacheManager(CacheManager inCacheManager)
	{
		fieldCacheManager = inCacheManager;
	}

	public ExecutorManager getExecutorManager()
	{
		if( fieldExecutorManager == null)
		{
			fieldExecutorManager = (ExecutorManager)getModuleManager().getBean(getCatalogId(), "executorManager",true);
		}
		return fieldExecutorManager;
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
				return "image";
			}
			if (mime.startsWith("video"))
			{
				return "camera-video";
			}
			if (mime.startsWith("audio"))
			{
				return "file-music";
			}
			if (mime.contains("pdf"))
			{
				return "file-earmark-pdf";
			}
			if (mime.endsWith("zip"))
			{
				return "file-earmark-zip";
			}
			if (mime.endsWith("document"))
			{
				return "file-earmark-word";
			}
			if (mime.contains("excel") || mime.contains("sheet"))  //application-vnd.ms-excel
			{
				return "file-earmark-excel";
			}
			if (mime.endsWith("xml"))  
			{
				if(inFormat.equals("dita")) {
					return "filetype-dita";
				}
				if(inFormat.equals("ditamap")) {
					return "filetype-ditamap";
				}
				return "filetype-xml";
			}
			if (mime.endsWith("html")) 
			{
				return "filetype-html";
			}
			if (mime.endsWith("css")) 
			{
				return "filetype-css";
			}
			if (mime.startsWith("text"))
			{
				return "card-text";
			}
		}
		//else
		{
			return "file-earmark";
		}
		//return mime;
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
			fieldReplacer = (Replacer)getModuleManager().getBean(getCatalogId(), "replacer");
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
	
	public String replaceFromMask(String inMask, Map extraVals, String locale) 
	{
		return replaceFromMask(inMask, null, null, extraVals, locale);
	}
	
	public String replaceFromMask(String inMask, Data inData, String inSearchType,  Map extraVals, String locale) 
	{

		Replacer replacer = getReplacer();
		
		Map newvals = new HashMap();
		
		if (inData != null) 
		{
			DataWithSearcher data = new DataWithSearcher(getSearcherManager(), getCatalogId(), inSearchType, inData);
			newvals.put("data", data);
		}
		
		newvals.put("formatteddate", DateStorageUtil.getStorageUtil().getTodayForDisplay());
		Date now = new Date();

		String date = DateStorageUtil.getStorageUtil().formatDateObj(now, "yyyy"); //TODO: Use DataStorage
		newvals.put("formattedyear", date);

		date = DateStorageUtil.getStorageUtil().formatDateObj(now, "MM"); //TODO: Use DataStorage
		newvals.put("formattedmonth", date);

		date = DateStorageUtil.getStorageUtil().formatDateObj(now, "dd"); //TODO: Use DataStorage
		newvals.put("formattedday", date);

		date = DateStorageUtil.getStorageUtil().formatDateObj(now, "HH"); //TODO: Use DataStorage
		newvals.put("formattedhour", date);
		
		if(extraVals != null) 
		{
			newvals.putAll(extraVals);
		}
		
		String val = replacer.replace(inMask, newvals);
		if( val.startsWith("$") && val.equals(inMask) )
		{
			return "";
		}
		//String val = getSearcherManager().getValue(getCatalogId(), inMask, inSearchType,  inData, extraVals, locale);
		return val;
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
	public String asLinkToOriginal(String inAssetId)
	{
		Asset asset = getAsset(inAssetId);
		
		if (asset == null)
		{
			return null;
		}
		return asLinkToOriginal(asset.getSourcePath(), asset.get("primaryfile"));
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
		//inPrimaryImageName = URLUtilities.encode(inPrimaryImageName);
		// TODO: Make this less redundant by making changes to Cumulus to
		// use a nicer source path such as 1234MyFile.eps.xconf
		out.append("/");
		out.append(inPrimaryImageName);
		String finalroot = URLUtilities.urlEscape(out.toString());
		return finalroot;
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
	public Page getOriginalDocument(Data inData)
	{
		Asset inAsset = (Asset)getAssetSearcher().loadData(inData);
		
		ContentItem item = getAssetManager().getOriginalContent(inAsset);
		if (item == null)
		{
			return null;
		}
		Page page = new Page(item.getPath(),null) //SPEED UP
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

	public ContentItem getOriginalContent(Data inAsset)
	{
		Asset asset = null;
		if( inAsset instanceof Asset)
		{
			asset = (Asset)inAsset;
		}
		else
		{
			asset = (Asset)getAssetSearcher().loadData(inAsset);
		}
		return getAssetManager().getOriginalContent(asset);
	}

	public String getPathToOriginal(Data inAsset)
	{
		Asset asset = null;
		if( inAsset instanceof Asset)
		{
			asset = (Asset)inAsset;
		}
		else
		{
			asset = (Asset)getAssetSearcher().loadData(inAsset);
		}
		String path = getAssetManager().getPathToOriginal(asset);
		return path;
	}

	
	public InputStream getOriginalDocumentStream(Asset inAsset) throws OpenEditException
	{
		return getAssetManager().getOriginalDocumentStream(inAsset);
	}
	
	public ContentItem getGeneratedContent(Data asset, String outputfile)
	{
		String outputname = generatedOutputName(asset, outputfile);
		ContentItem page = getPageManager().getRepository().get("/WEB-INF/data/" + getCatalogId() + "/generated/" + asset.getSourcePath() + "/" + outputname);
		return page;

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

	public Collection<Data> asListOfPreviewLinks(Data inAsset)
	{
		if(inAsset == null) {
			return null;
		}
		String rendertype = getMediaRenderType(inAsset.get("fileformat"));
		
		if(rendertype == null)
		{
			if(inAsset.get("fileformat") == "embedded")
			{
				rendertype = "image";   //assume jpg thumbnail was downloaded
			}
		}

		Collection<Data> presets = getPresetManager().getPresets(this, rendertype);
		
//		Collection<String> links = new ArrayList(presets.size());
//		for (Iterator iterator = presets.iterator(); iterator.hasNext();)
//		{
//			Data convertpreset = (Data) iterator.next();
//			String generatedname = convertpreset.get("generatedoutputfile");
//			String link = asLinkToPreview(inAsset, generatedname);
//			links.add(link);
//		}
		return presets;
	}

	/*
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
	*/
	
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
		String embeddedtype = inAsset.get("embeddedtype");
		if (embeddedtype != null)
		{
			//Default google images to large2
			if (embeddedtype.startsWith("google")) 
			{
				String rendertype = getMediaRenderType(inAsset);
				if (rendertype.equals("image") || rendertype.equals("video")) {
					return "large2";
				}
			}
			return embeddedtype;

		}
		
		return "large2";
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
		Asset asset = new BaseAsset(this);
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
		Asset data = (Asset)getAssetSearcher().loadData(inReq, assetid);
		return data;
	}

	public Asset getAsset(String inId)
	{
		Asset asset = (Asset) getAssetSearcher().loadData(inId);
		return asset;
	}
	public Asset getCachedAsset(String inId)
	{
		if( inId == null)
		{
			return null;
		}
		Asset asset = (Asset) getAssetSearcher().loadCachedData(inId);
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
			
			//remove 3 levels?
			String exportnameinpath = inPage.get("exportnameinpath");
			if (Boolean.parseBoolean(exportnameinpath))
			{
				// Take off the extra test.eps or large.jpg junk 
				sourcePath = PathUtilities.extractDirectoryPath(sourcePath);
			}
			String generatedfileinpath = inPage.get("generatedfileinpath");
			if( Boolean.parseBoolean(generatedfileinpath))
			{
				// Take off the extra test.eps or large.jpg junk 
				sourcePath = PathUtilities.extractDirectoryPath(sourcePath);
			}
			String sourcepathhasfilename = inPage.get("sourcepathhasfilename");
			if (Boolean.parseBoolean(sourcepathhasfilename))
			{
				// Take off the extra test.eps or large.jpg junk 
				sourcePath = PathUtilities.extractDirectoryPath(sourcePath);
			}
			
			if (sourcePath.endsWith("folder") || sourcePath.endsWith("_site.xconf")) //Why is this shere?
			{
				sourcePath = PathUtilities.extractDirectoryPath(sourcePath);
				//sourcePath = sourcePath + "/";
			}
			if( inPage.getPath().endsWith("video.m3u8"))
			{
				//This could be cut off TODO: make generic somehow
				sourcePath = sourcePath.substring(0, sourcePath.indexOf("video.m3u8") - 1);
			}
		}
		return sourcePath;
	}


	public void saveAsset(Asset inAsset, User inUser)
	{
		getAssetSearcher().saveData(inAsset, inUser);
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
		//getAssetArchive().clearAssets(); // lets hope they are all saved
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
		
		log.info("Removing generated images for: " + inAsset);

		PathProcessor processor = new PathProcessor()
		{
			public void processFile(ContentItem inContent, User inUser)
			{
				if( everything)
				{
					getPageManager().getRepository().remove(inContent);
					//log.info("All images removed.");
					return;
				}
				//getPageManager().removePage(page);
				if (inContent.getName().startsWith("customthumb") || inContent.getName().startsWith("image3000x3000"))
				{
					return;
				}
				//				if( inContent.getName().equals("document.pdf"))
				//				{
				//					return;
				//				}
				String type = PathUtilities.extractPageType(inContent.getPath());
				String fileformat = getMediaRenderType(type);
				if (!"default".equals(fileformat)) { //For fulltext.txt files
					Page page = getPageManager().getPage(inContent.getPath());
					getPageManager().removePage(page);
					log.info("Image removed: " + inContent.getName());
				}

			}
		};
		
		processor.setRecursive(true); //Should this be tr
		processor.setRootPath(path);
		processor.setPageManager(getPageManager());
		processor.process();
		//ContentItem original = getOriginalContent(inAsset);
		//Rerun Metadata
		//Why read metadata on deletion?
		//getAssetImporter().getAssetUtilities().getMetaDataReader().populateAsset(MediaArchive.this, original, inAsset );

	}
	
	// Remove one generated image
	public void removeGeneratedImage(Asset inAsset, final String prefix)
	{

		String path = "/WEB-INF/data/" + getCatalogId() + "/generated/" + inAsset.getSourcePath();
		if (inAsset.isFolder() && !path.endsWith("/"))
		{
			path = path + "/";

		}
		
		log.info("Removing generated images for: " + inAsset);

		PathProcessor processor = new PathProcessor()
		{
			public void processFile(ContentItem inContent, User inUser)
			{
				
				if (inContent.getName().contains(prefix))
				{
					Page page = getPageManager().getPage(inContent.getPath());
					getPageManager().removePage(page);
					log.info("Image removed: " + inContent.getName());
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
		log.info("Removing generated pages for: " + inAsset);

		PathProcessor processor = new PathProcessor()
		{
			public void processFile(ContentItem inContent, User inUser)
			{
				
				if (inContent.getName().contains(prefix) && inContent.getName().contains("page"))
				{

					Page page = getPageManager().getPage(inContent.getPath());
					getPageManager().removePage(page);
					log.info("Page removed: " + inContent.getName());
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

	public void firePathEvent(String operation, User inUser, Collection inHits)
	{
		String runpath = "/" + getCatalogId() + "/events/" + operation + ".html";
		PathEventManager manager = (PathEventManager) getModuleManager().getBean(getCatalogId(), "pathEventManager");
		WebPageRequest request = manager.getRequestUtils().createPageRequest(runpath, inUser);

		request.setRequestParameter("catalogid", getCatalogId());
		request.putPageValue("hits", inHits);
		manager.runPathEvent(runpath, request);
	}
	
	public void firePathEvent(String operation, User inUser, Map data)
	{
		String runpath = "/" + getCatalogId() + "/events/" + operation + ".html";
		PathEventManager manager = (PathEventManager) getModuleManager().getBean(getCatalogId(), "pathEventManager");
		WebPageRequest request = manager.getRequestUtils().createPageRequest(runpath, inUser);

		request.setRequestParameter("catalogid", getCatalogId());
		request.putPageValue("data", data);
		manager.runPathEvent(runpath, request);
	}

	public void fireDataEvent(User inUser, String inSearchType, String inAction, Data inData)
	{
		WebEvent event = new WebEvent();
		event.setSearchType(inSearchType);

		event.setCatalogId(getCatalogId());
		event.setOperation(inAction);
		event.setUser(inUser);
		event.setSource(this);
		event.setSourcePath(inData.getSourcePath()); //TODO: This should not be needed any more
		event.setProperty("dataid", inData.getId()); //Needed?
		event.setValue("data", inData);
		
		//archive.getWebEventListener()
		getEventManager().fireEvent(event);
	}

	public void fireDataEvent(User inUser, String inSearchType, String inAction, Collection<Data> inDatas)
	{
		WebEvent event = new WebEvent();
		event.setSearchType(inSearchType);

		event.setCatalogId(getCatalogId());
		event.setOperation(inAction);
		event.setUser(inUser);
		event.setSource(this);
		event.setValue("hits", inDatas);
		
		//archive.getWebEventListener()
		getEventManager().fireEvent(event);
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
	public void fireMediaEvent(String operation, User inUser, Collection inAssets)
	{
		 fireMediaEvent( inUser,  "asset",  operation,  inAssets);
	}
	public void fireMediaEvent(User inUser, String inSearchType, String operation, Collection inAssets)
	{
		WebEvent event = new WebEvent();
		event.setSearchType(inSearchType);
		event.setCatalogId(getCatalogId());
		event.setOperation(operation);
		event.setUser(inUser);
		event.setSource(this);
		

		//Support values?
		event.setValue("hits",inAssets);
		String[] all = new String[inAssets.size()];
		int i = 0;
		for (Iterator iterator = inAssets.iterator(); iterator.hasNext();) {
			Data child = (Data) iterator.next();
			all[i++] = child.getId();
		}
		event.setValue("assetids", all); //This is turned into reques params
		//event.setSourcePath("/"); //TODO: This should not be needed any more
		getEventManager().fireEvent(event);
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
			WebEvent event = new WebEvent();
			event.setSearchType(inSearchType);
			event.setCatalogId(getCatalogId());
			event.setOperation(operation);
			event.setUser(inUser);
			event.setSource(this);
			event.setSourcePath(asset.getSourcePath()); //TODO: This should not be needed any more
			event.setProperty("sourcepath", asset.getSourcePath());
			event.setProperty("assetids",  asset.getId() );
			event.setProperty("assetid", asset.getId());
			event.setProperty("dataid", asset.getId()); //Needed?
			event.setValue("asset", asset);
			//archive.getWebEventListener()
			getEventManager().fireEvent(event);
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
	/**
	 * @deprecated use fireGeneralEvent
	 * @param inMetadataType
	 * @param operation
	 * @param inParams
	 * @param inUser
	 */
	public void fireMediaEvent(String inMetadataType, String operation, Map inParams, User inUser)
	{
		fireGeneralEvent(inUser,inMetadataType,operation,inParams);
	}
	public void fireGeneralEvent(User inUser, String inSearchType, String inAction, Map inParams)
	{
		WebEvent event = new WebEvent();
		if( inParams != null)
		{
			event.setProperties(inParams);
		}
		event.setSearchType(inSearchType);

		event.setCatalogId(getCatalogId());
		event.setOperation(inAction);
		event.setUser(inUser);
		event.setSource(this);

		if( inParams != null && inParams.get("id") != null && event.getValue("dataid") == null )
		{
			event.setValue("dataid", inParams.get("id"));
		}
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
		/*
		if (categoryId == null)
		{
			categoryId = inReq.getRequestParameter("id"); //for tables
		}*/
		
		if (categoryId == null)
		{
			Page page = inReq.getPage();
			categoryId = page.get(CATEGORYID);
		}
		if (categoryId == null)
		{
			categoryId = (String)inReq.getPageValue("categoryid");
		}
		if (categoryId == null)
		{
			category = (Category) inReq.getPageValue("category");
		}

		if (category == null && categoryId == null)
		{
			categoryId = getIdFromPath(inReq);

			if (categoryId == null)
			{
				String path = inReq.getPath();
				categoryId = PathUtilities.extractPageName(path);
				if (categoryId.endsWith(".draft"))
				{
					categoryId = categoryId.replace(".draft", "");
				}
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
	
	public String getIdFromPath(WebPageRequest inReq)
	{
		String level = inReq.findActionValue("idlevel");
		if( level != null)
		{
			String[] levels = inReq.getContentPage().getPath().split("/");
			int pick = Integer.parseInt(level);
			int fromend = levels.length - pick - 1;
			return levels[fromend];
		}
		return null;
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
				log.info("Null value: " +  inId + " on catalog: " + getCatalogId());
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

	public String formatMinutesAndSeconds(Object inSeconds)
	{
		String text = null;
		if( inSeconds instanceof Double )
		{
			text = inSeconds.toString();
		}
		else
		{
			text = (String)inSeconds;
		}
		
		if (text == null || text.trim().length() == 0)
			return ":00";
		StringBuilder sb = new StringBuilder();
		int allSeconds = 0;
		try
		{
			float secs = Float.parseFloat(text);
			allSeconds = new Float(secs).intValue();
		}
		catch (NumberFormatException e)
		{
		} //not handled
		int minutes = 0;
		if( allSeconds == 60)
		{
			minutes = 1;
		}
		else
		{
			minutes = allSeconds > 60 ? allSeconds / 60 : 0;
		}
		int seconds = allSeconds % 60;
		String min = minutes > 0 ? String.valueOf(minutes) : "";
		String sec = seconds >= 10 ? String.valueOf(seconds) : seconds > 0 ? "0" + String.valueOf(seconds) : "00";
		sb.append(min + ":" + sec);
		return sb.toString();
	}
	
	public String formatMilliseconds(String inMilliseconds) 
	{
		if( inMilliseconds == null)
		{
			return null;
		}
		Double seconds = org.openedit.util.MathUtils.divide(Long.parseLong(inMilliseconds), 1000);
		return formatMinutesAndSeconds(seconds.toString());
	}
	
	public static String formatTime(String inSeconds) {
		if( inSeconds == null)
		{
			return null;
		}

		int totalSeconds = (int) Math.round(Double.parseDouble(inSeconds));

	    int hours = totalSeconds / 3600;
	    int minutes = (totalSeconds % 3600) / 60;
	    int seconds = totalSeconds % 60;

	    if (hours > 0) {
	        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
	    } else {
	        return String.format("%02d:%02d", minutes, seconds);
	    }
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
		if( searcher != null)
		{
			Data hit = (Data) searcher.searchById(inId);
			hit = searcher.loadData(hit); //not needed?
			return hit;
		}
		return null;

	}
	public Data getCachedData(WebPageRequest inReq, String inSearchType, String inId)
	{
		Data result = null;
		//TODO: Check the user permissions and check for multiedit
		if (inId.startsWith("multiedit:"))
		{
			CompositeData compositedata = (CompositeData) inReq.getSessionValue(inId);
			String hitssessionid = inId.substring("multiedit".length() + 1);
			HitTracker hits = (HitTracker) inReq.getSessionValue(hitssessionid);
			if (compositedata!= null && !compositedata.getSelectedResults().hasChanged(hits)) 
			{
				result = compositedata;
			}
			if (result == null)
			{
				if (hits == null)
				{
					log.error("Could not find " + hitssessionid);
					return null;
				}
				CompositeData composite = new BaseCompositeData(getSearcher(inSearchType), getEventManager(), hits);
				composite.setId(inId);
				result = composite;
				inReq.putSessionValue(inId, result);
			}
		}
		if (result == null)
		{
			result = (Data) getCachedData(inSearchType,inId);
		}
		return result;
	}
	
	public Data getCachedData(String inSearchType, String inId)
	{
		if (inId == null || inSearchType == null)
		{
			return null;
		}
		Searcher searcher = getSearcherManager().getExistingSearcher(getCatalogId(),inSearchType);
		if( searcher == null)
		{
			return null;
		}
		Data data = searcher.loadCachedData(inId);
		return data;
	}
	
	public HitTracker getList(String inSearchType)
	{
		return getSearcherManager().getList(getCatalogId(), inSearchType);
	}

	public HitTracker getCachedSearch(QueryBuilder inBuilder)
	{
		if( inBuilder == null)
		{
			throw new OpenEditException("Invalid search");
		}
		HitTracker tracker = inBuilder.getSearcher().getCachedSearch(inBuilder);
		return tracker;
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

	/**
	 * Use getUserProfile instead
	 * @param inId
	 * @return
	 */
	public User getUser(String inId)
	{
		//User user = getUserManager().getUserProfile(getCatalogId(), inId);
		User user = getUserManager().getUserSearcher().getUser(inId,true);
		return user;
	}

	public UserProfile getUserProfile(String inId)
	{
		UserProfile user = getUserProfileManager().getUserProfile(getCatalogId(), inId);
		//User user = getUserManager().getUserSearcher().getUser(inId,true);
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
		getCacheManager().clearAll(); //Bean based one
		getPresetManager().clearCaches();
		CacheManager shared = (CacheManager) getModuleManager().getBean("cacheManager"); //Not used anymore
		if( shared != null) //Universal one? 
		{
			shared.clearAll();
		}
		CacheManager manager = (CacheManager)getModuleManager().getBean("systemCacheManager");
		if( manager != null)
		{
			manager.clearAll();
		}
		
		CacheManager manager3 = (CacheManager)getModuleManager().getBean("systemExpireCacheManager");
		if( manager3 != null )
		{
			manager3.clearAll();
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
	
	public Collection<ContentItem> listGeneratedFiles(String  inSourcePath)
	{
		String path = "/WEB-INF/data/" + getCatalogId() + "/generated/" + inSourcePath + "/";
		Collection<ContentItem> children = new ArrayList();
		findItems(path, children);
		return children;
	}
	
	public Collection<ContentItem> listOriginalFiles(String  inSourcePath)
	{
		String path = "/WEB-INF/data/" + getCatalogId() + "/originals/" + inSourcePath;
		Collection<ContentItem> children = new ArrayList();
		ContentItem item   = getContent(path);
		if( item.isFolder())
		{
			findItems(path, children);
		}
		else if( item.exists() )
		{
			children.add(item);
		}
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
	public FolderManager getFolderManager()
	{
		FolderManager manager = (FolderManager) getModuleManager().getBean(getCatalogId(), "folderManager");
		return manager;
	}

	public void clearAll()
	{
		clearCaches();
		getPageManager().clearCache();
		getPageManager().getPageSettingsManager().clearCache();
		getSearcherManager().getPropertyDetailsArchive(getCatalogId()).clearCache();
		getSearcherManager().clear();
		getNodeManager().clear();
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

	/**
	 * I called this localQuery because I wanted to not interact with autocomplete on query()
	*/
	
	public QueryBuilder localQuery(String inSearchType)
	{
		QueryBuilder builder = getSearcher(inSearchType).query();
		builder.exact("emrecordstatus.mastereditclusterid", getNodeManager().getLocalClusterId());
		return builder;
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
						log.debug("badge not defined" + badgeid);
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
	
	public TemplateWebEmail createsystemEmail(InternetAddress email) {
		TemplateWebEmail webmail = (TemplateWebEmail) getModuleManager().getBean("templateWebEmail");//from spring
		String fromemail = getCatalogSettingValue("system_from_email");
		String fromemailname = getCatalogSettingValue("system_from_email_name");
		webmail.setRecipient(email);
		webmail.setFrom(fromemail);
		webmail.setFromName(fromemailname);
		
		return webmail;
	}

	public TemplateWebEmail createSystemEmail(User inSendTo, String inTemplatePath)
	{
		TemplateWebEmail webmail = null;
		try
		{
			InternetAddress to = new InternetAddress(inSendTo.getEmail(), inSendTo.getShortDescription());
			if (to != null) {
				webmail = createsystemEmail(to);
				webmail.setMailTemplatePath(inTemplatePath);
			}
		}
		catch (UnsupportedEncodingException e)
		{
			throw new OpenEditException(e);
		}
		return webmail;
	}
	
	
	public TemplateWebEmail createEmailFrom(String fromemail, String fromemailname, User inSendTo, String inTemplatePath)
	{
		TemplateWebEmail webmail = null;
		try
		{
			InternetAddress to = new InternetAddress(inSendTo.getEmail(), inSendTo.getShortDescription());
			if (to != null) {
				webmail =  (TemplateWebEmail) getModuleManager().getBean("templateWebEmail");
				webmail.setRecipient(to);
				webmail.setFrom(fromemail);
				webmail.setFromName(fromemailname);
				webmail.setMailTemplatePath(inTemplatePath);
			}
		}
		catch (UnsupportedEncodingException e)
		{
			throw new OpenEditException(e);
		}
		return webmail;
	}


	public TemplateWebEmail createSystemEmail(String email, String inTemplatePath)
	{
		
		User found = getUserManager().getUserByEmail(email);
		if( found != null)
		{
			return createSystemEmail(found, inTemplatePath);
		}
		
		TemplateWebEmail webmail = null;
		try
		{
			InternetAddress to = new InternetAddress(email, "");
			webmail = createsystemEmail(to);
			webmail.setMailTemplatePath(inTemplatePath);
		}
		catch (UnsupportedEncodingException e)
		{
			throw new OpenEditException(e);
		}
		return webmail;
	}
	
	public TemplateWebEmail createSystemEmailBody(User inSendTo, String inEmailBody)
	{
		TemplateWebEmail webmail = null;
		try
		{
			InternetAddress to = new InternetAddress(inSendTo.getEmail(), inSendTo.getShortDescription());
			if (to != null) {
				webmail = createsystemEmail(to);
				webmail.setMessage(inEmailBody);
			}
		}
		catch (UnsupportedEncodingException e)
		{
			throw new OpenEditException(e);
		}
		return webmail;
	}
	
	
	public TemplateWebEmail createSystemEmailBody(String email, String inEmailBody)
	{
		TemplateWebEmail webmail = null;
		try
		{
			InternetAddress to = new InternetAddress(email, "");
			if (to != null) {
				webmail = createsystemEmail(to);
				webmail.setMessage(inEmailBody);
			}
		}
		catch (UnsupportedEncodingException e)
		{
			throw new OpenEditException(e);
		}
		return webmail;
	}
	
	public TemplateWebEmail createSystemEmailBody(String email)
	{
		TemplateWebEmail webmail = null;
		try
		{
			InternetAddress to = new InternetAddress(email, "");
			if (to != null) {
				webmail = createsystemEmail(to);
			}
		}
		catch (UnsupportedEncodingException e)
		{
			throw new OpenEditException(e);
		}
		return webmail;
	}
	public void sendEmail(String inFrom, Map inParams, String inEmailto, String inTemplate)
	{
		User user = getUser(inFrom);
		sendEmail(user, inParams, inEmailto, inTemplate);		
	}

	public void sendEmail(User inFromUser, Map pageValues, String toemail, String templatePage)
	{
		RequestUtils rutil = (RequestUtils) getModuleManager().getBean("requestUtils");
		//UserProfile profile = (UserProfile) getSearcherManager().getData(getCatalogHome(),"userprofile",inFromUser.getId());
		Page template = getPageManager().getPage(templatePage);
		
		String appid = template.get("applicationid");
		
		UserProfile profile = (UserProfile) getProfileManager().loadUserProfile(this,appid,inFromUser.getId());

		BaseWebPageRequest newcontext = (BaseWebPageRequest) rutil.createVirtualPageRequest(templatePage,inFromUser,profile); 
		newcontext.putPageValues(pageValues);
		PostMail postmail = (PostMail)getModuleManager().getBean( "postMail");
		TemplateWebEmail mailer = postmail.getTemplateWebEmail();
		mailer.loadSettings(newcontext);
		mailer.setMailTemplatePath(templatePage);
		mailer.setRecipientsFromCommas(toemail);
		//mailer.setMessage(inOrder.get("sharenote"));
		mailer.send();
		log.info("email sent to :" + toemail);
	}
	

	public int getRealImageWidth(Data inHit)
	{
		if( inHit == null)
		{
			return -1;
		}
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
		if( inHit == null)
		{
			return -1;
		}

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
				
				return asLinkToPreview(asset, null, "image200x200");
			}
		}
		return null;
	}	
	
	

	/*
	 * @deprecated - Start usung asLinkToShare
	 */
	
	public String asLinkToDownload(Data inAsset, Data inPreset) {
		
		
		if (inAsset == null || inPreset == null)
		{
			return null;
		}
		String cdnprefix = getCatalogSettingValue("cdn_prefix");
		String finalroot = null;
		if (cdnprefix == null)
		{
			cdnprefix = "";
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
		
		/*
		String downloadroot = null;
		
		if(inCollectionId != null) {
			downloadroot = "/services/module/librarycollection/downloads/createpreset";
		} else {
			downloadroot = "/services/module/asset/downloads/generated";

		}
		*/
		
		String downloadroot = "/services/module/asset/downloads/generated";
		
		String sourcepath = inAsset.getSourcePath();
		
		String name = asExportFileName(inAsset, inPreset);
		String generatedfilename = inPreset.get("generatedoutputfile") + "/" + name;
		
		finalroot = cdnprefix + "/" + getMediaDbId() + downloadroot + "/" + sourcepath + "/" + generatedfilename;
		
		/*
		if(inCollectionId != null) 
		{
			finalroot = cdnprefix + "/" + getMediaDbId() + downloadroot +"/" + inCollectionId +"/" + sourcepath + "/" + generatedfilename;
		}
		*/
		
		finalroot = URLUtilities.urlEscape(finalroot);
		return finalroot;
	}

	public String asLinkToShare(String inSiteRoot, Data inAsset, Data inPreset)
	{
		if (inAsset == null || inPreset == null)
		{
			return null;
		}
		String cdnprefix = getCatalogSettingValue("cdn_prefix");

		if (cdnprefix == null)
		{
			cdnprefix = inSiteRoot;
			if( cdnprefix == null)
			{
				cdnprefix = "";
			}
		}
		
		String downloadroot = getMediaDbId() + "/services/module/asset";
		
		if (inPreset.getId().equals("0"))
		{
			String finalink = cdnprefix + "/" + downloadroot + "/downloads/originals/" + asLinkToOriginal(inAsset);
//			finalink = URLUtilities.urlEscape(finalink);
			return finalink;
		}
		
		String sourcepath = inAsset.getSourcePath();
		
		String exportedname = asExportFileName(inAsset, inPreset);
		String generatedfilename = inPreset.get("generatedoutputfile");
		String finalink = cdnprefix + "/" + downloadroot + "/generate/" + sourcepath + "/" + generatedfilename + "/" + exportedname;
		finalink = URLUtilities.urlEscape(finalink);
		return finalink;
	}
	
	public String asLinkToPreview(String inassetId, String inGeneratedoutputfile) 
	{
		Data asset = getAsset(inassetId);
		if(asset != null) {
			return asLinkToPreview(asset, inGeneratedoutputfile, false);
		}
		return null;
	}
	public String asLinkToPreview(Data inAsset, String inGeneratedoutputfile) 
	{
		
		return asLinkToPreview(inAsset, inGeneratedoutputfile, false);
	}	

	/*
	 * @Deprecated - No collectionid required anymore
	 * 
	 */
	public String asLinkToPreview(Data inAsset, String inCollectionId, String inGeneratedoutputfile)
	{
		return asLinkToPreview(inAsset, inGeneratedoutputfile, false);
	}
	
	/*
	 * @Deprecated - No collectionid required anymore
	 * 
	 */
	public String asLinkToPreview(Data inAsset, String inCollectionId, String inGeneratedoutputfile, boolean isExternalLink)
	{
		return asLinkToPreview(inAsset, inGeneratedoutputfile, isExternalLink);
	}
	
	/**
	 * This is create the path if needed
	 */
	public String asLinkToPreview(Data inAsset, String inGeneratedoutputfile, boolean isExternalLink)
	{
		if (inAsset == null)
		{
			return null;
		}
		
		String usefile = generatedOutputName(inAsset, inGeneratedoutputfile);
	
		String finalroot = null;
		
		String cdnprefix = "";
		if (isExternalLink)
		{
			cdnprefix = getCatalogSettingValue("cdn_prefix");
			if (cdnprefix == null)
			{
				cdnprefix = getCatalogSettingValue("siteroot");//
				
				if( cdnprefix == null)
				{
					cdnprefix = "";
				}
			}
		}
		String sourcepath = inAsset.getSourcePath();

		String downloadroot = null;
		
		if( inGeneratedoutputfile.endsWith("video.m3u8"))
		{
			downloadroot = "/services/module/asset/downloads/";
			//finalroot = cdnprefix + "/" + getMediaDbId() + downloadroot + "generatedpreview/" + sourcepath + "/" + inGeneratedoutputfile + "/360/" + usefile;
			finalroot = cdnprefix + "/" + getMediaDbId() + downloadroot + "generatedpreview/" + sourcepath + "/" + inGeneratedoutputfile;
		}
		else
		{
			downloadroot = "/services/module/asset/";
			String finalname = PathUtilities.extractPageName(inAsset.getName());
			finalroot = cdnprefix + "/" + getMediaDbId() + downloadroot + "generated/" + sourcepath + "/" + usefile; //+ "/" +  finalname + usefile;
			
		}
		finalroot = URLUtilities.urlEscape(finalroot);

		return finalroot;

	}
	
	public String generatedOutputName(Data inAsset, String inGeneratedoutputfile) 
	{ 
		String name = getPresetManager().generatedOutputName(this, inAsset, inGeneratedoutputfile); 
		return name;
	}

	public String asLinkToGenerated(Data inAsset, String inGeneratedoutputfile)
	{
		if (inAsset == null)
		{
			return null;
		}
		
		String usefile = generatedOutputName(inAsset, inGeneratedoutputfile);

		//String cdnprefix = getCatalogSettingValue("cdn_prefix");
		String sourcepath = inAsset.getSourcePath();

		String downloadroot = "/services/module/asset/generated/";  //Will not create anything and is fast
		String	finalroot =  "/" + getMediaDbId() + downloadroot + sourcepath + "/" + usefile;
		finalroot = URLUtilities.urlEscape(finalroot);
		return finalroot;
	}

	public String asLinkToGenerateOffset(Data inAsset, String inGeneratedoutputfile, double offset)
	{
		if (inAsset == null)
		{
			return null;
		}
		
		String usefile = generatedOutputName(inAsset, inGeneratedoutputfile);

		String filename = PathUtilities.extractPageName(usefile);
		String ext = PathUtilities.extractPageType(usefile);
		filename = filename + "offset" + offset + "." + ext;
		
		//String cdnprefix = getCatalogSettingValue("cdn_prefix");
		String sourcepath = inAsset.getSourcePath();
		String downloadroot = "/services/module/asset/generate/";  //Will not create anything and is fast
		String	finalroot =  "/" + getMediaDbId() + downloadroot + sourcepath + "/" + filename + "/" + filename;
		finalroot = URLUtilities.urlEscape(finalroot);
		return finalroot;
	}
	

	public boolean isCatalogSettingTrue(String string)
	{
		String catalogSettingValue = getCatalogSettingValue(string);
		return Boolean.parseBoolean(catalogSettingValue);
	}

	public void saveData(String inSearcher, Data inData)
	{
		Data existing = null;
		if (inData.getId() != null)
		{
			existing = (Data)getCacheManager().get("data" + inSearcher,inData.getId());
		}
		
		//getCacheManager().clear("searcher" + inSearcher); //Why?
		getSearcher(inSearcher).saveData(inData);
		if( existing != null)
		{
			getCacheManager().put("data" + inSearcher,inData.getId(),inData);
		}
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
	public UserNotifyManager getUserNotifyManager()
	{
		UserNotifyManager manager = (UserNotifyManager) getModuleManager().getBean(getCatalogId(), "userNotifyManager");
		return manager;
	}
	
	public int getCurrentYear() 
	{
		Date today = Calendar.getInstance().getTime();
		return today.getYear() + 1900;
	}
	
	public String specialValueCases(Object obj, String format) {
		switch (format) {
		case "month":
			DateFormat dateFormat  = new SimpleDateFormat("MMMM", Locale.US);
			return dateFormat.format(obj);
		default: return obj.toString();
		}
		
	}
	public String getValueText(String word, String id) {
		if (word.contains("${")) {
			String sub = word.replace("${","");
			sub = sub.replace("}","");
			String[] subs = sub.split("\\.");
			Data data = getSearcherManager().getData(getCatalogId(), subs[0], id);
			if (data != null) {
				Object result = data.getValue(subs[1]);
				if (subs.length >= 3) {
					result = specialValueCases(result, subs[2]);
				}
				return result.toString();
			}
			//return subs[1];  //lets return word 
		}
		
		return word;
	}
	public Collection<MultiValued> getValueList(PropertyDetail inDetail, MultiValued inData)
	{
		Collection<String> ids = inData.getValues(inDetail.getId());
		if(ids == null || ids.isEmpty()) 
		{
			return null;
		}
		Collection<MultiValued> results = null;
		if(ids.size() == 1)
		{
			results = new ArrayList(1);
			MultiValued res = (MultiValued) getCachedData(inDetail.getListId(), ids.iterator().next());
			if( res != null)
			{
				results.add(res);
			}
		}
		else
		{			
			results = query(inDetail.getListId()).ids(ids).search();
		}
		return results;
	}
	public String text(String text, String id)
	{
		if (text == null) {
			return "";
		}
		String[] all = text.split(" ");
		String result = "";
		for(int i =0; i < all.length; i++) {
			result += getValueText(all[i], id) + " ";
		}
		return result;
	}
	
	public Boolean isSnapshotDateOld(String dateStr) {
		try {
			String[] dateArr = dateStr.split("-");
			// Date date = new Date(Integer.parseInt(dateArr[0]), Integer.parseInt(dateArr[1]),Integer.parseInt( dateArr[2]), Integer.parseInt(dateArr[3]), Integer.parseInt(dateArr[4]));
			
			Calendar date = new GregorianCalendar();
			date.set(Calendar.YEAR, Integer.parseInt(dateArr[0]));
			date.set(Calendar.MONTH, Integer.parseInt(dateArr[1]) -1);
			date.set(Calendar.DATE, Integer.parseInt(dateArr[2]));
			date.set(Calendar.HOUR, Integer.parseInt(dateArr[3]));
			date.set(Calendar.MINUTE, Integer.parseInt(dateArr[4]));
			date.set(Calendar.SECOND, Integer.parseInt(dateArr[5]));
			
			return isSnapshotDateOld(date);
		} catch(Exception e) {
			log.error(e);
			return true;
		}
	}
	
	public Boolean isSnapshotDateOld(Calendar date) {
		Calendar warnDate = Calendar.getInstance();
		warnDate.add(Calendar.DAY_OF_YEAR, -5);	
		if (date.before(warnDate)) {
			return true;
		}
		return false;
	}
	
	public String healthColor(Data instance) {
		Date instanceSyncDate = (Date) instance.getValue("lastSyncPullDate");
		if (instanceSyncDate != null) {
			Calendar syncDate = Calendar.getInstance();
			syncDate.setTime(instanceSyncDate);
			Boolean isSyncOld = isSnapshotDateOld(syncDate);
			if (isSyncOld) {
				return "btn-danger";
			}
		}
		String clusterData = (String) instance.getValue("clusterhealth");
		if (clusterData != null && !clusterData.isEmpty()) {
			Double clusterHealth = Double.parseDouble(clusterData);;
			if (clusterHealth < 100) {
				return "btn-danger";
			}
		}
		
		return "btn-success";
	}
	
	public void clearCachedData(String inType, String inId)
	{
		getCacheManager().remove("data" + inType, inId);
	}
	
	public UserProfileManager getUserProfileManager()
	{
		return (UserProfileManager)getModuleManager().getBean(getCatalogId(),"userProfileManager",true);
	}
	
	public Asset getUserProfileAssetPortrait(Data inUser)
	{
		if( inUser == null)
		{
			return null;
		}
		Data userprofile = getUserProfile(inUser.getId());
		if( userprofile == null)
		{
			return null;
		}
		String userimageid = userprofile.get("assetportrait");
		if(userimageid == null)
		{
			userimageid = inUser.get("assetportrait");
		}
		if( userimageid != null)
		{
			return getAsset(userimageid);
		}
		
		return null;
		
	}

	public String asLinkToUserProfile(Data inUser)
	{
		Data asset = getUserProfileAssetPortrait(inUser);
		if (asset != null)
		{
			return asLinkToPreview(asset, null, "image200x200");
		}
		return null;
	}
	
	public EntityManager getEntityManager()
	{
		return (EntityManager)getModuleManager().getBean(getCatalogId(),"entityManager",true);
	}
	
	public void readMetadata(Collection<Asset> inAssets)
	{
		MetaDataReader reader = (MetaDataReader)getBean("metaDataReader");
		reader.populateAssets(this,inAssets);
		//asset.setValue("geo_point",null);
		
		for( Asset asset : inAssets)
		{
//			ContentItem content = getOriginalContent( asset );
//			contentitems.add(content);
			asset.setProperty("importstatus", "imported");
		}
		saveAssets( inAssets );
		firePathEvent("importing/assetsimported",null,inAssets);
		//log.info("saved 100 metadata readings");

	}
	
	public Map readImageSize(String imagesize) 
	{
		Map result = new HashMap();
		//image200x200.jpg
		Pattern p = Pattern.compile("(\\d+)x(\\d+)");  
		Matcher m = p.matcher(imagesize);
		while (m.find())  
		{
			result.put("w", m.group(1));
			result.put("h", m.group(2));
		}
		
		return result;
	}

	public DateStorageUtil getDateStorageUtil()
	{
		return DateStorageUtil.getStorageUtil();
	}

	public ConvertInstructions createInstructions(Asset inAsset, ContentItem inputfile)
	{
		ConvertInstructions ins = new ConvertInstructions(this);
		ins.setAsset(inAsset);
		ins.setInputFile(inputfile);
		return ins;
	}
	
	public ContentItem convertFile(ConvertInstructions instructions, ContentItem outputPage)
	{
		Asset inAsset = instructions.getAsset();
		String rendertype = instructions.getProperty("forcerendertype");
		if (rendertype == null)
		{
			rendertype = getMediaRenderType(inAsset);
		}

		ConversionManager manager = getTranscodeTools().getManagerByRenderType(rendertype);

		instructions.setOutputExtension(PathUtilities.extractPageType(outputPage.getName()));
		instructions.setOutputFile(outputPage);
		
		ConvertResult result = manager.createOutput(instructions); //skips if it already there
		
		return result.getOutput(); 
	}

	public UserProfileManager getProfileManager()
	{
		UserProfileManager manager = (UserProfileManager) getBean("userProfileManager");
		return manager;
	}

	public FaceProfileManager getFaceProfileManager()
	{
		FaceProfileManager manager = (FaceProfileManager) getBean("faceProfileManager");
		return manager;
	}
	
	public LlmConnection getLlmConnection(String inAiFunctionName)
	{
		String cacheName = "llmconnection";
		LlmConnection connection = (LlmConnection) getCacheManager().get(cacheName, inAiFunctionName);
		
		if(connection == null)
		{	
			Data aifunction = query("aifunction").id(inAiFunctionName).searchOne();
			if( aifunction == null)
			{
				log.info("Could not find AIFunction named " + inAiFunctionName + " using default");
				aifunction = query("aifunction").id("default").searchOne();
			}
			Data serverinfo = query("aiserver").exact("aifunction", aifunction.getId()).sort("ordering").searchOne();
			if( serverinfo == null)
			{
				serverinfo = query("aiserver").exact("aifunction", "default").sort("ordering").searchOne();
				if( serverinfo == null)
				{
					throw new OpenEditException("Could not find Connector for aifunction " + inAiFunctionName);
				}
			}
			String llm = serverinfo.get("connectionbean");
			connection = (LlmConnection) getModuleManager().getBean(getCatalogId(),llm, false);
			if("default".equals(aifunction.getId())) {
				// setting the name and id to the requested function name if default was used
				aifunction.setName(inAiFunctionName);
				aifunction.setId(inAiFunctionName);
			}
			connection.setAiFunctionData(aifunction);
			connection.setAiServerData(serverinfo);
			getCacheManager().put(cacheName, inAiFunctionName, connection);
			log.info(llm + " selected AI server: " + serverinfo.get("serverroot"));
		}
		
		return connection;
	}
	
	
	public boolean aiImageCreationAvailable(WebPageRequest inReq)
	{
		LlmConnection imagecreation = getLlmConnection("createImage");
		if (imagecreation != null)
		{
			if (imagecreation.getApiKey() != null)
			{
				return true;
			}
		}
		return false;
	}
	
}


