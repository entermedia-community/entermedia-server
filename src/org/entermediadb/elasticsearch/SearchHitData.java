package org.entermediadb.elasticsearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.data.BaseData;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.SaveableData;
import org.openedit.data.SearchData;
import org.openedit.data.ValuesMap;
import org.openedit.modules.translations.LanguageMap;
import org.openedit.util.DateStorageUtil;

public class SearchHitData extends BaseData implements Data, MultiValued, SaveableData,SearchData {
	protected Map fieldSearchData;
	protected SearchHit fieldSearchHit;
	protected PropertyDetails fieldPropertyDetails;
	private static final Log log = LogFactory.getLog(SearchHitData.class);

	public SearchHitData(SearchHit inHit, PropertyDetails inPropertyDetails) {
		setSearchHit(inHit);
		setPropertyDetails(inPropertyDetails);
	}

	public SearchHitData() {

	}

	public SearchHit getSearchHit() {
		return fieldSearchHit;
	}

	public void setSearchHit(SearchHit inSearchHit) {
		fieldSearchHit = inSearchHit;
		setId(inSearchHit.getId());
		setVersion(inSearchHit.getVersion());

	}

	public Long getVersion() 
	{
		Long l = getMap().getLong(".version");
		return l;
	}

	public void setVersion(long inVersion) 
	{
		setValue(".version", inVersion);
	}

	public PropertyDetails getPropertyDetails() {
		return fieldPropertyDetails;
	}

	public void setPropertyDetails(PropertyDetails inPropertyDetails) {
		fieldPropertyDetails = inPropertyDetails;
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
	public void setProperty(String inId, String inValue) {
		// TODO Auto-generated method stub
		super.setProperty(inId, inValue);
	}

	@Override
	public Collection<String> getValues(String inPreference) {
		Object result =  getValue(inPreference);
		if (result == null) {
			return null;
		}
		if (result instanceof Collection) {
			return (Collection) result;
		}
		ArrayList one = new ArrayList(1);
		one.add(result);
		return one;
	}

	
	

	
	
	@Override
	public Object getValue(String inId) {
		if (inId == null) {
			return null;
		}
		Object svalue = getMap().getObject(inId);
		if( svalue == ValuesMap.NULLVALUE)
		{
			return null;
		}
		if( svalue == null)
		{
			svalue = getFromDb(inId);
		}

		return svalue;
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

		if (getSearchHit() != null) {
			SearchHitField field = getSearchHit().field(key);
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
		set.addAll( getMap().keySet() );
		//set.add(".version");
		return set;
	}

	public ValuesMap getProperties() 
	{
		Set set = keySet();
		ValuesMap all = new ValuesMap();
		for (Iterator iterator = set.iterator(); iterator.hasNext();) 
		{
			String key = (String) iterator.next();
			Object val = getValue(key);
			if( val != null)
			{
				all.put(key, val);
			}
		}
		return all;
	}

	public String toString() 
	{
		String val = getName();
		if (val != null)
		{
			return val;
		} else {
			return getId();
		}
	}
}
