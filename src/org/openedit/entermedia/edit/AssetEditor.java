package org.openedit.entermedia.edit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.openedit.entermedia.Asset;
import org.openedit.entermedia.Category;
import org.openedit.entermedia.MediaArchive;

import com.openedit.OpenEditRuntimeException;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.users.User;

public class AssetEditor {
	protected MediaArchive fieldMediaArchive;
	protected Asset fieldCurrentAsset;
	protected PageManager fieldPageManager;
	
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
		 String id = getMediaArchive().getAssetArchive().nextAssetNumber();
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
		 String id = getMediaArchive().getAssetArchive().nextAssetNumber();
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
}
