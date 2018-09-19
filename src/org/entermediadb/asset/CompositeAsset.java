package org.entermediadb.asset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openedit.Data;
import org.openedit.data.CompositeData;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.ValuesMap;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.modules.translations.LanguageMap;

public class CompositeAsset extends Asset implements Data, CompositeData
{
	private static final long serialVersionUID = -7154445212382362391L;
	protected HitTracker fieldInitialSearchResults;
	protected HitTracker fieldSelectedResults;
	protected List<String> fieldRemovedCategories;
	protected List<String> fieldRemovedKeywords;
	protected List<Integer> fieldSelections;
	protected PropertyDetails fieldPropertyDetails;
	protected String fieldId;
	protected Collection fieldEditFields;
	protected Map<String, Object> commonValues = new HashMap();

	public Collection getEditFields()
	{
		return fieldEditFields;
	}

	public void setEditFields(Collection inEditFields)
	{
		fieldEditFields = inEditFields;
	}

	public CompositeAsset(MediaArchive inMediaArchive, HitTracker inHits)
	{
		setMediaArchive(inMediaArchive);
		setInitialSearchResults(inHits);

		reloadData();
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
		q.setSortBy("id");

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

	protected void checkSave(List<Asset> inTosave)
	{
		if (inTosave.size() > 99)
		{
			getMediaArchive().saveAssets(inTosave);
			inTosave.clear();
		}
	}

	public void removeKeyword(String inKey)
	{
		super.removeKeyword(inKey);
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
		super.removeCategory(inCategory);
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
			return getMap().toString(val);
		}

		return null;
	}

	public Object getValue(String inId)
	{
		if (size() > 0)
		{
			Object val = getMap().getValue(inId); //set by the user since last save
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

	protected Object getValueFromResults(String inKey)
	{
		Object val = commonValues.get(inKey);
		if (val != null)
		{
			return val;
		}
		Iterator iterator = getSelectedResults().iterator();
		if (!iterator.hasNext())
		{
			return null;
		}
		Data firstrow = (Data) iterator.next();
		String text = firstrow.get(inKey);
		while (iterator.hasNext())
		{
			Data data = (Data) iterator.next();
			String dataval = data.get(inKey);
			if (text == null)
			{
				if (text != dataval)
				{
					text = "";
					break;
				}
			}
			else if (text.length() > 0 && !text.equals(dataval))
			{
				//Maybe just out of order?
				boolean multi = isMulti(inKey);

				if (dataval != null && multi)
				{
					String[] vals = VALUEDELMITER.split(text);
					text = "";
					for (int i = 0; i < vals.length; i++)
					{
						if (dataval.contains(vals[i])) //vals are in an array
						{
							if (text.length() == 0)
							{
								text = vals[i];
							}
							else
							{
								text = text + " | " + vals[i];
							}
						}
					}
				}
				else
				{
					text = "";
					break;
				}

			}
		}
		if (text == null)
		{
			text = "";
		}
		val = text;

		if (text != null && !text.isEmpty())
		{

			PropertyDetail detail = getPropertyDetails().getDetail(inKey);
			if (detail.isMultiLanguage())
			{
				Object currentval = firstrow.getValue(inKey);

				if (currentval instanceof LanguageMap)
				{
					val = currentval;
				}
			}
		}

		commonValues.put(inKey, val);
		return val;
	}

	public void setValue(String inKey, Object inValue)
	{
		if (inValue == null)
		{
			inValue = "";
		}
		super.setValue(inKey, inValue);
	}

	public String getId()
	{
		return fieldId;
	}

	public String getName()
	{
		return "Multiple Data";
	}

	public void setName(String inName)
	{
		//Nothing to do here
	}

	public void setId(String inNewid)
	{
		fieldId = inNewid;
	}

	public String getSourcePath()
	{
		if (size() > 0)
		{
			Data first = (Data) getSelectedResults().first();
			return first.getSourcePath() + "multi" + size();
		}
		return null;
	}

	public void setSourcePath(String inSourcepath)
	{

	}

	public Iterator iterator()
	{
		return new AssetIterator(getSelectedResults().iterator());
	}

	@Override
	public void setValues(String inKey, Collection<String> inValues)
	{
		super.setValue(inKey, inValues);
	}

	/**
	 * Do not call this more than once! Because we use the hit results to check
	 * on previous saved
	 */
	public void saveChanges()
	{
		//compare keywords, categories and data. 
		List tosave = new ArrayList(500);

		Map savedvalues = new HashMap();

		for (Iterator iterator = getEditFields().iterator(); iterator.hasNext();)
		{
			String field = (String) iterator.next();
			Object newval = getMap().getObject(field); //set by the user since last save
			//See if the values changed
			Object oldval = getValueFromResults(field);

			if (newval != null && newval.toString().isEmpty())
			{
				newval = null;
			}
			if (oldval != null && oldval.toString().isEmpty())
			{
				oldval = null;
			}
			if (newval != oldval)
			{
				if (newval == null && oldval != null)
				{
					savedvalues.put(field, ValuesMap.NULLVALUE);
				}
				else
				{
					if (oldval == null && newval != null)
					{
						Object obj = getMap().getObject(field);
						savedvalues.put(field, obj);
					}
					else
					{
						if (field.equals("category-exact"))
						{
							savedvalues.put(field, collectCats(newval));
						}
						else
						{
							savedvalues.put(field, newval);
						}
					}
				}
			}
		}

		for (Iterator iterator = getSelectedResults().iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
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
					Asset asset = loadAsset(inloopasset, data, tosave);
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
					Asset asset = loadAsset(inloopasset, data, tosave);
					if (asset != null)
					{
						asset.removeKeyword(inKey);
						inloopasset = asset;
					}
				}
			}
			for (Iterator iterator2 = savedvalues.keySet().iterator(); iterator2.hasNext();)
			{
				String key = (String) iterator2.next();
				if ("assetid".equals(key))
				{
					continue;
				}
				Object datavalue = data.getValue(key);
				Object newval = savedvalues.get(key);
				if (datavalue == newval)
				{
					continue;
				}
				if (datavalue != null && datavalue.equals(newval))
				{
					continue;
				}

				Asset asset = loadAsset(inloopasset, data, tosave);
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

						LanguageMap langs = (LanguageMap) newval;
						for (Iterator iterator3 = langs.keySet().iterator(); iterator3.hasNext();)
						{
							String code = (String) iterator3.next();
							map.setText(code, langs.getText(code));
						}
						asset.setValue(key, map);
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
				getMediaArchive().saveAssets(tosave);
				tosave.clear();
			}
		}
		getMediaArchive().saveAssets(tosave);
		//getPropertiesPreviouslySaved().putAll(getPropertiesSet());
		setSelectedResults(null);
		commonValues = new HashMap();
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

	/**
	 * @param asset
	 * @param key
	 * @param added
	 *            Ones that needs to be added
	 * @param existing
	 *            Ones that are already on the asset
	 * @param old
	 *            Ones that need to be removed?
	 */
	protected void saveMultiValues(Asset asset, String key, Collection added, Collection existing, Collection previousCommonOnes)
	{
		HashSet set = new HashSet();
		for (Iterator iterator = existing.iterator(); iterator.hasNext();)
		{
			Object object = (Object) iterator.next();
			if (object instanceof String && ((String) object).isEmpty())
			{
				continue;
			}
			set.add(object);
		}

		set.addAll(added);

		//Need to remove any that are missing from combined
		previousCommonOnes.removeAll(added);
		set.removeAll(previousCommonOnes);

		asset.setValue(key, set);
		//System.out.println("Saving old value:" + datavalue + " saved: " + existing);
	}

	protected boolean isMulti(String key)
	{
		if (key.equals("libraries"))
		{
			return true;
		}
		PropertyDetail detail = getPropertyDetails().getDetail(key);

		boolean multi = detail != null && detail.isMultiValue();
		return multi;
	}

	protected Collection collect(Object existingvalue)
	{
		if (existingvalue == null)
		{
			return new HashSet();
		}
		if (existingvalue instanceof Collection)
		{
			return (Collection) existingvalue;
		}
		if (existingvalue instanceof String)
		{

			String val = (String) existingvalue;
			if (val.trim().isEmpty())
			{
				return new HashSet();
			}
			String[] vals = VALUEDELMITER.split(val);
			Set set = new HashSet(vals.length);
			for (int i = 0; i < vals.length; i++)
			{
				set.add(vals[i]);
			}
			return set;
		}
		return new HashSet();
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

	protected Asset loadAsset(Asset inFieldCurrentAsset, Data inData, List toSave)
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
			checkSave(toSave);
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
		getMap().clear();
		setValue("category-exact", null);

	}

	@Override
	public Collection<String> getValues(String inPreference)
	{

		Object currentlist = getValueFromResults(inPreference);
		return collect(currentlist);

	}

}
