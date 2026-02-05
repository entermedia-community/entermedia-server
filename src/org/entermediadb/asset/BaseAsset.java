/*
 * Created on Mar 2, 2004
 */
package org.entermediadb.asset;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.elasticsearch.SearchHitData;
import org.entermediadb.projects.LibraryCollection;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.MultiValued;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.SaveableData;
import org.openedit.modules.translations.LanguageMap;
import org.openedit.users.User;
import org.openedit.util.PathUtilities;

/**
 * @author cburkey
 * 
 */
public class BaseAsset extends SearchHitData implements MultiValued, SaveableData, Asset
{
	private static final Log log = LogFactory.getLog(Asset.class);

	protected MediaArchive fieldMediaArchive;
	// be shown in a list
	protected Collection fieldRelatedAssets;

	public BaseAsset()
	{
	}
	
	
	@Override
	public boolean isLocked() {
		if(get("lockedby") != null && get("lockedby").length() >0) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public User getLockOwner() {
		if(isLocked()) {
			return getMediaArchive().getUser(get("lockedby"));
		}
		return null;
	}

	@Override
	public boolean isDeleted()
	{
		String editstatus = get("editstatus");
		if( "7".equals( editstatus) )
		{
			return true;
		}
		return false;
	}
	
	@Override
	public boolean isFolder()
	{
		return getBoolean("isfolder");
	}

	@Override
	public void setFolder(boolean inIsFolder)
	{
		setProperty("isfolder", String.valueOf(inIsFolder));
	}

	public BaseAsset(MediaArchive inMediaArchive)
	{
		super(inMediaArchive.getAssetSearcher());
		setMediaArchive(inMediaArchive);
	}

	@Override
	public MediaArchive getMediaArchive()
	{
		return fieldMediaArchive;
	}

	@Override
	public void setMediaArchive(MediaArchive inMediaArchive)
	{
		fieldMediaArchive = inMediaArchive;
	}

	@Override
	public int getOrdering()
	{
		return (int) getValue("ordering");
	}

	@Override
	public void setOrdering(int inOrdering)
	{
		setValue("ordering", inOrdering);
	}

	/**
	 * This is an optional field
	 * 
	 * @return
	 */

	@Override
	public String getShortDescription()
	{

		return get("shortdescription");
	}

	@Override
	public void setShortDescription(String inDescription)
	{
		setProperty("shortdescription", inDescription);
	}

	@Override
	public String toString()
	{
		return toString("en");
	}
	@Override
	public String toString(String inLocale)
	{
		String text = getText("assettitle", inLocale);
			if( text == null)
			{
				text = getText("assettitle","en");
			}
			if( text == null)
			{
				text = getName();
			}	
			if ( text == null)
			{
				text = getId();
			}
			return text;
	}
	/**
	 * This will look in all the category objects if needed
	 */
	@Override

	public Object getValue(String inAttribute)
	{

		if ("fulltext".equals(inAttribute))
		{
			if (getMediaArchive() != null)
			{
				return getMediaArchive().getAssetSearcher().getFulltext(this);
			}
		}
		if ("category".equals(inAttribute) || "category-exact".equals(inAttribute) )
		{
			
			Collection categorylist = (Collection) getProperties().getValue("category-exact");
			if(categorylist == null)
			{
				categorylist = new ArrayList();
				Collection categories = (Collection) getFromDb("category-exact");
				if (categories != null)
				{
					for (Iterator iterator = categories.iterator(); iterator.hasNext();)
					{
						String categoryid = (String) iterator.next();
						Category category = getMediaArchive().getCategory(categoryid); //Cache this? Or lazy load em
						if (category != null)
						{
							categorylist.add(category);
						}
					}
				}
				getProperties().put("category-exact", categorylist);
				return categorylist;
			} 
		}
		if("islocked".equals(inAttribute)) {
			return isLocked();
		}

		return super.getValue(inAttribute);

	}

	@Override
	public void removeProperty(String inKey)
	{
		if (inKey != null && inKey.length() > 0)
		{
			getProperties().remove(inKey);
		}
		if ("category".equals(inKey))
		{
			getCategories().clear();
		}
	}
	@Override
	public void removeValue(String inKey, Object inNewValue)
	{
		if( inKey.equals("category"))
		{
			removeCategory((Category)inNewValue);
			return;
		}
		super.removeValue(inKey, inNewValue);
	}
	@Override
	public void addValue(String inKey, Object inNewValue)
	{
		if( inKey.equals("category"))
		{
			addCategory((Category)inNewValue);
			return;
		}
		super.addValue(inKey, inNewValue);
	}
	
	@Override
	public void addCategory(Category inCat)
	{
		if (inCat == null)
		{
			throw new IllegalArgumentException("Categories cannot be null");
		}
		removeCategoryByPath(inCat.getCategoryPath());
		if (!isInCategory(inCat))
		{
			addValue("category-exact",inCat);
		}
	}

	@Override
	public void removeCategory(Category inCategory)
	{
		Category found = null;
		Collection cats = getCategories();

		for (Iterator iter = cats.iterator(); iter.hasNext();)
		{
			Category element = (Category) iter.next();
			if (element.getId().equals(inCategory.getId()))
			{
				found = element;
				break;
			}
		}
		if (found != null)
		{
			cats.remove(found);
			getProperties().put("category-exact",cats);
		}
		//Resave all the parents
		Collection set = buildCategorySet();
		getProperties().put("category",set);
		
	}

	@Override
	public Set buildCategorySet()
	{
		HashSet allCatalogs = new HashSet();
		Collection catalogs = getCategories();
		//allCatalogs.addAll(catalogs);
		for (Iterator iter = catalogs.iterator(); iter.hasNext();)
		{
			Category catalog = (Category) iter.next();
			buildCategorySet(catalog, allCatalogs);
		}
		return allCatalogs;
	}

	protected void buildCategorySet(Category inCatalog, Set inCatalogSet)
	{
		inCatalogSet.add(inCatalog);
		Category parent = inCatalog.getParentCategory();
		if (parent != null)
		{
			buildCategorySet(parent, inCatalogSet);
		}
	}

	
	@Override
	public Collection<Category> getCategories()
	{
		Collection values = (Collection<Category>) getValue("category-exact");
		return values;
	}

	/**
	 * @deprecated
	 * @param inCategory
	 * @return
	 */
	@Override
	public boolean isInCatalog(Category inCategory)
	{
		return isInCategory(inCategory);
	}

	@Override
	public boolean isInCategory(Category inCat)
	{
		return isInCategory(inCat.getId());
	}

	@Override
	public boolean isInCategory(String inCategoryId)
	{
		for (Iterator iter = getCategories().iterator(); iter.hasNext();)
		{
			Category element = (Category) iter.next();
			if (element.getId().equals(inCategoryId))
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean isInCategoryPath(String inCategoryPath)
	{
		for (Iterator iter = getCategories().iterator(); iter.hasNext();)
		{
			Category element = (Category) iter.next();
			if (element.getCategoryPath().equals(inCategoryPath))
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean removeCategoryByPath(String inCategoryPath)
	{
		for (Iterator iter = getCategories().iterator(); iter.hasNext();)
		{
			Category element = (Category) iter.next();
			if (element.getCategoryPath().equals(inCategoryPath))
			{
				removeCategory(element);
				return true;
			}
		}
		return false;
	}

	@Override
	public Collection getCollections()
	{
		Collection collections = new ArrayList();
		for(Category cat: getCategories() )
		{
			LibraryCollection collection = getMediaArchive().getProjectManager().findCollectionForCategory(cat);
			if(collection != null)
			{
				// <a class="librarylabel" href="$home$apphome/views/modules/librarycollection/media/${collection.getId()}.html" class="collection">$collection</a>
				collections.add(collection);
			}
		}	
		return collections;
	}

/*
	public Collection getLibraries()
	{
		Collection cats = getCategories();
		if( cats == null || cats.isEmpty())
		{
			return Collections.emptyList();
		}
		Searcher librarysearcher = getMediaArchive().getSearcher("library");
		HashSet ids = new HashSet();
		for(Category cat: getCategories() )
		{
			for (Iterator iterator = cat.getParentCategories().iterator(); iterator.hasNext();)
			{
				Category parent = (Category) iterator.next();
				ids.add(parent.getId());
				
			}
			ids.add(cat.getId());
		}
		HitTracker hits = librarysearcher.query().orgroup("categoryid", ids).search();
		return hits;
	}
*/
	
	/*
	 * @deprecated
	 * 
	 * @see org.openedit.Data#setProperty(java.lang.String, java.lang.String)
	 */

	@Override
	public void clearCategories()
	{
		getCategories().clear();
	}

	@Override
	public boolean hasProperty(String inKey)
	{
		return hasValue(inKey);
	}

	@Override
	public void addKeyword(String inString)
	{
		if (inString == null)
		{
			log.debug("Null keyword");
		}
		addValue("keywords", inString);
	}

	@Override
	public void addKeywords(String inString)
	{
		if (inString == null)
		{
			log.debug("Null keyword");
		}
		else
		{
			//grab the "" stuff first
			int start = inString.indexOf("\"");
			while (start > -1)
			{
				int end = inString.indexOf("\"", start + 1);
				addKeyword(inString.substring(start + 1, end));
				start = inString.indexOf("\"", end + 1);
			}

			String[] keywords = inString.split("\\s+");
			for (int i = 0; i < keywords.length; i++)
			{
				String key = keywords[i].trim();
				if (key.length() == 0 || key.startsWith("\"") || key.endsWith("\""))
				{
					continue;
				}
				addKeyword(key);
			}
		}
	}

	@Override
	public Collection<String> getKeywords()
	{
		Collection<String> keywords = getValues("keywords");
		if (keywords == null)
		{
			keywords = Collections.EMPTY_LIST;
		}
		return keywords;
	}

	@Override
	public void removeKeyword(String inKey)
	{
		getKeywords().remove(inKey);
	}


	@Override
	public Date getDate(String inField, String inDateFormat)
	{
		return getProperties().getDate(inField, inDateFormat);
	}

//	public Collection getObjects(String inField)
//	{
//		Collection values = (Collection)getValue(inField);
//		return values;
//	}
	
	@Override
	public boolean isRelated(Asset inAsset)
	{
		for (Iterator i = getRelatedAssets().iterator(); i.hasNext();)
		{
			RelatedAsset related = (RelatedAsset) i.next();
			if (related.getRelatedToAssetId().equals(inAsset.getId()) && related.getRelatedToCatalogId().equals(inAsset.getCatalogId()))
			{
				return true;
			}
		}
		return false;
	}

	@Override
	public void setKeywords(Collection<String> inKeywords)
	{
		setValue("keywords", inKeywords);
	}

	@Override
	public void clearKeywords()
	{
		setValue("keywords", new ArrayList());
	}

	@Override
	public void incrementProperty(String property, int delta) throws Exception
	{
		String currentValue = get(property);
		int current = Integer.parseInt(currentValue);
		current = current + delta;
		setProperty(property, Integer.toString(current));
	}

	@Override
	public boolean hasRelatedAssets()
	{
		return getRelatedAssets().size() > 0;
	}

	@Override
	public Asset copy(String newId)
	{
		if (newId == null)
		{
			newId = getId();
		}
		Asset asset = new BaseAsset(getMediaArchive());
		asset.setId(newId);
		asset.setName(getName());
		asset.setOrdering(getOrdering());
		asset.setKeywords(getKeywords());
		asset.setProperties(new HashMap(getProperties()));

		Collection catalogs = getCategories();
		for (Iterator iter = catalogs.iterator(); iter.hasNext();)
		{
			Category element = (Category) iter.next();
			asset.addCategory(element);
		}

		asset.setKeywords(null);
		for (Iterator iterator = getKeywords().iterator(); iterator.hasNext();)
		{
			asset.addKeyword((String) iterator.next());
		}

		asset.getRelatedAssets().clear();
		for (Iterator iterator = getRelatedAssets().iterator(); iterator.hasNext();)
		{
			asset.addRelatedAsset((RelatedAsset) iterator.next());
		}

		return asset;
	}

	@Override
	public Asset copy()
	{
		return copy(null);
	}

	@Override
	public void setCategories(Collection <Category> inCatalogs)
	{
		setValue("category-exact", inCatalogs);
	}

	@Override
	public void setOriginalImagePath(String inPath)
	{
		setProperty("originalpath", inPath);
	}

	@Override
	public String getSourcePath()
	{
		return get("sourcepath");
	}

	@Override
	public void setSourcePath(String inSourcePath)
	{
		setProperty("sourcepath", inSourcePath);
	}

	@Override
	public String getSaveAsName()
	{
		String name = getName();
		if (name.indexOf(".") == -1)
		{
			String ext = get("fileformat");
			if (ext == null && getSourcePath().indexOf('.') != -1)
			{
				ext = getSourcePath().substring(getSourcePath().lastIndexOf('.') + 1);
			}
			if (ext != null)
			{
				name = name + "." + ext;
			}
		}
		return name;
	}

	@Override
	public void addRelatedAsset(RelatedAsset inRelationship)
	{
		if (inRelationship.getRelatedToAssetId().equals(getId()) && inRelationship.getRelatedToCatalogId().equals(getCatalogId()))
		{
			log.error("Can not relate asset to itself" + getId());
			return;
		}
		inRelationship.setAssetId(getId());
		getRelatedAssets().add(inRelationship);
	}

	@Override
	public Collection getRelatedAssets()
	{
		if (fieldRelatedAssets == null)
		{
			fieldRelatedAssets = new ArrayList();
		}
		return fieldRelatedAssets;
	}

	@Override
	public void removeRelatedAsset(String inCatalogId, String inAssetId)
	{
		RelatedAsset toRemove = null;
		for (Iterator iterator = getRelatedAssets().iterator(); iterator.hasNext();)
		{
			RelatedAsset related = (RelatedAsset) iterator.next();
			if (related.getRelatedToAssetId().equals(inAssetId) && inCatalogId.equals(related.getRelatedToCatalogId()))
			{
				toRemove = related;
				break;
			}
		}
		if (toRemove != null)
		{
			getRelatedAssets().remove(toRemove);
		}
		else
		{
			log.error("Could not find " + inAssetId);
		}

	}

	@Override
	public List getRelatedAssets(String inType)
	{
		ArrayList list = new ArrayList();
		for (Iterator iterator = getRelatedAssets().iterator(); iterator.hasNext();)
		{
			RelatedAsset asset = (RelatedAsset) iterator.next();
			if (inType.equals(asset.getType()))
			{
				list.add(asset);
			}
		}
		return list;

	}

	@Override
	public void clearRelatedAssets()
	{
		setRelatedAssets(null);
	}

	@Override
	public void setRelatedAssets(Collection inRelatedAssets)
	{
		fieldRelatedAssets = inRelatedAssets;
	}

	@Override
	public String getMediaName()
	{
		String primaryImageName = getPrimaryFile();
		if (primaryImageName == null)
		{
			primaryImageName = getName();
		}
		return primaryImageName;
	}

	@Override
	public String getPrimaryFile()
	{
		String file = get("primaryfile");
		return file;
	}

	@Override
	public void setPrimaryFile(String inPath)
	{
		setProperty("primaryfile", inPath);
	}

	@Override
	public String getCatalogId()
	{
		if( fieldMediaArchive == null)
		{
			return null;
		}
		return getMediaArchive().getCatalogId();
	}

	@Override
	public String getFileFormat()
	{
		String format = get("fileformat");
		if (format == null)
		{
			String orig = getPrimaryFile();
			if (orig == null)
			{
				orig = getName();
			}
			format = PathUtilities.extractPageType(orig);
		}
		if (format != null)
		{
			return format.toLowerCase();
		}
		return null;
	}
	
	@Override
	public String getDetectedFileFormat()
	{
		
		
		String format = get("detectedfileformat");
		if(format == null) {
			format = getFileFormat();
		}
		return format;
	}

	
	

	@Override
	public String getAttachmentByType(String inType)
	{
		String filename = get(inType + "file");

		return filename;
	}

	@Override
	public void setAttachmentFileByType(String inType, String inName)
	{
		setProperty(inType + "file", inName);
	}

	@Override
	public boolean hasKeywords()
	{

		return getKeywords() != null && getKeywords().size() > 0;
	}

	@Override
	public Category getDefaultCategory()
	{
		if (getCategories() != null && getCategories().size() > 0)
		{
			return (Category) getCategories().iterator().next();
		}
		return null;
	}

	@Override
	public int getInt(String inKey)
	{
		String val = get(inKey);
		if (val == null)
		{
			return 0;
		}
		if (val.contains("."))
		{
			return 0;
		}
		return Integer.valueOf(val);
	}

	@Override
	public long getLong(String inKey)
	{
		String val = get(inKey);
		if (val == null)
		{
			return 0;
		}
		if (val.contains("."))
		{
			return 0;
		}
		return Long.valueOf(val);
	}

	@Override
	public BigDecimal getBigDecimal(String inKey)
	{
		return getProperties().getBigDecimal(inKey);
	}

	
	
	@Override
	public float getUploadPercentage()
	{
		BigDecimal now = getBigDecimal("uploadprogress");
		BigDecimal total = getBigDecimal("filesize");
		if (total.doubleValue() > 0)
		{
			BigDecimal percentage = now.divide(total, 2, RoundingMode.HALF_UP);
			return percentage.floatValue();
		}
		return 0;
	}

	@Override
	public boolean isPropertyTrue(String inKey)
	{
		return getProperties().getBoolean(inKey);
	}

//	public void removeLibrary(String inLibraryid)
//	{
//		Collection<String> values = new ArrayList(getLibraries());
//		values.remove(inLibraryid);
//		setValue("libraries", values);
//
//	}

	@Override
	public void setValue(String inKey, Object inValue)
	{
		if ("keywords".equals(inKey))
		{
			if (inValue instanceof String)
			{
				String[] vals = VALUEDELMITER.split((String) inValue);
				inValue = Arrays.asList(vals);
			}
			log.debug("saving keyword " + getId() + " " + inValue);
		}
		else if ("category-exact".equals(inKey) || "category".equals(inKey))
		{
			if (inValue != null)
			{
				//This is annoying. We will need to fix categories when we save this asset
				Collection catids = null;
				if (inValue instanceof Collection)
				{
					catids = (Collection) inValue;
				}
				else
				{
					
					String ids = ((String) inValue).replaceAll(" " , "|");
					String[] vals = VALUEDELMITER.split(ids);
					catids = Arrays.asList(vals);
				}

				Collection cats = new HashSet();
				for (Iterator iterator = catids.iterator(); iterator.hasNext();)
				{
					Object row = iterator.next();
					if( row instanceof Category)
					{
						cats.add(row);
					}
					else
					{
						String id = (String)row;
						String[] ids = VALUEDELMITER.split(id.replaceAll(" " , "|"));
						for (int i = 0; i < ids.length; i++)
						{
							Category cat = getMediaArchive().getCategory(ids[i]);
							if (cat != null)
							{
								cats.add(cat);
							}						
						}
					}	
				}
				inKey = "category-exact";
				inValue = cats;
			}
		}
		else if (inValue instanceof Map)
		{
			PropertyDetail detail = getMediaArchive().getAssetPropertyDetails().getDetail(inKey);
			if (detail != null && detail.isMultiLanguage())
			{
				inValue = new LanguageMap((Map) inValue);
			}
		} 
		
		super.setValue(inKey, inValue);
	}
	
	@Override
	public void setTagsValue(String inKey, Object inValue)
	{
			if (inValue instanceof Collection)
			{
				Collection tagid = null;
				Collection tags = new HashSet();
				tagid = (Collection) inValue;
				for (Iterator iterator = tagid.iterator(); iterator.hasNext();)
				{
					Object row = iterator.next();
					tags.add((String)row);
				}
				inValue = tags;
				//String[] vals = VALUEDELMITER.split((String) inValue);
				//inValue = Arrays.asList(vals);
			}
			log.debug("saving tags field " + getId() + " " + inValue);
		
		super.setValue(inKey, inValue);
	}

	@Override
	public PropertyDetails getPropertyDetails()
	{
		// TODO Auto-generated method stub
		return getMediaArchive().getAssetPropertyDetails();
	}

	@Override
	public String getProperty(String inProperty)
	{
		return get(inProperty);
	}

	@Override
	public String getPath()
	{
		String alternative = get("archivesourcepath");
		if( alternative == null)
		{
			alternative = getSourcePath();
		}
		return alternative;
	}

	@Override
	public void removeChildCategory(Category inCatParent)
	{
		removeCategory(inCatParent);
		Collection children = new ArrayList(getCategories());
		for (Iterator iterator = children.iterator(); iterator.hasNext();)
		{
			Category cat = (Category) iterator.next();
			if( cat.hasParent(inCatParent.getId()) )
			{
				removeCategory(cat);
			}
		}
	}
	@Override
	public boolean isEquals(long filemmod)
	{
		Date existingdate = getDate("assetmodificationdate");
		if (existingdate != null)
		{
			//We need to ignore milliseconds since our parsed date will not have them
			if (existingdate != null)
			{
				long oldtime = existingdate.getTime();
				filemmod = filemmod / 1000;
				oldtime = oldtime / 1000;
				if (filemmod == oldtime)
				{
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public boolean clearParentCategories() {
		
		ArrayList toclear = new ArrayList();
		
		for (Iterator iterator = getCategories().iterator(); iterator.hasNext();) {
			Category cat = (Category) iterator.next();
			for (Iterator iterator2 = cat.getParentCategories().iterator(); iterator2.hasNext();) {
				Category parent = (Category) iterator2.next();
				if(parent.getId().equals(cat.getId())) {
					continue;
				}
				if(isInCategory(parent)) {
					toclear.add(parent);
				}
			}
			
		}
		for (Iterator iterator = toclear.iterator(); iterator.hasNext();) {
			Category cat = (Category) iterator.next();
			removeCategory(cat);
		}
		if(toclear.size() > 0) {
			return true;
		} else {
			return false;
		}
	}


	@Override
	public void toggleLock(User inUser)
	{
		if(isLocked()) {
			setValue("lockedby", null);
		} else {
			setValue("lockedby", inUser.getId());
		}
		
	}
	
	public String toJsonString()
	{
		StringBuffer output = new StringBuffer();
		if (getId() != null)
		{
			output.append("{ \"_id\": \"" + getId() + "\",");
		}
		output.append(" \"_source\" :");
		if (getSearchHit() == null)
		{
			JSONObject json = new JSONObject();
			for(Iterator iterator = getProperties().keySet().iterator(); iterator.hasNext();)
			{
				String key = (String) iterator.next();
				Object value = getProperties().get(key);
				if (value == null)
				{
					continue;
				}
				if (value instanceof Collection)
				{
					JSONArray jsonarray = new JSONArray();
					jsonarray.addAll((Collection) value);
					json.put(key, jsonarray);
				}
				else if (value instanceof Map)
				{
					JSONObject jsonmap = new JSONObject((Map) value);
					json.put(key, jsonmap);
				}
				else
				{
					json.put(key, value);
				}
			}
			output.append(json.toJSONString());
		}
		else if(!getProperties().isEmpty())
		{
			JSONObject json = new JSONObject(getSearchData());
			json.putAll(getProperties());
			output.append(json.toJSONString());
		}
		else 
		{
			output.append(getSearchHit().getSourceAsString());
		}
		output.append(" \n}");
		return output.toString();
	}
	
	public boolean hasValue(String inKey)
	{
		Object value = getValue(inKey);
		if (value != null)
		{
			return true;
		}
		return false;
	}

}