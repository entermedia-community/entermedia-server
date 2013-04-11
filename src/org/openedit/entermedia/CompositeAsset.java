package org.openedit.entermedia;

import java.util.ArrayList;
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

import com.openedit.hittracker.HitTracker;

public class CompositeAsset extends Asset implements Data, CompositeData
{
	private static final long serialVersionUID = -7154445212382362391L;
	protected MediaArchive fieldArchive;
	protected HitTracker fieldSelectedHits;
	protected String fieldId;
	protected List fieldRemovedCategories;
	protected List fieldRemovedKeywords;
	protected Map fieldPropertiesSet;
	
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

	
	public CompositeAsset(MediaArchive inMediaArchive, HitTracker inHits)
	{
		setArchive(inMediaArchive);
		setSelectedHits(inHits.getSelectedHitracker());
	}
	
	public int size()
	{
		return getSelectedHits().size();
	}
	public List getKeywords()
	{
		if( fieldKeywords == null )
		{
			Data first = (Data)getSelectedHits().first();
			String fwords = first.get("keywords");
			if( fwords != null && fwords.length() > 0)
			{
				String[] common = VALUEDELMITER.split(fwords);
				for (Iterator iterator = getSelectedHits().iterator(); iterator.hasNext();)
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
			Data first = (Data)getSelectedHits().first();
			if( first == null )
			{
				return Collections.EMPTY_LIST;
			}
			String fcats = first.get("category");
			if( fcats != null )
			{
				String[] catlist = fcats.split("\\|");
				for (Iterator iterator = getSelectedHits().iterator(); iterator.hasNext();)
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
			String val = (String)getPropertiesSet().get(inId);;
			if( val == null)
			{
				val = super.get(inId);
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
			val = ((Data)getSelectedHits().first()).get(inId);
			for (Iterator iterator = getSelectedHits().iterator(); iterator.hasNext();)
			{
				Data data = (Data) iterator.next();
				String newval = data.get(inId);
				if( val == null )
				{
					if( val != newval )
					{
						val = "";
						break;
					}
				}
				else if (!val.equals(newval))
				{
					//Maybe just out of order?
					if( newval != null && val.contains("|") )
					{
						String[] vals = VALUEDELMITER.split(val);
						val = "";
						for (int i = 0; i < vals.length; i++) 
						{
							if( newval.contains(vals[i]) )
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

			super.setProperty(inId, val);
			return val;
		}
		
		return null;
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
			Data first = (Data)getSelectedHits().first();
			return first.getSourcePath() + "multi" + size();
		}
		return null;
	}

	public void setSourcePath(String inSourcepath)
	{
		
	}
	
	public Iterator iterator()
	{
		return new AssetIterator(getSelectedHits().iterator());
	}
	
	public HitTracker getSelectedHits()
	{
		return fieldSelectedHits;
	}

	public void setSelectedHits(HitTracker inSelectedHits)
	{
		fieldSelectedHits = inSelectedHits;
	}

	public MediaArchive getArchive()
	{
		return fieldArchive;
	}

	public void setArchive(MediaArchive inArchive)
	{
		fieldArchive = inArchive;
	}

	public void saveChanges() 
	{
		//compare keywords, categories and data. 
		List tosave = new ArrayList(100);
		for (Iterator iterator = getSelectedHits().iterator(); iterator.hasNext();)
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
				String existingvalue = data.get(key);
				
				if( existingvalue == value )
				{
					continue;
				}
				if( existingvalue != null && existingvalue.equals(value) )
				{
					continue;
				}
				Asset asset = loadAsset( inloopasset, data, tosave);
				if( asset != null )
				{
					boolean multi = existingvalue != null && existingvalue.contains("|");
					if( !multi)
					{
						multi = value != null && value.contains("|");
					}
					if( !multi)
					{
						String uivalue = get(key);
						multi = uivalue != null && uivalue.contains("|");
					}
				
					if( multi )
					{
						//Need to add any that are set by user in value 
						Set existing = collect(existingvalue);
						Set added = collect(value);
						existing.addAll(added);
						
						//Need to remove any that are missing from combined
						Set old = collect(super.get(key));
						old.removeAll(added);
						existing.removeAll(old);
						asset.setValues(key, existing);
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
	}
	protected Set collect(String existingvalue) 
	{
		if( existingvalue == null)
		{
			return Collections.EMPTY_SET;
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
	
}
