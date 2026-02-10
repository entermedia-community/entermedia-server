package org.entermediadb.asset;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.openedit.MultiValued;
import org.openedit.data.DataLoaded;
import org.openedit.data.PropertyDetails;
import org.openedit.data.RecordStatusEnabled;
import org.openedit.data.SaveableData;
import org.openedit.data.SearchDataEnabled;
import org.openedit.users.User;

public interface Asset  extends MultiValued, SaveableData, SearchDataEnabled, RecordStatusEnabled, DataLoaded
{


	boolean isLocked();

	User getLockOwner();

	boolean isDeleted();

	boolean isFolder();

	void setFolder(boolean inIsFolder);

	MediaArchive getMediaArchive();

	void setMediaArchive(MediaArchive inMediaArchive);

	int getOrdering();

	void setOrdering(int inOrdering);

	/**
	 * This is an optional field
	 * 
	 * @return
	 */

	String getShortDescription();

	void setShortDescription(String inDescription);

	String toString(String inLocale);


	void removeProperty(String inKey);

	void addCategory(Category inCatid);

	void removeCategory(Category inCategory);

	Set buildCategorySet();

	Collection<Category> getCategories();

	/**
	 * @deprecated
	 * @param inCategory
	 * @return
	 */
	boolean isInCatalog(Category inCategory);

	boolean isInCategory(Category inCat);

	boolean isInCategory(String inCategoryId);

	Collection getCollections();

	//Collection getLibraries();

	void clearCategories();

	boolean hasProperty(String inKey);

	void addKeyword(String inString);

	void addKeywords(String inString);

	Collection<String> getKeywords();

	void removeKeyword(String inKey);

	Date getDate(String inField, String inDateFormat);

	boolean isRelated(Asset inAsset);

	void setKeywords(Collection<String> inKeywords);

	void clearKeywords();

	void incrementProperty(String property, int delta) throws Exception;

	boolean hasRelatedAssets();

	Asset copy(String newId);

	Asset copy();

	void setCategories(Collection<Category> inCatalogs);

	void setOriginalImagePath(String inPath);

	String getSaveAsName();

	void addRelatedAsset(RelatedAsset inRelationship);

	Collection getRelatedAssets();

	void removeRelatedAsset(String inCatalogId, String inAssetId);

	List getRelatedAssets(String inType);

	void clearRelatedAssets();

	void setRelatedAssets(Collection inRelatedAssets);

	String getMediaName();

	String getPrimaryFile();

	void setPrimaryFile(String inPath);

	String getCatalogId();

	String getFileFormat();

	String getDetectedFileFormat();

	String getAttachmentByType(String inType);

	void setAttachmentFileByType(String inType, String inName);

	boolean hasKeywords();

	Category getDefaultCategory();

	BigDecimal getBigDecimal(String inKey);

	float getUploadPercentage();

	boolean isPropertyTrue(String inKey);

	void setValue(String inKey, Object inValue);

	void setTagsValue(String inKey, Object inValue);

	PropertyDetails getPropertyDetails();

	String getProperty(String inProperty);

	String getPath();

	void removeChildCategory(Category inCatParent);

	boolean isEquals(long filemmod);

	boolean clearParentCategories();

	void toggleLock(User inUser);


}