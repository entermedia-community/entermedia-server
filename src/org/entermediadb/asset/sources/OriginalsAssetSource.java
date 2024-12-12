package org.entermediadb.asset.sources;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.importer.PathChangedListener;
import org.entermediadb.asset.scanner.AssetImporter;
import org.entermediadb.asset.search.AssetSearcher;
import org.entermediadb.asset.util.TimeParser;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.data.QueryBuilder;
import org.openedit.hittracker.HitTracker;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;
import org.openedit.repository.Repository;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.EmStringUtils;
import org.openedit.util.PathUtilities;

public class OriginalsAssetSource extends BaseAssetSource
{
	private static final Log log = LogFactory.getLog(OriginalsAssetSource.class);
	/* (non-Javadoc)
	 * @see org.entermediadb.asset.sources.AssetSource#getOriginalDocumentStream(org.entermediadb.asset.Asset)
	 */
	@Override
	public InputStream getOriginalDocumentStream(Asset inAsset) throws OpenEditException
	{
		ContentItem fullpath = getOriginalContent(inAsset);
		if (fullpath == null)
		{
			return null;
		}
		return fullpath.getInputStream();
	}
	public boolean isHotFolder()
	{
		return true;
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Bob#getOriginalContent(org.entermediadb.asset.Asset)
	 */
	
	/* (non-Javadoc)
	 * @see org.entermediadb.asset.sources.AssetSource#getOriginalContent(org.entermediadb.asset.Asset)
	 */
	@Override
	public ContentItem getOriginalContent(Asset inAsset)
	{
		String originalpath = "/WEB-INF/data" + getMediaArchive().getCatalogHome() + "/originals";
		String alternative = inAsset.getPath();
		originalpath = originalpath + "/" + alternative;
		String primaryname = inAsset.getPrimaryFile();
		if(primaryname != null && inAsset.isFolder() )
		{
			originalpath = originalpath + "/" + primaryname;
		}
		
		ContentItem page = getPageManager().getRepository().getStub(originalpath);
		return page;

	}

	
	
	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Bob#getPageManager()
	 */
	
	

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.Bob#setPageManager(org.openedit.page.manage.PageManager)
	 */
	
	@Override
	public boolean handles(Asset inAsset)
	{
		String name = getConfig().get("subfolder");
		if(inAsset == null) {
			return false;
		}
		if(inAsset.getSourcePath() == null) {
			return false;
		}
		if( inAsset.getSourcePath().startsWith(name))
		{
			return true;
		}
		return false;
	}

	@Override
	public boolean removeOriginal(User inUser, Asset inAsset)
	{
		ContentItem item = getOriginalContent(inAsset);
		if(inUser != null) {
			item.setAuthor(inUser.getId());
		}
		item.setMessage("deleted");
		
		if( item.exists() )
		{
			ContentItem preview = getMediaArchive().getPresetManager().outPutForGenerated(getMediaArchive(), inAsset, "image3000x3000");
			item.setPreviewImage(preview.getPath());
		}
		
		getPageManager().getRepository().remove(item);
		return true;
	}

	public Asset addNewAsset(Asset asset, List<ContentItem> temppages)
	{
		//move the pages
		String destination = "/WEB-INF/data" + getMediaArchive().getCatalogHome() + "/originals/" + asset.getSourcePath();
		ContentItem dest = getPageManager().getContent(destination);
		if(!dest.exists()){
			log.info("Could not attach file destination folder didn't exist: " + dest.getPath());
		}
		for (Iterator<ContentItem> iterator = temppages.iterator(); iterator.hasNext();)
		{
			ContentItem page = (ContentItem) iterator.next();
			if(!page.exists()){
				log.info("Could not attach file temp file doesn't exist: " + page.getPath());
			}
			getPageManager().getRepository().move(page, dest);
		}
		return asset;
	}
	public Asset replaceOriginal(Asset inAsset, List<ContentItem> inTemppages)
	{
			String destination = "/WEB-INF/data" + getMediaArchive().getCatalogHome() + "/originals/" + inAsset.getSourcePath();
			//copy the temppages in to the originals folder, but first check if this is a folder based asset
			ContentItem dest = getPageManager().getContent(destination);
				
			ContentItem page = (ContentItem) inTemppages.iterator().next();
			if(!page.exists()){
				log.info("Could not attach file temp file doesn't exist: " + page.getPath());
			}
			if( page.exists() )
			{
				ContentItem preview = getMediaArchive().getPresetManager().outPutForGenerated(getMediaArchive(), inAsset, "image3000x3000");
				page.setPreviewImage(preview.getPath());
			}

			getPageManager().getRepository().move(page, dest);
			Asset asset = getMediaArchive().getAssetBySourcePath(inAsset.getSourcePath());
			asset.setPrimaryFile(page.getName());
			ContentItem item = getOriginalContent(asset);
			getMediaArchive().removeGeneratedImages(asset,true);

			//getMediaArchive().getAssetImporter().getAssetUtilities().getMetaDataReader().updateAsset(getMediaArchive(), item, asset);
			asset.setProperty("editstatus", "1");
			asset.setProperty("importstatus", "reimported");
			asset.setProperty("previewstatus", "converting");
			getMediaArchive().saveAsset(asset, null);
			return asset;
	}



	protected ContentItem checkLocation(Asset inAsset, ContentItem inUploaded, User inUser)
	{
		ContentItem dest = getOriginalContent(inAsset);
		if(!inUploaded.getPath().equals(dest.getPath()))//move from tmp location to final location
		{
			Map props = new HashMap();
			props.put("absolutepath", dest.getAbsolutePath());
			getMediaArchive().fireMediaEvent("asset","savingoriginal",inAsset.getSourcePath(),props,inUser);
			getPageManager().getRepository().move(inUploaded, dest);
			getMediaArchive().fireMediaEvent("asset","savingoriginalcomplete",inAsset.getSourcePath(),props,inUser);
		}
		return dest;
	}

	
	class UploadedPage
	{
		protected Page inUpload;
		protected Page inDestPage;
		protected String sourcePath;
		protected Asset fieldAsset;
		protected boolean moved;
	}

	@Override
	public Asset assetOrginalSaved(Asset inAsset)
	{
		//Do nothing? The file is already in the oroiginasl folder
		return inAsset;
	}
	
		@Override
		public void detach()
		{
			String external = getConfig().get("externalpath");
			List configs = new ArrayList(getPageManager().getRepositoryManager().getRepositories());

			for (Iterator iterator = configs.iterator(); iterator.hasNext();)
			{
				Repository config = (Repository) iterator.next();
				if( config.getExternalPath().equals(external))
				{
					getPageManager().getRepositoryManager().removeRepository(config.getPath());
				}
			}
			
		}
		
		@Override
		public void refresh( ) 
		{
			MultiValued currentConfig = (MultiValued) getMediaArchive().getData("hotfolder", getConfig().getId());
			setConfig(currentConfig);
		}

		@Override
		public void saveConfig()
		{
			String toplevelfolder = getConfig().get("subfolder");
			//save subfolder with the value of the end of externalpath
			if( toplevelfolder == null )
			{
				String epath = getConfig().get("externalpath");
				if(epath!= null )
				{
					epath = epath.trim();
					epath = epath.replace('\\', '/');
					if( epath.endsWith("/"))
					{
						epath = epath.substring(0,epath.length() - 1);
					}
					toplevelfolder = PathUtilities.extractDirectoryName(epath + "/junk.html");
				}	
				if( toplevelfolder == null )
				{
					toplevelfolder = getConfig().getName();
				}	
				getConfig().setProperty("subfolder",toplevelfolder);
			}	
			getFolderSearcher().saveData(getConfig(), null);
			saveMount();
			
		}

		protected boolean skipModCheck()
		{
			long sincedate = 0;
			String since = getConfig().get("lastscanstart");
			if( since != null )
			{
				sincedate = DateStorageUtil.getStorageUtil().parseFromStorage(since).getTime();
			}
			boolean skipmodcheck = false;
			if( since != null )
			{
				long now = System.currentTimeMillis();
				String mod = getMediaArchive().getCatalogSettingValue("importing_modification_interval");
				if( mod == null)
				{
					mod = "1d";
				}
				long time = new TimeParser().parse(mod);
				long expires = sincedate + time; //once a week
				if( now < expires ) //our last time + 24 hours
				{
					skipmodcheck = true;
				}
			}
			return skipmodcheck;
		}

		
		public int importAssets(String inSubChangePath)
		{
			refresh();
			
			String base = "/WEB-INF/data/" + getMediaArchive().getCatalogId() + "/originals";
			String name = getConfig().get("subfolder");
			String path = base + "/" + name;

			AssetImporter importer = createImporter();

			Date started = new Date();
			
			boolean checkformod = skipModCheck();
			if( inSubChangePath != null )
			{
				path = path + "/" + inSubChangePath;
				checkformod = true;
			}
			
			boolean makelogs = false;
			String value = getMediaArchive().getCatalogSettingValue("show_hotfolder_status");
			if(Boolean.valueOf(value))
			{
				makelogs = true;
			}
			if( makelogs )
			{
				importer.fireHotFolderEvent(getMediaArchive(), "scan", "start", "Scanning " + path, null);
			}
			log.info(path + " scan started. mod check = " + checkformod);
			
			List<String> assetids = importer.processOn(base, path, checkformod, getMediaArchive(), null);
			if( makelogs )
			{
				importer.fireHotFolderEvent(getMediaArchive(), "scan", "finish", "Found assets: " + String.valueOf( assetids.size()), null);
			}
			getConfig().setProperty("lastscanstart", DateStorageUtil.getStorageUtil().formatForStorage(started));
			getFolderSearcher().saveData(getConfig(), null);

			long taken = ((new Date().getTime() - started.getTime())/6000L);
			log.info(getConfig() + " Imported " + assetids.size() + " assets in " + taken + " milli-seconds" );
			
			
			String monitor = getConfig().get("monitortree");
			if(Boolean.valueOf(monitor) )
			{
				initMonitor();
			}
			
			return assetids.size();
		}
		
		protected void initMonitor()
		{
			String base = "/WEB-INF/data/" + getMediaArchive().getCatalogId() + "/originals";
			String name = getConfig().get("subfolder");
			String path = base + "/" + name;
			Collection paths = getPageManager().getChildrenPaths(path, false);
			for (Iterator iterator = paths.iterator(); iterator.hasNext();)
			{
				String subdirectory = (String) iterator.next();
				ContentItem item = getPageManager().getContent(subdirectory);
				if( !item.getName().startsWith("."))
				{
					if(item.isFolder() && !getFolderMonitor().hasFolderTree(item.getAbsolutePath()))
					{
						//will scan each folder once then monitor it from now on
						getFolderMonitor().addPathChangedListener(item.getAbsolutePath(), new PathChangedListener()
						{
							@Override
							public void pathChanged(String inType, String inAbsolutePath)
							{
								String ending = inAbsolutePath.substring( item.getAbsolutePath().length() );
								importAssets(ending);
							}
						});
					}
				}
			}
			
		}

		protected AssetImporter createImporter()
		{
			AssetImporter importer = (AssetImporter)getMediaArchive().getModuleManager().getBean("assetImporter"); //Dont cache bean
			
			String excludes = getConfig().get("excludes");
			if( excludes != null )
			{
				List<String> list = EmStringUtils.split(excludes);
//				for (int i = 0; i < list.size(); i++)
//				{
//					String row = list.get(i).trim();
//					if( row.startsWith("/") &&  !row.startsWith(path))
//					{
//						row = path + row;
//					}
//					list.set(i, row);
//				}
				importer.setExcludeMatches(list);
			}
			importer.setIncludeMatches(getConfig().get("includes"));
			String attachments = getConfig().get("attachmenttrigger");
			if( attachments != null )
			{
				Collection attachmentslist = EmStringUtils.split(attachments);
				importer.setAttachmentFilters(attachmentslist);
			}
			
			return importer;
		}
	
		
		public void checkForDeleted()
		{
			MediaArchive archive = getMediaArchive();

			String base = "/WEB-INF/data/" + archive.getCatalogId() + "/originals";
			String name = getConfig().get("subfolder");
			String path = base + "/" + name ;
			List paths = getPageManager().getChildrenPaths(path);
			if( paths.size() == 0)
			{
				log.error("Found hot folder with no files, canceled delete request " + path);
				return;
			}
	
			//Making sure assets that we have in DB still exists. If not then mark as deleted
			AssetSearcher searcher = archive.getAssetSearcher();
			QueryBuilder q = searcher.query(); 
			HitTracker assets = null;
	//		String sourcepath = context.getRequestParameter("sourcepath");
	//		if(sourcepath == null)
	//		{
	//			q.all();
	//		}
	//		else
	//		{
				q.startsWith("sourcepath", name);
	//		}
			//q.not("editstatus","7").sort("sourcepathUp");
			q.sort("sourcepathUp");
			assets = q.search();
			assets.enableBulkOperations();
			int removed = 0;
			int existed = 0;	
			int modified = 0;
			List tosave = new ArrayList();
			String localClusterId = archive.getNodeManager().getLocalClusterId();
			for(Object obj: assets)
			{
				Data hit = (Data)obj;
			
				Asset asset = (Asset)searcher.loadData(hit);
				
				//verify we own this asset (cluster)
				Map emEditStatus = asset.getEmRecordStatus();
				String clusterid = (String) emEditStatus.get("mastereditclusterid");
				if (!localClusterId.equals(clusterid))
				{
					//Skip it, We do not own this asset
					continue;
				}
				
				ContentItem item = getOriginalContent(asset);
				boolean saveit = false;
				//log.info(item.getPath());
				if(!item.exists() )
				{
					removed++;
				    asset.setProperty("editstatus", "7"); //mark as deleted
				    saveit = true;
				}
				else 
				{
					existed++;
		            if("7".equals(asset.get("editstatus")))
		            {
					   asset.setProperty("editstatus", "6"); //restore files
					   saveit = true;
		            }
					//TODO: Should we have locked the asset?
					if( !asset.isEquals(item.getLastModified()))
					{
						archive.getAssetImporter().reImportAsset(archive,asset); //this saves it
						modified++;
					}
				}
				if( saveit )
				{
					tosave.add(asset);
					if( tosave.size() == 100 )
					{
						log.info("found modified: " + modified + " found deleted: " + removed + " found unmodified:" + existed );
						archive.saveAssets(tosave);
						tosave.clear();
					}
				}	
			}
			archive.saveAssets(tosave);
			tosave.clear();
			log.info("found modified: " + modified + " found deleted: " + removed + " found unmodified:" + existed );
			if( modified > 0 )
			{
				archive.fireSharedMediaEvent("conversions/runconversions");
			}
	
		}

		@Override
		public int removeExtraCategories()
		{
			MediaArchive archive = getMediaArchive();

			String base = "/WEB-INF/data/" + archive.getCatalogId() + "/originals";
			String name = getConfig().get("subfolder");
			String path = base + "/" + name ;
			List paths = getPageManager().getChildrenPaths(path);
			if( paths.size() == 0)
			{
				log.error("Found hot folder with no files, canceled delete request " + path);
				return -1;
			}
			ContentItem item = getPageManager().getContent(path);
			Category category = archive.getCategorySearcher().loadCategoryByPath(name);
			
			HitTracker entities = getMediaArchive().query("module").exact("isentity", true).search();
			Collection ids = entities.collectValues("id");
			

			
			int count = checkForExtraCategories(ids,item,category);
			return count;
		}
		protected int checkForExtraCategories(Collection entities, ContentItem inFolder, Category inParent)
		{
			HashMap names = new HashMap();
			if(inParent != null) 
			{
				for (Iterator iterator = inParent.getChildren().iterator(); iterator.hasNext();)
				{
					Category cat = (Category)iterator.next();
					names.put(cat.getName(),cat);
				}
			}
			
			List paths = getPageManager().getChildrenPaths(inFolder.getPath());
			for (Iterator iterator = paths.iterator(); iterator.hasNext();)
			{
				String subdirectory = (String) iterator.next();
				ContentItem item = getPageManager().getContent(subdirectory);
				if( item.isFolder())
				{
					Category folder = (Category)names.get(item.getName());
					names.remove(item.getName());
					if( folder != null)
					{
						checkForExtraCategories(entities,item,folder);
					}
				}
			}
			for (Iterator iterator = names.values().iterator(); iterator.hasNext();)
			{
				Category oldjunk = (Category) iterator.next();
				//Look for entities with this exact path
				QueryBuilder builder = getMediaArchive().query("modulesearch").exact("uploadsourcepath",oldjunk.getCategoryPath());
				builder.getQuery().setSearchTypes(entities);	
				
				Collection hits = builder.search();
				for (Iterator iterator2 = hits.iterator(); iterator2.hasNext();)
				{
					Data data = (Data) iterator2.next();
					String moduletype = data.get("entitysourcetype");
					log.info("Deleting missing path " + new JSONObject(data.getProperties()).toJSONString());
					getMediaArchive().getSearcher(moduletype).delete(data, null);
				}
				inParent.removeChild(oldjunk);
			}
			int deleted = names.size();
			if(deleted > 0) 
			{
				log.info("Removing: " + names.values());
				getMediaArchive().getCategorySearcher().deleteAll(names.values(), null);
			}
			return deleted;
		}

		@Override
		public void createSymbolicLink(Asset inAsset, String inCategoryPath)
		{
			ContentItem item = getOriginalContent(inAsset);
			if( !item.exists() )
			{
				Path from = Paths.get(item.getAbsolutePath());
				String target = "/WEB-INF/data" + getMediaArchive().getCatalogHome() + "/originals/" + inCategoryPath + "/" + inAsset.getName();
				Path to = Paths.get(target);
				try
				{
					if( Files.notExists(to) )
					{
						Files.createLink(from, to);
					}
				}
				catch (IOException e)
				{
					log.error("Could not make link " + to,e);
				}
				
				
			}
			
			
		}
}
