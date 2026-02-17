package org.entermediadb.asset.edit;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.BaseAsset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.comments.CommentArchive;
import org.openedit.OpenEditException;
import org.openedit.OpenEditRuntimeException;
import org.openedit.data.ValuesMap;
import org.openedit.hittracker.HitTracker;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;
import org.openedit.repository.ContentItem;
import org.openedit.repository.filesystem.FileItem;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.PathUtilities;
import org.openedit.util.XmlUtil;

public class AssetEditor
{
	protected XmlUtil fieldXmlUtil;
	protected MediaArchive fieldMediaArchive;
	//protected Asset fieldCurrentAsset;
	protected PageManager fieldPageManager;
	protected CommentArchive fieldCommentArchive;

	private static final Log log = LogFactory.getLog(AssetEditor.class);

	public Asset createAsset()
	{
		return new BaseAsset(getMediaArchive());
	}

	public void addToCategory(Asset inAsset, Category inCategory) throws OpenEditRuntimeException
	{
		inAsset.addCategory(inCategory);
	}

	public void deleteAsset(Asset inAsset, User inUser) throws OpenEditRuntimeException
	{
		getMediaArchive().removeGeneratedImages(inAsset, true);
		//Not needed getMediaArchive().getAssetArchive().deleteAsset(inAsset);
		getMediaArchive().getAssetSearcher().delete(inAsset,inUser);

		//		 if (getCurrentAsset() != null && inAsset.getId().equals(getCurrentAsset().getId()))
		//		 {
		//			 setCurrentAsset(null);
		//		 }
	}
	
	
	
	public void deleteAsset(Asset asset, HitTracker inTracker, boolean inDeleteOriginal, User inUser)
	{
		if (inTracker != null)
		{
			inTracker.removeSelection(asset.getId());
		}
		getMediaArchive().fireMediaEvent("deleting", inUser, asset);
		deleteAsset(asset,inUser);
		if (inDeleteOriginal)
		{
			getMediaArchive().getAssetManager().removeOriginal(inUser, asset);
		}
		
		getMediaArchive().fireMediaEvent("deleted", inUser, asset);
		log.info("Asset Deleted - assetid " + asset.getId() + " - user " + inUser + " - sourcepath: " + asset.getSourcePath() + " original: " + inDeleteOriginal);
	}


	public Asset getAsset(String inAssetId) throws OpenEditRuntimeException
	{
		Asset prod = getMediaArchive().getAsset(inAssetId);
		if (prod == null)
		{
			return null;
		}
		return prod;
	}

	//	 public Asset getCurrentAsset()
	//	 {
	//		 return fieldCurrentAsset;
	//	 }
	//
	//	 public void setCurrentAsset(Asset inCurrentAsset)
	//	 {
	//		 fieldCurrentAsset = inCurrentAsset;
	//	 }

	public Asset createAssetWithDefaults() throws OpenEditRuntimeException
	{
		Asset asset = createAsset();
		String id = getMediaArchive().getAssetSearcher().nextAssetNumber();
		asset.setId(id);
		Category cat = getMediaArchive().getCategoryArchive().getCategory("index");
		if (cat != null)
		{
			asset.addCategory(cat);
		}
		return asset;
	}

	public Asset createAssetWithDefaults(Asset inTemplateAsset, String newId) throws OpenEditRuntimeException
	{

		Asset asset = createAsset();
		String id = getMediaArchive().getAssetSearcher().nextAssetNumber();
		asset.setId(id);
		asset.setName(inTemplateAsset.getName());
		return asset;
	}

	public Asset copyAsset(Asset inAsset, String inId)
	{
		Asset asset = null;
		if (inAsset != null)
		{
			asset = new BaseAsset(inAsset.getMediaArchive());
			//asset.setCatalogId(inAsset.getCatalogId());
			asset.setName(inAsset.getName());
			if (asset.getKeywords().size() > 0)
			{
				asset.getKeywords().addAll(inAsset.getKeywords());
			}
			asset.setProperties(new ValuesMap(inAsset.getProperties()));
			asset.setId(inId);
		}
		return asset;
	}

	public Asset copyAsset(Asset inAsset, String inId, String inSourcePath)
	{
		Asset asset = copyAsset(inAsset, inId);
		asset.setSourcePath(inSourcePath);
		return asset;
	}

	public MediaArchive getMediaArchive()
	{
		return fieldMediaArchive;
	}

	public void setMediaArchive(MediaArchive mediaArchive)
	{
		fieldMediaArchive = mediaArchive;
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager pageManager)
	{
		fieldPageManager = pageManager;
	}

	public CommentArchive getCommentArchive()
	{
		return fieldCommentArchive;
	}

	public void setCommentArchive(CommentArchive commentArchive)
	{
		fieldCommentArchive = commentArchive;
	}

	public boolean makeFolderAsset(Asset inAsset, User inUser)
	{
		String oldSourcePath = inAsset.getSourcePath();
		PageManager pageManager = getPageManager();
		// need to figure out newsourcepath
		String newSourcePath = oldSourcePath;
		if (!newSourcePath.endsWith("/"))
		{
			newSourcePath = newSourcePath + "/";
		}
		//		inAsset.setSourcePath(newSourcePath);
		String dataRoot = "/WEB-INF/data/" + getMediaArchive().getCatalogId();

		// Move Comments
		// move comments from
		// 1 - catalog/data/comments/oldsourcepath
		// 2 - web-inf/data/comments/oldsourcepath
		// to: web.inf/data/comments/newsourcepath
		CommentArchive carchive = getCommentArchive();
		Collection allcomments = carchive.loadComments(getMediaArchive().getCatalogId(), oldSourcePath);
		allcomments.addAll(carchive.loadComments(getMediaArchive().getCatalogId(), oldSourcePath));
		carchive.saveComments("/WEB-INF/data/" + getMediaArchive().getCatalogId() + "/comments/" + newSourcePath, allcomments);

		// Move Originals
		Page oldAssets = pageManager.getPage(dataRoot + "/originals/" + oldSourcePath);
		Page newAssets = pageManager.getPage(dataRoot + "/originals/" + newSourcePath + oldAssets.getName());
		try
		{
			if (oldAssets.exists())
			{
				Page tempLocation = pageManager.getPage(oldAssets.getPath() + ".tmp");
				pageManager.movePage(oldAssets, tempLocation);
				pageManager.movePage(tempLocation, newAssets);
				//Set new assetmodificationdate time?
				if( newAssets.getContentItem() instanceof FileItem)
				{
					long lastmod = newAssets.getContentItem().getLastModified();
					lastmod = lastmod + 2000; //Change this a little so the DB notices the change for pull
					((FileItem)newAssets.getContentItem()).getFile().setLastModified(lastmod);
					inAsset.setValue("assetmodificationdate", new Date(lastmod));
				}
			}
			else
			{
				Page folder = pageManager.getPage(dataRoot + "/originals/" + newSourcePath);
				String path = folder.getContentItem().getAbsolutePath();
				new File(path).mkdirs();
			}
			Page folder = pageManager.getPage(dataRoot + "/originals/" + newSourcePath);
			if (!folder.exists())
			{
				throw new OpenEditException("Could not attach, originals folder may be read only");
			}
		}
		finally
		{
			inAsset.setPrimaryFile(oldAssets.getName());
		}
		inAsset.setFolder(true);
		
		//inAsset.setValue("assetmodificationdate", new Date());
		
		
		getMediaArchive().saveAsset(inAsset, inUser);

		//Save a file here for future use
		Page attachments = pageManager.getPage(dataRoot + "/originals/" + newSourcePath + "/attachments.txt");
		if (!attachments.exists())
		{
			pageManager.saveContent(attachments, inUser, "EnterMedia attachment holder", "empty");
		}
		// Don't do this if no changes were made otherwise the product gets
		// deleted!
		/*
		 * We dont use .xconf to store data files any more if
		 * (!oldSourcePath.equals(newSourcePath)) { // Remove old asset file
		 * File oldFile = new File(getMediaArchive().getRootDirectory(),
		 * "assets/" + oldSourcePath + ".xconf"); if (oldFile.exists()) { if
		 * (oldFile.delete()) { return true; } else {
		 * log.error("Could not delete parent folder."); } } else {
		 * log.error("Could not remove old product file: " +
		 * oldFile.getAbsolutePath()); } }
		 */
		//getMediaArchive().
		return true;
	}

	public Asset copyAsset(MediaArchive inArchive, Asset inAsset, String inDestSourcePath)
	{
		Asset newasset = inArchive.createAsset(inDestSourcePath);

		if (inAsset.isFolder() && !inDestSourcePath.endsWith("/"))
		{
			inDestSourcePath = inDestSourcePath + "/";
		}
		
		ValuesMap originaldata = new ValuesMap(inAsset.getProperties());
		//Copy everything but id and sourcepath
		originaldata.remove("id");
		originaldata.remove("sourcepath");
		newasset.setProperties(originaldata);
		newasset.setCategories(new ArrayList(inAsset.getCategories()));
		
		newasset.setSourcePath(inDestSourcePath);
		newasset.setFolder(inAsset.isFolder());

		//Copy any images or folders using OE File Manager
		String oldSourcePath = inAsset.getSourcePath(); 
		String oldpath = "/WEB-INF/data/" + inArchive.getCatalogId() + "/originals/" + oldSourcePath; //If its a folder if will include children
		String newpath = "/WEB-INF/data/" + inArchive.getCatalogId() + "/originals/" + inDestSourcePath;

		Page oldpage = inArchive.getPageManager().getPage(oldpath);
		Page newpage = inArchive.getPageManager().getPage(newpath);
		if (oldpage.exists())
		{
			inArchive.getPageManager().copyPage(oldpage, newpage);
		}

		Page oldthumbs = inArchive.getPageManager().getPage("/WEB-INF/data/" + inArchive.getCatalogId() + "/generated/" + inAsset.getSourcePath());
		if (!inDestSourcePath.endsWith("/"))
		{
			inDestSourcePath = inDestSourcePath + "/";
		}
		Page newthumbs = inArchive.getPageManager().getPage("/WEB-INF/data/" + inArchive.getCatalogId() + "/generated/" + inDestSourcePath);
		if (oldthumbs.exists())
		{
			inArchive.getPageManager().copyPage(oldthumbs, newthumbs);
		}
		return newasset;
	}

	public void fullyRemoveAsset(Asset inAsset, User inUser, boolean inKeepReleases)
	{
		//remove the releases if necessary
		if (!inKeepReleases)
		{
			Page releases = getPageManager().getPage("/WEB-INF/data/" + inAsset.getCatalogId() + "/releases/" + inAsset.getSourcePath());
			if (releases.exists())
			{
				getPageManager().removePage(releases);
			}
		}
		//remove the originals folder
		Page originals = getPageManager().getPage("/WEB-INF/data/" + inAsset.getCatalogId() + "/originals/" + inAsset.getSourcePath());
		if (originals.exists())
		{
			getPageManager().removePage(originals);
		}
		getMediaArchive().removeGeneratedImages(inAsset);

		//remove record
		deleteAsset(inAsset, inUser);

		//now let's get rid of everything
		Page data = getPageManager().getPage("/WEB-INF/data/" + inAsset.getCatalogId() + "/assets/" + inAsset.getSourcePath());
		if (data.exists())
		{
			getPageManager().removePage(data);
		}
		Page xconf = getPageManager().getPage(inAsset.getCatalogId() + "/assets/" + inAsset.getSourcePath());
		if (xconf.exists())
		{
			getPageManager().removePage(xconf);
		}
	}

	public Element createNewVersionData(Asset inCurrent, ContentItem inTosavePath, String inUserName, String inChangeType, String inUserMessage)
	{
		ContentItem inXmlFile = getVersionDataFile(inTosavePath.getPath());
        Document document = null;
		if ( inXmlFile.exists() )
		{
	        document = getXmlUtil().getXml(inXmlFile, "UTF-8").getDocument();
		}
		else
		{
			document = DocumentHelper.createDocument();
			Element root = document.addElement("versions");
		}
        Element version = document.getRootElement().addElement("version");
        
        int num = maxVersionNumber(inTosavePath.getPath());
        num++;
        
        version.addAttribute("number",Integer.toString( num )  );
        version.addAttribute("date",DateStorageUtil.getStorageUtil().getTodayForStorage());
       	version.addAttribute("author",inUserName);
        version.addAttribute("type",inChangeType);
        version.addAttribute("filesize", String.valueOf( inTosavePath.getLength()) );
        if( inUserMessage != null)
        {
        	version.setText(inUserMessage);
        }
        getXmlUtil().saveXml(document, inXmlFile.getOutputStream(), "UTF-8" );
        return version;
		//Dont copy thumnai or image just yet
	}
	
	public void backUpFilesForLastVersion(Asset inCurrent, ContentItem inPreviousFile, ContentItem inPreviewImage)
	{
		Element previous = null;
		ContentItem inXmlFile = getVersionDataFile(inPreviousFile.getPath());
		if ( inXmlFile.exists() )
		{
	        Element root = getXmlUtil().getXml(inXmlFile, "UTF-8");
	        //look for the last version
	        List<Element> elements = root.elements("version");
	        if( !elements.isEmpty() )
	        {
	        	previous = elements.get(elements.size() - 1);
	        }
		}
		if(  previous == null)
		{
			previous = createNewVersionData(inCurrent,inPreviousFile,inCurrent.get("owner"),Version.IMPORTED,null);
		}
    	String versionnum = previous.attributeValue("number");

    	String originalcopy = PathUtilities.extractDirectoryPath( inXmlFile.getPath() ) + "/" + versionnum + "~" + inPreviousFile.getName();
    	ContentItem destination = getPageManager().getRepository().getStub(originalcopy); 
		if ( !destination.exists() )
		{
			getPageManager().getRepository().move( inPreviousFile, destination ); //Move it?
			previous.addAttribute("filesize", String.valueOf( destination.getLength()) );
		}
		//Now the preview
		if( inPreviewImage != null && inPreviewImage.exists() )
		{
			String type = PathUtilities.extractPageType(inPreviewImage.getName());
        	String previewcopypath = PathUtilities.extractDirectoryPath( inXmlFile.getPath() ) + "/" + versionnum + "~"  + inPreviousFile.getName() + "preview." + type;
        	ContentItem previewdest = getPageManager().getRepository().getStub(previewcopypath); 
			getPageManager().getRepository().move( inPreviewImage, previewdest );
			previous.addAttribute("previewfilename", previewdest.getName());
		}
        getXmlUtil().saveXml(previous.getDocument(), inXmlFile.getOutputStream(), "UTF-8" );
	}
	
	public void replaceOrginal(Asset current, ContentItem inNewContent, String inEditedBy, String inMessage)
	{
		MediaArchive archive = getMediaArchive();

		ContentItem original = getMediaArchive().getOriginalContent(current);
		ContentItem preview = getMediaArchive().getPresetManager().outPutForGenerated(archive, current, "image3000x3000");
		backUpFilesForLastVersion(current,original,preview );
		//Read New Metadata

		getPageManager().getRepository().put(inNewContent); //Or move?
		
		getMediaArchive().getAssetImporter().getAssetUtilities().getMetaDataReader().populateAsset(archive, inNewContent, current );
		getMediaArchive().saveAsset(current);
		getMediaArchive().removeGeneratedImages(current, true);
		
		createNewVersionData(current,original,inEditedBy, Version.UIREPLACE, null );

		reloadThumbnails( current);
		 log.info("Original replaced: " + current.getId() + " Sourcepath: " + original.getPath());
	}
	
	public void restoreVersion(Asset current, String inEditUser, String inVersion)  
	{

		Version version = getVersion(current,inVersion);
		if( version.getBackUpPath() == null )
		{
			throw new OpenEditException("Must have backup");
		}
		ContentItem backup = getMediaArchive().getContent(version.getBackUpPath());
		if( !backup.exists() )
		{
			log.error("No such backup: " + backup.getPath() );
			return;
		}
		ContentItem original = getMediaArchive().getOriginalContent(current);
		//Backup the old asset
		ContentItem preview = getMediaArchive().getPresetManager().outPutForGenerated(getMediaArchive(), current, "image3000x3000");
		backUpFilesForLastVersion(current,original,preview );
		
		getPageManager().getRepository().copy( backup, original);
	        		
		//Version the new one
		getMediaArchive().getAssetImporter().getAssetUtilities().getMetaDataReader().populateAsset(getMediaArchive(), original, current );
		getMediaArchive().saveAsset(current);
		getMediaArchive().removeGeneratedImages(current, true);
		createNewVersionData(current,original,inEditUser, Version.RESTORE, null );

		reloadThumbnails( current);
	}

	public void reloadThumbnails( Asset inAsset)
	{
		//inReq.putPageValue("asset", inAsset);
		MediaArchive archive = getMediaArchive();
		HitTracker conversions = archive.query("conversiontask").exact("assetid", inAsset.getId()).search(); //This is slow, we should load up a bunch at once
		archive.getSearcher("conversiontask").deleteAll(conversions, null);
		archive.getPresetManager().queueConversions(archive, archive.getSearcher("conversiontask"), inAsset, true);
		
		//archive.getPresetManager().queueConversions(archive,archive.getSearcher("conversiontask"),current,true);
		//current.setProperty("importstatus", "imported");
		//archive.fireMediaEvent("importing/assetsimported", inReq.getUser());
		//archive.fireMediaEvent("conversions/thumbnailreplaced", inReq.getUser(), current);
		
		//Good idea?
		//archive.fireMediaEvent("conversions","runconversion", inReq.getUser(), inAsset);//block?
		//archive.fireMediaEvent("asset/saved", inReq.getUser(), current);
		archive.fireSharedMediaEvent("conversions/runconversions");
		//archive.saveAsset(current);
	}
	
	//protected SAXReader fieldReader;
	/**
	 * @param inPath
	 * @param version
	 * @return
	 */
	protected Version createRevision(String inPath, Element element) 
	{
		//	<version number="1" author="admin" date="2323525233" type="added">Some message</version>
		Version version = new Version();
		version.setEditUser(element.attributeValue("author"));
		version.setVersion(Integer.parseInt( element.attributeValue("number") ) );
		version.setPreviewFileName(element.attributeValue("previewfilename"));
		String size = element.attributeValue("filesize");
		if( size != null)
		{
			version.setFileSize( Long.parseLong( size ));
		}

		String comment = element.getTextTrim();
		version.setUserMessage(comment);
		version.setChangeType(element.attributeValue("type"));
		
		String filename = PathUtilities.extractFileName(inPath);
		
		String basefolder = PathUtilities.extractDirectoryPath(inPath);
		String backUpPath = basefolder  + "/.versions/" + version.getVersion() + "~" + filename;
		version.setBackUpPath(backUpPath);
		
		if( version.getPreviewFileName() != null )
		{
			String previewBackUpPath = basefolder + "/.versions/" + version.getPreviewFileName();
			version.setPreviewBackUpPath(previewBackUpPath);
		}
		
		String modtime = element.attributeValue("date");
		if( modtime != null)
		{
			if( modtime.contains(" ") )
			{
				version.setEditDate(DateStorageUtil.getStorageUtil().parseFromStorage(modtime));
			}
			else
			{
				version.setEditDate(new Date(Long.parseLong(modtime)));
			}
		}
		return version;
	}

	public Version getVersion(Asset inAsset, String inVersion)  
	{
		if( inVersion == null)
		{
			return null;
		}
		List<Version> all = getVersions(inAsset);
		int inV = Integer.parseInt(inVersion);
		for (Iterator iterator = all.iterator(); iterator.hasNext();)
		{
			Version version = (Version) iterator.next();
			if( version.getVersion() == inV)
			{
				return version;
			}	
		}
		return null;
	}
	
	public List<Version> getVersions(Asset inAsset) 
	{
		ContentItem item = getMediaArchive().getOriginalContent(inAsset);
		List<Version> vers = getVersions(item.getPath());
		
		
		//We should always add ourself if we have an asset
		if( vers.isEmpty() )
		{
			Version originalversion = new Version();
			originalversion.setEditUser(inAsset.get("owner"));
			originalversion.setEditDate(inAsset.getDate("assetaddeddate"));
			originalversion.setChangeType(Version.IMPORTED);
			originalversion.setVersion(1);
			originalversion.setFileSize( item.getLength() );
			vers = new ArrayList();
			vers.add(originalversion);
		}
		Collections.reverse(vers);
		return vers;
	}
	public List<Version> getVersions(String inPath) 
	{
		ContentItem inXmlFile = getVersionDataFile(inPath);
		if ( !inXmlFile.exists() )
		{
			return Collections.EMPTY_LIST;
		}
		List<Version> versions = new ArrayList();

        Element root = getXmlUtil().getXml(inXmlFile, "UTF-8");
        for (Iterator iterator = root.elementIterator("version"); iterator.hasNext();)
		{
			Element element = (Element) iterator.next();
			Version next = createRevision(inPath, element);
			versions.add(next);
		}
		return versions;
	}

	public XmlUtil getXmlUtil()
	{
		if( fieldXmlUtil == null)
		{
			fieldXmlUtil = new XmlUtil();
		}
		return fieldXmlUtil;
	}
	public void setXmlUtil(XmlUtil inXmlUtil)
	{
		fieldXmlUtil = inXmlUtil;
	}


protected int maxVersionNumber( String  inSaving )
{
	String inPath = PathUtilities.extractDirectoryPath(inSaving);
	Collection<String> children = getPageManager().getChildrenPaths(inPath + "/.versions/"); 

	int maxVersionNum = 0;
	if( children.isEmpty() )
	{
		return 0;
	}
	String filename = PathUtilities.extractFileName(inSaving);
	for (Iterator iterator = children.iterator(); iterator.hasNext();)
	{
		String version = (String)iterator.next();
		if( version.endsWith(filename) )
		{
			String verfilename = PathUtilities.extractFileName(version);
			int versionNum = extractVersionNumberFromFilename( verfilename );
			maxVersionNum = Math.max( maxVersionNum, versionNum );
		}
	}
	return maxVersionNum;
}

protected int extractVersionNumberFromFilename( String inVersionPath )
{
	int separator = inVersionPath.indexOf( '~' );
	if (separator >= 0 )
	{
		try
		{
			//.versions/1~test.jpg
			String name = PathUtilities.extractFileName(inVersionPath);
			separator = inVersionPath.indexOf( '~' );
			if( separator == -1)
			{
				return 0;
			}
			return Integer.parseInt( name.substring( 0, separator ) );
		}
		catch( NumberFormatException e )
		{
			log.error("Should not get errors " + e);
		}
		return 0;
	}
	else
	{
		return 0;
	}
}	

protected ContentItem getVersionDataFile( String inOriginalPath )
{
	String folder = PathUtilities.extractDirectoryPath(inOriginalPath);
	String name = PathUtilities.extractFileName(inOriginalPath);
	String abspath = folder + "/.versions/" + name + ".metadata.xml"; 
	
	//Page item = getPageManager().getPage(abspath,false); //Mount points?
	//File metadata = new File( item.getContentItem().getAbsolutePath() );
	
	ContentItem item = getPageManager().getRepository().getStub(abspath);
	
	return item;
}


}
