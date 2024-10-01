package org.entermediadb.elasticsearch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.highlight.HighlightField;
import org.entermediadb.data.FullTextLoader;
import org.entermediadb.location.Position;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.data.BaseData;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.SaveableData;
import org.openedit.data.SearchData;
import org.openedit.data.Searcher;
import org.openedit.data.ValuesMap;
import org.openedit.modules.translations.LanguageMap;
import org.openedit.util.DateStorageUtil;

public class SearchHitData extends BaseData implements Data, MultiValued, SaveableData,SearchData 
{
	private static final Log log = LogFactory.getLog(SearchHitData.class);

	protected Map fieldSearchData;
	protected SearchHit fieldSearchHit;
	protected PropertyDetails fieldPropertyDetails;
	protected Searcher fieldSearcher;

	public SearchHitData()
	{
	}
	public SearchHitData(SearchHit inHit, Searcher inSearcher) {
		setSearchHit(inHit);
		setSearcher(inSearcher);
	}

	public SearchHitData(Searcher inSearcher)
	{
		setSearcher(inSearcher);
	}
	public Searcher getSearcher()
	{
		return fieldSearcher;
	}

	public void setSearcher(Searcher inSearcher)
	{
		fieldSearcher = (Searcher)inSearcher;
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

	public PropertyDetails getPropertyDetails() 
	{
		if( fieldPropertyDetails == null && fieldSearcher != null)
		{
			return fieldSearcher.getPropertyDetails();
		}
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
			Collection collection = Arrays.asList(vals);
			return collection;

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

		if ("fulltext".equals(inId))
		{
			if (getSearcher() != null)
			{
				if( getSearchHit() == null)
				{
					log.info("Missing search hit");
					return ((FullTextLoader)getSearcher()).getFulltext(this);
				}
				else
				{
					return ((FullTextLoader)getSearcher()).getFulltext(this,getSearchHit().getType());
				}
			}
			else
			{
				log.info("Missing searcher");
			}
		}

		Object svalue = getMap().getObject(inId);
		if( svalue == ValuesMap.NULLVALUE || svalue == ValuesMap.NULLSTRING)
		{
			return null;
		}
		if( svalue == null)
		{
			svalue = getFromDb(inId);
		}
//		if(svalue == null){
//			svalue = 
//		}
//		
		
		
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
			Map fields = getSearchHit().getFields();
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
	
	public String toJsonString()
	{
		StringBuffer output = new StringBuffer();
		output.append("{ \"_id\": \"" + getId() + "\",");
		output.append(" \"_source\" :");
		output.append(getSearchHit().getSourceAsString());
		output.append(" \n}");
		return output.toString();
	}
	
	
	public List getHighlights(String inField) {
		ArrayList highlights = new ArrayList();

		if(getSearchHit() == null) {
			return highlights;
		}
		Map<String, HighlightField> highlightFields = getSearchHit().getHighlightFields();
		HighlightField field = highlightFields.get(inField);
		if(field == null) {
			return highlights;
		}
		Text[] fragments = field.getFragments();
		for (Text text : fragments)
		{
			String frag = text.string();
			highlights.add(frag);
		}
		return highlights;
	}
	
	public Map getEmRecordStatus()
	{
		return (Map)getFromDb("emrecordstatus");
	}
	
}
