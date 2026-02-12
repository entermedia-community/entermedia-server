package org.entermediadb.asset;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.BaseCompositeData;
import org.openedit.data.CompositeData;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.Searcher;
import org.openedit.data.ValuesMap;
import org.openedit.event.EventManager;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.modules.translations.LanguageMap;
import org.openedit.users.User;

public class CompositeAsset extends BaseCompositeData implements Data, CompositeData, Asset
{
	protected MediaArchive fieldMediaArchive;
	
	public CompositeAsset(Searcher inSearcher, EventManager inManager, HitTracker inHits)
	{
		super(inSearcher, inManager, inHits);
	}

	public CompositeAsset(MediaArchive inMediaArchive, HitTracker inHits)
	{
		setMediaArchive(inMediaArchive);
		setSearcher(inMediaArchive.getAssetSearcher());
		setInitialSearchResults(inHits);
		setEventManager(inMediaArchive.getEventManager());

		//super(inMediaArchive.getAssetSearcher(),inMediaArchive.getEventManager(), inHits);
		reloadData();

	}

	@Override
	public boolean isLocked()
	{
		//throw new OpenEditException("Unimplemented");
		return false;
	}

	@Override
	public User getLockOwner()
	{
		throw new OpenEditException("Unimplemented");
		//return null;
	}

	@Override
	public boolean isDeleted()
	{
		throw new OpenEditException("Unimplemented");
		//return false;
	}

	@Override
	public boolean isFolder()
	{
		throw new OpenEditException("Unimplemented");
		//return false;
	}

	@Override
	public void setFolder(boolean inIsFolder)
	{
		throw new OpenEditException("Unimplemented");
		
	}

	@Override
	public MediaArchive getMediaArchive()
	{
		return fieldMediaArchive;
		//return null;
	}

	@Override
	public void setMediaArchive(MediaArchive inMediaArchive)
	{
		fieldMediaArchive = inMediaArchive;
	}

	@Override
	public int getOrdering()
	{
		throw new OpenEditException("Unimplemented");
		//return 0;
	}

	@Override
	public void setOrdering(int inOrdering)
	{
		throw new OpenEditException("Unimplemented");
		
	}

	@Override
	public String getShortDescription()
	{
		throw new OpenEditException("Unimplemented");
		//return null;
	}

	@Override
	public void setShortDescription(String inDescription)
	{
		throw new OpenEditException("Unimplemented");
		
	}

	@Override
	public String toString(String inLocale)
	{
		throw new OpenEditException("Unimplemented");
		//return null;
	}

	@Override
	public void removeProperty(String inKey)
	{
		throw new OpenEditException("Unimplemented");
		
	}

	@Override
	public void addCategory(Category inCatid)
	{
		throw new OpenEditException("Unimplemented");
		
	}

	

	@Override
	public Set buildCategorySet()
	{
		throw new OpenEditException("Unimplemented");
		//return null;
	}

	
	@Override
	public boolean isInCatalog(Category inCategory)
	{
		throw new OpenEditException("Unimplemented");
		//return false;
	}

	@Override
	public boolean isInCategory(Category inCat)
	{
		throw new OpenEditException("Unimplemented");
		//return false;
	}

	@Override
	public boolean isInCategory(String inCategoryId)
	{
		throw new OpenEditException("Unimplemented");
		//return false;
	}

	@Override
	public Collection getCollections()
	{
		throw new OpenEditException("Unimplemented");
		//return null;
	}

	@Override
	public void clearCategories()
	{
		throw new OpenEditException("Unimplemented");
		
	}

	@Override
	public boolean hasProperty(String inKey)
	{
		throw new OpenEditException("Unimplemented");
		//return false;
	}

	@Override
	public void addKeyword(String inString)
	{
		throw new OpenEditException("Unimplemented");
		
	}

	@Override
	public void addKeywords(String inString)
	{
		throw new OpenEditException("Unimplemented");
		
	}



	@Override
	public Date getDate(String inField, String inDateFormat)
	{
		throw new OpenEditException("Unimplemented");
		//return null;
	}

	@Override
	public boolean isRelated(Asset inAsset)
	{
		throw new OpenEditException("Unimplemented");
		//return false;
	}

	@Override
	public void setKeywords(Collection<String> inKeywords)
	{
		throw new OpenEditException("Unimplemented");
		
	}

	@Override
	public void clearKeywords()
	{
		throw new OpenEditException("Unimplemented");
		
	}

	@Override
	public void incrementProperty(String inProperty, int inDelta) throws Exception
	{
		throw new OpenEditException("Unimplemented");
		
	}

	@Override
	public boolean hasRelatedAssets()
	{
		throw new OpenEditException("Unimplemented");
		//return false;
	}

	@Override
	public Asset copy(String inNewId)
	{
		throw new OpenEditException("Unimplemented");
		//return null;
	}

	@Override
	public Asset copy()
	{
		throw new OpenEditException("Unimplemented");
		//return null;
	}

	@Override
	public void setCategories(Collection<Category> inCatalogs)
	{
		throw new OpenEditException("Unimplemented");
		
	}

	@Override
	public void setOriginalImagePath(String inPath)
	{
		throw new OpenEditException("Unimplemented");
		
	}

	@Override
	public String getSaveAsName()
	{
		throw new OpenEditException("Unimplemented");
		//return null;
	}

	@Override
	public void addRelatedAsset(RelatedAsset inRelationship)
	{
		throw new OpenEditException("Unimplemented");
		
	}

	@Override
	public Collection getRelatedAssets()
	{
		throw new OpenEditException("Unimplemented");
		//return null;
	}

	@Override
	public void removeRelatedAsset(String inCatalogId, String inAssetId)
	{
		throw new OpenEditException("Unimplemented");
		
	}

	@Override
	public List getRelatedAssets(String inType)
	{
		throw new OpenEditException("Unimplemented");
		//return null;
	}

	@Override
	public void clearRelatedAssets()
	{
		throw new OpenEditException("Unimplemented");
		
	}

	@Override
	public void setRelatedAssets(Collection inRelatedAssets)
	{
		throw new OpenEditException("Unimplemented");
		
	}

	@Override
	public String getMediaName()
	{
		throw new OpenEditException("Unimplemented");
		//return null;
	}

	@Override
	public String getPrimaryFile()
	{
		throw new OpenEditException("Unimplemented");
		//return null;
	}

	@Override
	public void setPrimaryFile(String inPath)
	{
		throw new OpenEditException("Unimplemented");
		
	}

	@Override
	public String getCatalogId()
	{
		throw new OpenEditException("Unimplemented");
		//return null;
	}

	@Override
	public String getFileFormat()
	{
		String value = get("fileformat");
		return value;
		//return null;
	}

	@Override
	public String getDetectedFileFormat()
	{
		throw new OpenEditException("Unimplemented");
		//return null;
	}

	@Override
	public String getAttachmentByType(String inType)
	{
		throw new OpenEditException("Unimplemented");
		//return null;
	}

	@Override
	public void setAttachmentFileByType(String inType, String inName)
	{
		throw new OpenEditException("Unimplemented");
		
	}

	@Override
	public boolean hasKeywords()
	{
		throw new OpenEditException("Unimplemented");
		//return false;
	}

	@Override
	public Category getDefaultCategory()
	{
		throw new OpenEditException("Unimplemented");
		//return null;
	}

	@Override
	public BigDecimal getBigDecimal(String inKey)
	{
		throw new OpenEditException("Unimplemented");
		//return null;
	}

	@Override
	public float getUploadPercentage()
	{
		throw new OpenEditException("Unimplemented");
		//return 0;
	}

	@Override
	public boolean isPropertyTrue(String inKey)
	{
		throw new OpenEditException("Unimplemented");
		//return false;
	}

	@Override
	public void setTagsValue(String inKey, Object inValue)
	{
		throw new OpenEditException("Unimplemented");
		
	}

	@Override
	public String getPath()
	{
		throw new OpenEditException("Unimplemented");
		//return null;
	}

	@Override
	public void removeChildCategory(Category inCatParent)
	{
		throw new OpenEditException("Unimplemented");
		
	}

	@Override
	public boolean isEquals(long inFilemmod)
	{
		throw new OpenEditException("Unimplemented");
		//return false;
	}

	@Override
	public boolean clearParentCategories()
	{
		throw new OpenEditException("Unimplemented");
		//return false;
	}

	@Override
	public void toggleLock(User inUser)
	{
		throw new OpenEditException("Unimplemented");
		
	}

	@Override
	public Map getSearchData()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setSearchData(Map inSearchHit)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public Map getEmRecordStatus()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	
	private static final long serialVersionUID = -7154445212382362391L;
	protected HitTracker fieldInitialSearchResults;
	protected HitTracker fieldSelectedResults;
	protected List<String> fieldRemovedCategories;
	protected List<String> fieldRemovedKeywords;
	protected List<Integer> fieldSelections;
	protected PropertyDetails fieldPropertyDetails;
	protected String fieldId;
	protected Collection fieldEditFields;

	public Collection getEditFields()
	{
		return fieldEditFields;
	}

	public void setEditFields(Collection inEditFields)
	{
		fieldEditFields = inEditFields;
	}

	public HitTracker getInitialSearchResults()
	{
		return fieldInitialSearchResults;
	}

	public void setInitialSearchResults(HitTracker inInitialSearchResults)
	{
		fieldInitialSearchResults = inInitialSearchResults;
	}

	public HitTracker getSelectedResults()
	{
		if (fieldSelectedResults == null)
		{
			reloadData();
		}
		return fieldSelectedResults;
	}

	public void setSelectedResults(HitTracker inCurrentSearchResults)
	{
		fieldSelectedResults = inCurrentSearchResults;
	}

	//	protected Map<String,String> fieldPropertiesPreviouslySaved;
	//	
	//	public Map<String,String> getPropertiesPreviouslySaved() 
	//	{
	//		if (fieldPropertiesPreviouslySaved == null) 
	//		{
	//			fieldPropertiesPreviouslySaved = new HashMap<String,String>();
	//		}
	//		return fieldPropertiesPreviouslySaved;
	//	}

	public PropertyDetails getPropertyDetails()
	{
		return getMediaArchive().getAssetPropertyDetails();
	}

	public List getRemovedCategories()
	{
		if (fieldRemovedCategories == null)
		{
			fieldRemovedCategories = new ArrayList();
		}

		return fieldRemovedCategories;
	}

	public void setRemovedCategories(List inRemovedCategories)
	{
		fieldRemovedCategories = inRemovedCategories;
	}

	public List getRemovedKeywords()
	{
		if (fieldRemovedKeywords == null)
		{
			fieldRemovedKeywords = new ArrayList();
		}

		return fieldRemovedKeywords;
	}

	public void setRemovedKeywords(List inRemovedKeywords)
	{
		fieldRemovedKeywords = inRemovedKeywords;
	}

	protected void reloadData()
	{
		HitTracker existing = getInitialSearchResults();
		SearchQuery q = existing.getSearchQuery().copy();
		if(existing.getSearchQuery().getSortBy() != null) {
			q.setSortBy(existing.getSearchQuery().getSortBy());
		} else {
			q.setSortBy("id");

		}

		HitTracker selecteddata = getMediaArchive().getAssetSearcher().search(q);
		if (existing.isAllSelected())
		{
			//rerun the search
			selecteddata.selectAll();
		}
		else
		{
			selecteddata.setSelections(existing.getSelections());
			selecteddata.setShowOnlySelected(true);
		}
		selecteddata.enableBulkOperations();
		setSelectedResults(selecteddata);

		getProperties().clear();
	}

	public int size()
	{
		return getSelectedResults().size();
	}

	public Collection<String> getKeywords()
	{
		Collection fieldKeywords = (Collection) super.getValues("keywords"); //TODO: Is this right?
		if (fieldKeywords == null)
		{
			Data first = (Data) getSelectedResults().first();
			String fwords = first.get("keywords");
			if (fwords != null && fwords.length() > 0)
			{
				String[] common = VALUEDELMITER.split(fwords);
				for (Iterator iterator = getSelectedResults().iterator(); iterator.hasNext();)
				{
					Data data = (Data) iterator.next();
					String words = data.get("keywords");
					for (int i = 0; i < common.length; i++)
					{
						String k = common[i];
						if (words == null || (k != null && !words.contains(k)))
						{
							common[i] = null;
						}
					}
				}
				fieldKeywords = new ArrayList();
				for (int i = 0; i < common.length; i++)
				{
					if (common[i] != null)
					{
						fieldKeywords.add(common[i].trim());
					}
				}
			}
			else
			{
				fieldKeywords = new ArrayList();
			}
		}
		return new ArrayList(fieldKeywords);
	}

	protected void checkSave(WebPageRequest inReq, Collection<Data> inTosave)
	{
		if (inTosave.size() > 99)
		{
			saveAll(inReq,inTosave);
			inTosave.clear();
		}
	}

	public void removeKeyword(String inKey)
	{
		//super.removeKeyword(inKey);
		getRemovedKeywords().add(inKey);
	}

	public Collection getCategories()
	{
		Collection val = (Collection) getValue("category-exact");
		//		List mycats = null;
		//		if(vals instanceof List){
		//			 mycats = (List)vals;
		//
		//		}
		//		if(vals instanceof Collection){
		//			Collection target = (Collection)vals;
		//			ArrayList finalvals = new ArrayList();
		//			for (Iterator iterator = target.iterator(); iterator.hasNext();)
		//			{
		//				Object object = (Object) iterator.next();
		//				if(object instanceof String)
		//				{
		//					Category cat = getMediaArchive().getCategory((String) object);
		//					finalvals.add(cat);
		//
		//				} 
		//				else if(object instanceof Category){
		//					finalvals.add(object);
		//				}
		//			}
		//			
		//			mycats = finalvals;
		//		}
		//		
		//		if( mycats == null )
		//		{
		//			Data first = (Data)getSelectedResults().first();
		//			if( first == null )
		//			{
		//				return Collections.EMPTY_LIST;
		//			}
		//			String fcats = first.get("category-exact");
		//			if( fcats != null )
		//			{
		//				String[] catlist = fcats.split("\\|");
		//				for (Iterator iterator = getSelectedResults().iterator(); iterator.hasNext();)
		//				{
		//					Data data = (Data) iterator.next();
		//					String cats = data.get("category-exact");
		//					if( cats != null )
		//					{
		//						for (int i = 0; i < catlist.length; i++)
		//						{
		//							String  catid = catlist[i];
		//							if(catid != null && !cats.contains(catid) )
		//							{
		//								catlist[i] = null;
		//							}
		//						}
		//					}
		//				}
		//				ArrayList categories = new ArrayList();
		//				for (int i = 0; i < catlist.length; i++)
		//				{
		//					String  catid = catlist[i];
		//					if( catid != null )
		//					{
		//						Category cat = getMediaArchive().getCategory(catid.trim());
		//						if( cat != null )
		//						{
		//							categories.add( cat );
		//						}
		//					}
		//				}
		//				Collections.sort(categories);
		//				mycats  = categories;
		//			}
		//			else
		//			{
		//				mycats = new ArrayList();
		//			}
		//		}
		return val;
	}

	public void removeCategory(Category inCategory)
	{
		//super.removeCategory(inCategory);
		getRemovedCategories().add(inCategory);

	}

	public String getProperty(String inKey)
	{
		return get(inKey);
	}

	public String get(String inId)
	{
		if (size() > 0)
		{
			Object val = getValue(inId);
			return getPropertiesSet().toString(val);
		}

		return null;
	}

	public Object getValue(String inId)
	{
		if (size() > 0)
		{
			Object val = getPropertiesSet().getValue(inId); //set by the user since last save
			if (val != null) //already set to a value
			{
				return val;
			}
			//			if( val == null ) 
			//			{
			//				return null;
			//			}
			//return something only if all the values match the first record
			Object sval = getValueFromResults(inId);
			if (inId.equals("category-exact"))
			{
				if (sval == null || sval.toString().isEmpty())
				{
					return Collections.EMPTY_LIST;
				}
				Collection col = collect(sval);
				Collection categories = new HashSet();
				for (Iterator iterator = col.iterator(); iterator.hasNext();)
				{
					String object = (String) iterator.next();
					Category cat = getMediaArchive().getCategory(object);
					if (cat != null)
					{
						categories.add(cat);
					}
				}
				return categories;
			}
			return sval;
		}

		return null;
	}


	public Iterator iterator()
	{
		return new AssetIterator(getSelectedResults().iterator());
	}

	public void saveChanges(WebPageRequest inReq)
	{
		//compare keywords, categories and data. 
		List<Data> tosave = new ArrayList(500);

		Map safevalues = new HashMap();

		for (Iterator iterator = getEditFields().iterator(); iterator.hasNext();)
		{
			String field = (String) iterator.next();
			addSafeValue(field, safevalues);
		}

		
		for (Iterator iterator = getSelectedResults().iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			getEventManager().fireDataEditEvent(inReq, getSearcher(), data);
			Asset inloopasset = null;

			//			for (Iterator iterator2 = getCategories().iterator(); iterator2.hasNext();)
			//			{
			//				Category cat = (Category) iterator2.next();
			//				Collection cats = (Collection)data.getValue("category-exact");
			//				if( cats == null || !cats.contains(cat) )
			//				{
			//					Asset asset = loadAsset( inloopasset, data, tosave);
			//					if( asset != null )
			//					{
			//						asset.addCategory(cat);
			//						inloopasset = asset;
			//					}
			//				}
			//			}
			for (Iterator iterator2 = getRemovedCategories().iterator(); iterator2.hasNext();)
			{
				Category cat = (Category) iterator2.next();
				Collection cats = (Collection) data.getValue("category-exact");
				if (cats != null && cats.contains(cat))
				{
					Asset asset = loadAsset(inReq,inloopasset, data, tosave);
					if (asset != null)
					{
						asset.removeCategory(cat);
						inloopasset = asset;
					}
				}
			}

			//			for (Iterator iterator2 = getKeywords().iterator(); iterator2.hasNext();)
			//			{
			//				String inKey = (String) iterator2.next();
			//				Collection keywords = (Collection)data.getValue("keywords");
			//				if( keywords != null && !keywords.contains(inKey) )
			//				{
			//					Asset asset = loadAsset( inloopasset, data, tosave);
			//					if( asset != null )
			//					{
			//						asset.addKeyword(inKey);
			//						inloopasset = asset;
			//					}
			//				}
			//			}
			//			
			for (Iterator iterator2 = getRemovedKeywords().iterator(); iterator2.hasNext();)
			{
				String inKey = (String) iterator2.next();
				inKey = inKey.trim();
				Collection keywords = (Collection) data.getValue("keywords");
				if (keywords != null && keywords.contains(inKey))
				{
					Asset asset = loadAsset(inReq,inloopasset, data, tosave);
					if (asset != null)
					{
						asset.removeKeyword(inKey);
						inloopasset = asset;
					}
				}
			}
			for (Iterator iterator2 = safevalues.keySet().iterator(); iterator2.hasNext();)
			{
				String key = (String) iterator2.next();
				if ("assetid".equals(key))
				{
					continue;
				}
				Object datavalue = data.getValue(key);
				Object newval = safevalues.get(key);
				if (datavalue == newval)
				{
					continue;
				}
				if (datavalue != null && datavalue.equals(newval))
				{
					continue;
				}
				Asset asset = loadAsset(inReq, inloopasset, data, tosave);
				if (asset != null)
				{
					PropertyDetail detail = getPropertyDetails().getDetail(key);
					if (detail.isMultiValue())
					{
						//Need to add any that are set by user in value 
						Collection added = collect(newval);
						Collection existing = collect(datavalue);
						Collection previousCommonOnes = collect(getValueFromResults(key)); //Do this only once somehow 
						saveMultiValues(asset, key, added, existing, previousCommonOnes);
					}
					else if (detail.isMultiLanguage())
					{
						LanguageMap map = null;
						if (datavalue instanceof LanguageMap)
						{
							map = (LanguageMap) datavalue;
						}
						if (map == null)
						{
							map = new LanguageMap();
						}
						if( newval instanceof LanguageMap ) 
						{
							LanguageMap langs = (LanguageMap) newval;
							for (Iterator iterator3 = langs.keySet().iterator(); iterator3.hasNext();)
							{
								String code = (String) iterator3.next();
								map.setText(code, langs.getText(code));
							}
							asset.setValue(key, map);
						}

					}
					else if (detail.getId().equals("category-exact"))
					{
						//Need to add any that are set by user in value 
						Collection<Category> catsadded = (Collection<Category>) newval;
						Collection<Category> catsexisting = collectCats(datavalue);
						Collection<Category> catspreviousCommonOnes = collectCats(getValueFromResults(key)); //Do this only once somehow 
						saveMultiValues(asset, key, catsadded, catsexisting, catspreviousCommonOnes);
					}
					else
					{
						if (newval == ValuesMap.NULLVALUE)
							newval = null;
						asset.setValue(key, newval);
					}
					inloopasset = asset;
				}
			}
			if (tosave.size() > 1000)
			{
				saveAll(inReq, tosave);

				tosave.clear();
			}
		}
		saveAll(inReq, tosave);
		
		//getPropertiesPreviouslySaved().putAll(getPropertiesSet());
		setSelectedResults(null);
		commonCachedValues = new HashMap();
	}

	protected void addSafeValue(String field, Map safevalues) {
		Object newval = getPropertiesSet().getValue(field); //set by the user since last save
		//See if the values changed
		Object oldval = getValueFromResults(field); 
		//A blank string means no common value.
		//A null means empty

		if( newval == null && oldval == null)
		{
			return;
		}
		
		if (newval != null && newval instanceof String)  //Clean up the newval
		{
			String snewval = newval.toString();
			if( snewval.isEmpty())
			{
				newval = null;
			}
		}
		 
		if (oldval != null && oldval.toString().isEmpty())
		{
			oldval = null;
		}

		PropertyDetail detail = getPropertyDetails().getDetail(field);
		//See if there is a newval
		if (newval != null && !newval.equals(oldval))
		{

			/*if (newval == null && oldval != null)
			{
				safevalues.put(field, ValuesMap.NULLVALUE);
			}
			else
			*/
			if (field.equals("category-exact"))
			{
				safevalues.put(field, collectCats(newval));
			}
			
			else if (detail.isMultiLanguage() && oldval != null)
			{
				
				LanguageMap newvall = (LanguageMap)newval;
				LanguageMap oldvall = (LanguageMap)oldval;
				LanguageMap langs = (LanguageMap) oldvall;
				for (Iterator iterator3 = langs.keySet().iterator(); iterator3.hasNext();)
				{
					String code = (String) iterator3.next();
					if(newvall.getText(code) == null)
					{
						newvall.setText(code, "");
					}
				}
				safevalues.put(field, newvall);
			}
			else
			{
				safevalues.put(field, newval);
			}
		}
	}

	

	protected Collection<Category> collectCats(Object existingvalue)
	{
		HashSet<Category> cats = new HashSet<Category>();
		if (existingvalue == null)
		{
			return cats;
		}
		Collection existingvalues = null;
		if (existingvalue instanceof Collection)
		{
			//likely be strings in here
			existingvalues = (Collection) existingvalue;
		}
		else
		{
			String val = (String) existingvalue;
			if (val.trim().isEmpty())
			{
				return cats;
			}
			String[] vals = VALUEDELMITER.split(val);
			existingvalues = new HashSet(vals.length);
			for (int i = 0; i < vals.length; i++)
			{
				existingvalues.add(vals[i]);
			}
		}

		for (Iterator iterator = existingvalues.iterator(); iterator.hasNext();)
		{

			Object val = iterator.next();
			if (val instanceof String)
			{

				String catid = (String) val;
				Category cat = getMediaArchive().getCategory(catid);
				if (cat != null)
				{
					cats.add(cat);
				}
			}
			else
			{
				cats.add((Category) val);
			}

		}

		return cats;
	}


	
	

	//
	class AssetIterator implements Iterator
	{
		Iterator fieldDataIterator;

		public AssetIterator(Iterator inHitsIterator)
		{
			fieldDataIterator = inHitsIterator;
		}

		public boolean hasNext()
		{
			return fieldDataIterator.hasNext();
		}

		@Override
		public Object next()
		{
			Data next = (Data) fieldDataIterator.next();

			return getMediaArchive().getAssetBySourcePath(next.getSourcePath());
		}

		public void remove()
		{
		}

	}

	protected Asset loadAsset(WebPageRequest inReq, Asset inFieldCurrentAsset, Data inData, List toSave)
	{
		if (inFieldCurrentAsset == null)
		{
			inFieldCurrentAsset = (Asset) getMediaArchive().getAssetSearcher().loadData(inData);
		}
		else
		{
			return inFieldCurrentAsset;
		}
		if (inFieldCurrentAsset != null)
		{
			checkSave(inReq,toSave);
			toSave.add(inFieldCurrentAsset);
		}
		return inFieldCurrentAsset;
	}

	public String toString()
	{
		return getId();
	}

	public void refresh()
	{
		getPropertiesSet().clear();
		setValue("category-exact", null);

	}

}
