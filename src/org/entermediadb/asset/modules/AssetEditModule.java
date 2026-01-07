package org.entermediadb.asset.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
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
import org.entermediadb.asset.edit.Version;
import org.entermediadb.asset.scanner.AssetImporter;
import org.entermediadb.asset.scanner.ExiftoolMetadataExtractor;
import org.entermediadb.asset.search.AssetSearcher;
import org.entermediadb.asset.upload.FileUpload;
import org.entermediadb.asset.upload.FileUploadItem;
import org.entermediadb.asset.upload.UploadRequest;
import org.entermediadb.asset.xmp.XmpWriter;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.WebServer;
import org.openedit.data.PropertyDetail;
import org.openedit.data.QueryBuilder;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;
import org.openedit.repository.Repository;
import org.openedit.repository.filesystem.FileRepository;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;
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

	public UploadRequest getUploadedPages(WebPageRequest inReq)
	{
		
//		List unzipped = (List) inReq.getPageValue("unzippedfiles");
//		if (unzipped != null && unzipped.size() > 0)
//		{
//			for (Iterator iterator = unzipped.iterator(); iterator.hasNext();)
//			{
//				Page page = (Page) iterator.next();
//				contentitems.add(page.getContentItem());
//			}
//		}
//		else
//		{
		UploadRequest map = (UploadRequest) inReq.getPageValue("uploadrequest");
		
		return map;
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
		boolean deleteoriginal = Boolean.parseBoolean(inContext.getRequestParameter("deleteoriginal"));
		HitTracker tracker = editor.getMediaArchive().getAssetSearcher().loadHits(inContext);
		int deleted = 0;
		String assetid = inContext.getRequestParameter("assetid");
		Asset asset;
		if (assetid != null)
		{
			asset = editor.getMediaArchive().getAsset(assetid);
			editor.deleteAsset(asset, tracker, deleteoriginal, inContext.getUser());
			deleted++;
			
		}
		else
		{
			for (Iterator iterator = tracker.getSelectedHitracker().iterator(); iterator.hasNext();)
			{
				Data data = (Data) iterator.next();
				
				asset = (Asset) editor.getMediaArchive().getAssetSearcher().loadData(data);
				editor.deleteAsset(asset, tracker, deleteoriginal, inContext.getUser());
				deleted++;
				
			}
		}
		inContext.putPageValue("rowsedited", String.valueOf(deleted));

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
		asset.setValue(inFieldName, existing);
		getMediaArchive(inReq).saveAsset(asset, inReq.getUser());
	}

	protected XmpWriter getXmpWriter()
	{
		XmpWriter writer = (XmpWriter) getBeanLoader().getBean("xmpWriter");
		return writer;

	}

	/*
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
	*/

	//Attachment handling of files
	public void attachToAssetFromUploads(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		//String basepath  = "/WEB-INF/data" + archive.getCatalogHome() + "/temp/" + inReq.getUserName() + "/";
		Asset asset = getAsset(inReq);
		UploadRequest temppages = getUploadedPages(inReq);

		//copy the temppages in to the originals folder, but first check if this is a folder based asset
		if (!asset.isFolder())
		{
			makeFolderAsset(inReq);
		}
		archive.getAssetManager().addNewAsset(asset, temppages.getSavedContentItems());
		getAttachmentManager().processAttachments(archive, asset, false);//don't reprocess everything else

		inReq.putPageValue("asset", asset);
	}

	public void replacePrimaryAsset(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		//String basepath  = "/WEB-INF/data" + archive.getCatalogHome() + "/temp/" + inReq.getUserName() + "/";
		Asset asset = getAsset(inReq);
		UploadRequest uploadRequest = getUploadedPages(inReq);
		List temppages = uploadRequest.getSavedContentItems();
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

		inReq.setRequestParameter("assetids", new String[] { asset.getId() });

		archive.getPresetManager().reQueueConversions(archive, asset);
		archive.fireSharedMediaEvent("conversions/runconversions");

		getAttachmentManager().processAttachments(archive, asset, true);//don't reprocess everything else
		inReq.putPageValue("asset", asset);
	}
	
	public void replaceOriginal(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		//String basepath  = "/WEB-INF/data" + archive.getCatalogHome() + "/temp/" + inReq.getUserName() + "/";
		Asset asset = getAsset(inReq);
		UploadRequest uploadRequest = getUploadedPages(inReq);
		List temppages = uploadRequest.getSavedContentItems();
		if (temppages.isEmpty())
		{
			throw new OpenEditException("No uploads found");
		}

		archive.getAssetManager().replaceOriginal(asset, temppages); 
		archive.fireMediaEvent("originalreplaced", inReq.getUser(), asset);

		inReq.setRequestParameter("assetids", new String[] { asset.getId() });

		archive.getPresetManager().reQueueConversions(archive, asset);
		archive.fireSharedMediaEvent("conversions/runconversions");

		inReq.putPageValue("asset", asset);
	}

	public void createAssetFromUploads(final WebPageRequest inReq) throws Exception
	{
		UploadRequest pages = getUploadedPages(inReq);
		getAssetImporter().createAssetsFromPages(getMediaArchive(inReq), pages, inReq);
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
		String catalogid = inReq.findPathValue("catalogid");

		Asset asset = getAssetImporter().createAssetFromExistingFile(getMediaArchive(catalogid), inReq.getUser(), sourcepath);
		getAssetImporter().saveAsset(getMediaArchive(inReq), inReq.getUser(), asset);
		if (asset == null)
		{
			return;
		}
//		if (asset instanceof CompositeAsset)
//		{
//			asset.setId("multiedit:new");
//			inReq.putSessionValue(asset.getId(), asset);
//		}
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

	/**
	 * @deprecated use Import Hot Folder script?
	 * @param inReq
	 */
	public void importFromOriginals(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String assetRoot = "/WEB-INF/data/" + archive.getCatalogId() + "/originals/";

		getAssetImporter().importAndSearch(inReq, archive, assetRoot, assetRoot);

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

		getAssetImporter().importAndSearch(inReq, archive, mountpath, assetRoot);
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
			Data data = getAssetImporter().buildDataObject(inReq, searcher.getPropertyDetails());
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
		MultiValued data = (MultiValued) inReq.getPageValue("asset");
		if (data == null)
		{
			return;
		}
		//Asset asset = getM
		if (data.getId().contains("multiedit:")) {
			return;
		}

		int count = data.getInt("assetvotes");
		if(count == 0)
		{
			return;
		}
		MediaArchive archive = getMediaArchive(inReq);
		String catalogid = inReq.findPathValue("catalogid");
		Searcher searcher = getSearcherManager().getSearcher(catalogid, "assetvotes");
		if (searcher == null)
		{
			throw new OpenEditException("Unable to load searcher for assetvotes.");
		}
		QueryBuilder q = searcher.query();
		q.named("voteshits");
		q.exact("assetid", data.getId());
		HitTracker hits = archive.getCachedSearch(q);

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
		if (count != hits.size())
		{
			data.setProperty("assetvotes", String.valueOf(hits.size()));
			//async asset save?
			Asset asset = (Asset)archive.getAssetSearcher().loadData(data);
			archive.fireMediaEvent("assetsave", inReq.getUser(), asset);
		}

	}

	public void deleteAssetVote(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		User user = inReq.getUser();
		if (asset != null) {
			removeVote(asset, archive, user);
			loadAssetVotes(inReq);
			inReq.putPageValue("assetid", asset.getId());
		}
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

	public void voteForAsset(WebPageRequest inReq) throws Exception
	{
		/*
		 * #set($searcher = $searcherManager.getSearcher($catalogid,
		 * "assetvotes")) #set($dateformat =
		 * $searcher.getDetail("time").getDateFormat()) #set($date =
		 * $dateformat.format($today)) #set($alreadyVoted = $uservote)
		 * $context.putPageValue("votetoremove", $uservote)
		 */
		MediaArchive archive = getMediaArchive(inReq);
		String assetId = inReq.getRequestParameter("assetid");
		
		Asset asset;
		
		if(assetId == null) {
			HitTracker tracker = archive.getAssetSearcher().loadHits(inReq);
			for (Iterator iterator = tracker.getSelectedHitracker().iterator(); iterator.hasNext();)
			{
				Data assetdata = (Data) iterator.next();
				asset = (Asset) archive.getAssetSearcher().loadData(assetdata);
				if (asset != null) {
					getAssetImporter().getAssetUtilities().voteForAsset(asset, archive, inReq.getUser());
				 }
			}
		}
		else {
			asset = getAsset(inReq);
			if (asset != null)
			{
				if (asset != null) {
					getAssetImporter().getAssetUtilities().voteForAsset(asset, archive, inReq.getUser());
					inReq.putPageValue("assetid", asset.getId());
				}
			}
		}
		
		
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
		String hitssessionid = inReq.getRequestParameter("assethitssessionid");//expects session id
		if (hitssessionid == null)
		{
			return null;
		}
		if (hitssessionid.startsWith("selected"))
		{
			hitssessionid = hitssessionid.substring("selected".length());
		}

		String moduleid = inReq.findPathValue("module");
		HitTracker hits = loadHitTracker(inReq, moduleid);
		
		//this could be out of date if we saved already. Just grab the selection and let the composite refresh each data row
		if (hits == null)
		{
			log.error("Could not find hittracker" );
			return null;
		}

		//Now always reload the selected nodes and only pass in those nodes to multi-edit
		MediaArchive archive = getMediaArchive(inReq);

		//		//lost selections?
		//		HitTracker old = hits;
		//		hits  = store.getAssetSearcher().getAllHits(inReq);
		//		hits.loadPreviousSelections(old);
		//		
		if (!hits.hasSelections())
		{
			log.error("No assets selected hittracker");
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
		Searcher searcher = archive.getSearcher("asset");
		Data composite = searcher.loadData(inReq,assetid);
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

		String moduleid = inReq.findPathValue("module");
		HitTracker hits = loadHitTracker(inReq, moduleid);
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
		MediaArchive archive = getMediaArchive(inReq);
		
		UploadRequest properties = (UploadRequest)inReq.getPageValue("uploadrequest");
		if( properties == null)
		{
			FileUpload command = new FileUpload();
			command.setPageManager(getPageManager());
			properties = command.parseArguments(inReq);
			inReq.putPageValue("uploadrequest",properties);
		}
		if (properties == null)
		{
			properties = (UploadRequest) inReq.getPageValue("properties");
		}
		if (properties == null)
		{
			return;
		}
		if(properties.getUploadItems().size() == 0) {
			return;
		}
		
		String moduleid  = inReq.findPathValue("module");
		
		String id = null;
		Searcher searcher = null;
		
		String pageval = inReq.findActionValue("pageval");
		if (pageval == null) 
		{
			pageval = "data";
		}
		Data target = (Data)inReq.getPageValue(pageval);
		
		if (target != null)
		{
			id = target.get("id");
			
			String searchtype = inReq.findValue("searchtype");
			if (searchtype != null)
			{
				searcher = archive.getSearcher(searchtype);
			}
			else {
				target = null; //we cant continue without a searcher
			}
		}
		
		
		//If no pageval with data provided
		if (target == null)
		{
		
			if(moduleid == null)
			{
				moduleid = inReq.findValue("searchtype");
			}
			
			id = inReq.getRequestParameter("id");
			
			if(id == null) {
				id = inReq.getRequestParameter("id.value");
			}
			if(id == null) {
				id = inReq.getRequestParameter("entityid");
			}
			if (moduleid != null && id != null)
			{
				target = archive.getData(moduleid, id);
			}

			searcher = archive.getSearcher(moduleid);
			
			if (target == null  && id == null && searcher != null) //new record
			{
				target = searcher.createNewData();
				String[] fields = inReq.getRequestParameters("field");
				searcher.updateData(inReq, fields, target);  //TODO: Skip if save = false
				searcher.saveData(target, inReq.getUser());
				id = target.getId();
				inReq.setRequestParameter("id", id);
			}
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
			String sourcepath = null;
			if (target != null)
			{							
				PropertyDetail detail = searcher.getDetail(detailid);
				if (detail != null)
				{
					String sourcemask = detail.get("sourcepath");					
//					//Legacy work around
//					if( sourcemask != null && sourcemask.contains("uploadsourcepath") && target.get("sourcepath") == null) 
//					{
//						log.error("Remove ${data.sourcepath}/${filename} from entities");
//						sourcemask = null; 
//					}
					if( sourcemask == null)
					{
						if(searcher.getDetail("sourcepath") != null )
						{
							Data entitmodule = archive.getCachedData("module",moduleid);
							Category cat = archive.getEntityManager().loadDefaultFolder(entitmodule,target, inReq.getUser());
							if( cat != null)
							{
								sourcepath = cat.getCategoryPath() + "/" + item.getName();
							}
						}
					}
					if( sourcepath == null && sourcemask != null)
					{
						//String sourcepath = inReq.getRequestParameter(detailid + ".sourcepath"); //Is this risky?
						
						Map variables = inReq.getParameterMap();
						variables.put("userid", inReq.getUser().getId());
						variables.put("id", id);
						variables.put("filename", item.getName());
						if (target != null)
						{
							variables.put("data", target);
						}

						sourcepath = getAssetImporter().getAssetUtilities().createSourcePathFromMask(archive, null, inReq.getUser(), item.getName(), sourcemask, variables);
						if( sourcepath.endsWith("/"))
						{
							sourcepath = sourcepath + item.getName();
						}
					}
				} else {
					Data entitmodule = archive.getCachedData("module",moduleid);
					Category cat = archive.getEntityManager().loadDefaultFolder(entitmodule,target, inReq.getUser());
					if( cat != null)
					{
						sourcepath = cat.getCategoryPath() + "/chat/" +  inReq.getUserName() + "/" + item.getName();
					}
				}
				
			}
			String path = "";
			if (sourcepath != null)
			{
				path = "/WEB-INF/data/" + archive.getCatalogId() + "/originals/" + sourcepath;
			}
			else 
			{
				sourcepath = getAssetImporter().getAssetUtilities().createSourcePath(inReq, archive, item.getName()); 
				//Todo: Remove the iten.getName
				path = "/WEB-INF/data/" + archive.getCatalogId() + "/originals/" + sourcepath + "/" + item.getName();
			}
			
			sourcepath = sourcepath.replace("//", "/"); //in case of missing data
			path = path.replace("//", "/");

			Page originalfile = archive.getPageManager().getPage(path);
			if( originalfile.exists() )
			{
				Asset existingasset = archive.getAssetBySourcePath(sourcepath);
				if( existingasset != null)
				{
					ContentItem preview = archive.getPresetManager().outPutForGenerated(archive, existingasset, "image3000x3000");
					archive.getAssetEditor().backUpFilesForLastVersion(existingasset,originalfile.getContentItem(),preview );
				}
			}
			properties.saveFileAs(item, path, inReq.getUser());

			boolean assigncategory = archive.isCatalogSettingTrue("assigncategoryonupload");

			String assigncategoryval  = inReq.findValue("assigncategory");
			if( assigncategoryval != null)
			{
				assigncategory = Boolean.parseBoolean(assigncategoryval);
			}

			//MediaArchive inArchive, User inUser, Page inAssetPage)
			Asset current = archive.getAssetBySourcePath(sourcepath);
			//log.info(current.getId());
			//This will create a new one if current was null.
			current = getAssetImporter().getAssetUtilities().populateAsset(null, item.getSavedPage().getContentItem(), archive, sourcepath, inReq.getUser());
			
			
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
			archive.saveAsset(current, inReq.getUser());
			log.info("Asset saved: " + current.getSourcePath());
			
			archive.getAssetEditor().createNewVersionData(current, originalfile.getContentItem(), inReq.getUserName(), Version.UPLOADED, null);
			
			inReq.putPageValue("newasset", current);
			if( target != null)
			{
				target.setValue(detailid,current.getId());
				inReq.setRequestParameter(detailid + ".value", current.getId());
				searcher.saveData(target, inReq.getUser());
				
			}
			
			savedassets.add(current);
			
			archive.fireMediaEvent("importing", "assetuploaded", inReq.getUser(), current); 
			
			archive.fireMediaEvent("assetcreated", inReq.getUser(), current);
			
			Boolean delayimport = Boolean.parseBoolean(inReq.findActionValue("delayimport"));
			if(!delayimport) 
			{
				archive.fireSharedMediaEvent("importing/assetscreated");  //Kicks off an async saving
			}
			
			//archive.fireSharedMediaEvent("importing/importassets");  //Non blocking
			

			/*
			//--who is using this?
			final String currentcollection = inReq.getRequestParameter("collectionid");

			if (currentcollection != null)
			{
				ProjectManager manager = (ProjectManager) getModuleManager().getBean(archive.getCatalogId(), "projectManager");
				manager.addAssetToCollection(archive, currentcollection, current);
			}
			//--
			*/
		}
		inReq.putPageValue("savedassets", savedassets);
		inReq.putPageValue("hits", savedassets);  //Some evets requires hits variable

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

	public void addAssetsToCategory(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String targetcategoryid = inReq.getRequestParameter("targetcategoryid");
		Category targetparent = archive.getCategory(targetcategoryid);
		String[] assetids = inReq.getRequestParameters("assetid");
		if( assetids != null)
		{
			Collection tosave = new ArrayList();

			for (int i = 0; i < assetids.length; i++)
			{
				Asset asset = archive.getAsset(assetids[i]);
				asset.addCategory(targetparent);
				tosave.add(asset);
			}
			archive.saveAssets(tosave);
		}

	}
}
