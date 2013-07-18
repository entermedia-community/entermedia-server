package org.openedit.entermedia;

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

import com.openedit.OpenEditException;
import com.openedit.hittracker.HitTracker;

public class CompositeAsset extends Asset implements Data, CompositeData
{
	private static final long serialVersionUID = -7154445212382362391L;
	protected MediaArchive fieldArchive;
	protected HitTracker fieldInitialSearchResults; 
	protected HitTracker fieldSelectedResults; 
	protected List<String> fieldRemovedCategories;
	protected List<String> fieldRemovedKeywords;
	protected Map<String,String> fieldPropertiesSet;
	protected List<Integer> fieldSelections;
	protected PropertyDetails fieldPropertyDetails;
	protected String fieldId;
	
	public CompositeAsset(MediaArchive inMediaArchive, HitTracker inHits)
	{
		setArchive(inMediaArchive);
		setInitialSearchResults(inHits);
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
		if( fieldSelectedResults == null)
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
		return fieldArchive.getAssetPropertyDetails();
	}

	public Map getPropertiesSet()
	{
		if (fieldPropertiesSet == null)
		{
			fieldPropertiesSet = new HashMap();
		}
		return fieldPropertiesSet;
	}

	public void setPropertiesSet(Map inPropertiesSet)
	{
		fieldPropertiesSet = inPropertiesSet;
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
		// TODO Auto-generated method stub
		HitTracker existing = getInitialSearchResults();
		HitTracker selecteddata = getArchive().getAssetSearcher().searchByIds(existing.getSelections() );
		selecteddata.setHitsPerPage(10000);
		setSelectedResults(selecteddata);
		getProperties().clear();
	}

	public int size()
	{
		return getSelectedResults().size();
	}
	public List getKeywords()
	{
		if( fieldKeywords == null )
		{
			Data first = (Data)getSelectedResults().first();
			String fwords = first.get("keywords");
			if( fwords != null && fwords.length() > 0)
			{
				String[] common = VALUEDELMITER.split(fwords);
				for (Iterator iterator = getSelectedResults().iterator(); iterator.hasNext();)
				{
					Data data = (Data) iterator.next();
					String words = data.get("keywords");
					for (int i = 0; i < common.length; i++)
					{
						String k = common[i];
						if(words == null || ( k != null && !words.contains(k) ))
						{
							common[i] = null;
						}
					}
				}
				fieldKeywords = new ArrayList();
				for (int i = 0; i < common.length; i++)
				{
					if( common[i] != null )
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
		return fieldKeywords;
	}


	protected void checkSave(List<Asset> inTosave)
	{
		if( inTosave.size() > 99 )
		{
			getArchive().saveAssets(inTosave);
			inTosave.clear();
		}
	}

	public void removeKeyword(String inKey)
	{
		super.removeKeyword(inKey);
		getRemovedKeywords().add(inKey);
	}

	public List getCategories()
	{
		if( fieldCategories == null )
		{
			Data first = (Data)getSelectedResults().first();
			if( first == null )
			{
				return Collections.EMPTY_LIST;
			}
			String fcats = first.get("category");
			if( fcats != null )
			{
				String[] catlist = fcats.split("\\|");
				for (Iterator iterator = getSelectedResults().iterator(); iterator.hasNext();)
				{
					Data data = (Data) iterator.next();
					String cats = data.get("category");
					if( cats != null )
					{
						for (int i = 0; i < catlist.length; i++)
						{
							String  catid = catlist[i];
							if(catid != null && !cats.contains(catid) )
							{
								catlist[i] = null;
							}
						}
					}
				}
				ArrayList categories = new ArrayList();
				for (int i = 0; i < catlist.length; i++)
				{
					String  catid = catlist[i];
					if( catid != null )
					{
						Category cat = getArchive().getCategory(catid.trim());
						if( cat != null )
						{
							categories.add( cat );
						}
					}
				}
				Collections.sort(categories);
				fieldCategories  = categories;
			}
			else
			{
				fieldCategories = new ArrayList();
			}
		}
		return fieldCategories;
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
			String val = (String)getPropertiesSet().get(inId); //set by the user since last save
//			if( val == null && fieldPropertiesPreviouslySaved != null)
//			{
//				val = (String)getPropertiesPreviouslySaved().get(inId);
//			}
			if( val == null)
			{
				val = super.get(inId); //set by looking over the cached results
			}
			if( val != null ) //already set to a value
			{
				if( val.length() == 0 )
				{
					return null; //set to empty
				}
				return val;
			}
//			if( val == null ) 
//			{
//				return null;
//			}
			//return something only if all the values match the first record
			val = getValueFromResults(inId);
			//getPropertiesPreviouslySaved().put(inId, val);
			super.setProperty(inId, val);
			return val;
		}
		
		return null;
	}
	protected String getValueFromResults(String inKey) 
	{
		String val;
		val = ((Data)getSelectedResults().first()).get(inKey);
		for (Iterator iterator = getSelectedResults().iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			String dataval = data.get(inKey);
			if( val == null )
			{
				if( val != dataval )
				{
					val = "";
					break;
				}
			}
			else if (val.length() > 0 && !val.equals(dataval))
			{
				//Maybe just out of order?
				boolean multi = isMulti(inKey);
				
				if( dataval != null && multi )
				{
					String[] vals = VALUEDELMITER.split(val);
					val = "";
					for (int i = 0; i < vals.length; i++) 
					{
						if( dataval.contains(vals[i]) ) //vals are in an array
						{
							if( val.length() == 0 )
							{
								val = vals[i];
							}
							else
							{
								val = val + " | " + vals[i];
							}
						}
					}
				}
				else
				{
					val = "";
					break;
				}
			}
		}
		if(	val == null )
		{
			val = "";
		}
		return val;
	}
	public void setProperty(String inKey, String inValue)
	{
		if( inValue == null )
		{
			inValue = "";
		}
		//getProperties().put(inKey, inValue);
		getPropertiesSet().put(inKey,inValue);
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
		if( size() > 0)
		{
			Data first = (Data)getSelectedResults().first();
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
	
	public MediaArchive getArchive()
	{
		return fieldArchive;
	}

	public void setArchive(MediaArchive inArchive)
	{
		fieldArchive = inArchive;
	}

	/**
	 * Do not call this more than once!
	 * Because we use the hit results to check on previous saved 
	 */
	public void saveChanges() 
	{
		//compare keywords, categories and data. 
		List tosave = new ArrayList(100);
		for (Iterator iterator = getSelectedResults().iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			Asset inloopasset = null;
			
			for (Iterator iterator2 = getCategories().iterator(); iterator2.hasNext();)
			{
				Category cat = (Category) iterator2.next();
				String cats = data.get("category");
				if( !cats.contains(cat.getId() ) )
				{
					Asset asset = loadAsset( inloopasset, data, tosave);
					if( asset != null )
					{
						asset.addCategory(cat);
						inloopasset = asset;
					}
				}
			}
			for (Iterator iterator2 = getRemovedCategories().iterator(); iterator2.hasNext();)
			{
				Category cat = (Category) iterator2.next();
				String cats = data.get("category");
				if( cats.contains(cat.getId() ) )
				{
					Asset asset = loadAsset( inloopasset, data, tosave);
					if( asset != null )
					{
						asset.removeCategory(cat);
						inloopasset = asset;
					}
				}
			}
			
			for (Iterator iterator2 = getKeywords().iterator(); iterator2.hasNext();)
			{
				String inKey = (String) iterator2.next();
				String existing = data.get("keywords");
				if( existing == null || !existing.contains(inKey) )
				{
					Asset asset = loadAsset( inloopasset, data, tosave);
					if( asset != null )
					{
						asset.addKeyword(inKey);
						inloopasset = asset;
					}
				}
			}
			
			for (Iterator iterator2 = getRemovedKeywords().iterator(); iterator2.hasNext();)
			{
				String inKey = (String) iterator2.next();
				inKey = inKey.trim();
				String existing = data.get("keywords");
				if( existing != null && existing.contains(inKey) )
				{
					Asset asset = loadAsset( inloopasset, data, tosave);
					if( asset != null )
					{
						asset.removeKeyword(inKey);
						inloopasset = asset;
					}
				}
			}
			for (Iterator iterator2 = getPropertiesSet().keySet().iterator(); iterator2.hasNext();)
			{
				String key = (String) iterator2.next();
				if( "assetid".equals(key))
				{
					continue;
				}
					
				String value = (String)getPropertiesSet().get(key);
				String datavalue = data.get(key);
				
				if( datavalue == value )
				{
					continue;
				}
				if( datavalue != null && datavalue.equals(value) )
				{
					continue;
				}
				Asset asset = loadAsset( inloopasset, data, tosave);
				if( asset != null )
				{
					boolean multi = isMulti(key);

					if( multi )
					{
						//Need to add any that are set by user in value 
						Set added = collect(value);
						Set existing = collect(datavalue);
						Set previousCommonOnes = collect(getValueFromResults(key)); 
						saveMultiValues(asset, key, added, existing,previousCommonOnes);
					}
					else
					{
						asset.setProperty(key, value);
					}
					inloopasset = asset;
				}
			}
			if( tosave.size() > 1000)
			{
				getArchive().saveAssets(tosave);
				tosave.clear();
			}
		}
		getArchive().saveAssets(tosave);
		//getPropertiesPreviouslySaved().putAll(getPropertiesSet());
		setSelectedResults(null);
	}

	/**
	 * @param asset
	 * @param key
	 * @param added Ones that needs to be added
	 * @param existing Ones that are already on the asset
	 * @param old Ones that need to be removed?
	 */
	protected void saveMultiValues(Asset asset, String key, Set added, Set existing, Set previousCommonOnes) 
	{
		existing.addAll(added);
		
		//Need to remove any that are missing from combined
		previousCommonOnes.removeAll(added);
		existing.removeAll(previousCommonOnes);
		asset.setValues(key, existing);
		//System.out.println("Saving old value:" + datavalue + " saved: " + existing);
	}
	protected boolean isMulti(String key) 
	{
		if( key.equals( "libraries") )
		{
			return true;
		}
		PropertyDetail detail = getPropertyDetails().getDetail(key);
		
		boolean multi = detail != null && detail.isMultiValue();
		return multi;
	}

	protected Set collect(String existingvalue) 
	{
		if( existingvalue == null || existingvalue.length() == 0)
		{
			return new HashSet();
		}
		String[] vals = VALUEDELMITER.split(existingvalue);
		Set set = new HashSet(vals.length);
		for (int i = 0; i < vals.length; i++) {
			set.add(vals[i]);
		}
		return set;
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
			Data next = (Data)fieldDataIterator.next();
			
			return getArchive().getAssetBySourcePath(next.getSourcePath());
		}

		public void remove()
		{
		}
		
		
	}
	
	protected Asset loadAsset(Asset inFieldCurrentAsset, Data inData, List toSave)
	{
		if( inFieldCurrentAsset == null )
		{
			inFieldCurrentAsset =  getArchive().getAssetBySourcePath(inData.getSourcePath());
		}
		else
		{
			return inFieldCurrentAsset;
		}
		if( inFieldCurrentAsset != null )
		{
			toSave.add(inFieldCurrentAsset);
			checkSave(toSave);
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
		fieldCategories = null;
		fieldKeywords = null;
		
	}
	@Override
	public Collection<String> getValues(String inPreference) {
		String currentlist =getValueFromResults(inPreference); 
		return collect(currentlist);
		
	}
	
	
}
