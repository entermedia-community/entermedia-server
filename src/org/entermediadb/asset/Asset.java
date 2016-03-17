/*
 * Created on Mar 2, 2004
 */
package org.entermediadb.asset;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.MultiValued;
import org.openedit.OpenEditRuntimeException;
import org.openedit.data.SaveableData;
import org.openedit.data.ValuesMap;
import org.openedit.page.Page;
import org.openedit.util.PathUtilities;

/**
 * @author cburkey
 * 
 */
public class Asset implements MultiValued, SaveableData
{
	private static final Log log = LogFactory.getLog(Asset.class);

	protected Page fieldSourcePage;
	protected String fieldDescription;
	protected List fieldCategories;
	//protected Collection<String> fieldLibraries;
	protected ValuesMap fieldMap;
	
	protected List<String> fieldKeywords;
	protected MediaArchive fieldMediaArchive;
	// be shown in a list
	protected Collection fieldRelatedAssets;

	public Asset()
	{
	}

	public Collection<String> getValues(String inPreference)
	{
		return getMap().getValues(inPreference);
	}
	public void setValues(String inKey, Collection<String> inValues)
	{
		if( inValues == null || inValues.size() == 0)
		{
			removeProperty(inKey);
		}
		else
		{
			getMap().put(inKey, inValues);
		}
	}
	
	public boolean isFolder()
	{
		return Boolean.parseBoolean(get("isfolder"));
	}

	public void setFolder(boolean inIsFolder)
	{
		setProperty("isfolder",String.valueOf(inIsFolder));
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
		return (int)getValue("ordering");
	}

	public void setOrdering(int inOrdering)
	{
		setValue("ordering",inOrdering);
	}

	public String getName()
	{
		return get("name");
	}

	public void setName(String inName)
	{
		if (inName != null)
		{
			inName = inName.trim();
		}
		setProperty("name", inName);
	}

	/**
	 * This is an optional field
	 * 
	 * @return
	 */

	public String getShortDescription()
	{

		return getProperty("shortdescription");
	}

	public void setShortDescription(String inDescription)
	{
		setProperty("shortdescription", inDescription);
	}

	public String toString()
	{
		String title = get("assettitle");
		if ( title != null)
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

	public String getId()
	{
		return (String)getValue("id");
	}

	public void setId(String inString)
	{
		setValue("id",inString);
	}

	/**
	 * This will look in all the category objects if needed
	 */

	public String get(String inAttribute)
	{
		
		if ("fulltext".equals(inAttribute))
		{
			if(getMediaArchive() != null){
				return getMediaArchive().getAssetSearcher().getFulltext(this);
			} 
		}
		if ("keywords".equals(inAttribute))
		{
			List<String> keywords = getKeywords();
			if( keywords.size() == 0 )
			{
				return null;
			}
			StringBuffer out = new StringBuffer();
			for (Iterator iterator = keywords.iterator(); iterator.hasNext();)
			{
				String key = (String) iterator.next();
				out.append(key);
				if( iterator.hasNext() )
				{
					out.append(" | ");
				}
			}
			return out.toString();
		}
		else if ("category".equals(inAttribute))
		{
			List<Category> categories = getCategories();
			if( categories.size() == 0 )
			{
				return null;
			}
			StringBuffer out = new StringBuffer();
			for (Iterator iterator = categories.iterator(); iterator.hasNext();)
			{
				Category cat = (Category) iterator.next();
				out.append(cat.getId());
				if( iterator.hasNext() )
				{
					out.append(" | ");
				}
			}
			return out.toString();
		}

		Object value = getValue(inAttribute);
		if( value == null)
		{
			return null;
		}
		return String.valueOf(value);
		// if ( value instanceof PageProperty)
		// {
		// PageProperty prop = (PageProperty)value;
		// return prop.getValue();
		// }
//		if (value == null)
//		{
			// Loop over all the catalogs and look for hit
//			for (Iterator iter = getCategories().iterator(); iter.hasNext();)
//			{
//				Category cat = (Category) iter.next();
//				value = cat.get(inAttribute);
//				if (value != null)
//				{
//					return value;
//				}
//			}
//		}

	}

	public void removeProperties(String[] inKeys)
	{
		for (int i = 0; i < inKeys.length; i++)
		{
			removeProperty(inKeys[i]);
		}
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
		}
		return fieldCategories;
	}
	
	
	
	

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
		if( libraries == null)
		{
			libraries = Collections.EMPTY_LIST;
		}
		return libraries;
				
	}
	protected ValuesMap getMap()
	{
		if (fieldMap == null)
		{
			fieldMap = new ValuesMap();
		}
		return fieldMap;
	}
	
	public Map getProperties()
	{
		return getMap();
	}

	public String getProperty(String inKey)
	{
		if ("id".equals(inKey))
		{
			return getId();
		}
//		if ("sourcepath".equals(inKey))
//		{
//			return getSourcePath();
//		}
		if ("category-exact".equals(inKey))
		{
			StringBuffer buffer = new StringBuffer();
			List cats = getCategories();
			for (Iterator iterator = cats.iterator(); iterator.hasNext();)
			{
				Category cat = (Category) iterator.next();
				buffer.append(cat.getId());
				if( iterator.hasNext() )
				{
					buffer.append(" | ");
				}
			}
			return buffer.toString();
		}
		String value = (String) getProperties().get(inKey);
		return value;
	}

	public void setProperties(Map inAttributes)
	{
		getMap().putAll(inAttributes);
	}

	public void setProperty(String inKey, String inValue)
	{
		if ("id".equals(inKey))
		{
			setId(inValue);
		}
//		else if ("sourcepath".equals(inKey))
//		{
//			setSourcePath(inValue);
//		}
		else if ("keywords".equals(inKey))
		{
			getKeywords().clear();
			if (inValue != null)
			{
				if( inValue.contains("|") )
				{
					String[] vals = VALUEDELMITER.split(inValue);
					for (int i = 0; i < vals.length; i++)
					{
						addKeyword(vals[i]);						
					}
				}
//				else if( inValue.contains(",") ) //Removed this because the new tag editor uses | now
//				{
//						String[] vals = inValue.split(",");
//						for (int i = 0; i < vals.length; i++)
//						{
//							addKeyword(vals[i]);						
//						}				
//				}
				else
				{
					addKeyword(inValue);
				}
			}
		}
		else if ("category-exact".equals(inKey))
		{
			if (inValue != null)
			{
				//This is annoying. We will need to fix categories when we save this asset
				getCategories().clear();
				if( inValue.contains("|") )
				{
					String[] vals = VALUEDELMITER.split(inValue);
					for (int i = 0; i < vals.length; i++)
					{
						Category cat = getMediaArchive().getCategory(vals[i]);
						if( cat != null)
						{
							addCategory(cat);
						}
					}
				}
				else
				{
					Category cat = getMediaArchive().getCategory(inValue);
					if( cat != null)
					{
						addCategory(cat);
					}
				}
			}
		}
		else
		{
			if (inValue != null && inValue.length() > 0)
			{
				getProperties().put(inKey, inValue);
			}
			else
			{
				getProperties().remove(inKey);
			}
		}
	}

	public void clearCategories()
	{
		getCategories().clear();
	}

	public boolean hasProperty(String inKey)
	{
		String value = getProperty(inKey);
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
		else if( !getKeywords().contains(inString) )
		{
			getKeywords().add(inString);
		}
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
			while( start > -1 )
			{
				int end = inString.indexOf("\"", start + 1);
				addKeyword(inString.substring(start + 1,end));
				start = inString.indexOf("\"",end +1);
			} 
			
			String[] keywords = inString.split("\\s+");
			for(int i = 0; i < keywords.length; i++)
			{
				String key = keywords[i].trim();
				if( key.length() == 0 || key.startsWith("\"") || key.endsWith("\""))
				{
					continue;
				}
				addKeyword(key);
			}
		}
	}

	public List<String> getKeywords()
	{
		if (fieldKeywords == null)
		{
			fieldKeywords = new ArrayList<String>();
		}
		return fieldKeywords;
	}

	public void removeKeyword(String inKey)
	{
		getKeywords().remove(inKey);
	}

	public Date getDate(String inField, String inDateFormat)
	{
		return getMap().getDate(inField,inDateFormat);
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

	public void setKeywords(List<String> inKeywords)
	{
		fieldKeywords = inKeywords;
	}

	public void clearKeywords()
	{
		if (fieldKeywords != null)
		{
			fieldKeywords.clear();
		}
	}

	public void incrementProperty(String property, int delta) throws Exception
	{
		String currentValue = getProperty(property);
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
		Asset asset = new Asset();
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
		return getProperty("sourcepath");
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
			String ext = getProperty("fileformat");
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
		String file = getProperty("primaryfile");
		return file;
	}
	
	public void setPrimaryFile(String inPath)
	{
		setProperty("primaryfile", inPath);
	}

	public String getCatalogId()
	{
		return (String)getValue("catalogid");
	}

	public void setCatalogId(String inCatalogId)
	{
		setValue("catalogid",inCatalogId);
	}

	public String getFileFormat()
	{
		String format = get("fileformat");
		if (format == null)
		{
			String orig = getPrimaryFile();
			if( orig == null)
			{
				orig = getName();
			}
			format = PathUtilities.extractPageType(orig);
		}
		if(format!= null)
		{
			return format.toLowerCase();
		}
		return null;
	}

	public String getAttachmentByType(String inType)
	{
		String filename = getProperty(inType + "file");
		
		return filename;
	}
	public void setAttachmentFileByType(String inType, String inName)
	{
		setProperty(inType + "file", inName);
	}
	public boolean hasKeywords()
	{

		return fieldKeywords != null && fieldKeywords.size() > 0;
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
		if( val == null)
		{
			return 0;
		}
		if( val.contains("."))
		{
			return 0;
		}
		return Integer.valueOf(val);
	}
	public long getLong(String inKey)
	{
		String val = get(inKey);
		if( val == null)
		{
			return 0;
		}
		if( val.contains("."))
		{
			return 0;
		}
		return Long.valueOf(val);
	}
	public BigDecimal getBigDecimal(String inKey)
	{
		return getMap().getBigDecimal(inKey);
	}
	
	public float getUploadPercentage() {
		BigDecimal now=getBigDecimal("uploadprogress");
		BigDecimal total = getBigDecimal("filesize");
		if( total.doubleValue() > 0)
		{
			BigDecimal percentage=now.divide(total,2,RoundingMode.HALF_UP);
			return percentage.floatValue();
		}
		return 0;
	}

	public boolean isPropertyTrue(String inKey) 
	{
		return getMap().getBoolean(inKey);
	}
	public void addLibrary(String inLibraryid) {
		addValue("libraries", inLibraryid);
		
	}
	public void removeLibrary(String inLibraryid) 
	{
		Collection<String> values = new ArrayList( getLibraries() );
		values.remove(inLibraryid);
		setValue("libraries", values);
		
	}

	@Override
	public Object getValue(String inKey)
	{
		return getMap().get(inKey);
	}

	@Override
	public void setValue(String inKey, Object inValue)
	{
		getMap().put(inKey, inValue);
	}

	@Override
	public void addValue(String inKey, Object inNewValue)
	{
		getMap().addValue(inKey, inNewValue);
	}

	@Override
	public void removeValue(String inKey, Object inOldValue)
	{
		getMap().removeValue(inKey, inOldValue);
	}

}