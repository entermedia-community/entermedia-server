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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.elasticsearch.SearchHitData;
import org.openedit.MultiValued;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.SaveableData;
import org.openedit.data.ValuesMap;
import org.openedit.modules.translations.LanguageMap;
import org.openedit.repository.ContentItem;
import org.openedit.util.PathUtilities;

/**
 * @author cburkey
 * 
 */
public class Asset extends SearchHitData implements MultiValued, SaveableData
{
	private static final Log log = LogFactory.getLog(Asset.class);

	//protected Page fieldSourcePage;
	protected String fieldDescription;
	protected List fieldCategories;
	//protected Collection<String> fieldLibraries;
	protected ValuesMap fieldMap;
	protected MediaArchive fieldMediaArchive;
	// be shown in a list
	protected Collection fieldRelatedAssets;

	public Asset()
	{
	}

	

	public boolean isFolder()
	{
		
		String isfolder = get("isfolder");
		if (isfolder == null)
		{
			ContentItem item = getMediaArchive().getOriginalFileManager().getOriginalContent(this);
			setFolder(item.isFolder());
		}
		
		return Boolean.parseBoolean(get("isfolder"));
	}

	public void setFolder(boolean inIsFolder)
	{
		setProperty("isfolder", String.valueOf(inIsFolder));
	}

	public Asset(MediaArchive inMediaArchive)
	{
		setMediaArchive(inMediaArchive);
	}

	public MediaArchive getMediaArchive()
	{
		return fieldMediaArchive;
	}

	public void setMediaArchive(MediaArchive inMediaArchive)
	{
		fieldMediaArchive = inMediaArchive;
	}

	public int getOrdering()
	{
		return (int) getValue("ordering");
	}

	public void setOrdering(int inOrdering)
	{
		setValue("ordering", inOrdering);
	}

	

	/**
	 * This is an optional field
	 * 
	 * @return
	 */

	public String getShortDescription()
	{

		return get("shortdescription");
	}

	public void setShortDescription(String inDescription)
	{
		setProperty("shortdescription", inDescription);
	}

	public String toString()
	{
		String title = get("assettitle");
		if (title != null)
		{
			return title;
		}
		else if (getName() != null)
		{
			return getName();
		}
		else
		{
			return getId();
		}
	}

	

	
	/**
	 * This will look in all the category objects if needed
	 */

	public Object getValue(String inAttribute)
	{

		if ("fulltext".equals(inAttribute))
		{
			if (getMediaArchive() != null)
			{
				return getMediaArchive().getAssetSearcher().getFulltext(this);
			}
		}
		
//		if ("category".equals(inAttribute))
//		{
//			List<Category> categories = getCategories();
//			
//			return categories;
//		}
		
		return super.getValue(inAttribute);

	}

	

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

	public void addCategory(Category inCatid)
	{
		if (inCatid == null)
		{
			throw new IllegalArgumentException("Categories cannot be null");
		}
		if (!isInCatalog(inCatid))
		{
			getCategories().add(inCatid);
		}
	}

	public void removeCategory(Category inCategory)
	{
		
		Collection categories = (Collection) getValues("category-exact");
		categories.remove(inCategory.getId());
		Collection fullcategories = (Collection) getValues("category");
		fullcategories.remove(inCategory.getId());
		Category found = null;
		for (Iterator iter = getCategories().iterator(); iter.hasNext();)
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
			getCategories().remove(found);
		}
	}

	public List<Category> getCategories()
	{
		if (fieldCategories == null)
		{
			fieldCategories = new ArrayList();
			
				Collection categories = (Collection) getValues("category-exact");
				if (categories != null)
				{
					for (Iterator iterator = categories.iterator(); iterator.hasNext();)
					{
						String categoryid = (String) iterator.next();
						Category category = getMediaArchive().getCategory(categoryid); //Cache this? Or lazy load em
						if (category != null)
						{
							fieldCategories.add(category);
						}
					}
				}
			
			
		}
		return fieldCategories;
	}
	/**
	 * @deprecated
	 * @param inCategory
	 * @return
	 */
	public boolean isInCatalog(Category inCategory)
	{
		return isInCategory(inCategory);
	}

	public boolean isInCategory(Category inCat)
	{
		return isInCategory(inCat.getId());
	}

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

	public Collection<String> getLibraries()
	{
		Collection<String> libraries = getValues("libraries");
		if (libraries == null)
		{
			libraries = Collections.EMPTY_LIST;
		}
		return libraries;

	}

	
	/*
	 * @deprecated
	 * 
	 * @see org.openedit.Data#setProperty(java.lang.String, java.lang.String)
	 */
	

	public void clearCategories()
	{
		getCategories().clear();
	}

	public boolean hasProperty(String inKey)
	{
		String value = get(inKey);
		if (value != null)
		{
			return true;
		}
		return false;
	}

	public void addKeyword(String inString)
	{
		if (inString == null)
		{
			log.debug("Null keyword");
		}
		addValue("keywords", inString);
	}

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

	public Collection<String> getKeywords()
	{
		Collection<String> keywords = getValues("keywords");
		if (keywords == null)
		{
			keywords = Collections.EMPTY_LIST;
		}
		return keywords;
	}

	public void removeKeyword(String inKey)
	{
		getKeywords().remove(inKey);
	}

	public Date getDate(String inField, String inDateFormat)
	{
		return getMap().getDate(inField, inDateFormat);
	}

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

	public void setKeywords(Collection<String> inKeywords)
	{
		setValues("keywords", inKeywords);
	}

	public void clearKeywords()
	{
		setValues("keywords", new ArrayList());
	}

	public void incrementProperty(String property, int delta) throws Exception
	{
		String currentValue = get(property);
		int current = Integer.parseInt(currentValue);
		current = current + delta;
		setProperty(property, Integer.toString(current));
	}

	public boolean hasRelatedAssets()
	{
		return getRelatedAssets().size() > 0;
	}

	public Asset copy(String newId)
	{
		if (newId == null)
		{
			newId = getId();
		}
		Asset asset = new Asset(getMediaArchive());
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

	public Asset copy()
	{
		return copy(null);
	}

	public void setCategories(List inCatalogs)
	{
		fieldCategories = inCatalogs;
	}

	public void setOriginalImagePath(String inPath)
	{
		setProperty("originalpath", inPath);
	}

	public String getSourcePath()
	{
		return get("sourcepath");
	}

	public void setSourcePath(String inSourcePath)
	{
		setProperty("sourcepath", inSourcePath);
	}

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

	public Collection getRelatedAssets()
	{
		if (fieldRelatedAssets == null)
		{
			fieldRelatedAssets = new ArrayList();
		}
		return fieldRelatedAssets;
	}

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

	public void clearRelatedAssets()
	{
		setRelatedAssets(null);
	}

	public void setRelatedAssets(Collection inRelatedAssets)
	{
		fieldRelatedAssets = inRelatedAssets;
	}

	public String getMediaName()
	{
		String primaryImageName = getPrimaryFile();
		if (primaryImageName == null)
		{
			primaryImageName = getName();
		}
		return primaryImageName;
	}

	public String getPrimaryFile()
	{
		String file = get("primaryfile");
		return file;
	}

	public void setPrimaryFile(String inPath)
	{
		setProperty("primaryfile", inPath);
	}

	public String getCatalogId()
	{
		return (String) getValue("catalogid");
	}

	public void setCatalogId(String inCatalogId)
	{
		setValue("catalogid", inCatalogId);
	}

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

	public String getAttachmentByType(String inType)
	{
		String filename = get(inType + "file");

		return filename;
	}

	public void setAttachmentFileByType(String inType, String inName)
	{
		setProperty(inType + "file", inName);
	}

	public boolean hasKeywords()
	{

		return getKeywords() != null && getKeywords().size() > 0;
	}

	public Category getDefaultCategory()
	{
		if (getCategories() != null && getCategories().size() > 0)
		{
			return (Category) getCategories().get(0);
		}
		return null;
	}

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

	public BigDecimal getBigDecimal(String inKey)
	{
		return getMap().getBigDecimal(inKey);
	}

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

	public boolean isPropertyTrue(String inKey)
	{
		return getMap().getBoolean(inKey);
	}

	public void addLibrary(String inLibraryid)
	{
		addValue("libraries", inLibraryid);

	}

	public void removeLibrary(String inLibraryid)
	{
		Collection<String> values = new ArrayList(getLibraries());
		values.remove(inLibraryid);
		setValue("libraries", values);

	}

	@Override
	public void setValue(String inKey, Object inValue)
	{
		if ("keywords".equals(inKey))
		{
			if( inValue instanceof String)
			{
				String[] vals = VALUEDELMITER.split((String )inValue );
				inValue = Arrays.asList(vals);
			}	
		}
		else if ("category-exact".equals(inKey))
		{
			if (inValue != null)
			{
				//This is annoying. We will need to fix categories when we save this asset
				getCategories().clear();
				Collection catids = null;
				if (inValue instanceof Collection)
				{
					catids = (Collection) inValue;
				}
				else
				{
					String ids = (String) inValue;
					String[] vals = VALUEDELMITER.split(ids);
					catids = Arrays.asList(vals);
				}

				for (Iterator iterator = catids.iterator(); iterator.hasNext();)
				{
					String id = (String) iterator.next();
					Category cat = getMediaArchive().getCategory(id);
					if (cat != null)
					{
						addCategory(cat);
					}
				}
			}
		}
		if (inValue instanceof Map)
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
	public PropertyDetails getPropertyDetails() {
		// TODO Auto-generated method stub
		return getMediaArchive().getAssetPropertyDetails();
	}
	
	
	public String getProperty(String inProperty){
		return get(inProperty);
	}



	public String getPath()
	{
		return get("archivesourcepath") == null ?  getSourcePath():get("archivesourcepath");
	}
	
}