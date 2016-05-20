package org.entermediadb.elasticsearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.data.BaseData;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.SaveableData;
import org.openedit.modules.translations.LanguageMap;

public class SearchHitData extends BaseData implements Data, MultiValued, SaveableData
{
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

	public SearchHitData(SearchHit inHit, PropertyDetails inPropertyDetails)
	{
		setSearchHit(inHit);
		setId(getSearchHit().getId());
		setPropertyDetails(inPropertyDetails);
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
	public void setProperty(String inId, String inValue)
	{
		// TODO Auto-generated method stub
		super.setProperty(inId, inValue);
	}
	@Override
	public Collection<String> getValues(String inPreference)
	{
		Object result = getValue(inPreference);
		if( result == null)
		{
			return null;
		}
		if( result instanceof Collection)
		{
			return (Collection)result;
		}
		ArrayList one = new ArrayList(1);
		one.add( result);
		return one;
	}
	@Override
	public Object getValue(String inId)
	{
		if(inId == null){
			return null;
		}
		Object svalue = super.getValue(inId);
		if (svalue != null)
		{
			return svalue;
		}
		svalue = getFromDb(inId);

		return svalue;
	}
	
	protected Object getFromDb(String inId)
	{
		if (inId.equals(".version"))
		{
			if (getSearchHit().getVersion() > -1)
			{
				return String.valueOf(getSearchHit().getVersion());
			}
			return null;
		}
		Object value = null;
		SearchHitField field = getSearchHit().field(inId);
		if (field != null)
		{
			value = field.getValue();
		}
		if( value == null)
		{
			value = getSearchHit().getSource().get(inId);
		}
		if( value == null)
		{
			PropertyDetail detail = getPropertyDetails().getDetail(inId);
			if( detail != null)
			{
				String legacy = detail.get("legacy");
				if( legacy != null)
				{
					value = getValue(legacy);
				}
			}
		}
		else{
			if(value instanceof Map){
				PropertyDetail detail = getPropertyDetails().getDetail(inId);
				if(detail.isMultiLanguage()){
					LanguageMap map = new LanguageMap((Map)value);
					value = map;
					
				}
	
			}
		}
		return value;
	}

	public Iterator keys()
	{
		return getProperties().keySet().iterator();
	}

	public Map getProperties()
	{
		Map all = new HashMap();
		for (Iterator iterator = getSearchHit().getSource().keySet().iterator(); iterator.hasNext();)
		{
			String key = (String) iterator.next();
			String val = get(key);
			all.put(key, val);
		}
		String version = get(".version");
		if (version != null)
		{
			all.put(".version", version);
		}
		if( fieldMap != null)
		{
			all.putAll(super.getProperties());
		}

		return all;
	}

	public String toString()
	{
		if (getName() != null)
		{
			return getName();
		}
		else
		{
			return getId();
		}
	}
}
