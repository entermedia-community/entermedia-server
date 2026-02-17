/*
 * Created on Oct 2, 2005
 */
package org.entermediadb.asset.scanner;

import java.io.File;
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
import org.entermediadb.asset.AssetUtilities;
import org.entermediadb.asset.BaseAsset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.fetch.UrlMetadataImporter;
import org.entermediadb.asset.search.AssetSearcher;
import org.entermediadb.asset.upload.FileUploadItem;
import org.entermediadb.asset.upload.UploadRequest;
import org.entermediadb.find.EntityManager;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.BaseData;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.Searcher;
import org.openedit.event.WebEvent;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.ListHitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.modules.translations.LanguageMap;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;
import org.openedit.util.PathUtilities;

public class AssetImporter
{
	// protected List fieldInputTypes;
	protected PageManager fieldPageManager;
	private static final Log log = LogFactory.getLog(AssetImporter.class);
	protected Boolean fieldLimitSize;
	protected AssetUtilities fieldAssetUtilities;
    protected List<UrlMetadataImporter> fieldUrlMetadataImporters;
    protected boolean fieldUseFolders = false;
//    
//	protected List fieldIncludeExtensions;
//	protected List fieldExcludeExtensions;
//	protected List fieldExcludeFolderMatch;
    
    protected List<String> fieldExcludeMatches;
    protected String fieldIncludeMatches;
    protected Collection fieldAttachmentFilters;
    
	public Collection getAttachmentFilters()
	{
		return fieldAttachmentFilters;
	}

	public void setAttachmentFilters(Collection inAttachmentFilters)
	{
		fieldAttachmentFilters = inAttachmentFilters;
	}

	public List<String> getExcludeMatches()
	{
		return fieldExcludeMatches;
	}

	public void setExcludeMatches(List<String> inExcludeFolders)
	{
		fieldExcludeMatches = inExcludeFolders;
	}

	public String getIncludeMatches()
	{
		return fieldIncludeMatches;
	}

	public void setIncludeMatches(String inIncludeFiles)
	{
		fieldIncludeMatches = inIncludeFiles;
	}


	public AssetUtilities getAssetUtilities()
	{
			return fieldAssetUtilities;
	}

	public void setAssetUtilities(AssetUtilities inAssetUtilities)
	{
		fieldAssetUtilities = inAssetUtilities;
	}

	public void processOnAll(String inRootPath, final MediaArchive inArchive, User inUser)
	{
		for (Iterator iterator = getPageManager().getChildrenPaths(inRootPath).iterator(); iterator.hasNext();)
		{
			String path = (String) iterator.next();
			Page topLevelPage = getPageManager().getPage(path);
			if (topLevelPage.isFolder() && !topLevelPage.getPath().endsWith("/CVS") && !topLevelPage.getPath().endsWith(".versions"))
			{
				processOn(inRootPath, path,false,inArchive, inUser);
			}
		}
	}
	public void reImportAsset(MediaArchive mediaArchive, Asset inAsset)
	{
		ContentItem itemFile = mediaArchive.getOriginalContent(inAsset);
		getAssetUtilities().getMetaDataReader().updateAsset(mediaArchive, itemFile, inAsset);
		inAsset.setProperty("previewstatus", "converting");
		mediaArchive.saveAsset(inAsset);
		mediaArchive.removeGeneratedImages(inAsset, true);
		
		PresetCreator presets = mediaArchive.getPresetManager();
		Searcher tasksearcher = mediaArchive.getSearcher("conversiontask");
		presets.queueConversions(mediaArchive, tasksearcher, inAsset);

	}
	
	public List<String> processOn(String inRootPath, String inStartingPoint, boolean checkformod, final MediaArchive inArchive, User inUser)
	{
		//AssetPathProcessor finder = new AssetPathProcessor();
		AssetPathProcessor finder = new CachedAssetPathProcessor();
		finder.setModificationCheck(checkformod);
		finder.setMediaArchive(inArchive);
		finder.setAssetImporter(this);
		finder.setPageManager(getPageManager());
		finder.setRootPath(inRootPath);
		finder.setAssetUtilities(getAssetUtilities());
		finder.setExcludeMatches(getExcludeMatches()); //The rest should be filtered by the mount itself
		finder.setIncludeMatches(getIncludeMatches());
		finder.setAttachmentFilters(getAttachmentFilters());
		String value = inArchive.getCatalogSettingValue("show_hotfolder_status");
		if(Boolean.valueOf(value))
		{
			finder.setShowLogs(true);
		}
		finder.processAssets(inStartingPoint, inUser);
			
		
		// Windows, for instance, has an absolute file system path limit of 256
		// characters
//		if( isOnWindows() )
//		{
//			checkPathLengths(inArchive, assets);
//		}
		return finder.assetsids;
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}


	public Asset createAssetFromExistingFile( MediaArchive inArchive, User inUser, String inSourcepath)
	{
		String catalogid = inArchive.getCatalogId();
		
		String originalspath = "/WEB-INF/data/" + catalogid + "/originals/";
		Page page = getPageManager().getPage(originalspath + inSourcepath );
		if( !page.exists() )
		{
			return null;
		}

		//String ext = PathUtilities.extractPageType(page.getName());
		return createAssetFromPage(inArchive, inUser, page);
	}
	public Asset createAssetFromPage(MediaArchive inArchive, boolean infolderbased, User inUser, Page inAssetPage, String inAssetId)
	{
		Asset asset = createAssetFromPage(inArchive,infolderbased,inUser,inAssetPage.getContentItem(),inAssetId);
		return asset;
	}
	public Asset createAssetFromPage(MediaArchive inArchive, boolean infolderbased, User inUser, ContentItem inContent, String inAssetId)
	{
		Asset asset = getAssetUtilities().createAssetIfNeeded(inContent,infolderbased, inArchive, inUser);

		if( asset == null)
		{
			//Should never call this
			String originals = "/WEB-INF/data" +  inArchive.getCatalogHome() + "/originals/";
			String sourcepath = inContent.getPath().substring(originals.length());
			asset = inArchive.getAssetBySourcePath(sourcepath);
			return asset;
		}
		if( asset.getId() == null) 
		{
			asset.setId(inAssetId);
		}
		//saveAsset(inArchive, inUser, asset);
		
		
		return asset;
	}

	public void saveAsset(MediaArchive inArchive, User inUser, Asset asset) {
		boolean existing = true;
		if( asset.get("recordmodificationdate") == null )
		{
			existing = false;
		}

		inArchive.saveAsset(asset, inUser);
		if( existing )
		{
			inArchive.fireMediaEvent("originalmodified",inUser, asset);				
		}
		else
		{
			inArchive.fireMediaEvent("assetcreated",inUser, asset);
		}
		if( "needsdownload".equals( asset.get("importstatus") ) )
		{
			inArchive.fireSharedMediaEvent("importing/fetchdownloads");
		}
		else 
		{
			inArchive.fireSharedMediaEvent("importing/assetscreated");
		}
	}

	protected Asset createAssetFromPage(MediaArchive inArchive, User inUser, Page inAssetPage)
	{
		return createAssetFromPage(inArchive, false, inUser, inAssetPage, null);
	}
	
	public List removeExpiredAssets(MediaArchive archive, String sourcepath, User inUser)
	{
		AssetSearcher searcher = archive.getAssetSearcher();
		SearchQuery q = searcher.createSearchQuery();
		HitTracker assets = null;
		if(sourcepath == null)
		{
			assets = searcher.getAllHits();
		}
		else
		{
			q.addStartsWith("sourcepath", sourcepath);
			assets = searcher.search(q);
		}
		List<String> removed = new ArrayList<String>();
		List<String> sourcepaths= new ArrayList<String>();
		
		for(Object obj: assets)
		{
			Data hit = (Data)obj;
			sourcepaths.add(hit.get("sourcepath")); //TODO: Move to using page of hits
			if( sourcepaths.size() > 250000)
			{
				log.error("Should not load up so many paths");
				break;
			}
		}
		for(String path: sourcepaths)
		{
			Asset asset = archive.getAssetBySourcePath(path);
			if( asset == null)
			{
				continue;
			}
			String assetsource = asset.getSourcePath();
			String pathToOriginal = "/WEB-INF/data" + archive.getCatalogHome() + "/originals/" + assetsource;
			if(asset.isFolder() && asset.getPrimaryFile() != null)
			{
				pathToOriginal = pathToOriginal + "/" + asset.getPrimaryFile();
			}
			Page page = getPageManager().getPage(pathToOriginal);
			if(!page.exists())
			{
				removed.add(asset.getSourcePath());
				archive.removeGeneratedImages(asset);
				archive.getAssetSearcher().delete(asset, inUser);
			}
		}
		return removed;
	}

	public Asset createAssetFromFetchUrl(MediaArchive inArchive, String inUrl, User inUser, String inSourcePath, String inFileName, String inId)
	{
		for(UrlMetadataImporter importer: getUrlMetadataImporters())
		{
			Asset asset = importer.importFromUrl(inArchive, inUrl, inUser,  inSourcePath,  inFileName, inId);
			if( asset != null )
			{
				return asset;
			}
		}
		return null;
	}
		
	public List<UrlMetadataImporter> getUrlMetadataImporters()
	{
		if(fieldUrlMetadataImporters == null)
		{
			return new ArrayList<UrlMetadataImporter>();
		}
		return fieldUrlMetadataImporters;
	}

	public void setUrlMetadataImporters(List<UrlMetadataImporter> urlMetadataImporters)
	{
		fieldUrlMetadataImporters = urlMetadataImporters;
	}

	public void fetchMediaForAsset(MediaArchive inArchive, Asset inAsset, User inUser)
	{
			for(UrlMetadataImporter importer: getUrlMetadataImporters())
			{
				importer.fetchMediaForAsset(inArchive, inAsset,inUser);
			}
	}
	
	/**
	 * For this to work, inSourcePath needs to have an extention, i.e.
	 * newassets/admin/118/picture.jpg
	 * @param inStructions
	 * @param inSourcePath
	 * @return
	 */
	public Asset createAsset(MediaArchive inArchive, String inSourcePath)
	{
		String extension = PathUtilities.extractPageType(inSourcePath);
		if (extension != null)
		{
			Asset asset = new BaseAsset(inArchive); //throw away
			//asset.setCatalogId(inArchive.getCatalogId());
	//		asset.setId(inArchive.getAssetArchive().nextAssetNumber());
			asset.setSourcePath(inSourcePath);
			extension = extension.toLowerCase();
			asset.setProperty("fileformat", extension);
	//		inArchive.saveAsset(asset, null);
			return asset;
		}
		return null;
	}

	public void fireHotFolderEvent(MediaArchive inArchive, String operation, String inFunctionType, String inLog, User inUser)
	{
			WebEvent event = new WebEvent();
			event.setOperation(operation);
			event.setSearchType("hotfolder");
			event.setCatalogId(inArchive.getCatalogId());
			event.setUser(inUser);
			event.setSource(this);
			event.setProperty("functiontype", inFunctionType);
			event.setProperty("log", inLog);
			inArchive.getEventManager().fireEvent(event);
	}
	

	public Map<String, ContentItem> savePages(WebPageRequest inReq, MediaArchive inArchive,UploadRequest inUploadRequest)
	{
		//if we are uploading into a collection?
		Boolean incollection = inReq.findValue("currentcollection") != null;

		String inputsourcepath = inReq.findValue("sourcepath");
		
		Map pages = new HashMap();
		for (Iterator iterator = inUploadRequest.getSavedContentItems().iterator(); iterator.hasNext();)
		{
			ContentItem contentitem = (ContentItem) iterator.next();

			String filename = contentitem.getName();
			if (filename.startsWith("tmp") && filename.indexOf('_') > -1)
			{
				filename = filename.substring(filename.indexOf('_') + 1);
			}
			String rootpath = inUploadRequest.getRootPath(contentitem.getPath());
			String filepath = contentitem.getPath().substring(rootpath.length());					

			String assetsourcepath = null;
			String basepath = "/WEB-INF/data/" + inArchive.getCatalogId() + "/originals/";
			if (inputsourcepath == null)
			{
				assetsourcepath = inArchive.getAssetImporter().getAssetUtilities().createSourcePath(inReq, inArchive, filepath);
				
				if (assetsourcepath.endsWith("/"))
				{
					assetsourcepath = assetsourcepath + filepath;  
				}
			}
			else if (inputsourcepath.endsWith("/")) //EMBridge expects the filename to be added on
			{
				assetsourcepath = inputsourcepath + filepath;
			}
			else
			{
				assetsourcepath = inputsourcepath;
			}
			
			
			
			//Create Entity, should be here? Test again
			if( Boolean.parseBoolean(inReq.getRequestParameter("createentity")))
			{
				String archivesourcepath = inReq.getRequestParameter("archivesourcepath");
				String entitytype = inReq.getRequestParameter("entitytype");
				Searcher s = inArchive.getSearcher(entitytype );
				Data newone = s.createNewData();
				newone.setName(PathUtilities.extractPageName(filepath));
				newone.setValue("archivesourcepath", archivesourcepath+'/'+filepath);
				s.saveData(newone);
			}
			
			//Disable unzip zip files
			if (false && incollection && filename.toLowerCase().endsWith(".zip"))
			{
				try
				{
					String directory = PathUtilities.extractDirectoryPath(contentitem.getPath());

					Page unzipfolder = getPageManager().getPage(directory + "unzip/");
					File folder = new File(unzipfolder.getContentItem().getAbsolutePath());
					folder.mkdirs();
					String collectionfolder = PathUtilities.extractDirectoryPath(assetsourcepath);
					Collection files = getPageManager().getZipUtil().unzip(contentitem.getInputStream(), folder);
					for (Iterator iterator2 = files.iterator(); iterator2.hasNext();)
					{
						File one = (File) iterator2.next();
						String ending = one.getAbsolutePath().substring(folder.getAbsolutePath().length()).replace("\\", "/");
						Page upload = getPageManager().getPage(unzipfolder + ending);

						//Change source paths for each file and subfolders
						pages.put(collectionfolder + ending, upload);
						//This will replace the assets
					}
				}
				catch (Exception ex)
				{
					log.error("Could not unzip : " + filename, ex);
				}
			}
			else
			{
				ContentItem dest = getPageManager().getContent(basepath + assetsourcepath);
//				int i = 2;
//				while (dest.exists())
//				{
//					String pagename = PathUtilities.extractPageName(assetsourcepath);
//					String tmppath = assetsourcepath.replace(pagename, pagename + "_" + i);
//					dest = getPageManager().getContent(basepath + tmppath);
//					if (!dest.exists())
//					{
//						assetsourcepath = tmppath;
//						break;
//					}
//					i++;
//				}
				pages.put(assetsourcepath, contentitem);
			}
		}
		return pages;
	}
	protected void findUploadTeam(WebPageRequest inReq, MediaArchive archive, ListHitTracker tracker)
	{
		String groupid = inReq.getRequestParameter("viewgroup");
		if (groupid != null)
		{
			for (Iterator iterator = tracker.iterator(); iterator.hasNext();)
			{
				Asset asset = (Asset) iterator.next();
				asset.setProperty("uploadteam", groupid);
				archive.getAssetSecurityArchive().grantGroupViewAccess(archive, groupid, asset);
			}

		}
	}

	public void createAssetsFromPages(MediaArchive inArchive, UploadRequest inUploadRequest, WebPageRequest inReq)
	{
		//final boolean createCategories = Boolean.parseBoolean( inReq.findValue("assetcreateuploadcategories"));

		final Map metadata = readMetaData(inReq, inArchive, "");
		final String currentcollection = (String) metadata.get("collectionid");

		boolean assigncategory =  true;
		
		String inputsourcepath = inReq.findValue("sourcepath");
		if( inputsourcepath != null && Boolean.parseBoolean(inReq.getRequestParameter("createentityfolder")))
		{
			FileUploadItem item = inUploadRequest.getFirstItem();
			String root = PathUtilities.extractRootDirectory(item.getName());
			Category topcat = inArchive.createCategoryPath(inputsourcepath + root);
			String entitytype = inReq.getRequestParameter("entitytype");
			String entityid = inReq.getRequestParameter("selected"  + entitytype);
			Collection vals = topcat.getValues(entitytype);
			if(vals == null || !vals.contains(entityid))
			{
				topcat.addValue(entitytype, entityid);
				inArchive.saveData("category", topcat);
			}
			metadata.put("field","category");
			metadata.put("category.value", topcat.getId());
			assigncategory = true;
		}
		if( inputsourcepath != null && Boolean.parseBoolean(inReq.getRequestParameter("createentityparent")))
		{
			FileUploadItem item = inUploadRequest.getFirstItem();
			Category topcat = inArchive.createCategoryPath(inputsourcepath);
			String entitytype = inReq.getRequestParameter("entitytype");
			String entityid = inReq.getRequestParameter("selected"  + entitytype);
			Collection vals = topcat.getValues(entitytype);
			if(vals == null || !vals.contains(entityid))
			{
				topcat.addValue(entitytype, entityid);
				inArchive.saveData("category", topcat);
			}
			metadata.put("field","category");
			metadata.put("category.value", topcat.getId());
			assigncategory = true;
		}

		
		final Map<String, ContentItem> pages = savePages(inReq, inArchive, inUploadRequest);
		final User user = inReq.getUser();
		inArchive.getAssetSearcher();

		//findUploadTeam(inReq, archive, tracker); TODO:Do this is assetsimportedcustom
		if (pages.size() == 0)
		{
			log.error("No pages uploaded");
			return;
		}
		

//		String threaded = inReq.findValue("threadedupload");
//		if (Boolean.valueOf(threaded) )
//		{
//			ExecutorManager manager = (ExecutorManager) getModuleManager().getBean(archive.getCatalogId(), "executorManager");
//
//			Runnable runthis = new Runnable()
//			{
//				public void run()
//				{
//					saveFilesAndImport(archive, currentcollection, assigncategory, metadata, pages, user);
//				}
//			};
//			manager.execute("importing", runthis);
//		}
//		else
//		{
			//The uploader sends one at a time anyways
			Collection tracker = saveFilesAndImport(inArchive, currentcollection, assigncategory, metadata, pages, user);
			inReq.putPageValue("assets", tracker);
//		}
		if( currentcollection != null && inReq.getUserProfile() != null)
		{
			inReq.getUserProfile().setProperty("lastselectedcollection", currentcollection);
		}

		
		//Update Primary Images in Collections and Entities
		
		//Nope, use the normal assignment tool
		
		//EntityManager entityManager = (EntityManager) inArchive.getEntityManager();
		//entityManager.updateCollection(tracker, currentcollection, user);
		//entityManager.updateEntities(tracker, metadata, user);
		//inArchive.fireSharedMediaEvent("importing/assetscreated");
	}
	
	
	public HitTracker saveFilesAndImport(final MediaArchive archive, final String currentcollection, final boolean createCategories, final Map metadata, final Map pages, final User user)
	{
		HitTracker tracker = archive.getAssetManager().saveFilesAndImport(currentcollection, createCategories, metadata, pages, user);
		return tracker;
	}

	

	public Map readMetaData(WebPageRequest inReq, MediaArchive archive, String prefix)
	{
		String[] fields = inReq.getRequestParameters("field");
		Map vals = new HashMap();

		String[] categories = inReq.getRequestParameters(prefix + "categoryid");
		List cats = new ArrayList();
		if (categories != null)
		{
			for (int i = 0; i < categories.length; i++)
			{
				Category cat = archive.getCategory(categories[i]);
				if (cat != null)
				{
					cats.add(cat);
				}
			}
		}

		String catlist = inReq.getRequestParameter(prefix + "category.values");
		if (catlist != null)
		{
			categories = catlist.split("\\s");
			for (int i = 0; i < categories.length; i++)
			{
				Category cat = archive.getCategory(categories[i]);
				if (cat != null)
				{
					cats.add(cat);
				}
			}
		}

		//This is old dont use
		categories = inReq.getRequestParameters(prefix + "categories");
		if (categories != null)
		{
			for (int i = 0; i < categories.length; i++)
			{
				Category cat = archive.getCategory(categories[i]);
				if (cat != null)
				{
					cats.add(cat);
				}
			}
		}

		categories = inReq.getRequestParameters(prefix + "category.value");
		if (categories != null)
		{
			for (int i = 0; i < categories.length; i++)
			{
				Category cat = archive.getCategory(categories[i]);
				if (cat != null)
				{
					cats.add(cat);
				}
			}
		}
		vals.put("categories", cats);
		
		
		if (fields != null)
		{
			for (int i = 0; i < fields.length; i++)
			{
				String afield = fields[i];
				if( afield.equals("category"))
				{
					//already handled above
					continue;
				}
				Object val = inReq.getRequestParameter(prefix + afield + "value");
				if (val == null)
				{
					val = inReq.getRequestParameter(prefix + afield + ".value");
				}
				if (val == null)
				{
					String[] array = inReq.getRequestParameters(prefix + afield + ".values");
					if (array == null) {
						array = inReq.getRequestParameters(prefix + afield + ".values");
					}
					if (array != null)
					{
						val = Arrays.asList(array);
					}
				}
				String[] language = inReq.getRequestParameters(prefix + afield + ".language");
				if (language != null)
				{
					LanguageMap lmap = new LanguageMap();
					for (int j = 0; j < language.length; j++)
					{
						String lang = language[j];
						String langval = inReq.getRequestParameter(prefix + afield + "." + lang + ".value");
						if (langval == null)
						{
							langval = inReq.getRequestParameter(prefix + afield + "." + lang); //legacy
						}
						if (langval == null)
						{
							langval = inReq.getRequestParameter(prefix + afield + ".language." + (j + 1)); //legacy
						}
						if (langval != null)
						{
							lmap.setText(lang, langval);
						}
					}
					val = lmap;
				}

				if (val != null)
				{
					if (val instanceof Collection && ((Collection) val).isEmpty())
					{
						continue;
					}
					vals.put(afield, val);
				}
			}
		}
		String collectionid = inReq.getRequestParameter("collectionid");
		if (collectionid == null)
		{
			collectionid = inReq.getRequestParameter("currentcollection");
		}
		if (collectionid == null)
		{
			collectionid = inReq.getRequestParameter("currentcollection.value");
		}

		if (collectionid != null && collectionid.trim().length() == 0)
		{
			collectionid = null;
		}
		//Deal with library.value and create new collection
		if (collectionid == null)
		{
			String newcollection = inReq.getRequestParameter("newcollection");
			if (newcollection != null)
			{
				Searcher librarycollectionsearcher = archive.getSearcher("librarycollection");
				Data collection = librarycollectionsearcher.createNewData();
				collection.setName(newcollection);
				String libraryid = inReq.getRequestParameter("library.value");
				if (libraryid == null)
				{
					libraryid = inReq.getUser().getId();
				}
				collection.setProperty("library", libraryid);
				collection.setProperty("owner", inReq.getUser().getId());
				collection.setValue("creationdate", new Date());
				librarycollectionsearcher.saveData(collection, null);
				collectionid = collection.getId();

				String libraries = inReq.getRequestParameter("libraries.value");
				if (libraries == null)
				{
					vals.put("libraries", libraryid);
					/*
					 * Not needed
					 * 
					 * 
					 * When we uploads we can just set sourcepath. Use Archiving
					 * to move to a library. just add a category based on what
					 * the user picked for collection
					 * 
					 * For imports just set categories like we already do
					 * 
					 * Change Collections to be normal categories path s and
					 * make createTree look at the folderpath not the ID's so we
					 * can use weird ID's
					 * 
					 */

					Data library = archive.getData("library", libraryid);
					inReq.setRequestParameter("category.value", library.get("categoryid"));
					inReq.setRequestParameter("libraries.value", libraryid);
				}
				inReq.setRequestParameter("currentcollection", collectionid);
			}
		}
		if (collectionid != null)
		{
			vals.put("collectionid", collectionid);
		}

		return vals;
	}

	public Page getAssetsPage(MediaArchive inArchive, String inSourcePath)
	{

		String prefix = inArchive.getCatalogHome() + "/assets/";
		String path = prefix + inSourcePath;
		Page page = getPageManager().getPage(path);
		return page;
	}


	public void importAndSearch(WebPageRequest inReq, MediaArchive inArchive, String mountpath, String assetRoot)
	{
		List<String> created = processOn(assetRoot, assetRoot, false, inArchive, inReq.getUser());

		SearchQuery search = inArchive.getAssetSearcher().createSearchQuery();
		int max = Math.min(10000, created.size());
		for (int i = 0; i < max; i++)
		{
			search.addMatches("id", created.get(i));
		}
		HitTracker lht = inArchive.getAssetSearcher().cachedSearch(inReq, search);
		lht.getSearchQuery().setResultType("search");
		lht.getSearchQuery().setHitsName("hits");
		inReq.putPageValue(lht.getHitsName(), lht);
		inReq.putSessionValue(lht.getSessionId(), lht);
		inReq.putPageValue("catalogid", inArchive.getCatalogId());
		inReq.putPageValue("numrecords", new Integer(created.size()));

	}
	
	public Data buildDataObject(WebPageRequest inReq, PropertyDetails inDetails)
	{
		Data data = new BaseData();
		for (Iterator i = inDetails.iterator(); i.hasNext();)
		{
			PropertyDetail detail = (PropertyDetail) i.next();
			String value = inReq.getRequestParameter(detail.getId());
			if (value != null)
			{
				data.setProperty(detail.getId(), value);
			}
		}
		return data;
	}

	
}