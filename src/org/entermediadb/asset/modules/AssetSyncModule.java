package org.entermediadb.asset.modules;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.EnterMedia;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.scanner.AssetImporter;
import org.entermediadb.asset.scanner.UploadQueue;
import org.entermediadb.asset.upload.FileUpload;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.ListHitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.PathUtilities;

public class AssetSyncModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(AssetSyncModule.class);
	protected FileUpload fieldFileUpload;
	protected AssetImporter fieldAssetAssetImporter;
	protected UploadQueue fieldUploadQueue; //map of assets being uploaded. clear out old oneson start, check owner, clear  on error or completed
	
	protected UploadQueue getUploadQueue()
	{
		if (fieldUploadQueue == null)
		{
			fieldUploadQueue = (UploadQueue)getModuleManager().getBean("uploadQueue");
		}

		return fieldUploadQueue;
	}

	protected void setUploadQueue(UploadQueue inUploadQueue)
	{
		fieldUploadQueue = inUploadQueue;
	}

	public AssetImporter getAssetImporter()
	{
		return fieldAssetAssetImporter;
	}

	public void setAssetImporter(AssetImporter inAssetAssetImporter)
	{
		fieldAssetAssetImporter = inAssetAssetImporter;
	}

	public FileUpload getFileUpload()
	{
		return fieldFileUpload;
	}

	public void setFileUpload(FileUpload inFileUpload)
	{
		fieldFileUpload = inFileUpload;
	}

	public void createAssetFromLocalPaths(WebPageRequest inReq) throws Exception
	{
		//TODO check for permissions
		String[] localpaths = inReq.getRequestParameters("localfilepath");
		if( localpaths != null)
		{
			String[] names = inReq.getRequestParameters("name");
			String[] parents = inReq.getRequestParameters("parentpath");
			String[] sizes = inReq.getRequestParameters("filesize");
			String[] sourcepath = inReq.getRequestParameters("sourcepath");
			String[] prefixs = inReq.getRequestParameters("uploadprefix");
			//log.info("Got: " + localpaths.length );
			// move the upload into a source path with a valid asset object
			MediaArchive archive = getMediaArchive(inReq);
			
			String assethome = "users/" + inReq.getUserName();
			Category cat =	archive.getCategoryArchive().createCategoryTree(assethome);
			cat.setName(inReq.getUser().getScreenName());	
			//call the add new asset for this user based on the path they gave us
			//redirect to the catalog userpage to track status on these assets
			//TODO: deal with folders
			String[] fields = inReq.getRequestParameters("field");

			List<String> allids = new ArrayList();
			ListHitTracker tracker = new ListHitTracker();
			for (int i = 0; i < localpaths.length; i++) 
			{
				if( localpaths[i].trim().length() == 0)
				{
					continue;
				}
				
				String currentSourcePath = sourcepath[i];
				Asset existing = archive.getAssetBySourcePath(currentSourcePath);
				Asset toadd = new Asset(archive);
				toadd.setId(archive.getAssetSearcher().nextAssetNumber());
				if (existing != null) 
				{
					String startpart = PathUtilities.extractPagePath(currentSourcePath);
					startpart = startpart + "_" + toadd.getId();
					
					currentSourcePath = startpart + "." + PathUtilities.extractPageType(currentSourcePath); 
				}

				toadd.setSourcePath(currentSourcePath);
				toadd.setProperty("localpath", localpaths[i]);
				toadd.setProperty("importstatus", "uploading");
				toadd.setProperty("previewtatus", "0");
				toadd.setProperty("editstatus", "1");
				toadd.setProperty("filesize", sizes[i]);
				toadd.setProperty("owner", inReq.getUserName());
				toadd.setProperty("datatype", "original");
				toadd.setProperty("assetaddeddate", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
				toadd.setName(names[i]);
				toadd.addCategory(cat);
				// NOOO. The file is not uploaded yet you dolt!
				// Whoops, sorry.

				if( fields != null)
				{
					for (int f = 0; f < fields.length; f++)
					{
						String val = inReq.getRequestParameter(prefixs[i] + fields[f]+ ".value");
						if( val != null)
						{
							toadd.setProperty(fields[f],val);
						}
					}
				}
				allids.add(toadd.getId());
				tracker.add(toadd);
			}
			archive.saveAssets(tracker, inReq.getUser());
			if(tracker.size() > 0)
			{
			//	archive.fireMediaEvent("assetuploadpending",inReq.getUser(),(Asset)tracker.first(),allids);
			}
			//TODO: Replace with a cached search using the assetid's
			SearchQuery query = archive.getAssetSearcher().createSearchQuery();
			query.addMatches("editstatus", "1");
			inReq.putPageValue("uploadedassets",tracker); 
			log.info("Uploaded queued " + tracker.size());
			inReq.setRequestParameter("reporttype","01newlyuploaded");
			archive.getAssetSearcher().cachedSearch(inReq, query);
		}
		
	}	

	public void searchForPendingAssets(WebPageRequest inReq) throws Exception
	{
		String applicationid = inReq.findValue("applicationid");

//		String searchcurrentcatalogonly=inReq.findValue("searchcurrentcatalogonly");
		Searcher searcher=null;
//		if (Boolean.parseBoolean(searchcurrentcatalogonly)) 
//		{
			String catalogid = inReq.findValue("catalogid");
			if( catalogid == null)
			{
				return;
			}
			searcher = getSearcherManager().getSearcher(catalogid, "asset"); 
//		} 
//		else 
//		{
//			searcher = (CompositeSearcher)getSearcherManager().getSearcher(applicationid, "compositeLucene");
//		}
		
		SearchQuery query = searcher.createSearchQuery();
		query.addMatches("importstatus", "uploading");
		query.addMatches("editstatus", "*"); //needed to override the filter
		
		User user = inReq.getUser();
		if( user!= null )
		{
			query.addMatches("owner", user.getUserName());
		}
		else
		{
			log.debug("No user");
			return;
		}
		query.setName("syncassets");
		//log.info(inReq.getUser());
		query.setFireSearchEvent(false);
		query.setResultType("search");
		query.setHitsName("pendingassets");
		
		HitTracker assets = new ListHitTracker();
		HitTracker inprogress = searcher.cachedSearch(inReq, query);

		EnterMedia entermedia = getEnterMedia(inReq);
		if( inprogress != null && inprogress.size() > 0)
		{
			for (Iterator iterator = inprogress.getPageOfHits().iterator(); iterator.hasNext();)
			{
				Data data = (Data) iterator.next();
				String localcatalogid = data.get("catalogid");
				String sourcepath = data.getSourcePath();	
				Asset existing = entermedia.getAssetBySourcePath(localcatalogid, sourcepath);
				updateUploadStatus(localcatalogid, existing);
				assets.add(existing);
			}
		}
		inReq.putPageValue("pendingassets", assets);
	}
	public void searchForPendingAsset(WebPageRequest inReq) throws Exception
	{
		String assetid = inReq.getRequestParameter("assetid");
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = archive.getAsset(assetid);
		updateUploadStatus(archive.getCatalogId(), asset);
		inReq.putPageValue("asset",asset);
	}
	protected void updateUploadStatus(String catalogid, Asset existing)
	{
		String status = existing.get("importstatus");
		if( "uploading".equals(status)) //may be moving
		{
			String path = "/WEB-INF/data/" + catalogid + "/temp/" + existing.getSourcePath();					//must match UploadQueue.java
			ContentItem tmp = getPageManager().getRepository().getStub(path);
			existing.setProperty("uploadprogress", String.valueOf(tmp.getLength()));
		}
	}

	public void receiveData(WebPageRequest inReq) throws Exception
	{
		// Why would you want to upload to the produts root directory?
		EnterMedia entermedia = getEnterMedia(inReq);
		String appletname = inReq.getRequest().getHeader("x-appletname");
		if( appletname == null)
		{
			log.error("No applet name passed in");
			return;
		}
		getUploadQueue().processUpload(inReq, getFileUpload(), appletname, entermedia);
	}
	
	

}
