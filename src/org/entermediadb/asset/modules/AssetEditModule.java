package org.entermediadb.asset.modules;

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
import org.entermediadb.asset.Category;
import org.entermediadb.asset.CompositeAsset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.attachments.AttachmentManager;
import org.entermediadb.asset.convert.ConversionManager;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.entermediadb.asset.edit.AssetEditor;
import org.entermediadb.asset.scanner.AssetImporter;
import org.entermediadb.asset.scanner.ExiftoolMetadataExtractor;
import org.entermediadb.asset.search.AssetSearcher;
import org.entermediadb.asset.upload.FileUpload;
import org.entermediadb.asset.upload.FileUploadItem;
import org.entermediadb.asset.upload.UploadRequest;
import org.entermediadb.asset.xmp.XmpWriter;
import org.entermediadb.projects.ProjectManager;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.WebServer;
import org.openedit.data.BaseData;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.ListHitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.modules.translations.LanguageMap;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;
import org.openedit.repository.Repository;
import org.openedit.repository.filesystem.FileRepository;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.ExecutorManager;
import org.openedit.util.PathUtilities;

public class AssetEditModule extends BaseMediaModule
{

	protected WebServer fieldWebServer;
	protected static final String CATEGORYID = "categoryid";
	protected FileUpload fieldFileUpload;

	public AttachmentManager getAttachmentManager()
	{
		return fieldAttachmentManager;
	}

	public void setAttachmentManager(AttachmentManager fieldAttachmentManager)
	{
		this.fieldAttachmentManager = fieldAttachmentManager;
	}

	protected AssetImporter fieldAssetAssetImporter;
	protected AttachmentManager fieldAttachmentManager;

	private static final Log log = LogFactory.getLog(AssetEditModule.class);

	public List<ContentItem> getUploadedPages(WebPageRequest inReq)
	{
		List contentitems = new ArrayList();

		List unzipped = (List) inReq.getPageValue("unzippedfiles");
		if (unzipped != null && unzipped.size() > 0)
		{
			for (Iterator iterator = unzipped.iterator(); iterator.hasNext();)
			{
				Page page = (Page) iterator.next();
				contentitems.add(page.getContentItem());
			}
		}
		else
		{
			UploadRequest map = (UploadRequest) inReq.getPageValue("uploadrequest");
			if (map != null)
			{
				List uploadItems = map.getUploadItems();
				if (uploadItems != null)
				{
					for (Iterator iterator = uploadItems.iterator(); iterator.hasNext();)
					{
						FileUploadItem uploadItem = (FileUploadItem) iterator.next();
						Page uploaded = uploadItem.getSavedPage();
						contentitems.add(uploaded.getContentItem());
					}
				}
			}
		}

		return contentitems;
	}

	public boolean makeFolderAsset(WebPageRequest inReq) throws Exception
	{
		Asset asset = getAsset(inReq);
		if (asset == null)
		{
			return false;
		}
		if (asset.isFolder())
		{
			return true;
		}
		else
		{
			getAssetEditor(inReq).makeFolderAsset(asset, inReq.getUser());
			getAttachmentManager().syncAttachments(inReq, getMediaArchive(inReq), asset, true);
			return true;
		}
	}

	public void writeXmpData(WebPageRequest inReq) throws Exception
	{
		//XmpWriter writer = (XmpWriter) getModuleManager().getBean("xmpWriter");
		String assetid = inReq.getRequestParameter("assetid");
		if (assetid == null)
		{
			assetid = inReq.getRequestParameter("id");
		}
		MediaArchive mediaArchive = getMediaArchive(inReq);
		Asset asset = mediaArchive.getAsset(assetid);
		if (asset == null)
		{
			//log
			return;
		}
		boolean didSave = false;
		if (mediaArchive.isTagSync(asset.getFileFormat()))
		{
			didSave = getXmpWriter().saveMetadata(mediaArchive, asset);
		}

		inReq.putPageValue("didSave", new Boolean(didSave));
	}

	public AssetEditor getAssetEditor(WebPageRequest inContext) throws OpenEditException
	{
		MediaArchive mediaarchive = getMediaArchive(inContext);
		AssetEditor editor = (AssetEditor) inContext.getSessionValue("AssetEditor" + mediaarchive.getCatalogId());
		if (editor == null)
		{
			editor = (AssetEditor) getModuleManager().getBean("assetEditor");
			editor.setMediaArchive(mediaarchive);

			inContext.putSessionValue("AssetEditor" + mediaarchive.getCatalogId(), editor);
		}
		inContext.putPageValue("AssetEditor", editor);

		return editor;
	}

	/*
	 * public void copyAsset(WebPageRequest inContext) throws OpenEditException
	 * { Asset asset = getAsset(inContext);
	 * 
	 * String originalsourcepath = asset.getSourcePath(); AssetEditor editor =
	 * getAssetEditor(inContext);
	 * 
	 * String targetName = inContext.getRequestParameter("name"); String
	 * newSourcePath; String sourceDirectory =
	 * inContext.findValue("defaultsourcedirectory"); if(sourceDirectory ==
	 * null) { sourceDirectory = PathUtilities
	 * .extractDirectoryPath(originalsourcepath); if
	 * (originalsourcepath.endsWith("/")) { sourceDirectory = PathUtilities
	 * .extractDirectoryPath(sourceDirectory); } } if
	 * (sourceDirectory.endsWith("/")) { sourceDirectory =
	 * sourceDirectory.substring(0, sourceDirectory.length() - 2); }
	 * 
	 * boolean createAsFolder =
	 * Boolean.parseBoolean(inContext.findValue("createasfolder"));
	 * 
	 * if(targetName != null) // Is this really used? Seems wrong somehow... {
	 * newSourcePath = sourceDirectory + targetName + "/"; } else { if
	 * (createAsFolder) { newSourcePath = sourceDirectory + "/" + newId; } else
	 * { newSourcePath = sourceDirectory + "/" +newId + ".data"; } }
	 * if(newSourcePath.startsWith("/")) { newSourcePath =
	 * newSourcePath.substring(1); }
	 * 
	 * if (newSourcePath.equals(originalsourcepath)) { return; //can't copy to
	 * itself }
	 * 
	 * Asset newasset = editor.copyAsset(asset, null, newSourcePath);
	 * 
	 * newasset.setFolder(createAsFolder);
	 * 
	 * //Copy any images or folders using OE File Manager String newpath =
	 * "/WEB-INF/data/" + editor.getMediaArchive().getCatalogId() +
	 * "/originals/" + newSourcePath + "/"; String oldpath = "/WEB-INF/data/" +
	 * editor.getMediaArchive().getCatalogId() + "/originals/" +
	 * originalsourcepath + "/";
	 * 
	 * Page newpage = getPageManager().getPage(newpath); Page oldpage =
	 * getPageManager().getPage(oldpath);
	 * 
	 * //Check for flag indicating that the image should not be copied boolean
	 * copyimage = Boolean.parseBoolean(inContext.findValue("copyimage")); if(
	 * !copyimage ) { //Remove the image reference from the xconf
	 * newasset.removeProperties(new String[] { "originalfile", "primaryfile",
	 * "fileformat", "imagefile"}); //create a blank directory
	 * getPageManager().putPage(newpage); } else { //copy the original assets
	 * directory (including the image) getPageManager().copyPage(oldpage,
	 * newpage); }
	 * 
	 * newasset.setName(targetName);
	 * 
	 * Collection categories = asset.getCategories(); for (Iterator iter =
	 * categories.iterator(); iter.hasNext();) { Category element = (Category)
	 * iter.next(); newasset.addCategory(element); }
	 * 
	 * Page oldPage = getPageManager().getPage(
	 * editor.getMediaArchive().getCatalogHome() + "/assets/" +
	 * asset.getSourcePath() + ".html"); if (oldPage.exists()) { Page newPage =
	 * getPageManager().getPage( editor.getMediaArchive().getCatalogHome() +
	 * "/assets/" + newasset.getSourcePath() + ".html"); try {
	 * getPageManager().copyPage(oldPage, newPage); } catch (RepositoryException
	 * re) { throw new OpenEditException(re); } }
	 * 
	 * // Remove the PDF text newasset.removeProperty("fulltext");
	 * editor.getMediaArchive().saveAsset(newasset, inContext.getUser());
	 * inContext.setRequestParameter("assetid", newasset.getId()); if(
	 * inContext.getRequestParameters("field") != null) {
	 * saveAssetProperties(inContext); }
	 * inContext.setRequestParameter("assetid", asset.getId());
	 * inContext.setRequestParameter("targetsourcepath",
	 * newasset.getSourcePath()); inContext.setRequestParameter("newassetid",
	 * newasset.getId()); inContext.putPageValue("target", newasset);
	 * 
	 * //copyJoinData(asset, newasset); }
	 */
	/*
	 * protected void copyJoinData(Asset source, Asset target) { PropertyDetails
	 * properties =
	 * getMediaArchive(source.getCatalogId()).getAssetPropertyDetails(); List
	 * lists = properties.getDetailsByProperty("type", "textjoin");
	 * lists.addAll(properties.getDetailsByProperty("type", "datejoin"));
	 * HashSet processed = new HashSet(); for (Iterator iterator =
	 * lists.iterator(); iterator.hasNext();) { PropertyDetail detail =
	 * (PropertyDetail) iterator.next(); String detailid = detail.getId(); if
	 * (detailid.indexOf(".") > 0) { detailid = detailid.split("\\.")[0]; } if
	 * (processed.contains(detailid)) { continue; } else {
	 * processed.add(detailid); }
	 * 
	 * FilteredTracker tracker = new FilteredTracker();
	 * tracker.setSearcher(getSearcherManager().getSearcher(detail.getCatalogId(
	 * ), detailid)); tracker.filter("assetid", source.getId()); HitTracker hits
	 * = tracker.filtered();
	 * 
	 * Searcher targetSearcher =
	 * getSearcherManager().getSearcher(target.getCatalogId(), detailid); if
	 * (targetSearcher != null && hits != null && hits.size() > 0) { List data =
	 * new ArrayList(); for (Iterator iterator2 = hits.iterator();
	 * iterator2.hasNext();) { Data sourcedata = (Data)iterator2.next();
	 * ElementData item = null; if(sourcedata instanceof DocumentData){ item =
	 * (ElementData) targetSearcher.searchById(sourcedata.getId()); } else{ item
	 * = (ElementData) sourcedata; } if(item == null){ continue; }
	 * 
	 * Data newItem = targetSearcher.createNewData();
	 * newItem.setSourcePath(target.getSourcePath()); for (Iterator iterator3 =
	 * item.getElement().attributes().iterator(); iterator3.hasNext();) {
	 * Attribute property = (Attribute) iterator3.next(); if
	 * (property.getName().equals("assetid")) { newItem.setProperty("assetid",
	 * target.getId()); } else if (!property.getName().equals("id")) {
	 * newItem.setProperty(property.getName(), property.getValue()); }
	 * 
	 * }
	 * 
	 * data.add(newItem); } targetSearcher.saveAllData(data, null); } } }
	 */
	public void saveAssetResultsEdits(WebPageRequest inRequest) throws OpenEditException
	{
		MediaArchive store = getMediaArchive(inRequest);
		String[] fields = inRequest.getRequestParameters("editfield");
		if (fields != null)
		{
			for (int i = 0; i < fields.length; i++)
			{
				String key = fields[i];
				String assetid = key.substring(0, key.indexOf("."));
				String fieldid = key.substring(key.indexOf(".") + 1);
				String value = inRequest.getRequestParameter(key);
				Asset asset = store.getAsset(assetid);
				if (asset == null)
				{
					throw new OpenEditException("Asset is not found " + key);
				}
				String oldvalue = asset.get(key);
				asset.setProperty(fieldid, value);
				// null check
				if (value != null && !value.equals(oldvalue))
				{
					store.getAssetSearcher().saveData(asset, inRequest.getUser());
				}
				else if (oldvalue != null && !oldvalue.equals(value))
				{
					store.getAssetSearcher().saveData(asset, inRequest.getUser());
				}
			}
		}
	}

	/**
	 * Removes generated images (medium, thumbs, etc) for a asset.
	 * 
	 * @param inRequest
	 *            The web request. Needs a <code>assetid</code> or
	 *            <code>sourcePath</code> request parameter.
	 */
	public void removeAssetImages(WebPageRequest inRequest)
	{
		Asset asset = getAsset(inRequest);
		MediaArchive archive = getMediaArchive(inRequest);

		archive.removeGeneratedImages(asset, false);
	}
	//	public Data createMultiEditData(WebPageRequest inReq) throws Exception
	//	{
	//		String hitsname = inReq.getRequestParameter("multihitsname");//expects session id
	//		if( hitsname == null)
	//		{
	//			return null;
	//		}
	//		MediaArchive store = getMediaArchive(inReq);
	//		HitTracker hits = (HitTracker) inReq.getSessionValue(hitsname);
	//		if( hits == null)
	//		{
	//			log.error("Could not find " + hitsname);
	//			return null;
	//		}
	//		CompositeAsset composite = new CompositeAsset();
	//		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
	//		{
	//			Data target = (Data) iterator.next();
	//			Asset p = null;
	//			if( target instanceof Asset)
	//			{
	//				p = (Asset)target;
	//			}
	//			else
	//			{
	//				p = store.getAssetBySourcePath(target.getSourcePath());
	//			}
	//			if( p != null)
	//			{
	//				composite.addData(p);
	//			}
	//		}
	//		composite.setId("multiedit:"+hitsname);
	//		//set request param?
	//		inReq.setRequestParameter("assetid",composite.getId());
	//		inReq.putPageValue("data", composite);
	//		inReq.putPageValue("asset", composite);
	//		inReq.putSessionValue(composite.getId(), composite);
	//		
	//		return composite;
	//	}

	/*
	 * public void selectAsset(WebPageRequest inContext) throws
	 * OpenEditException { AssetEditor editor = getAssetEditor(inContext);
	 * Object assetPageValue = inContext.getPageValue("asset"); Asset asset =
	 * null; if (assetPageValue != null && assetPageValue instanceof Asset) //it
	 * may be a CompositeAsset, which would throw an exception { asset = (Asset)
	 * assetPageValue; }
	 * 
	 * if (asset == null) { String id =
	 * inContext.getRequestParameter("assetid"); if (id == null) { id =
	 * PathUtilities.extractPageName(inContext.getContentPage() .getPath()); }
	 * if (id != null) { asset = editor.getAsset(id); } } if (asset == null) {
	 * return; } editor.setCurrentAsset(asset); inContext.putPageValue("asset",
	 * asset); }
	 */

	public void deleteAssets(WebPageRequest inContext) throws OpenEditException
	{
		AssetEditor editor = getAssetEditor(inContext);
		String[] assetIds = inContext.getRequestParameters("assetid");
		if (assetIds == null)
		{
			return;
		}
		Asset asset;

		HitTracker tracker = editor.getMediaArchive().getAssetSearcher().loadHits(inContext);

		for (int i = 0; i < assetIds.length; i++)
		{
			if (assetIds[i].startsWith("multiedit:"))
			{
				try
				{
					CompositeAsset assets = (CompositeAsset) inContext.getSessionValue(assetIds[i]);
					for (Iterator iterator = assets.iterator(); iterator.hasNext();)
					{
						asset = (Asset) iterator.next();
						if (tracker != null)
						{
							tracker.removeSelection(asset.getId());
						}
						editor.getMediaArchive().fireMediaEvent("deleting", inContext.getUser(), asset);
						editor.deleteAsset(asset);
						String ok = inContext.getRequestParameter("deleteoriginal");
						if (Boolean.parseBoolean(ok))
						{
							editor.getMediaArchive().getAssetManager().removeOriginal(asset);
						}
						editor.getMediaArchive().fireMediaEvent("deleted", inContext.getUser(), asset);
						log.info("Asset has been deleted by user " + inContext.getUserName() + " assetid:" + asset.getId() + " sourcepath: " + asset.getSourcePath() + " original: " + ok);
					}
				}
				catch (Exception e)
				{
					continue;
				}
			}
			else
			{
				asset = editor.getAsset(assetIds[i]);
				if (asset != null)
				{
					if (tracker != null)
					{
						tracker.removeSelection(asset.getId());
					}
					editor.getMediaArchive().fireMediaEvent("deleting", inContext.getUser(), asset);
					editor.deleteAsset(asset);
					String ok = inContext.getRequestParameter("deleteoriginal");
					if (Boolean.parseBoolean(ok))
					{
						editor.getMediaArchive().getAssetManager().removeOriginal(asset);
					}
					editor.getMediaArchive().fireMediaEvent("deleted", inContext.getUser(), asset);
					log.info("Asset has been deleted by user " + inContext.getUserName() + " assetid:" + asset.getId() + " sourcepath: " + asset.getSourcePath() + " original: " + ok);
				}
			}
		}
	}

	/*
	 * public void saveAsset(WebPageRequest inContext) throws OpenEditException
	 * { String saveAsNew = inContext.getRequestParameter("saveasnew");
	 * AssetEditor editor = getAssetEditor(inContext); Asset asset =
	 * editor.getCurrentAsset();
	 * 
	 * String newId = inContext.getRequestParameter("newassetid"); // was id
	 * changed? if (newId != null && !newId.equals(asset.getId())) { Asset
	 * newasset = editor.copyAsset(asset, newId); Collection catalogs =
	 * asset.getCategories(); for (Iterator iter = catalogs.iterator();
	 * iter.hasNext();) { Category element = (Category) iter.next();
	 * newasset.addCategory(element); } if (saveAsNew == null ||
	 * saveAsNew.equalsIgnoreCase("false")) { Page oldPage =
	 * getPageManager().getPage( editor.getMediaArchive().getCatalogHome() +
	 * "/assets/" + asset.getId() + ".html"); if (oldPage.exists()) { Page
	 * newPage = getPageManager().getPage(
	 * editor.getMediaArchive().getCatalogHome() + "/assets/" + newasset.getId()
	 * + ".html"); try { getPageManager().movePage(oldPage, newPage); } catch
	 * (RepositoryException re) { throw new OpenEditException(re); } }
	 * 
	 * editor.deleteAsset(asset); // changing asset id, and erase // the old id
	 * // editor.getMediaArchive().reindexAll(); } else { Page oldPage =
	 * getPageManager().getPage( editor.getMediaArchive().getCatalogHome() +
	 * "/assets/" + asset.getId() + ".html"); if (oldPage.exists()) { Page
	 * newPage = getPageManager().getPage(
	 * editor.getMediaArchive().getCatalogHome() + "/assets/" + newasset.getId()
	 * + ".html"); try { getPageManager().copyPage(oldPage, newPage); } catch
	 * (RepositoryException re) { throw new OpenEditException(re); } } } asset =
	 * newasset; }
	 * 
	 * asset.setName(inContext.getRequestParameter("name")); //
	 * asset.setDescription( inContext.getRequestParameter( "description" // )
	 * );
	 * 
	 * editor.getMediaArchive().saveAsset(asset, inContext.getUser()); asset =
	 * editor.getAsset(asset.getId()); editor.setCurrentAsset(asset);
	 * 
	 * inContext.putPageValue("asset", asset);
	 * inContext.setRequestParameter("assetid", asset.getId()); }
	 */
	public void addAssetValues(WebPageRequest inReq) throws OpenEditException
	{
		Asset asset = getAsset(inReq);
		String inFieldName = inReq.getRequestParameter("fieldname");
		Collection existing = asset.getValues(inFieldName);
		String value = inReq.getRequestParameter(inFieldName + ".value");
		if (value != null)
		{
			if (existing == null)
			{
				existing = new ArrayList();
			}
			else
			{
				existing = new ArrayList(existing);
			}
			if (!existing.contains(value))
			{
				existing.add(value);
				asset.setValue(inFieldName, existing);
				getMediaArchive(inReq).saveAsset(asset, inReq.getUser());
			}
		}
	}

	public void removeAssetValues(WebPageRequest inReq) throws OpenEditException
	{
		Asset asset = getAsset(inReq);
		String inFieldName = inReq.getRequestParameter("fieldname");
		Collection existing = asset.getValues(inFieldName);
		String value = inReq.getRequestParameter(inFieldName + ".value");
		if (existing == null)
		{
			existing = new ArrayList();
		}
		else
		{
			existing = new ArrayList(existing);
		}
		existing.remove(value);
		asset.setValues(inFieldName, existing);
		getMediaArchive(inReq).saveAsset(asset, inReq.getUser());
	}

	protected XmpWriter getXmpWriter()
	{
		XmpWriter writer = (XmpWriter) getBeanLoader().getBean("xmpWriter");
		return writer;

	}

	public void saveAssetProperties(WebPageRequest inReq) throws OpenEditException
	{
		MediaArchive archive = getMediaArchive(inReq);
		String[] fields = inReq.getRequestParameters("field");
		if (fields == null)
		{
			return;
		}
		String assetid = inReq.getRequestParameter("assetid");
		//<input type="hidden" name="assetid" value="$asset.id"/>
		Asset asset = getAsset(inReq);
		archive.getAssetSearcher().saveDetails(inReq, fields, asset, assetid);
	}

	//Attachment handling of files
	public void attachToAssetFromUploads(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		//String basepath  = "/WEB-INF/data" + archive.getCatalogHome() + "/temp/" + inReq.getUserName() + "/";
		Asset asset = getAsset(inReq);
		List<ContentItem> temppages = getUploadedPages(inReq);

		//copy the temppages in to the originals folder, but first check if this is a folder based asset
		if (!asset.isFolder())
		{
			makeFolderAsset(inReq);
		}
		archive.getAssetManager().addNewAsset(asset, temppages);
		getAttachmentManager().processAttachments(archive, asset, false);//don't reprocess everything else

		inReq.putPageValue("asset", asset);
	}

	public void replacePrimaryAsset(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		//String basepath  = "/WEB-INF/data" + archive.getCatalogHome() + "/temp/" + inReq.getUserName() + "/";
		Asset asset = getAsset(inReq);
		List<ContentItem> temppages = getUploadedPages(inReq);

		if (temppages.isEmpty())
		{
			throw new OpenEditException("No uploads found");
		}
		//move the old file into the .versions folder?

		if (!asset.isFolder())
		{
			makeFolderAsset(inReq);
		}

		archive.getAssetManager().replaceOriginal(asset, temppages);

		//TODO:Call reimport

		inReq.setRequestParameter("assetids", new String[] { asset.getId() });

		archive.getPresetManager().reQueueConversions(archive, asset);
		archive.fireSharedMediaEvent("conversions/runconversions");

		getAttachmentManager().processAttachments(archive, asset, true);//don't reprocess everything else
		inReq.putPageValue("asset", asset);
	}

	public void createAssetFromUploads(final WebPageRequest inReq) throws Exception
	{
		final List<ContentItem> pages = getUploadedPages(inReq);
		createAssetsFromPages(pages, inReq);
	}

	public void appendRecentUploads(WebPageRequest inReq) throws Exception
	{
		List recent = (List) inReq.getSessionValue("recent-uploads");
		if (recent == null)
		{
			recent = new ArrayList();
			inReq.putSessionValue("recent-uploads", recent);
		}
		Asset asset = (Asset) inReq.getPageValue("asset");
		if (asset != null)
		{
			if (asset.getId() != null && !recent.contains(asset.getId()))
			{
				recent.add(asset.getId());
			}
		}
		HitTracker hits = (HitTracker) inReq.getPageValue("hits");
		if (hits != null)
		{
			for (Iterator iterator = hits.iterator(); iterator.hasNext();)
			{
				Data hit = (Data) iterator.next();
				if (hit.getId() != null && !recent.contains(hit.getId()))
				{
					recent.add(hit.getId());
				}
			}
		}
	}

	public void clearRecentUploads(WebPageRequest inReq) throws Exception
	{
		inReq.removeSessionValue("recent-uploads");
	}

	public void loadRecentUploads(WebPageRequest inReq) throws Exception
	{
		List recent = (List) inReq.getSessionValue("recent-uploads");
		if (recent == null)
		{
			return;
		}
		MediaArchive archive = getMediaArchive(inReq);
		if (archive == null)
		{
			return;
		}
		AssetSearcher searcher = archive.getAssetSearcher();
		SearchQuery query = searcher.createSearchQuery();
		SearchQuery allassetsquery = searcher.createSearchQuery();
		for (Iterator iterator = recent.iterator(); iterator.hasNext();)
		{
			String id = (String) iterator.next();
			allassetsquery.addExact("id", id);
		}
		allassetsquery.setAndTogether(false);
		query.addChildQuery(allassetsquery);
		searcher.cachedSearch(inReq, query);
		if (Boolean.parseBoolean(inReq.findValue("clearonload")))
		{
			clearRecentUploads(inReq);
		}
	}

	public void createAssetsFromFile(WebPageRequest inReq)
	{
		String sourcepath = inReq.findValue("sourcepath");
		String catalogid = inReq.findValue("catalogid");

		Asset asset = getAssetImporter().createAssetFromExistingFile(getMediaArchive(catalogid), inReq.getUser(), sourcepath);
		getAssetImporter().saveAsset(getMediaArchive(inReq), inReq.getUser(), asset);
		if (asset == null)
		{
			return;
		}
		if (asset instanceof CompositeAsset)
		{
			asset.setId("multiedit:new");
			inReq.putSessionValue(asset.getId(), asset);
		}
		inReq.setRequestParameter("id", asset.getId());
		inReq.putPageValue("asset", asset);
	}

	/*
	 * public void createAssetsFromTemp(WebPageRequest inReq) { MediaArchive
	 * archive = getMediaArchive(inReq); ListHitTracker tracker = new
	 * ListHitTracker();
	 * tracker.getSearchQuery().setCatalogId(archive.getCatalogId());
	 * 
	 * String[] uploadprefixes = inReq.getRequestParameters("uploadprefix");
	 * 
	 * //String basepath = "/WEB-INF/data" + archive.getCatalogHome() + "/temp/"
	 * + inReq.getUserName() + "/"; for (int i = 0; i < uploadprefixes.length;
	 * i++) { String path = inReq.getRequestParameter(uploadprefixes[i] +
	 * "temppath"); path = path.replace("\r\n",""); path = path.replace("\n",
	 * ""); path = path.replace("\r", ""); Page page =
	 * getPageManager().getPage(path); if(!page.exists()){ log.info("Page: " +
	 * page.getPath() + " does not exist"); } readMetaData(inReq,
	 * archive,uploadprefixes[i], page, tracker); }
	 * 
	 * //set the group view permissions if something was passed in
	 * findUploadTeam(inReq, archive, tracker); //TODO: Move into the loop
	 * archive.saveAssets(tracker, inReq.getUser());
	 * 
	 * String hitsname = inReq.findValue("hitsname"); if (hitsname != null) {
	 * tracker.getSearchQuery().setHitsName(hitsname); }
	 * inReq.putSessionValue(tracker.getSessionId(), tracker);
	 * inReq.putPageValue(tracker.getHitsName(), tracker);
	 * 
	 * List allids = new ArrayList(); for (Iterator iterator =
	 * tracker.iterator(); iterator.hasNext();) { Asset asset = (Asset)
	 * iterator.next(); allids.add(asset.getId());
	 * archive.fireMediaEvent("importing/assetuploaded",inReq.getUser(),asset);
	 * } Asset sample = (Asset)tracker.first(); if( sample != null) {
	 * archive.fireMediaEvent("importing/assetsuploaded",inReq.getUser(),sample,
	 * allids); } }
	 */
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

	protected void createAssetsFromPages(List<ContentItem> inPages, WebPageRequest inReq)
	{
		final MediaArchive archive = getMediaArchive(inReq);
		//final boolean createCategories = Boolean.parseBoolean( inReq.findValue("assetcreateuploadcategories"));

		final Map metadata = readMetaData(inReq, archive, "");
		final String currentcollection = (String) metadata.get("collectionid");

		boolean oktoadd = archive.isCatalogSettingTrue("assigncategoryonupload");

		if( currentcollection != null)
		{
			oktoadd = true; //Should use the mask to make any sub-categories
		}

		Object cat = metadata.get("category.value");

		if( cat != null)
		{
			oktoadd = false; //Will already exist
		}
		boolean assigncategory =  oktoadd;
		
		final Map<String, ContentItem> pages = savePages(inReq, archive, inPages);
		final User user = inReq.getUser();

		//findUploadTeam(inReq, archive, tracker); TODO:Do this is assetsimportedcustom
		if (inPages.size() == 0)
		{
			log.error("No pages uploaded");
			return;
		}
		String threaded = inReq.findValue("threadedupload");
		if (Boolean.valueOf(threaded))
		{
			ExecutorManager manager = (ExecutorManager) getModuleManager().getBean(archive.getCatalogId(), "executorManager");

			Runnable runthis = new Runnable()
			{
				public void run()
				{
					saveFilesAndImport(archive, currentcollection, assigncategory, metadata, pages, user);
				}
			};
			manager.execute("importing", runthis);
		}
		else
		{
			Collection tracker = saveFilesAndImport(archive, currentcollection, assigncategory, metadata, pages, user);
			inReq.putPageValue("assets", tracker);
		}
		if( currentcollection != null && inReq.getUserProfile() != null)
		{
			inReq.getUserProfile().setProperty("lastselectedcollection", currentcollection);
		}
	}

	protected HitTracker saveFilesAndImport(final MediaArchive archive, final String currentcollection, final boolean createCategories, final Map metadata, final Map pages, final User user)
	{
		HitTracker tracker = archive.getAssetManager().saveFilesAndImport(currentcollection, createCategories, metadata, pages, user);
		return tracker;
	}

	protected Map<String, ContentItem> savePages(WebPageRequest inReq, MediaArchive inArchive, List<ContentItem> inPages)
	{
		//if we are uploading into a collection?
		Boolean incollection = inReq.findValue("currentcollection") != null;

		Map pages = new HashMap();
		for (Iterator iterator = inPages.iterator(); iterator.hasNext();)
		{
			ContentItem contentitem = (ContentItem) iterator.next();

			String filename = contentitem.getName();
			if (filename.startsWith("tmp") && filename.indexOf('_') > -1)
			{
				filename = filename.substring(filename.indexOf('_') + 1);
			}

			String inputsourcepath = inReq.findValue("sourcepath");
			String assetsourcepath = null;
			String basepath = "/WEB-INF/data/" + inArchive.getCatalogId() + "/originals/";
			if (inputsourcepath == null)
			{
				assetsourcepath = inArchive.getAssetImporter().getAssetUtilities().createSourcePath(inReq, inArchive, filename);
				if (assetsourcepath.endsWith("/"))
				{
					assetsourcepath = assetsourcepath + contentitem.getName();
				}
			}
			else if (inputsourcepath.endsWith("/")) //EMBridge expects the filename to be added on
			{
				assetsourcepath = inputsourcepath + filename;
			}
			else
			{
				assetsourcepath = inputsourcepath;
			}

			if (incollection && filename.toLowerCase().endsWith(".zip"))
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
				int i = 2;
				while (dest.exists())
				{
					String pagename = PathUtilities.extractPageName(assetsourcepath);
					String tmppath = assetsourcepath.replace(pagename, pagename + "_" + i);
					dest = getPageManager().getContent(basepath + tmppath);
					if (!dest.exists())
					{
						assetsourcepath = tmppath;
						break;
					}
					i++;
				}
				pages.put(assetsourcepath, contentitem);
			}
		}
		return pages;
	}

	protected Map readMetaData(WebPageRequest inReq, MediaArchive archive, String prefix)
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
		vals.put("categories", cats);

		if (fields != null)
		{
			for (int i = 0; i < fields.length; i++)
			{
				String afield = fields[i];
				Object val = inReq.getRequestParameters(prefix + afield + ".value");
				if (val == null)
				{
					String[] array = inReq.getRequestParameters(prefix + afield + ".values");
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

	public void checkHasPrimary(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Asset target = getAsset(inReq);
		if (target.getPrimaryFile() == null)
		{
			String destination = "/WEB-INF/data" + archive.getCatalogHome() + "/originals/" + target.getSourcePath();
			List paths = getPageManager().getChildrenPaths(destination);
			if (paths.size() > 0)
			{
				for (Iterator iterator = paths.iterator(); iterator.hasNext();)
				{
					String path = (String) iterator.next();
					if (!path.contains(".versions"))
					{
						Page page = getPageManager().getPage(path);
						target.setPrimaryFile(page.getName());
						removeAssetImages(inReq);
						archive.saveAsset(target, null);

						break;
					}

				}
			}

		}

	}

	public void selectPrimaryAsset(WebPageRequest inReq) throws Exception
	{
		String primaryname = inReq.getRequestParameter("filename");
		String imagefilename = inReq.getRequestParameter("imagefilename"); //Dont support this any longer since we have conversion thumbs
		MediaArchive archive = getMediaArchive(inReq);

		Asset target = getAsset(inReq);
		String ext = PathUtilities.extractPageType(primaryname);

		if (target != null)
		{
			if (ext != null)
			{
				target.setProperty("fileformat", ext.toLowerCase());
			}
			if (primaryname != null)
			{
				target.setPrimaryFile(PathUtilities.extractFileName(primaryname));
			}
			if (imagefilename != null)
			{
				target.setProperty("imagefile", PathUtilities.extractFileName(imagefilename));
			}
			ContentItem itemFile = archive.getOriginalContent(target);

			// We're going to allow the metadata reader to replace this asset's properties
			// but we want to keep old values the reader is not going to replace
			archive.removeGeneratedImages(target, true);
			getAssetImporter().getAssetUtilities().getMetaDataReader().updateAsset(archive, itemFile, target);
			target.setProperty("previewstatus", "converting");
			archive.saveAsset(target, inReq.getUser());

			archive.getPresetManager().reQueueConversions(archive, target);
			archive.fireSharedMediaEvent("conversions/runconversions");
		}
	}

	public void uploadToDataDirectory(WebPageRequest inReq) throws Exception
	{
		// Why would you want to upload to the produts root directory?

		UploadRequest map = (UploadRequest) inReq.getPageValue("uploadrequest");
		List unzipped = (List) inReq.getPageValue("unzippedfiles");
		// Final destination
		String assetRoot = inReq.findValue("assetrootfolder");

		MediaArchive archive = getMediaArchive(inReq);

		for (Iterator iterator = map.getUploadItems().iterator(); iterator.hasNext();)
		{
			FileUploadItem item = (FileUploadItem) iterator.next();
			String path = item.getSavedPage().getPath();
			if (unzipped != null && unzipped.size() > 0 && path.toLowerCase().endsWith(".zip"))
			{
				continue;
			}
			getAssetImporter().processOn(assetRoot, path, false, archive, inReq.getUser());
		}

		if (unzipped != null)
		{
			for (Iterator iterator = unzipped.iterator(); iterator.hasNext();)
			{
				Page page = (Page) iterator.next();
				getAssetImporter().processOn(assetRoot, page.getPath(), false, archive, inReq.getUser());
			}
		}
	}

	public void checkImports(WebPageRequest inReq) throws Exception
	{
		// Why would you want to upload to the produts root directory?
		UploadRequest map = (UploadRequest) inReq.getPageValue("uploadrequest");
		List unzipped = (List) inReq.getPageValue("unzippedfiles");
		// Final destination
		String assetRoot = inReq.findValue("assetrootfolder");

		MediaArchive archive = getMediaArchive(inReq);

		for (Iterator iterator = map.getUploadItems().iterator(); iterator.hasNext();)
		{
			FileUploadItem item = (FileUploadItem) iterator.next();
			String path = item.getSavedPage().getPath();
			if (unzipped != null && unzipped.size() > 0 && path.toLowerCase().endsWith(".zip"))
			{
				continue;
			}
			//	public List<String> processOn(String inRootPath, String inStartingPoint, boolean checkformod, final MediaArchive inArchive, User inUser)

			getAssetImporter().processOn(assetRoot, path, false, archive, inReq.getUser());
		}

		if (unzipped != null)
		{
			for (Iterator iterator = unzipped.iterator(); iterator.hasNext();)
			{
				Page page = (Page) iterator.next();
				getAssetImporter().processOn(assetRoot, page.getPath(), false, archive, inReq.getUser());
			}
		}
	}

	public Page getAssetsPage(MediaArchive inArchive, String inSourcePath)
	{

		String prefix = inArchive.getCatalogHome() + "/assets/";
		String path = prefix + inSourcePath;
		Page page = getPageManager().getPage(path);
		return page;
	}

	/**
	 * @deprecated use Import Hot Folder script?
	 * @param inReq
	 */
	public void importFromOriginals(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String assetRoot = "/WEB-INF/data/" + archive.getCatalogId() + "/originals/";

		importAndSearch(inReq, archive, assetRoot, assetRoot);

	}

	public void runAutoMountImport(WebPageRequest inReq)
	{
		//filterout
		String catid = inReq.getRequestParameter("importcatalog");
		String path = inReq.getRequestParameter("importpath");
		if (path.endsWith("\\"))
		{
			path = path.substring(0, path.length() - 1);
		}
		String foldername = null;
		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("windows"))
		{
			foldername = path.substring(path.lastIndexOf("\\") + 1);
		}
		else
		{
			foldername = path.substring(path.lastIndexOf("/") + 1);
		}
		String mountpath = "/WEB-INF/data/" + catid + "/originals/" + foldername;
		Repository repo = getPageManager().getRepositoryManager().getRepository(mountpath);
		boolean addnew = false;
		if (repo == null)
		{
			repo = new FileRepository();
			addnew = true;
		}
		String filter = inReq.findValue("filterin");
		String filterout = inReq.findValue("filterout");
		repo.setExternalPath(path);
		repo.setPath(mountpath);
		repo.setFilterOut(filterout);
		repo.setFilterIn(filter);
		if (addnew)
		{
			getPageManager().getRepositoryManager().addRepository(repo);
		}
		getWebServer().saveMounts(getPageManager().getRepositoryManager().getRepositories());

		String assetRoot = "/WEB-INF/data/" + catid + "/originals/";
		MediaArchive archive = getMediaArchive(inReq);

		importAndSearch(inReq, archive, mountpath, assetRoot);
	}

	protected void importAndSearch(WebPageRequest inReq, MediaArchive inArchive, String mountpath, String assetRoot)
	{
		List<String> created = getAssetImporter().processOn(assetRoot, assetRoot, false, inArchive, inReq.getUser());

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

	//	public void runImport(WebPageRequest inReq)
	//	{
	//		MediaArchive archive = getMediaArchive(inReq);
	//		String sourcePath = inReq.getRequestParameter("sourcepath");
	//		if (sourcePath == null || sourcePath.length() == 0)
	//		{
	//			sourcePath = "/";
	//		}
	//		Page dir = getAssetsPage(archive, sourcePath);
	//		String importType = dir.getProperty("importtype");
	//		
	//		String mounts = dir.getProperty("mountsonly");
	//		boolean mountsonly = false;
	//		if( mounts != null )
	//		{
	//			mountsonly = Boolean.parseBoolean(mounts);
	//		}
	//		
	//		String assetRoot = archive.getCatalogHome() + "/data/originals/";
	//
	//		String start = assetRoot;
	//		if (sourcePath.length() > 1)
	//		{
	//			start = start + sourcePath;
	//		}
	//		int deletecount = 0;
	//		List created = null;
	//		if ("imageimportconvert".equals(importType))
	//		{
	//			// Are we creating a new folder?
	//			String newFolder = inReq.getRequestParameter("newname");
	//			if (newFolder != null)
	//			{
	//				start = start + newFolder;
	//			}
	//			// delete all the old assets
	//			AssetSearcher searcher = archive.getAssetSearcher();
	//			SearchQuery q = searcher.createSearchQuery();
	//			q.addStartsWith("sourcepath", sourcePath);
	//			HitTracker old = searcher.search(q);
	//			searcher.deleteFromIndex(old);
	//			String importFilters = inReq.getRequestParameter("importextensions");
	//			created = getAssetImporter().processOn(assetRoot, start, importFilters, archive, mountsonly, inReq.getUser());
	//
	//			// Delete old assets not present anymore
	//			for (Iterator iterator = old.iterator(); iterator.hasNext();)
	//			{
	//				Document hit = (Document) iterator.next();
	//				Asset p = archive.getAssetBySourcePath(hit.get("sourcepath"));
	//				if (p != null)
	//				{
	//					SearchQuery sq = searcher.createSearchQuery();
	//					sq.addMatches("sourcepath", hit.get("sourcepath"));
	//					HitTracker tracker = searcher.search(sq);
	//					if (tracker.size() == 0)
	//					{
	//						// Asset is not in the index anymore...delete it from
	//						// disk
	//						archive.removeGeneratedImages(p);
	//						archive.getAssetSearcher().delete(p, inReq.getUser());
	//						deletecount++;
	//					}
	//					
	//				}
	//			}
	//		}
	//		ListHitTracker lht = new ListHitTracker(created);
	//		lht.getSearchQuery().setHitsName("hits");
	//		lht.getSearchQuery().setCatalogId(archive.getCatalogId());
	//		
	//		inReq.putSessionValue(lht.getSessionId(), lht);
	//		inReq.putPageValue(lht.getHitsName(), lht);
	//		
	//		inReq.putPageValue("numdeletes", deletecount);
	//		inReq.putPageValue("numrecords", new Integer(created.size()));
	//	}

	public void removeExpiredAssets(WebPageRequest inReq)
	{
		String sourcepath = inReq.getRequestParameter("sourcepath");
		MediaArchive archive = getMediaArchive(inReq);
		List removed = getAssetImporter().removeExpiredAssets(archive, sourcepath, inReq.getUser());
		inReq.putPageValue("removedassets", removed);
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

	public void redirectToGallery(WebPageRequest inReq)
	{
		MediaArchive store = getMediaArchive(inReq);

		String include = inReq.findValue("includefilter");
		int index = include.lastIndexOf("/");
		String dir = include.substring(0, index);

		String redirTo = "/" + store.getCatalogId() + "/layout/assets/files/index.html?path=" + dir;
		inReq.redirectPermanently(redirTo);
	}

	/**
	 * @param inReq
	 *            public void encodeVideos(WebPageRequest inReq) { String
	 *            hitsname = inReq.findValue("hitsname"); if (hitsname == null)
	 *            { hitsname = "hits"; } HitTracker tracker = (HitTracker)
	 *            inReq.getPageValue(hitsname); if (tracker == null) { return; }
	 * 
	 *            final MediaArchive archive = getMediaArchive(inReq);
	 * 
	 *            //queue up some video encoding. We should do this some other
	 *            way
	 * 
	 *            for (Iterator iterator = tracker.iterator();
	 *            iterator.hasNext();) { Object object = (Object)
	 *            iterator.next(); if (!(object instanceof Asset)) { continue; }
	 *            final Asset asset = (Asset) object; String render =
	 *            archive.getMediaRenderType(asset.getFileFormat()); if
	 *            (!render.equals("video")) { continue; } Runnable runner = new
	 *            Runnable() { public void run() { try { ConvertInstructions
	 *            instructions = new ConvertInstructions();
	 *            instructions.setForce(true); instructions.setInputExtension(
	 *            asset.getFileFormat() );
	 *            instructions.setOutputExtension("flv");
	 *            instructions.setAssetSourcePath(asset.getSourcePath());
	 *            archive.getCreatorManager().createOutput( instructions ); }
	 *            catch(Exception e) { log.error("Couldn't convert video", e); }
	 *            } }; // BaseTask task = new BaseTask(); // task.addAction(new
	 *            RunnableAction(runner) ); // getTaskManager().addTask(task); }
	 *            }
	 */

	protected Data buildDataObject(WebPageRequest inReq, PropertyDetails inDetails)
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

	public void saveUsageHistory(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Searcher searcher = getSearcherManager().getSearcher(archive.getCatalogId(), "usagehistory");
		String[] assetids = inReq.getRequestParameters("assetid");
		ArrayList<Data> newrecords = new ArrayList<Data>();

		if (assetids == null)
		{
			return;
		}

		for (String assetid : assetids)
		{
			Data data = buildDataObject(inReq, searcher.getPropertyDetails());
			PropertyDetail detail = searcher.getDetail("date");
			if (detail != null)
			{
				data.setProperty(detail.getId(), DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
			}
			detail = searcher.getDetail("user");
			if (detail != null)
			{
				data.setProperty(detail.getId(), inReq.getUserName());
			}
			detail = searcher.getDetail("assetid");
			if (detail != null)
			{
				data.setProperty(detail.getId(), assetid);
			}
			Asset asset = archive.getAsset(assetid);
			data.setSourcePath(asset.getSourcePath());
			searcher.saveData(data, inReq.getUser());
			newrecords.add(data);
		}
		inReq.putPageValue("newrecord", newrecords);
	}

	public void loadUsageHistory(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Searcher searcher = getSearcherManager().getSearcher(archive.getCatalogId(), "usagehistory");

		String assetid = inReq.getRequestParameter("assetid");
		if (assetid == null)
		{
			Asset asset = getAsset(inReq);
			if (asset == null)
			{
				return;
			}
			assetid = asset.getId();
		}

		if (assetid == null)
		{
			return;
		}
		HitTracker hits = searcher.fieldSearch("assetid", assetid);
		inReq.putPageValue("history", hits);
		inReq.putPageValue("historySearcher", searcher);
	}

	public void saveSelectionProperties(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String[] assetids = inReq.getRequestParameters("assetselect_" + archive.getCatalogId());
		String[] fields = inReq.getRequestParameters("field");
		for (int i = 0; i < assetids.length; i++)
		{
			Asset asset = archive.getAsset(assetids[i]);
			if (asset == null)
			{
				continue;
			}
			for (int j = 0; j < fields.length; j++)
			{
				String value = inReq.getRequestParameter(asset.getId() + "." + fields[j] + ".value");
				if (value == null)
				{
					value = inReq.getRequestParameter(fields[j] + ".value");
				}
				asset.setProperty(fields[j], value);
				archive.getAssetSearcher().saveData(asset, inReq.getUser());
			}
		}
	}

	public void fireAssetEvent(WebPageRequest inReq)
	{
		String type = inReq.getPageProperty("asseteventtype");
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		archive.fireMediaEvent(type, inReq.getUser(), asset);

	}

	public WebServer getWebServer()
	{
		return fieldWebServer;
	}

	public void setWebServer(WebServer inWebServer)
	{
		fieldWebServer = inWebServer;
	}

	public void resizedAsset(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String width = (String) inReq.getRequestParameter("width");
		String height = (String) inReq.getRequestParameter("height");
		String scalex = (String) inReq.getRequestParameter("scalex");
		String scaley = (String) inReq.getRequestParameter("scaley");

		Asset asset = getAsset(inReq);
		if (scalex == null && scaley == null)
		{
			asset.setProperty("width", width);
			asset.setProperty("height", height);
		}
		else
		{
			float fscalex = Float.parseFloat(scalex);
			float fscaley = Float.parseFloat(scaley);
			int iwidth = Integer.parseInt(width);
			int iheight = Integer.parseInt(height);
			if (fscalex > 0)
			{
				asset.setProperty("width", Integer.toString((int) (iwidth * fscalex)));
			}
			if (fscaley > 0)
			{
				asset.setProperty("height", Integer.toString((int) (iheight * fscaley)));
			}
		}
		archive.saveAsset(asset, inReq.getUser());
		archive.removeGeneratedImages(asset);
	}

	/**
	 * Update vote counts
	 * 
	 * @param inReq
	 * @throws Exception
	 */
	public void loadAssetVotes(WebPageRequest inReq)
	{
		Asset asset = (Asset) inReq.getPageValue("asset");
		if (asset == null)
		{
			return;
		}
		String catalogid = inReq.findValue("catalogid");

		Searcher searcher = getSearcherManager().getSearcher(catalogid, "assetvotes");
		if (searcher == null)
		{
			throw new OpenEditException("Unable to load searcher for assetvotes.");
		}
		SearchQuery q = searcher.createSearchQuery();
		q.setHitsName("voteshits");
		q.addMatches("assetid", asset.getId());
		HitTracker hits = searcher.cachedSearch(inReq, q);

		String username = inReq.getUserName();
		if (username != null)
		{
			for (Object hit : hits)
			{
				if (username.equals(hits.getValue(hit, "username")))
				{
					inReq.putPageValue("alreadyvoted", Boolean.TRUE);
					break;
				}
			}
		}
		int count = asset.getInt("assetvotes");
		if (count != hits.size())
		{
			asset.setProperty("assetvotes", String.valueOf(hits.size()));
			MediaArchive archive = getMediaArchive(inReq);
			//async asset save?
			archive.fireMediaEvent("assetsave", inReq.getUser(), asset);
		}

	}

	public void deleteAssetVote(WebPageRequest ex)
	{
		Asset asset = getAsset(ex);
		User user = ex.getUser();

		MediaArchive archive = getMediaArchive(ex);
		removeVote(asset, archive, user);
		loadAssetVotes(ex);
	}

	public void removeVote(Asset asset, MediaArchive archive, User user)
	{
		Searcher searcher = getSearcherManager().getSearcher(archive.getCatalogId(), "assetvotes");
		Data row = (Data) searcher.searchById(user.getUserName() + "_" + asset.getId());
		if (row != null)
		{
			searcher.delete(row, user);
		}
		archive.fireMediaEvent("userunlikes", user, asset);

	}

	public void voteForAsset(WebPageRequest ex) throws Exception
	{
		/*
		 * #set($searcher = $searcherManager.getSearcher($catalogid,
		 * "assetvotes")) #set($dateformat =
		 * $searcher.getDetail("time").getDateFormat()) #set($date =
		 * $dateformat.format($today)) #set($alreadyVoted = $uservote)
		 * $context.putPageValue("votetoremove", $uservote)
		 */
		MediaArchive archive = getMediaArchive(ex);

		Asset asset = getAsset(ex);

		voteForAsset(asset, archive, ex.getUser());
		loadAssetVotes(ex);
	}

	public void voteForAsset(Asset asset, MediaArchive archive, User inUser)
	{
		Searcher searcher = getSearcherManager().getSearcher(archive.getCatalogId(), "assetvotes");
		//	DateFormat dateformat = searcher.getDetail("time").getDateFormat();
		if (asset.getId().contains("multiedit:"))
		{
			throw new OpenEditException("Can't edit votes");
		}
		Data row = searcher.createNewData();
		String username = inUser.getUserName();

		row.setId(username + "_" + asset.getId());
		String date = DateStorageUtil.getStorageUtil().formatForStorage(new Date());
		row.setValue("votetime", date);
		row.setValue("assetid", asset.getId());
		row.setValue("username", inUser.getUserName());
		row.setSourcePath(asset.getSourcePath());
		searcher.saveData(row, inUser);
		archive.fireMediaEvent("userlikes", inUser, asset);
		//archive.getAssetSearcher().updateIndex(asset); //get the rank updated
	}

	public void saveAssetData(WebPageRequest inReq) throws Exception
	{
		Asset asset = getAsset(inReq);
		getMediaArchive(inReq).saveAsset(asset, inReq.getUser());
	}

	public void markAssetsDeleted(WebPageRequest inReq) throws OpenEditException
	{
		String[] sourcepath = inReq.getRequestParameters("sourcepath");
		if (sourcepath != null)
		{
			MediaArchive archive = getMediaArchive(inReq);
			List assets = new ArrayList();
			for (int i = 0; i < sourcepath.length; i++)
			{
				Asset asset = archive.getAssetBySourcePath(sourcepath[i]);
				if (asset != null)
				{
					asset.setProperty("editstatus", "7");
					assets.add(asset);
				}
			}
			String deleterecord = inReq.getRequestParameter("deleterecord");
			if (Boolean.parseBoolean(deleterecord))
			{
				archive.saveAssets(assets, inReq.getUser());
				for (Iterator iterator = assets.iterator(); iterator.hasNext();)
				{
					Asset asset = (Asset) iterator.next();
					archive.getAssetSearcher().delete(asset, inReq.getUser());
				}
			}
			else
			{
				archive.saveAssets(assets, inReq.getUser());
			}
			inReq.putPageValue("deletedlist", assets);
		}
	}
	
	//Deprecated use: MediaArchiveModule.getAsset()

	public Data createMultiEditDataFromSelections(WebPageRequest inReq) throws Exception
	{
		String hitssessionid = inReq.getRequestParameter("hitssessionid");//expects session id
		if (hitssessionid == null)
		{
			return null;
		}
		if (hitssessionid.startsWith("selected"))
		{
			hitssessionid = hitssessionid.substring("selected".length());
		}

		//Make a new search based on everyone being selected
		HitTracker hits = (HitTracker) inReq.getSessionValue(hitssessionid); //this could be out of date if we saved already. Just grab the selection and let the composite refresh each data row
		if (hits == null)
		{
			log.error("Could not find " + hitssessionid);
			return null;
		}

		//Now always reload the selected nodes and only pass in those nodes to multi-edit
		MediaArchive store = getMediaArchive(inReq);

		//		//lost selections?
		//		HitTracker old = hits;
		//		hits  = store.getAssetSearcher().getAllHits(inReq);
		//		hits.loadPreviousSelections(old);
		//		
		if (!hits.hasSelections())
		{
			log.error("No assets selected " + hitssessionid);
			return null;
		}

		//		CompositeAsset composite = new CompositeAsset();
		//		for (Iterator iterator = hits.getSelectedHits().iterator(); iterator.hasNext();)
		//		{
		//			Object target = (Object) iterator.next();
		//			Asset p = null;
		//			if( target instanceof Asset)
		//			{
		//				p = (Asset)target;
		//			}
		//			else
		//			{
		//				String id = hits.getValue(target, "id");
		//				p = store.getAsset(id);
		//			}
		//			if( p != null)
		//			{
		//				composite.addData(p);
		//			}
		//		}

		//		HitTracker freshhits = store.getAssetSearcher().cachedSearch(inReq,hits.getSearchQuery());
		//freshhits.setSelections(hits.getSelections()); //TODO: What if the order changes?
		//HitTracker selected = freshhits.getSelectedHitracker();
		String assetid = "multiedit:" + hitssessionid;
		inReq.removeSessionValue(assetid);
		Asset composite = store.getAsset(assetid, inReq);
		inReq.setRequestParameter("assetid", assetid);
		inReq.putPageValue("data", composite);
		inReq.putPageValue("asset", composite);
		inReq.putSessionValue(composite.getId(), composite);

		return composite;
	}

	public void originalModified(WebPageRequest inRequest) throws Exception
	{
		String[] assetids = inRequest.getRequestParameters("assetids");
		MediaArchive mediaArchive = getMediaArchive(inRequest);
		int missing = 0;
		for (int i = 0; i < assetids.length; i++)
		{
			Asset asset = mediaArchive.getAsset(assetids[i]);
			if (asset == null)
			{
				log.error("Missing asset " + assetids[i]);
				continue;
			}
			getAssetImporter().reImportAsset(mediaArchive, asset);
		}
		mediaArchive.fireSharedMediaEvent("conversions/runconversions");
	}

	/**
	 * Grab first asset that was selected
	 * 
	 * @param inReq
	 * @return
	 */
	public Asset loadAssetFromSelection(WebPageRequest inReq)
	{
		Object found = inReq.getPageValue("asset");
		if (found instanceof Asset)
		{
			return (Asset) found;
		}
		Asset asset = null;
		String hitssessionid = inReq.getRequestParameter("hitssessionid");//expects session id
		if (hitssessionid != null)
		{
			HitTracker hits = (HitTracker) inReq.getSessionValue(hitssessionid);
			if (hits != null && hits.hasSelections())
			{
				String assetid = inReq.getRequestParameter("assetid");
				if (assetid != null && assetid.startsWith("multiedit:"))
				{
					asset = (Asset) inReq.getSessionValue(assetid);
				}
				else if (assetid == null)
				{
					String id = hits.getFirstSelected();
					asset = getMediaArchive(inReq).getAsset(id);
				}
				if (asset != null)
				{
					inReq.putPageValue("asset", asset);
				}
			}
		}
		//		if( asset == null )
		//		{
		//			return getAsset(inReq);
		//		}
		return asset;

	}

	public void handleUploads(WebPageRequest inReq)
	{

		//		final MediaArchive archive = getMediaArchive(inReq);
		//		final Map metadata = readMetaData(inReq,archive,"");
		//
		//		final Map pages = savePages(inReq,archive,inPages);
		//		final User user = inReq.getUser();
		//		

		FileUpload command = new FileUpload();
		command.setPageManager(getPageManager());
		UploadRequest properties = command.parseArguments(inReq);

		if (properties == null)
		{
			properties = (UploadRequest) inReq.getPageValue("properties");
		}
		if (properties == null)
		{
			return;
		}
		String searchtype = inReq.findValue("searchtype");
		String id = inReq.getRequestParameter("id");
		final String currentcollection = inReq.getRequestParameter("collectionid");

		Data target = null;

		MediaArchive archive = getMediaArchive(inReq);
		if (searchtype != null && id != null)
		{
			target = archive.getData(searchtype, id);
		}
		if (id == null && target == null)
		{
			Searcher searcher = archive.getSearcher(searchtype);
			target = searcher.createNewData();
			searcher.saveData(target, inReq.getUser());
			id = target.getId();
			inReq.setRequestParameter("id", id);
			String[] fields = inReq.getRequestParameters("field");

			searcher.updateData(inReq, fields, target);  //TODO: Skip if save = false
		}
		Collection savedassets = new ArrayList();
		
		for (Iterator iterator = properties.getUploadItems().iterator(); iterator.hasNext();)
		{
			FileUploadItem item = (FileUploadItem) iterator.next();

			String name = item.getFieldName();
			if (item.getName().length() == 0)
			{
				continue;
			}
			String[] splits = name.split("\\.");
			String detailid = splits[1];
			//String sourcepath = inReq.getRequestParameter(detailid + ".sourcepath"); //Is this risky?
			String sourcepath = null;
			HashMap variables = new HashMap();
			variables.put("userid", inReq.getUser().getId());
			variables.put("id", id);
			if (target != null)
			{
				variables.put("data", target);
			}

			if (searchtype != null)
			{
				Searcher searcher = archive.getSearcher(searchtype);
				PropertyDetail detail = searcher.getDetail(detailid);
				if (detail != null)
				{
					sourcepath = detail.get("sourcepath");
					sourcepath = archive.getSearcherManager().getValue(archive.getCatalogId(), sourcepath, variables);

				}
			}
			if (sourcepath == null)
			{
				sourcepath = getAssetImporter().getAssetUtilities().createSourcePath(inReq, archive, item.getName());
			}
			String path = "/WEB-INF/data/" + archive.getCatalogId() + "/originals/" + sourcepath + "/" + item.getName();
			sourcepath = sourcepath.replace("//", "/"); //in case of missing data
			path = path.replace("//", "/");

			properties.saveFileAs(item, path, inReq.getUser());

			boolean assigncategory = archive.isCatalogSettingTrue("assigncategoryonupload");

			String assigncategoryval  = inReq.findValue("assigncategory");
			if( assigncategoryval != null)
			{
				assigncategory = Boolean.parseBoolean(assigncategoryval);
			}

			//MediaArchive inArchive, User inUser, Page inAssetPage)

			Asset current = getAssetImporter().getAssetUtilities().populateAsset(null, item.getSavedPage().getContentItem(), archive, sourcepath, inReq.getUser());
			archive.saveAsset(current, inReq.getUser());
			current.setPrimaryFile(item.getName());
			current.setProperty("name", item.getName());

			if (assigncategory)
			{
				Category defaultcat = archive.getCategorySearcher().createCategoryPath(sourcepath);
				current.clearCategories();
				current.addCategory(defaultcat);
			}

			//TODO: Use the standard ways we upload 
			//		Collection tracker = saveFilesAndImport(archive, currentcollection, metadata, pages, user);

			//			current.setProperty("owner", inReq.getUser().getId());
			archive.removeGeneratedImages(current, true);
			archive.saveAsset(current, null);
			inReq.putPageValue("newasset", current);
			inReq.setRequestParameter(detailid + ".value", current.getId());
			archive.fireMediaEvent("importing", "assetuploaded", inReq.getUser(), current);
			archive.fireMediaEvent("assetcreated", inReq.getUser(), current);
			archive.fireMediaEvent("importing", "assetsimported", inReq.getUser(), current);

			if (currentcollection != null)
			{
				ProjectManager manager = (ProjectManager) getModuleManager().getBean(archive.getCatalogId(), "projectManager");
				manager.addAssetToCollection(archive, currentcollection, current);
			}
			savedassets.add(current);
		}
		inReq.putPageValue("savedassets", savedassets);

	}

	public void toggleAssetLock(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);

		asset.toggleLock(inReq.getUser());
		archive.saveAsset(asset);

	}

	public void rotateAsset(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		ContentItem item = archive.getOriginalContent(asset);

		String custompath = "/WEB-INF/data/" + archive.getCatalogId() + "/generated/" + asset.getSourcePath() + "/customthumb.jpg";
		Page custom = archive.getPageManager().getPage(custompath);
		Page temp = null;
		if (custom.exists())
		{
			temp = archive.getPageManager().getPage("/WEB-INF/temp/customthumb.jpg");
			archive.getPageManager().movePage(custom, temp);
		}

		archive.removeGeneratedImages(asset, true);

		ConvertInstructions ins = new ConvertInstructions(archive);
		ins.setAsset(asset);
		if (temp != null)
		{
			ins.setInputFile(temp.getContentItem());
		}
		else
		{
			ins.setInputFile(item);

		}

		String rotation = inReq.findValue("rotate");
		ins.setProperty("rotate", rotation);
		ConversionManager manager = archive.getTranscodeTools().getManagerByFileFormat(asset.getFileFormat());
		String findValue = inReq.findValue("rotateoriginal");
		boolean original = Boolean.parseBoolean(findValue);
		ins.setForce(true);

		if (original)
		{
			ins.setOutputFile(item);
		}
		else
		{

			ins.setOutputFile(custom.getContentItem());
		}
		ConvertResult r = manager.createOutput(ins);
		
		archive.getPresetManager().reQueueConversions(archive, asset);
		archive.fireSharedMediaEvent("conversions/runconversions");

		Asset tempasset = (Asset) archive.getAssetSearcher().createNewData();
		tempasset.setSourcePath("temp");
		ExiftoolMetadataExtractor extractor = (ExiftoolMetadataExtractor) archive.getBean("exiftoolMetadataExtractor");

		extractor.extractData(archive, r.getOutput(), tempasset);

		asset.setValue("height", tempasset.getValue("height"));
		asset.setValue("width", tempasset.getValue("width"));
		archive.saveAsset(asset);
		inReq.putPageValue("asset", asset);

	}

}
