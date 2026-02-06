package org.entermediadb.elasticsearch;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.entermediadb.location.Position;
import org.openedit.MultiValued;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.ValuesMap;
import org.openedit.modules.translations.LanguageMap;
import org.openedit.util.DateStorageUtil;

public class ValuesMapWithSearchData<K, V> extends ValuesMap
{
	protected Map fieldSearchData;
	protected SearchHit fieldSearchHit;
	protected PropertyDetails fieldPropertyDetails;
	
	public PropertyDetails getPropertyDetails()
	{
		return fieldPropertyDetails;
	}
	public void setPropertyDetails(PropertyDetails inPropertyDetails)
	{
		fieldPropertyDetails = inPropertyDetails;
	}
	protected Object getFromDb(String inId) {
//		if (inId.equals(".version")) {
//			if (getVersion() > -1) {
//				return String.valueOf(getVersion());
//			}
//			return null;
//		}
		//log.info(getSearchHit().getSourceAsString());
		String detailid = inId;
		if( detailid.endsWith("_int"))
		{
			detailid = inId.substring(0, inId.length() - 4);
		}
		String key = detailid;
		Object value = null;
		PropertyDetail detail = getPropertyDetails().getDetail(detailid);
		if (detail != null && detail.isMultiLanguage()) {
			key = key + "_int";
		}

		if (getSearchHit() != null) 
		{
			if( key.equals("_score") )
			{
				return getSearchHit().getScore();
			}
			SearchHitField field = getSearchHit().field(key);
			//Map fields = getSearchHit().getFields();
			if (field != null) {
				value = field.getValue();
			}
		}
		if (value == null && getSearchData() != null) {
			value = getSearchData().get(key);
			
			
			if (value instanceof Map) {
				Map map = (Map)value;
				if(map.isEmpty()){
					value = null;
				}
			}
			
			if(detail != null && detail.isDate() && value instanceof String) {
				return DateStorageUtil.getStorageUtil().parseFromStorage((String) value);
			}
			
			
			if( detail != null && detail.isGeoPoint() && value instanceof Map)
			{
				Position pos = new Position((Map)value);
				value = pos;
			}
		}
		if (value == null) {

			if (detail != null && getSearchData() != null)
			{
				String legacy = detail.get("legacy");
				if (legacy != null) 
				{
					value = getSearchData().get(legacy);
				}
				if (value == null && !inId.equals(key)) {
					value = getSearchData().get(inId); //check without the _int if !inId.equals(key) ?
				}
			}
			if(value ==null){
				if(getSearchData() != null){
					
					//TODO: THis is redundant to above for internationaled fields
					value = getSearchData().get(inId + "_int");
					if(value != null && value instanceof Map){
						LanguageMap map = new LanguageMap((Map) value);
						if(map.keySet().size() == 1){  //TODO: Not needed
							return map.get("en");
						} else{
							return map.toString();
						}
					}
				}
			}
		}

		if (value != null && detail != null && detail.isMultiLanguage()) {
			if (value instanceof Map) 
			{
				LanguageMap map = new LanguageMap((Map) value);
				value = map;
			}
			else if (value instanceof String) 
			{
				LanguageMap map = new LanguageMap();
				map.put("en", value);
				value = map;
			}
		}

		if (detail != null && "name".equals(inId) && !detail.isMultiLanguage() && value instanceof Map) {
			LanguageMap map = new LanguageMap((Map) value);

			value = map.get("en");
		}

		return value;
	}
	public Map getSearchData() {
		if (fieldSearchData == null && getSearchHit() != null) {
			fieldSearchData = getSearchHit().getSource();
		}
		return fieldSearchData;
	}

	public void setSearchData(Map inSearchHit) {
		fieldSearchData = inSearchHit;
	}

	@Override
	public Object get(Object inKey)
	{
		Object val = getValue((String)inKey);
		return val;
	}
	
	public Object getValue(String inId)
	{
		Object val = super.getValue(inId);
		if( val == NULLVALUE)
		{
			return null;
		}
		else if( val != null)
		{
			return val;
		}
		
		val = getFromDb(inId);
		
		return val;
		
	}
	
	
	@Override
	public Collection<String> getValues(String inPreference) {
		Object result =  getValue(inPreference);
		if (result == null) {
			return null;
		}
		if (result instanceof Collection) {
			return new ArrayList((Collection) result);
		}
		if( result instanceof String)
		{
			String inVal = (String)result;
			String[] vals;
			if (inVal.contains("|"))
			{
				vals = MultiValued.VALUEDELMITER.split(inVal);
			}
			else
			{
				vals = new String[] { inVal };
			}
			Collection collection = new ArrayList(Arrays.asList(vals));
			return collection;

		}
		
		ArrayList one = new ArrayList(1);
		one.add(result);
		return one;
	}
	

	public Set keySet() 
	{
		Set set = new HashSet();
		if( getSearchData() != null)
		{
			for (Iterator iterator = getSearchData().keySet().iterator(); iterator.hasNext();) 
			{
				String key = (String)iterator.next();
				if( key.endsWith("_int"))
				{
					key = key.substring(0, key.length() - 4);
				}
				set.add(key);
			}
		}	
		set.addAll( super.keySet() );
		//set.add(".version");
		return set;
	}
	@Override
	public int size()
	{
		int size = super.size();
		size = size + getSearchData().size();
		return size;
	}
	
	@Override
	public boolean isEmpty()
	{
		return false;
	}
	
	 @Override
	 public Set<Map.Entry<K, V>> entrySet() 
	 {
		 List keys = new ArrayList(keySet());
		 
	        return new AbstractSet<>() {

	            @Override
	            public Iterator<Map.Entry<K, V>> iterator() {
	                return new Iterator<>() {
	                    private int index = 0;

	                    @Override
	                    public boolean hasNext() {
	                        return index < keys.size();
	                    }

	                    @Override
	                    public Map.Entry<K, V> next() {
	                        if (!hasNext()) {
	                            throw new NoSuchElementException();
	                        }
	                        return new Entry(keys, index++);
	                    }
	                };
	            }

	            @Override
	            public int size() {
	                return keys.size();
	            }
	        };
	    }

	    /* ---------------- Entry Implementation ---------------- */

	    private class Entry implements Map.Entry<K, V> {
	        private final int index;
	        List keys = null;
	        Entry(List inKeys, int index) {
	            this.index = index;
	            this.keys = inKeys;
	        }

	        @Override
        @SuppressWarnings("unchecked")
        public K getKey() {
            return (K) this.keys.get(index);
        }

        @Override
        @SuppressWarnings("unchecked")
        public V getValue() {
            return (V) ValuesMapWithSearchData.this.getValue((String) keys.get(index));
        }

        @Override
        @SuppressWarnings("unchecked")
        public V setValue(V value) {
            V oldValue = getValue();
            ValuesMapWithSearchData.this.put((K) keys.get(index), value);
            return oldValue;
        }

        @Override
        public int hashCode() {
            return Objects.hash(getKey(), getValue());
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry<?, ?> entry)) return false;
            return Objects.equals(getKey(), entry.getKey())
                && Objects.equals(getValue(), entry.getValue());
        }
    }
	
	public SearchHit getSearchHit()
	{
		return fieldSearchHit;
	}
	public void setSearchHit(SearchHit inSearchHit)
	{
		fieldSearchHit = inSearchHit;
	}
	
	@Override
	public String toString()
	{
		return super.toString();
	}
}
