package org.openedit.entermedia.edit;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.Category;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.modules.CategoryEditModule;

import com.openedit.OpenEditException;
import com.openedit.OpenEditRuntimeException;
import com.openedit.comments.CommentArchive;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.users.User;

public class AssetEditor {
	protected MediaArchive fieldMediaArchive;
	protected Asset fieldCurrentAsset;
	protected PageManager fieldPageManager;
	protected CommentArchive fieldCommentArchive;
	
	private static final Log log = LogFactory.getLog(AssetEditor.class);
	
	public Asset createAsset()
	 {
		 return new Asset();
	 }

	 public void addToCategory(Asset inAsset, Category inCategory) throws OpenEditRuntimeException
	 {
		 inAsset.addCategory(inCategory);
	 }

	 public void deleteAsset(Asset inAsset) throws OpenEditRuntimeException
	 {
		 getMediaArchive().removeGeneratedImages(inAsset);
		 getMediaArchive().getAssetArchive().deleteAsset(inAsset);
		 getMediaArchive().getAssetSearcher().deleteFromIndex(inAsset);

		 if (getCurrentAsset() != null && inAsset.getId().equals(getCurrentAsset().getId()))
		 {
			 setCurrentAsset(null);
		 }
	 }

	 public Asset getAsset(String inAssetId) throws OpenEditRuntimeException
	 {
		 Asset prod = getMediaArchive().getAsset(inAssetId);
		 if ( prod == null)
		 {
			 return null;
		 }
		 return prod;
	 }

	 public Asset getCurrentAsset()
	 {
		 return fieldCurrentAsset;
	 }

	 public void setCurrentAsset(Asset inCurrentAsset)
	 {
		 fieldCurrentAsset = inCurrentAsset;
	 }

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
			 asset = new Asset();
			 asset.setId(inId);
			 asset.setCatalogId(inAsset.getCatalogId());
			 asset.setName(inAsset.getName());
			 asset.getKeywords().addAll(inAsset.getKeywords());
			 asset.setProperties(new HashMap(inAsset.getProperties()));
		 }
		 return asset;
	 }	

	 public Asset copyAsset(Asset inAsset, String inId, String inSourcePath) 
	 {
		 Asset asset = copyAsset(inAsset, inId);
		 asset.setSourcePath(inSourcePath);
		 return asset;
	 }

	 
	public MediaArchive getMediaArchive() {
		return fieldMediaArchive;
	}

	public void setMediaArchive(MediaArchive mediaArchive) {
		fieldMediaArchive = mediaArchive;
	}

	public PageManager getPageManager() {
		return fieldPageManager;
	}

	public void setPageManager(PageManager pageManager) {
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
	
	public boolean makeFolderAsset(Asset inAsset, User inUser) {
		String oldSourcePath = inAsset.getSourcePath();
		PageManager pageManager = getPageManager();
		// need to figure out newsourcepath
		String newSourcePath = oldSourcePath;
		if( !newSourcePath.endsWith("/"))
		{
			newSourcePath = newSourcePath + "/";
		}
		inAsset.setSourcePath(newSourcePath);
		String dataRoot = "/WEB-INF/data/" + getMediaArchive().getCatalogId();

		// Move Comments
		// move comments from
		// 1 - catalog/data/comments/oldsourcepath
		// 2 - web-inf/data/comments/oldsourcepath
		// to: web.inf/data/comments/newsourcepath
		CommentArchive carchive = getCommentArchive();
		Collection allcomments = carchive.loadComments(getMediaArchive().getCatalogId(),  oldSourcePath);
		allcomments.addAll(carchive.loadComments( getMediaArchive().getCatalogId() , oldSourcePath));
		carchive.saveComments("/WEB-INF/data/" + getMediaArchive().getCatalogId() + "/comments/" + newSourcePath, allcomments);

		// Move Originals
		Page oldAssets = pageManager.getPage(dataRoot + "/originals/" + oldSourcePath);
		Page newAssets = pageManager.getPage(dataRoot + "/originals/" + newSourcePath + oldAssets.getName());
		try {
			if (oldAssets.exists()) {
				Page tempLocation = pageManager.getPage(oldAssets.getPath() + ".tmp");
				pageManager.movePage(oldAssets, tempLocation);
				pageManager.movePage(tempLocation, newAssets);
			}
			else
			{
				Page folder = pageManager.getPage(dataRoot + "/originals/" + newSourcePath );
				String path = folder.getContentItem().getAbsolutePath();
				new File(path).mkdirs();
			}
			Page folder = pageManager.getPage(dataRoot + "/originals/" + newSourcePath );
			if( !folder.exists() )
			{
				throw new OpenEditException("Could not attach, originals folder may be read only");
			}
		} finally {
			inAsset.setProperty("primaryfile", oldAssets.getName());
		}
		inAsset.setFolder(true);
		getMediaArchive().saveAsset(inAsset, inUser);
		getMediaArchive().getAssetArchive().clearAssets();

		// Don't do this if no changes were made otherwise the product gets
		// deleted!
		if (!oldSourcePath.equals(newSourcePath)) {
			// Remove old asset file
			File oldFile = new File(getMediaArchive().getRootDirectory(), "assets/"
					+ oldSourcePath + ".xconf");
			if (oldFile.exists()) {
				if (oldFile.delete()) {
					return true;
				} else {
					log.error("Could not delete parent folder.");
				}
			} else {
				log.error("Could not remove old product file: "
						+ oldFile.getAbsolutePath());
			}
		}
		return true;
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
		if(originals.exists())
		{
			getPageManager().removePage(originals);
		}
		getMediaArchive().removeGeneratedImages(inAsset);
		
		//remove record
		deleteAsset(inAsset);
		
		//now let's get rid of everything
		Page data = getPageManager().getPage("/WEB-INF/data/" + inAsset.getCatalogId() + "/assets/" + inAsset.getSourcePath());
		if(data.exists())
		{
			getPageManager().removePage(data);
		}
		Page xconf = getPageManager().getPage(inAsset.getCatalogId() + "/assets/" + inAsset.getSourcePath());
		if(xconf.exists())
		{
			getPageManager().removePage(xconf);
		}
	}
}
